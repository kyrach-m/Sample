package com.ch.core.base

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * View 防抖点击扩展
 *
 * 使用 Flow.debounce 实现点击防抖，避免重复点击。
 * 默认防抖间隔 500ms。
 *
 * 用法示例：
 * ```
 * button.setThrottleClick {
 *     // 处理点击事件
 * }
 * ```
 *
 * @param interval 防抖间隔（毫秒），默认 500ms
 * @param action 点击回调
 */
@OptIn(FlowPreview::class)
fun View.setThrottleClick(interval: Long = 500L, action: (View) -> Unit) {
    val clickFlow = MutableSharedFlow<View>(extraBufferCapacity = 64)
    val scope = CoroutineScope(Job() + Dispatchers.Main)

    scope.launch {
        clickFlow
            .debounce(interval)
            .collectLatest { view ->
                action(view)
            }
    }

    setOnClickListener { view ->
        clickFlow.tryEmit(view)
    }
}
