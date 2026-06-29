package com.ch.sample.web

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.ch.core.common.logger.Logger

/**
 * 通用 WebView Activity
 *
 * 用于路由降级处理，当目标页面不存在时，
 * 可以跳转到 H5 页面作为降级方案。
 */
class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WebViewActivity"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: "about:blank"
        val title = intent.getStringExtra(EXTRA_TITLE)

        Logger.d(TAG, "加载 URL: $url")

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Logger.d(TAG, "页面加载完成: $url")
                }
            }
            loadUrl(url)
        }

        setContentView(webView)

        title?.let {
            supportActionBar?.title = it
        }
    }

    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
