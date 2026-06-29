package com.ch.core.common.util

import android.content.Context
import android.util.TypedValue

/**
 * 密度转换工具类
 *
 * 提供 dp/px/sp 之间的相互转换。
 * 内部使用 [TypedValue.applyDimension] 实现，确保与系统行为一致。
 */
object DensityUtil {

    /**
     * dp 转 px
     *
     * @param context 上下文
     * @param dp dp 值
     * @return 对应的 px 值（四舍五入取整）
     */
    fun dp2px(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * px 转 dp
     *
     * @param context 上下文
     * @param px px 值
     * @return 对应的 dp 值
     */
    fun px2dp(context: Context, px: Float): Float {
        val density = context.resources.displayMetrics.density
        return if (density > 0f) px / density else 0f
    }

    /**
     * sp 转 px
     *
     * 用于字体尺寸转换，会跟随系统字体缩放设置
     *
     * @param context 上下文
     * @param sp sp 值
     * @return 对应的 px 值（四舍五入取整）
     */
    fun sp2px(context: Context, sp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * px 转 sp
     *
     * @param context 上下文
     * @param px px 值
     * @return 对应的 sp 值
     */
    fun px2sp(context: Context, px: Float): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return if (scaledDensity > 0f) px / scaledDensity else 0f
    }
}
