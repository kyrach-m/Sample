package com.ch.sample.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.ch.core.common.logger.Logger
import com.ch.middleware.router.annotation.Route
import com.ch.middleware.router.RouterPath

/**
 * 通用 WebView Activity
 *
 * 用于加载 H5 页面，作为路由降级处理的备用页面。
 * 当路由找不到目标页面时，会跳转到此页面加载 H5 404 页面。
 *
 * @param url H5 页面地址（通过 intent params 传入，key 为 "url"）
 * @param title 页面标题（可选，通过 intent params 传入，key 为 "title"）
 */
@SuppressLint("SetJavaScriptEnabled")
@Route(path = RouterPath.Web.WEB_VIEW)
class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WebViewActivity"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            Logger.e(TAG, "WebViewActivity 启动失败：缺少 url 参数")
            finish()
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        webView = WebView(this).also {
            it.layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(webView)

        setupWebView(url, title)
    }

    private fun setupWebView(url: String, title: String) {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            Logger.d(TAG, "加载 WebView: $url")
            loadUrl(url)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
