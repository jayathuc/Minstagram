package com.jayathu.minstagram.presentation.browser

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

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

}
