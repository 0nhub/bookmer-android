package com.bookmer.browser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bookmer.browser.BookmerServices
import com.bookmer.browser.data.AppSettings
import com.bookmer.browser.data.BrowserTab
import java.io.ByteArrayInputStream

private class HideElementsBridge(private val model: BrowserViewModel) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(json: String?) {
        if (json.isNullOrBlank()) return
        main.post { model.handleHideElementsMessage(json) }
    }
}

/** Native tap → JS pickAt. Android WebView often ignores DOM overlay touch handlers. */
private class HidePickTouchHelper(private val model: BrowserViewModel) : android.view.View.OnTouchListener {
    private var startX = 0f
    private var startY = 0f
    private var moved = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
        if (!model.isHideElementsActive || model.hideIsPreviewing || model.showHideElementsTip) {
            return false
        }
        val web = v as? WebView ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                moved = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!moved && (kotlin.math.abs(event.x - startX) > 18f || kotlin.math.abs(event.y - startY) > 18f)) {
                    moved = true
                }
                return false
            }
            MotionEvent.ACTION_UP -> {
                if (moved) return false
                // View coords ≈ CSS client coords on modern WebView (scale≈1 with viewport).
                @Suppress("DEPRECATION")
                val scale = web.scale.coerceAtLeast(0.01f)
                val cssX = event.x / scale
                val cssY = event.y / scale
                model.ensureHideBootstrapThen {
                    web.evaluateJavascript(HideElementsScript.pickAtJavaScript(cssX, cssY)) { raw ->
                        decodePickResult(raw)?.let(model::handleHideElementsMessage)
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                moved = false
                return false
            }
        }
        return false
    }

    private fun decodePickResult(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        // pickAt returns selectionInfo object — wrap as selection message if needed.
        val decoded = runCatching { org.json.JSONArray("[$raw]").getString(0) }.getOrNull()
            ?: return null
        if (decoded == "null" || decoded.isBlank()) return null
        return runCatching {
            val obj = org.json.JSONObject(decoded)
            if (obj.has("type")) decoded
            else org.json.JSONObject()
                .put("type", "selection")
                .put("hasSelection", obj.optString("selector").isNotBlank())
                .put("label", obj.optString("label"))
                .put("selector", obj.optString("selector"))
                .toString()
        }.getOrNull()
    }
}
interface BrowserPermissionHost {
    fun handleWebPermission(request: PermissionRequest)
    fun handleGeolocation(origin: String, callback: GeolocationPermissions.Callback)
}

interface BrowserFileChooserHost {
    fun openWebFileChooser(callback: android.webkit.ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean
}

/** Reports clamp-at-edge overscroll so Full Screen can reveal the exit pill (iOS pull-past-edge). */
internal class BookmerWebView(context: Context) : WebView(context) {
    var onEdgeOverscroll: ((atTop: Boolean, atBottom: Boolean) -> Unit)? = null

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        if (!clampedY) return
        val max = (computeVerticalScrollRange() - computeVerticalScrollExtent()).coerceAtLeast(0)
        val offset = computeVerticalScrollOffset()
        val atTop = offset <= 0
        val atBottom = max <= 8 || offset >= max - 2
        onEdgeOverscroll?.invoke(atTop, atBottom)
    }
}

@SuppressLint("SetJavaScriptEnabled")
object BrowserWebConfigurator {
    private val trackerHosts = setOf(
        "google-analytics.com", "googletagmanager.com", "doubleclick.net", "facebook.net",
        "segment.io", "mixpanel.com", "hotjar.com", "amplitude.com", "clarity.ms", "scorecardresearch.com"
    )

