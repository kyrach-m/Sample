package com.ch.sample.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ch.core.common.logger.Logger
import com.ch.core.ui.theme.AppTheme

/**
 * 通用 WebView Activity（Compose 版本）
 *
 * 用于路由降级处理，当目标页面不存在时，
 * 可以跳转到 H5 页面作为降级方案。
 *
 * 设计说明：
 * - 继承 [ComponentActivity]，纯 Compose 技术栈
 * - 使用 [AndroidView] 包裹原生 WebView（WebView 无法用 Compose 原生实现）
 * - 使用 [BackHandler] 处理 WebView 内部返回导航
 * - WebView 生命周期通过 [DisposableEffect] 管理，确保页面销毁时释放资源
 */
class WebViewActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WebViewActivity"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = intent.getStringExtra(EXTRA_URL) ?: "about:blank"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        Logger.d(TAG, "加载 URL: $url")

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var webView by remember { mutableStateOf<WebView?>(null) }

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        Logger.d(TAG, "页面加载完成: $url")
                                    }
                                }
                                loadUrl(url)
                                webView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // WebView 返回键处理：优先 WebView 内部后退
                    BackHandler(enabled = webView?.canGoBack() == true) {
                        webView?.goBack()
                    }

                    // 页面销毁时释放 WebView 资源
                    DisposableEffect(Unit) {
                        onDispose {
                            webView?.destroy()
                        }
                    }
                }
            }
        }
    }
}
