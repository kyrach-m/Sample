package com.ch.sample.startup

import android.content.Context
import com.ch.core.common.logger.Logger
import com.ch.core.storage.db.AppDatabase
import com.ch.service.startup.dag.StartupTask

class DatabaseWarmUpTask : StartupTask() {

    override val name: String = "DatabaseWarmUpTask"

    override fun priority(): Int = 100

    override fun execute(context: Context) {
        val startTime = System.currentTimeMillis()
        AppDatabase.warmUp(context)
        val duration = System.currentTimeMillis() - startTime
        Logger.d(TAG, "数据库预热完成，耗时: ${duration}ms")
    }

    companion object {
        private const val TAG = "DatabaseWarmUpTask"
    }
}
