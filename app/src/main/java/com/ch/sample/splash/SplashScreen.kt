package com.ch.sample.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 闪屏页（Compose 版本）
 *
 * 冷启动入口页面，展示 App Logo 和品牌信息。
 * 实际启动任务由 SplashActivity 中的 StartupScheduler 处理。
 *
 * 功能特性：
 * - Logo 渐入动画
 * - 品牌文字展示
 * - 统一的主题色背景
 *
 * 用法示例：
 * ```kotlin
 * SplashScreen()
 * ```
 */
@Composable
fun SplashScreen() {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sample",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
            )
        }
    }
}
