package com.gondroid.quoteanime.presentation.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.gondroid.quoteanime.R

/**
 * In-app browser for the app's own web pages (terms, privacy). Keeping them here instead of
 * firing an `ACTION_VIEW` means the user never leaves the task: back returns to wherever they
 * came from — the paywall mid-purchase, say — instead of to a browser's tab stack.
 *
 * Non-http schemes (mailto:, tel:, market:) still hand off to the system, since a WebView
 * can't render them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    title: String,
    onNavigateBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // In-page history first: the system back gesture should walk the site back before it
    // pops this screen off the app's back stack.
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (canGoBack) webView?.goBack() else onNavigateBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView, pageUrl: String?) {
                                isLoading = false
                                canGoBack = view.canGoBack()
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: android.webkit.WebResourceRequest
                            ): Boolean {
                                val target = request.url
                                if (target.scheme == "http" || target.scheme == "https") return false
                                return runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, target))
                                    true
                                }.getOrElse { true }
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(url)
                        webView = this
                    }
                }
            )
        }
    }
}

/**
 * Opens [url] in whichever installed app claims it (Instagram, Facebook…), falling back to the
 * in-app WebView. The point is that a link never dumps the user into an external browser: either
 * the real app handles it, or we render it ourselves.
 */
fun openExternalLink(
    context: android.content.Context,
    url: String,
    openInApp: () -> Unit
) {
    // Before API 30 there's no way to say "resolve this only if a non-browser app claims it",
    // and guessing package names per network isn't worth it — render it in-app instead.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        openInApp()
        return
    }
    val nativeApp = Intent(Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
    try {
        context.startActivity(nativeApp)
    } catch (_: ActivityNotFoundException) {
        openInApp()
    }
}
