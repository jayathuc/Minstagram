package com.jayathu.minstagram.presentation.browser

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jayathu.minstagram.util.JavaScriptInjector

class MinstagramWebViewClient(
    private val onPageStarted: () -> Unit = {},
    private val onPageLoaded: () -> Unit = {},
    private val onPageError: (String) -> Unit = {}
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // Only inject distraction-blocking scripts on authenticated feed pages.
        // Skip auth pages entirely so login/2FA/challenge flows are not interfered with.
        if (!isAuthPage(url)) {
            view?.let {
                it.evaluateJavascript(JavaScriptInjector.HIDE_DISTRACTIONS, null)
                it.evaluateJavascript(JavaScriptInjector.DISABLE_AUTOPLAY, null)
                it.evaluateJavascript(JavaScriptInjector.BLOCK_EXPLORE_NAVIGATION, null)
            }
        }
        onPageLoaded()
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onPageError("Load failed (${error?.errorCode}): ${error?.description}")
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true) {
            val status = errorResponse?.statusCode ?: 0
            if (status >= 400) {
                onPageError("HTTP $status on ${request.url}")
            }
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url ?: return false
        val host = url.host ?: return false

        // Allow non-Instagram domains (needed for auth flows, CDNs, Facebook OAuth, etc.)
        if (!host.contains("instagram.com")) return false

        // Block navigation to Explore and Shop within instagram.com
        val path = url.path ?: return false
        if (path.startsWith("/explore") ||
            path.startsWith("/shop") ||
            path.startsWith("/marketplace")
        ) {
            return true
        }

        return false
    }

    private fun isAuthPage(url: String?): Boolean {
        if (url == null) return true
        return url.contains("/accounts/") ||
            url.contains("accounts.instagram.com") ||
            url.contains("/login") ||
            url.contains("/signup") ||
            url.contains("/challenge/") ||
            url.contains("/two_factor")
    }
}
