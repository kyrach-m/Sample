package com.ch.sample.startup

import android.content.Context
import com.ch.core.common.logger.Logger
import com.ch.service.startup.dag.StartupTask

/**
 * 配置预加载任务
 *
 * 启动时预加载远程配置、AB 实验配置等。
 * 框架脚手架版本，实际项目中需实现具体逻辑。
 */
class ConfigPreloadTask : StartupTask() {

    override val name: String = "ConfigPreloadTask"

    override fun execute(context: Context) {
        Logger.d(TAG, "配置预加载任务执行（框架脚手架版本）")
        // TODO: 实际项目中实现配置预加载逻辑
    }

    companion object {
        private const val TAG = "ConfigPreloadTask"
    }
}
