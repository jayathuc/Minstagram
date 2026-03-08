package com.jayathu.minstagram.presentation.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.util.JavaScriptInjector

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    intention: SessionIntention,
    onSessionEnd: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Collected separately so only SessionBanner recomposes every second,
    // not the entire screen including the WebView container.
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        SessionExitDialog(
            intention = intention,
            elapsedSeconds = elapsedSeconds,
            onStay = { showExitDialog = false },
            onFinish = onSessionEnd
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SessionBanner(
            intention = intention,
            elapsedSeconds = elapsedSeconds,
            onExit = { showExitDialog = true }
        )

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        val errorMessage = uiState.errorMessage
        if (errorMessage != null) {
            ErrorView(
                message = errorMessage,
                onRetry = {
                    viewModel.clearError()
                    webViewRef?.reload()
                }
            )
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        val mainWebView = this
                        // WebView defaults to WRAP_CONTENT; without MATCH_PARENT it collapses
                        // to zero height in Compose's AndroidView, showing black behind it.
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Enable cookies including across subdomains (required for
                        // www.instagram.com <-> accounts.instagram.com auth redirects)
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(mainWebView, true)
                        }

                        webViewClient = MinstagramWebViewClient(
                            onPageStarted = { viewModel.setLoading(true) },
                            onPageLoaded = { viewModel.setLoading(false) },
                            onPageError = { viewModel.setError(it) }
                        )
                        webChromeClient = object : WebChromeClient() {
                            // Handle window.open() calls (e.g. Instagram login popup)
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val popupWebView = WebView(context)
                                popupWebView.settings.javaScriptEnabled = true
                                // Redirect any URL the popup navigates to into the main WebView
                                popupWebView.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        mainWebView.loadUrl(url)
                                        return true
                                    }
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        url?.let { mainWebView.loadUrl(it) }
                                    }
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = popupWebView
                                resultMsg?.sendToTarget()
                                return true
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            userAgentString = JavaScriptInjector.MOBILE_USER_AGENT
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportMultipleWindows(true)
                            // Keep default (true): videos require a tap to play.
                            // Setting this to false caused all feed videos to autoplay
                            // simultaneously while scrolling, saturating the hardware decoder.
                        }

                        // Prevent black screen on dark-mode devices:
                        // 1. Force white background so transparent WebView doesn't show dark theme behind it.
                        setBackgroundColor(android.graphics.Color.WHITE)
                        // 2. On API 29-32, disable forced darkening so WebView doesn't invert Instagram's styling.
                        if (Build.VERSION.SDK_INT in 29..32) {
                            @Suppress("DEPRECATION")
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
                            }
                        }
                        // Load the login page directly — skips the app-install interstitial.
                        // Instagram auto-redirects to the feed if already logged in.
                        loadUrl("https://www.instagram.com/accounts/login/")
                        webViewRef = this
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SessionBanner(
    intention: SessionIntention,
    elapsedSeconds: Int,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = intention.emoji,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = intention.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "\u23F1 ${formatDuration(elapsedSeconds)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onExit,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "End session",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionExitDialog(
    intention: SessionIntention,
    elapsedSeconds: Int,
    onStay: () -> Unit,
    onFinish: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onStay,
        title = {
            Text(text = "End Session?")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your intention: ${intention.emoji} ${intention.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Text(
                    text = "\u23F1 Time spent: ${formatDuration(elapsedSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(onClick = onFinish) {
                Text("Finish Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onStay) {
                Text("Stay")
            }
        }
    )
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "Could not load Instagram",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}
