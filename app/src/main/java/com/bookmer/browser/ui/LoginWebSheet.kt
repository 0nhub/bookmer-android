package com.bookmer.browser.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bookmer.browser.data.BookmerUrls
import java.net.URLDecoder

/**
 * iOS [BookmerLoginWebSheet]: open bookmer.com/login, wait for `_act` cookie / localStorage token.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebSheet(
    onAuthenticated: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Rounded.Close, "Close")
                }
                Text(
                    "Bookmer",
                    Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (loading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress / 100f
                                    loading = newProgress < 100
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val scheme = request.url.scheme?.lowercase().orEmpty()
                                    return scheme.isNotEmpty() && scheme !in setOf("http", "https", "about")
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    loading = false
                                    if (!finished) tryExtractToken(view) { token ->
                                        if (token != null && !finished) {
                                            finished = true
                                            onAuthenticated(token)
                                        }
                                    }
                                }
                            }
                            loadUrl(BookmerUrls.LOGIN)
                        }
                    },
                    update = { web ->
                        if (!finished) {
                            tryExtractToken(web) { token ->
                                if (token != null && !finished) {
                                    finished = true
                                    onAuthenticated(token)
                                }
                            }
                        }
                    },
                )
                if (loading && progress < 0.15f) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            error?.let {
                Text(
                    it,
                    Modifier.fillMaxWidth().padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val poll = object : Runnable {
            override fun run() {
                if (finished) return
                val token = readActCookie()
                if (!token.isNullOrBlank()) {
                    finished = true
                    onAuthenticated(token)
                    return
                }
                handler.postDelayed(this, 400)
            }
        }
        handler.postDelayed(poll, 400)
        onDispose { handler.removeCallbacksAndMessages(null) }
    }
}

private fun tryExtractToken(webView: WebView, done: (String?) -> Unit) {
    val js = """
        (function() {
          var match = document.cookie.match(/(?:^|;\\s*)_act=([^;]+)/);
          if (match && match[1]) return decodeURIComponent(match[1]);
          try {
            var ls = localStorage.getItem('_act') || localStorage.getItem('accessToken') || '';
            if (ls) return ls;
          } catch (e) {}
          return '';
        })();
    """.trimIndent()
    webView.evaluateJavascript(js) { raw ->
        val cleaned = raw?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != "null" }
        if (!cleaned.isNullOrBlank()) {
            done(URLDecoder.decode(cleaned, Charsets.UTF_8.name()))
        } else {
            done(readActCookie())
        }
    }
}

private fun readActCookie(): String? {
    val manager = CookieManager.getInstance()
    val domains = listOf(
        "https://www.bookmer.com",
        "https://bookmer.com",
        "https://id.bookmer.com",
        "https://api.bookmer.com",
    )
    for (domain in domains) {
        val cookie = manager.getCookie(domain) ?: continue
        cookie.split(';').forEach { part ->
            val trimmed = part.trim()
            if (trimmed.startsWith("_act=")) {
                val value = trimmed.removePrefix("_act=").trim()
                if (value.isNotEmpty()) {
                    return runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
                }
            }
        }
    }
    return null
}

fun ensureWebAccessCookie(token: String) {
    if (token.isBlank()) return
    val manager = CookieManager.getInstance()
    val maxAge = 60 * 60 * 24 * 28
    listOf(
        "https://www.bookmer.com" to ".bookmer.com",
        "https://bookmer.com" to ".bookmer.com",
        "https://id.bookmer.com" to ".bookmer.com",
        "https://api.bookmer.com" to ".bookmer.com",
    ).forEach { (url, domain) ->
        manager.setCookie(url, "_act=$token; Domain=$domain; Path=/; Secure; Max-Age=$maxAge")
    }
    manager.flush()
}
