package com.ch.sample.home

import com.ch.core.base.event.ViewEvent

/**
 * Dashboard 页面事件
 *
 * 定义 Dashboard 页面中 ViewModel 向 UI 层发送的一次性事件。
 */
sealed class DashboardEvent : ViewEvent {

    /**
     * 追加日志事件
     *
     * @property message 日志消息内容
     */
    data class AppendLog(val message: String) : DashboardEvent()

    /**
     * 显示消息事件（Toast/Snackbar）
     *
     * @property message 消息内容
     */
    data class ShowMessage(val message: String) : DashboardEvent()
}
