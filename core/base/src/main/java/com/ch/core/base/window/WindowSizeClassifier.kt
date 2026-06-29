package com.ch.core.base.window

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 屏幕尺寸分类器（响应式布局工具）
 *
 * 基于 AndroidX WindowManager 库，将屏幕宽度分为三个等级，
 * 适配手机、折叠屏、平板等不同尺寸设备。
 *
 * 尺寸分级（参考 Material Design 响应式布局规范）：
 * - [WindowSizeClass.COMPACT]：紧凑型，宽度 < 600dp（手机竖屏）
 * - [WindowSizeClass.MEDIUM]：中型，600dp <= 宽度 < 840dp（平板竖屏、折叠屏展开）
 * - [WindowSizeClass.EXPANDED]：展开型，宽度 >= 840dp（平板横屏、桌面模式）
 *
 * 使用场景：
 * - 根据屏幕尺寸选择不同的布局（单列/双列/三列）
 * - 折叠屏展开/折叠时自动调整界面
 * - 大屏设备上优化内容展示
 *
 * 用法示例：
 * ```kotlin
 * class MyActivity : BaseActivity<ActivityMyBinding, ...>() {
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         lifecycleScope.launch {
 *             repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                 currentScreenType.collect { sizeClass ->
 *                     when (sizeClass) {
 *                         WindowSizeClass.COMPACT -> setupSingleColumn()
 *                         WindowSizeClass.MEDIUM -> setupTwoColumns()
 *                         WindowSizeClass.EXPANDED -> setupThreeColumns()
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes">支持不同屏幕尺寸</a>
 */
object WindowSizeClassifier {

    /**
     * 窗口尺寸等级
     *
     * @property COMPACT 紧凑型（手机）
     * @property MEDIUM 中型（折叠屏展开、平板竖屏）
     * @property EXPANDED 展开型（平板横屏、桌面模式）
     */
    enum class WindowSizeClass {
        COMPACT,
        MEDIUM,
        EXPANDED
    }

    /** 中型屏幕最小宽度 (dp) */
    private const val MEDIUM_WIDTH_DP = 600

    /** 展开型屏幕最小宽度 (dp) */
    private const val EXPANDED_WIDTH_DP = 840

    /**
     * 根据当前窗口宽度计算尺寸等级
     *
     * @param activity Activity
     * @return 窗口尺寸等级
     */
    fun getWindowSizeClass(activity: Activity): WindowSizeClass {
        val windowMetrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(activity)
        val widthDp = windowMetrics.bounds.width() / activity.resources.displayMetrics.density
        return when {
            widthDp >= EXPANDED_WIDTH_DP -> WindowSizeClass.EXPANDED
            widthDp >= MEDIUM_WIDTH_DP -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.COMPACT
        }
    }

    /**
     * 创建窗口尺寸变化的 StateFlow
     *
     * 监听窗口尺寸变化（如旋转、折叠屏展开/折叠）时自动更新。
     * 在 Activity 的 onCreate 中创建，随生命周期自动管理。
     *
     * @param activity Activity
     * @param lifecycleOwner LifecycleOwner（通常是 Activity 自身）
     * @return 当前屏幕类型的 StateFlow
     */
    fun createWindowSizeFlow(
        activity: Activity,
        lifecycleOwner: LifecycleOwner
    ): StateFlow<WindowSizeClass> {
        val initialSize = getWindowSizeClass(activity)
        val stateFlow = MutableStateFlow(initialSize)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val currentSize = getWindowSizeClass(activity)
                if (stateFlow.value != currentSize) {
                    stateFlow.value = currentSize
                }
            }
        }

        return stateFlow.asStateFlow()
    }

    /**
     * 判断当前是否为大屏设备（宽度 >= 600dp）
     *
     * @param activity Activity
     * @return true=大屏（平板/折叠屏展开），false=小屏（手机）
     */
    fun isLargeScreen(activity: Activity): Boolean {
        return getWindowSizeClass(activity) != WindowSizeClass.COMPACT
    }

    /**
     * 判断当前是否为平板级大屏（宽度 >= 840dp）
     *
     * @param activity Activity
     * @return true=超大屏（平板横屏/桌面模式）
     */
    fun isExpandedScreen(activity: Activity): Boolean {
        return getWindowSizeClass(activity) == WindowSizeClass.EXPANDED
    }
}