    fun create(context: Context, tab: BrowserTab, model: BrowserViewModel): WebView = BookmerWebView(context).apply {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        overScrollMode = View.OVER_SCROLL_ALWAYS
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportMultipleWindows(!model.settings.blockPopups)
        settings.javaScriptCanOpenWindowsAutomatically = !model.settings.blockPopups
        CookieManager.getInstance().setAcceptCookie(!model.settings.blockCookies)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, !model.settings.blockCookies)
        applyUserAgent(this, tab, model.settings)
        // Do not force a percentage scale — 0 lets the viewport meta drive mobile layout.
        if (tab.pageZoom != 100) setInitialScale(tab.pageZoom)
        addJavascriptInterface(HideElementsBridge(model), HideElementsScript.BRIDGE_NAME)
        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            model.promptDownload(url, userAgent, contentDisposition, mimeType, contentLength)
        }
        setFindListener { activeMatchOrdinal, numberOfMatches, _ -> model.onFindResult(if (numberOfMatches == 0) 0 else activeMatchOrdinal + 1) }
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { model.onPageStarted(tab.id, url) }
            override fun onPageFinished(view: WebView, url: String) { model.onPageFinished(tab.id, url, view.title.orEmpty()) }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme
                val host = request.url.host?.lowercase().orEmpty()
                val blocked = model.settings.blockedSites.firstOrNull { host == it.host || host.endsWith(".${it.host}") }
                if (blocked != null) {
                    blocked.redirectUrl?.let(view::loadUrl) ?: model.presentBlockedPage(request.url.toString())
                    return true
                }
                if (model.settings.blockAppBanners && !request.hasGesture() && (scheme == "market" || host == "play.google.com" || host == "apps.apple.com")) return true
                if (scheme == "http" || scheme == "https") return false
                return runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)); true }.getOrDefault(true)
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!model.settings.blockTrackers) return null
                val host = request.url.host?.lowercase().orEmpty()
                if (trackerHosts.any { host == it || host.endsWith(".$it") }) {
                    return WebResourceResponse("text/plain", "utf-8", 204, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                }
                return null
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) = model.onProgress(tab.id, newProgress)
            override fun onJsAlert(view: WebView, url: String, message: String, result: android.webkit.JsResult): Boolean {
                // Swallow leftover debug alerts from older Hide Element stubs so they can't loop-block the UI.
                if (message.contains("Tap an element", ignoreCase = true) &&
                    message.contains("hide", ignoreCase = true)
                ) {
                    result.confirm()
                    return true
                }
                return super.onJsAlert(view, url, message, result)
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                (context as? BrowserPermissionHost)?.handleWebPermission(request) ?: request.deny()
            }
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                (context as? BrowserPermissionHost)?.handleGeolocation(origin, callback) ?: callback.invoke(origin, false, false)
            }
            override fun onShowFileChooser(webView: WebView, filePathCallback: android.webkit.ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean =
                (context as? BrowserFileChooserHost)?.openWebFileChooser(filePathCallback, fileChooserParams) ?: false
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                if (model.settings.blockPopups && !isUserGesture) return false
                val target = WebView(context)
                target.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        model.load(request.url.toString(), inNewTab = true); view.destroy(); return true
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = target
                resultMsg.sendToTarget()
                return true
            }
        }
        setOnScrollChangeListener { view, _, scrollY, _, oldScrollY ->
            if (tab.id != model.selectedTabId) return@setOnScrollChangeListener
            model.onPageScrolled(view as WebView, scrollY, oldScrollY)
        }
        onEdgeOverscroll = { atTop, atBottom -> model.onImmersiveOverscroll(this, atTop, atBottom) }
    }

    fun applyUserAgent(webView: WebView, tab: BrowserTab, settings: AppSettings) {
        val alias = BookmerServices.alias
        val desktop = alias.effectiveDesktop(tab.prefersDesktopWebsite)
        webView.settings.userAgentString = alias.userAgent(webView.context, desktop)
        // Always honor the page viewport meta; overview mode is for desktop zoom-out only.
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = desktop
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        if (tab.pageZoom == 100) {
            webView.setInitialScale(0)
        } else {
            webView.setInitialScale(tab.pageZoom)
        }
    }

    fun injectPageRules(webView: WebView, settings: AppSettings, tabPrefersDesktop: Boolean = false) {
        val scripts = buildString {
            append("document.querySelectorAll('meta[name=apple-itunes-app]').forEach(e=>e.remove());")
            if (settings.blockAppBanners) {
                append("document.querySelectorAll('[class*=app-banner],[id*=app-banner],[class*=smart-banner]').forEach(e=>e.remove());")
            }
            if (settings.blockYouTubeAds && webView.url?.contains("youtube.com") == true) {
                append("document.querySelectorAll('.ytp-ad-overlay-container,.video-ads').forEach(e=>e.remove());document.querySelector('.ytp-ad-skip-button-modern,.ytp-ad-skip-button')?.click();")
            }
        }
        if (scripts.isNotEmpty()) webView.evaluateJavascript("(()=>{$scripts})()", null)
        if (!BookmerServices.alias.isOff) {
            webView.evaluateJavascript(BookmerServices.alias.injectionScript(tabPrefersDesktop), null)
        }
    }
}

@Composable
fun BrowserWebView(model: BrowserViewModel, modifier: Modifier = Modifier) {
    val tab = model.currentTab
    val hideActive = model.isHideElementsActive
    val hideTouch = androidx.compose.runtime.remember(tab.id) { HidePickTouchHelper(model) }
    key(tab.id) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val web = model.webViews[tab.id] ?: BrowserWebConfigurator.create(context, tab, model).also { model.registerWebView(tab.id, it) }
                (web.parent as? ViewGroup)?.removeView(web)
                web.setOnTouchListener(hideTouch)
                (web as? BookmerWebView)?.let { bw ->
                    bw.overScrollMode = android.view.View.OVER_SCROLL_ALWAYS
                    bw.onEdgeOverscroll = { atTop, atBottom -> model.onImmersiveOverscroll(bw, atTop, atBottom) }
                }
                web
            },
            update = { web ->
                BrowserWebConfigurator.applyUserAgent(web, model.currentTab, model.settings)
                if (!model.currentTab.isBookmerHome && web.url == null) model.currentTab.url?.let(web::loadUrl)
                web.setOnTouchListener(hideTouch)
                if (hideActive) {
                    web.evaluateJavascript(HideElementsScript.ENSURE_PICKING, null)
                }
            }
        )
        DisposableEffect(tab.id) { onDispose { /* WebView intentionally retained with its tab. */ } }
    }
}
