package com.ch.service.logger.worker

import android.content.Context
import com.ch.core.common.logger.Logger
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.WorkerFactory
import androidx.work.ListenableWorker
import com.ch.service.logger.db.AnalyticsRepository
import com.ch.service.logger.db.PendingEventEntity
import java.util.concurrent.TimeUnit

/**
 * 埋点事件上传接口
 *
 * 业务层实现此接口，将事件发送到后端 API。
 * 通过 [AnalyticsUploadWorker.setUploader] 注入。
 */
fun interface EventUploader {
    /**
     * 批量上传事件
     *
     * @param events 待上传的事件列表
     * @return true=上传成功，false=上传失败
     */
    suspend fun upload(events: List<com.ch.service.logger.db.PendingEventEntity>): Boolean
}

/**
 * 埋点事件上传 Worker
 *
 * 使用 WorkManager 实现后台批量上传埋点事件。
 * 监听网络连接状态，仅在联网时执行上传。
 *
 * 核心特性：
 * - 定期执行（每 5 分钟一次）
 * - 需要网络连接（NetworkType.CONNECTED）
 * - 批量上传，成功后删除本地记录
 * - 失败时 retryCount++，最多重试 3 次
 * - 超过重试上限的事件自动清理
 *
 * 使用示例：
 * ```kotlin
 * // 在 Application.onCreate 中初始化
 * AnalyticsUploadWorker.enqueue(context)
 * ```
 *
 * 上传流程：
 * 1. 从本地数据库读取待上报事件（最多 50 条）
 * 2. 批量发送到服务端（需业务层实现 [uploadEvents]）
 * 3. 成功：删除本地记录
 * 4. 失败：每条记录 retryCount++，超过 3 次的自动删除
 */
class AnalyticsUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AnalyticsUploadWorker"

        /**
         * 定期任务唯一标识
         */
        private const val WORK_NAME = "analytics_upload_work"

        /**
         * 定期执行间隔（分钟）
         */
        private const val REPEAT_INTERVAL_MINUTES = 15L

        /**
         * 每批上传最大事件数
         */
        private const val BATCH_SIZE = 50

        /**
         * 事件上传器（由业务层注入）
         */
        @Volatile
        private var uploader: EventUploader? = null

        /**
         * 设置事件上传器
         *
         * 应在 [enqueue] 之前调用。
         *
         * @param eventUploader 上传器实现
         */
        fun setUploader(eventUploader: EventUploader?) {
            uploader = eventUploader
            Logger.d(TAG, "事件上传器已${if (eventUploader != null) "注入" else "注销"}")
        }

        /**
         * 注册定期上传任务
         *
         * 在 Application.onCreate 中调用。
         * 使用 ExistingPeriodicWorkPolicy.KEEP 避免重复注册。
         *
         * @param context Application Context
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AnalyticsUploadWorker>(
                REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Logger.d(TAG, "埋点上传定期任务已注册（间隔 ${REPEAT_INTERVAL_MINUTES} 分钟）")
        }
    }

    /**
     * 埋点仓库
     */
    private val repository = AnalyticsRepository.getInstance(context)

    /**
     * 工作执行入口
     *
     * 执行批量上传逻辑：
     * 1. 读取待上报事件
     * 2. 调用上传接口
     * 3. 成功删除 / 失败重试
     */
    override suspend fun doWork(): Result {
        Logger.d(TAG, "开始执行埋点上传任务...")

        try {
            // 1. 清理超过重试上限的事件
            repository.cleanExceededEvents()

            // 2. 读取待上报事件
            val events = repository.getPendingEvents(BATCH_SIZE)
            if (events.isEmpty()) {
                Logger.d(TAG, "没有待上报的事件")
                return Result.success()
            }

            Logger.d(TAG, "读取到 ${events.size} 条待上报事件")

            // 3. 批量上传
            val eventUploader = uploader
            if (eventUploader == null) {
                Logger.w(TAG, "事件上传器未注入，跳过上传")
                return Result.success()
            }

            val success = eventUploader.upload(events)

            return if (success) {
                // 上传成功，删除本地记录
                val ids = events.map { it.id }
                repository.deleteEvents(ids)
                Logger.d(TAG, "上传成功，已删除 ${ids.size} 条本地记录")
                Result.success()
            } else {
                // 上传失败，增加重试次数
                events.forEach { event ->
                    repository.incrementRetryCount(event.id)
                }
                Logger.w(TAG, "上传失败，${events.size} 条事件 retryCount++")
                Result.retry()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "埋点上传任务异常", e)
            return Result.retry()
        }
    }
}
