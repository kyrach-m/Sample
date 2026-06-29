package com.ch.sample.startup

import android.content.Context
import com.ch.core.common.logger.Logger
import com.ch.service.startup.dag.StartupTask

/**
 * 数据库预热任务
 *
 * 启动时预初始化数据库连接、预热缓存数据等。
 * 框架脚手架版本，实际项目中需实现具体逻辑。
 */
class DatabaseWarmUpTask : StartupTask() {

    override val name: String = "DatabaseWarmUpTask"

    override fun execute(context: Context) {
        Logger.d(TAG, "数据库预热任务执行（框架脚手架版本）")
        // TODO: 实际项目中实现数据库预热逻辑
    }

    companion object {
        private const val TAG = "DatabaseWarmUpTask"
    }
}
