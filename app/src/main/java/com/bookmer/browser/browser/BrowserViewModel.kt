package com.bookmer.browser.browser

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.bookmer.browser.BookmerServices
import com.bookmer.browser.data.AppSettings
import com.bookmer.browser.data.BookmerItem
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.BrowserTab
import com.bookmer.browser.data.HideElementConfirmDraft
import com.bookmer.browser.data.HiddenElementScope
import com.bookmer.browser.data.HideElementsTipStore
import com.bookmer.browser.data.ImmersiveTipStore
import com.bookmer.browser.data.ItemKind
import com.bookmer.browser.data.SearchEngine
import com.bookmer.browser.data.SitePermissionStore
import com.bookmer.browser.data.TabPreviewStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

enum class Overlay { NONE, TABS, SETTINGS, HISTORY, TAB_HISTORY, DOWNLOADS, NAVIGATE, BOOKMARK_TOOLS }

/** Full-screen sheets that should not fight the UI thread with WebView progress / chrome updates. */
private fun Overlay.occludesBrowsingChrome(): Boolean = when (this) {
    Overlay.NONE, Overlay.TABS -> false
    else -> true
}

data class PendingCollect(val url: String, val title: String)
data class PendingDownload(val url: String, val userAgent: String?, val contentDisposition: String?, val mimeType: String?, val size: Long) {
    val filename: String get() = URLUtil.guessFileName(url, contentDisposition, mimeType)
}

data class DownloadEntry(
    val id: Long,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
)
data class ReaderContent(val title: String, val text: String, val url: String)
data class ConnectionDetails(val host: String, val isSecure: Boolean)

/** One row in Tab History — a page from this tab's WebView back-forward list only. */
data class TabHistoryEntry(
    val index: Int,
    val title: String,
    val url: String,
    val isCurrent: Boolean,
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    val bookmarks = BookmerServices.bookmarks
    val history = BookmerServices.history
    val preferences = BookmerServices.preferences
    val session = BookmerServices.session
    val api = BookmerServices.api
    val sync = BookmerServices.sync
    val sitePermissions = BookmerServices.sitePermissions
    val hiddenElements = BookmerServices.hiddenElements
    val alias = BookmerServices.alias
    val pro = BookmerServices.pro
    val tabs = mutableStateListOf<BrowserTab>()
    val webViews = mutableStateMapOf<String, WebView>()
    val downloads = mutableStateListOf<DownloadEntry>()
    /** Live page snapshots for the Arc-style tab deck (not persisted). */
    val tabPreviews = mutableStateMapOf<String, android.graphics.Bitmap>()
    /** Software-draw of the live Collection layer (set only while CollectionScreen is composed). */
    var collectionSnapshotProvider: (() -> Bitmap?)? = null
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshTasks = mutableMapOf<String, Runnable>()
    /** Original page URL while Google Translate is showing the free web view. */
    private val translateOriginalUrls = mutableMapOf<String, String>()

    var selectedTabId by mutableStateOf("")
        private set
    var overlay by mutableStateOf(Overlay.NONE)
    var bookmarkToolsKind by mutableStateOf("broken")
    var addressText by mutableStateOf("")
    var isEditingAddress by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var pageProgress by mutableStateOf(0)
    /** Shown while Google Translate navigation is in flight (iOS parity). */
    var isTranslating by mutableStateOf(false)
        private set
    private var translateFeedbackStartedAt = 0L
    private val endTranslateFeedback = Runnable { isTranslating = false }
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isImmersive by mutableStateOf(false)
    var isNearPageTop by mutableStateOf(false)
    var isNearPageBottom by mutableStateOf(false)
    var showImmersiveTip by mutableStateOf(false)
    private var immersiveTopExitLatched = false
    private var immersiveBottomExitLatched = false
    var isFindOnPage by mutableStateOf(false)
    var findQuery by mutableStateOf("")
    var findMatches by mutableStateOf(0)
    var toolbarCollapsed by mutableStateOf(false)
    /** Swipe-down on the address chrome — stays minimized until the thin strip is tapped. */
    var toolbarStickyCollapsed by mutableStateOf(false)
        private set
    var pendingCollect by mutableStateOf<PendingCollect?>(null)
    var pendingDownload by mutableStateOf<PendingDownload?>(null)
    var blockedPageUrl by mutableStateOf<String?>(null)
    var toastMessage by mutableStateOf<String?>(null)
    var readerContent by mutableStateOf<ReaderContent?>(null)
    var readerLoading by mutableStateOf(false)
    var authInProgress by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)
    var sharingFolder by mutableStateOf(false)
    var connectionDetails by mutableStateOf<ConnectionDetails?>(null)
    var showLoginWeb by mutableStateOf(false)

    var isHideElementsActive by mutableStateOf(false)
    var hideHasSelection by mutableStateOf(false)
    var hideIsPreviewing by mutableStateOf(false)
    var hideSelectionLabel by mutableStateOf("")
    var hideSelectionSelector by mutableStateOf("")
    var hideConfirmDraft by mutableStateOf<HideElementConfirmDraft?>(null)
    var areHiddenElementsRevealed by mutableStateOf(false)
    var showHideElementsManage by mutableStateOf(false)
    var showHideElementsTip by mutableStateOf(false)

    val currentTab: BrowserTab
        get() = tabs.firstOrNull { it.id == selectedTabId }
            ?: tabs.firstOrNull()
            ?: BrowserTab(prefersDesktopWebsite = preferences.settings.desktopByDefault)
    val showsCollectionHome: Boolean get() = currentTab.isBookmerHome
    val activeWebView: WebView? get() = webViews[selectedTabId]
    val settings: AppSettings get() = preferences.settings

    init {
        restoreSession()
        closeInactiveTabsIfNeeded()
        updateNavigationState()
        if (session.value.isSignedIn) { refreshProfile(); sync.pull() }
        tabs.filter { it.autoRefreshSeconds > 0 }.forEach { scheduleAutoRefresh(it.id) }
    }

    fun showOverlay(value: Overlay) {
        if (isHideElementsActive) endHideElements(cancel = true)
        if (value == Overlay.TABS) {
            if (overlay == Overlay.TABS) return
            // Snapshot page content ONLY (software WebView draw) before the deck covers it.
            // Never use window PixelCopy here — that grabs chrome / the tab deck itself.
            freezeAttachedPreview(selectedTabId)
            // Drop stale in-memory thumbs (may still be old chrome/deck captures).
            val keepId = selectedTabId
            tabPreviews.keys.filter { it != keepId }.toList().forEach { id ->
                tabPreviews.remove(id)?.takeIf { !it.isRecycled }?.recycle()
            }
            hydrateTabPreviewsFromDisk()
            overlay = Overlay.TABS
            return
        }
        overlay = value
    }
    fun showBookmarkTools(kind: String) {
        bookmarkToolsKind = kind
        overlay = Overlay.BOOKMARK_TOOLS
    }
    fun dismissOverlay() { overlay = Overlay.NONE }

    fun createTab(select: Boolean = true): String {
        rememberFolderPath()
        val tab = BrowserTab(prefersDesktopWebsite = settings.desktopByDefault)
        tabs.add(tab)
        if (select) {
            selectedTabId = tab.id
            bookmarks.navigateToFolder(BookmerUrls.ROOT)
            addressText = ""
            expandToolbar()
        }
        persistSession()
        updateNavigationState()
        return tab.id
    }

    val selectedTabIndex: Int
        get() = tabs.indexOfFirst { it.id == selectedTabId }.let { if (it >= 0) it else 0 }
    val canGoToPreviousTab: Boolean get() = selectedTabIndex > 0
    val canGoToNextTab: Boolean get() = selectedTabIndex < tabs.lastIndex

    fun goToPreviousTab() {
        if (!canGoToPreviousTab) return
        selectTab(tabs[selectedTabIndex - 1].id)
    }

    fun goToNextTab() {
        if (!canGoToNextTab) return
        selectTab(tabs[selectedTabIndex + 1].id)
    }

    fun goToNextTabOrCreate() {
        if (canGoToNextTab) goToNextTab() else createTab()
    }

    fun closeTab(id: String) {
        if (tabs.size == 1) { openHome(); return }
        val index = tabs.indexOfFirst { it.id == id }
        refreshTasks.remove(id)?.let(refreshHandler::removeCallbacks)
        translateOriginalUrls.remove(id)
        tabPreviews.remove(id)
        TabPreviewStore.remove(id)
        webViews.remove(id)?.destroy()
        if (index >= 0) tabs.removeAt(index)
        if (selectedTabId == id) selectTab(tabs[index.coerceAtMost(tabs.lastIndex)].id)
        persistSession()
        TabPreviewStore.retainOnly(tabs.map { it.id }.toSet())
    }

    fun closeAllTabs() {
        webViews.values.forEach(WebView::destroy)
        webViews.clear()
        tabPreviews.clear()
        TabPreviewStore.clear()
        translateOriginalUrls.clear()
        tabs.clear()
        createTab()
    }

    /** Load cached thumbs for background tabs (Compose only attaches the selected WebView). */
    fun hydrateTabPreviewsFromDisk() {
        tabs.forEach { tab ->
            if (tabPreviews[tab.id] != null) return@forEach
            TabPreviewStore.load(tab.id)?.let { tabPreviews[tab.id] = it }
        }
    }

    /**
     * Refresh in-memory previews: disk for background tabs, live capture for the active one.
     * Only call while the WebView is still visible (not under the tab deck).
     */
    fun captureTabPreviews() {
        hydrateTabPreviewsFromDisk()
        captureActiveTabPreview()
    }

    fun captureActiveTabPreview(done: (() -> Unit)? = null) {
        val tabId = selectedTabId
        val tab = tabs.firstOrNull { it.id == tabId }
        if (tab == null) {
            done?.invoke()
            return
        }
        if (tab.isBookmerHome) {
            collectionSnapshotProvider?.invoke()?.let { commitPreview(tabId, it) }
            done?.invoke()
            return
        }
        // Do not capture while the tab deck covers the WebView — that produced recursive
        // previews of the switcher chrome (trash / + / nested cards).
        if (overlay.occludesBrowsingChrome() || overlay == Overlay.TABS) {
            done?.invoke()
            return
        }
        val web = webViews[tabId]
        if (web == null || web.width <= 0 || web.height <= 0 || !web.isAttachedToWindow) {
            done?.invoke()
            return
        }
        snapshotWebView(web) { bitmap ->
            if (overlay != Overlay.TABS) commitPreview(tabId, bitmap)
            else if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
            done?.invoke()
        }
    }

    private fun commitPreview(tabId: String, bitmap: Bitmap?) {
        if (bitmap == null) return
        if (overlay == Overlay.TABS) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        val scaled = scalePreview(bitmap)
        if (scaled !== bitmap && !bitmap.isRecycled) bitmap.recycle()
        if (isMostlyBlank(scaled)) {
            if (!scaled.isRecycled) scaled.recycle()
            return
        }
        tabPreviews[tabId]?.takeIf { it !== scaled && !it.isRecycled }?.recycle()
        tabPreviews[tabId] = scaled
        TabPreviewStore.save(tabId, scaled)
    }

    /**
     * Capture only the WebView's page pixels — never window PixelCopy.
     * Window copies include Compose chrome and (if the deck is open) the tab switcher itself.
     */
    fun snapshotCollectionRegion(view: View, left: Float, top: Float, width: Float, height: Float): Bitmap? {
        val w = width.toInt()
        val h = height.toInt()
        if (w <= 1 || h <= 1 || view.width <= 0 || view.height <= 0) return null
        val maxWidth = 480
        val scale = if (w > maxWidth) maxWidth.toFloat() / w else 1f
        val bw = (w * scale).roundToInt().coerceAtLeast(1)
        val bh = (h * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        return runCatching {
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            canvas.clipRect(0f, 0f, w.toFloat(), h.toFloat())
            canvas.translate(-left, -top)
            view.draw(canvas)
            bitmap
        }.getOrElse {
            if (!bitmap.isRecycled) bitmap.recycle()
            null
        }
    }

    private fun snapshotWebView(web: WebView, callback: (Bitmap?) -> Unit) {
        val w = web.width
        val h = web.height
        if (w <= 0 || h <= 0) {
            callback(null)
            return
        }
        // Draw already scaled for the tab deck — cheaper than full-res + downscale.
        val maxWidth = 480
        val scale = if (w > maxWidth) maxWidth.toFloat() / w else 1f
        val bw = (w * scale).roundToInt().coerceAtLeast(1)
        val bh = (h * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        runCatching {
            val previous = web.layerType
            web.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            try {
                bitmap.eraseColor(android.graphics.Color.WHITE)
                val canvas = Canvas(bitmap)
                canvas.scale(scale, scale)
                web.draw(canvas)
            } finally {
                web.setLayerType(previous, null)
            }
            callback(bitmap)
        }.getOrElse {
            if (!bitmap.isRecycled) bitmap.recycle()
            callback(null)
        }
    }

    private fun scalePreview(source: Bitmap, maxWidth: Int = 480): Bitmap {
        if (source.width <= maxWidth) return source
        val height = (source.height * (maxWidth.toFloat() / source.width)).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, maxWidth, height, true)
    }

    val hasOnlyStartPageTab: Boolean
        get() = tabs.size == 1 && tabs.firstOrNull()?.isBookmerHome == true

    fun selectTab(id: String) {
        if (id == selectedTabId) return
        endHideElements(cancel = true)
        // Freeze the leaving tab before Compose detaches its WebView.
        freezeAttachedPreview(selectedTabId)
        rememberFolderPath()
        selectedTabId = id
        val tab = currentTab
        bookmarks.folderStack.clear(); bookmarks.folderStack.addAll(tab.folderPath)
        addressText = if (tab.isBookmerHome) "" else tab.url.orEmpty()
        exitImmersive()
        areHiddenElementsRevealed = false
        expandToolbar()
        updateNavigationState()
        persistSession()
    }

    /** Immediate software snapshot while the WebView is still attached (before Compose detaches it). */
    private fun freezeAttachedPreview(tabId: String) {
        if (overlay == Overlay.TABS) return
        val tab = tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.isBookmerHome) {
            collectionSnapshotProvider?.invoke()?.let { commitPreview(tabId, it) }
            return
        }
        val web = webViews[tabId] ?: return
        if (web.width <= 0 || web.height <= 0) return
        snapshotWebView(web) { bitmap -> commitPreview(tabId, bitmap) }
    }

    /**
     * Reject failed HW captures (solid black / transparent / solid white erase).
     * Do NOT reject dark sites (Netflix etc.) — those have non-pure blacks and color.
     */
    private fun isMostlyBlank(bitmap: Bitmap): Boolean {
        val stepX = max(1, bitmap.width / 12)
        val stepY = max(1, bitmap.height / 12)
        var samples = 0
        var empty = 0
        var pureWhite = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val c = bitmap.getPixel(x, y)
                val a = (c ushr 24) and 0xFF
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                samples++
                when {
                    a < 8 -> empty++
                    r < 6 && g < 6 && b < 6 -> empty++
                    r > 250 && g > 250 && b > 250 -> pureWhite++
                }
                x += stepX
            }
            y += stepY
        }
        if (samples == 0) return true
        val emptyRatio = empty.toFloat() / samples
        val whiteRatio = pureWhite.toFloat() / samples
        return emptyRatio > 0.96f || whiteRatio > 0.96f
    }

    fun openHome() {
        endHideElements(cancel = true)
        endTranslateFeedbackNow()
        rememberFolderPath()
        updateTab { it.copy(isBookmerHome = true, title = "Bookmer", lastVisitedAt = System.currentTimeMillis()) }
        addressText = ""
        exitImmersive()
        isFindOnPage = false
        blockedPageUrl = null
        areHiddenElementsRevealed = false
        expandToolbar()
        updateNavigationState()
    }

    fun load(raw: String, inNewTab: Boolean = false, immersive: Boolean = false) {
        val url = resolveAddress(raw) ?: return
        if (url == BookmerUrls.HOME) { if (inNewTab) createTab() else openHome(); return }
        if (currentTab.isBookmerHome && !inNewTab) freezeAttachedPreview(selectedTabId)
        if (inNewTab) createTab()
        endHideElements(cancel = true)
        endTranslateFeedbackNow()
        translateOriginalUrls.remove(selectedTabId)
        areHiddenElementsRevealed = false
        val blocked = blockedDestination(url)
        if (blocked != null) {
            if (blocked.startsWith("http")) load(blocked) else blockedPageUrl = url
            return
        }
        blockedPageUrl = null
        updateTab { it.copy(url = url, isBookmerHome = false, isPageTranslated = false, lastVisitedAt = System.currentTimeMillis()) }
        addressText = url
        isEditingAddress = false
        if (immersive) enterImmersive() else exitImmersive()
        webViews[selectedTabId]?.loadUrl(url)
        persistSession()
        updateNavigationState()
    }

    fun presentBlockedPage(url: String) {
        activeWebView?.stopLoading()
        blockedPageUrl = url
        updateTab { it.copy(url = url, isBookmerHome = false, title = "Blocked") }
    }

    /** URL shown in the address field when not editing (iOS `displayedAddress`). */
    fun displayedAddress(): String {
        blockedPageUrl?.let { return it }
        if (showsCollectionHome) return ""
        return currentTab.url.orEmpty()
    }

    fun beginEditingAddress() {
        isEditingAddress = true
        if (toolbarCollapsed || toolbarStickyCollapsed) expandToolbar()
        addressText = displayedAddress()
    }

    fun cancelEditingAddress() {
        isEditingAddress = false
        addressText = displayedAddress()
    }

    fun submitAddress() = load(addressText)

    fun goBack(): Boolean {
        if (showsCollectionHome) {
            if (bookmarks.back()) { rememberFolderPath(); return true }
            return false
        }
        val web = activeWebView
        if (web?.canGoBack() == true) web.goBack() else openHome()
        return true
    }

    fun goForward() {
        if (showsCollectionHome && currentTab.url != null) {
            updateTab { it.copy(isBookmerHome = false) }
            addressText = currentTab.url.orEmpty()
        } else activeWebView?.goForward()
        updateNavigationState()
    }

    /**
     * Pages in this tab's WebView back-forward list only — not the global History store.
     * Order matches iOS: forward (newest first), current, then back (oldest → nearest).
     */
    fun tabHistoryEntries(): List<TabHistoryEntry> {
        val web = activeWebView ?: return emptyList()
        val list = web.copyBackForwardList()
        if (list.size <= 0) return emptyList()
        val current = list.currentIndex.coerceIn(0, list.size - 1)
        val entries = ArrayList<TabHistoryEntry>(list.size)
        for (i in list.size - 1 downTo current + 1) {
            val item = list.getItemAtIndex(i) ?: continue
            if (isSyntheticTabHistoryUrl(item.url)) continue
            entries += TabHistoryEntry(
                index = i,
                title = item.title?.trim().orEmpty().ifBlank { hostLabel(item.url) },
                url = item.url,
                isCurrent = false,
            )
        }
        list.getItemAtIndex(current)?.let { item ->
            if (!isSyntheticTabHistoryUrl(item.url)) {
                entries += TabHistoryEntry(
                    index = current,
                    title = item.title?.trim().orEmpty().ifBlank { hostLabel(item.url) },
                    url = item.url,
                    isCurrent = true,
                )
            }
        }
        for (i in 0 until current) {
            val item = list.getItemAtIndex(i) ?: continue
            if (isSyntheticTabHistoryUrl(item.url)) continue
            entries += TabHistoryEntry(
                index = i,
                title = item.title?.trim().orEmpty().ifBlank { hostLabel(item.url) },
                url = item.url,
                isCurrent = false,
            )
        }
        return entries
    }

    fun goToTabHistoryEntry(entry: TabHistoryEntry) {
        val web = activeWebView ?: return
        val list = web.copyBackForwardList()
        if (entry.index !in 0 until list.size) return
        val delta = entry.index - list.currentIndex
        if (showsCollectionHome) {
            rememberFolderPath()
            updateTab { it.copy(isBookmerHome = false) }
            exitImmersive()
            isFindOnPage = false
            blockedPageUrl = null
            expandToolbar()
        }
        if (delta != 0) {
            web.goBackOrForward(delta)
        } else {
            list.getItemAtIndex(entry.index)?.url?.let { addressText = it }
            updateNavigationState()
        }
        dismissOverlay()
        persistSession()
    }

    private fun isSyntheticTabHistoryUrl(url: String?): Boolean {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty() || raw.equals("about:blank", true)) return true
        if (raw.startsWith("bookmer://", ignoreCase = true)) return true
        return false
    }

    private fun hostLabel(url: String): String =
        runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: url

    fun reloadOrStop() { activeWebView?.let { if (isLoading) it.stopLoading() else it.reload() } }

    fun toggleDesktop() {
        val next = !currentTab.prefersDesktopWebsite
        updateTab { it.copy(prefersDesktopWebsite = next) }
        val tab = tabs.firstOrNull { it.id == selectedTabId } ?: return
        activeWebView?.let { BrowserWebConfigurator.applyUserAgent(it, tab, settings); it.reload() }
    }

    fun setZoom(value: Int) {
        val zoom = value.coerceIn(50, 200)
        updateTab { it.copy(pageZoom = zoom) }
        activeWebView?.setInitialScale(if (zoom == 100) 0 else zoom)
    }

    fun setAutoRefresh(seconds: Int) {
        updateTab { it.copy(autoRefreshSeconds = seconds.coerceAtLeast(0)) }
        scheduleAutoRefresh(selectedTabId)
    }

    private fun scheduleAutoRefresh(tabId: String) {
        refreshTasks.remove(tabId)?.let(refreshHandler::removeCallbacks)
        val interval = tabs.firstOrNull { it.id == tabId }?.autoRefreshSeconds ?: return
        if (interval <= 0) return
        val task = object : Runnable {
            override fun run() {
                val tab = tabs.firstOrNull { it.id == tabId } ?: return
                if (!tab.isBookmerHome) webViews[tabId]?.reload()
                refreshHandler.postDelayed(this, interval * 1000L)
            }
        }
        refreshTasks[tabId] = task
        refreshHandler.postDelayed(task, interval * 1000L)
    }

    fun enterReader() {
        val web = activeWebView ?: return
        val url = currentTab.url ?: return
        readerLoading = true
        val script = """(()=>JSON.stringify({title:document.title||'',text:(document.querySelector('article')||document.querySelector('main')||document.body).innerText||''}))()"""
        web.evaluateJavascript(script) { raw ->
            readerLoading = false
            runCatching {
                val decoded = JSONArray("[$raw]").getString(0)
                val json = JSONObject(decoded)
                ReaderContent(json.optString("title", currentTab.title), json.optString("text"), url)
            }.onSuccess { readerContent = it }.onFailure { toastMessage = "Reader is not available on this page" }
        }
    }

    fun translatePage() {
        if (isTranslating) return
        if (currentTab.isPageTranslated) {
            showOriginalPage()
            return
        }
        val url = currentTab.url?.takeIf { it.startsWith("http") } ?: return
        val source = unwrapGoogleTranslateUrl(url) ?: url
        val lang = settings.translateLanguage.ifBlank { "en" }
        translateOriginalUrls[selectedTabId] = source
        updateTab { it.copy(isPageTranslated = true) }
        beginTranslateFeedback()
        val gt = googleTranslateUrl(source, lang)
        addressText = source
        activeWebView?.loadUrl(gt)
    }

    fun showOriginalPage() {
        endTranslateFeedbackNow()
        val original = translateOriginalUrls.remove(selectedTabId)
            ?: unwrapGoogleTranslateUrl(currentTab.url)
            ?: return
        updateTab { it.copy(isPageTranslated = false, url = original) }
        addressText = original
        activeWebView?.loadUrl(original)
    }

    private fun beginTranslateFeedback() {
        refreshHandler.removeCallbacks(endTranslateFeedback)
        translateFeedbackStartedAt = System.currentTimeMillis()
        isTranslating = true
        // Safety: never leave the pill spinning forever if load events are missed.
        refreshHandler.postDelayed(endTranslateFeedback, 20_000L)
    }

    private fun scheduleEndTranslateFeedback() {
        if (!isTranslating) return
        refreshHandler.removeCallbacks(endTranslateFeedback)
        val remain = (480L - (System.currentTimeMillis() - translateFeedbackStartedAt)).coerceAtLeast(0L)
        refreshHandler.postDelayed(endTranslateFeedback, remain)
    }

    private fun endTranslateFeedbackNow() {
        refreshHandler.removeCallbacks(endTranslateFeedback)
        isTranslating = false
    }

    private var lastHideBootstrapAttemptMs = 0L

    private val hideSelectionPoll = object : Runnable {
        override fun run() {
            if (!isHideElementsActive) return
            val web = activeWebView ?: return
            web.evaluateJavascript(HideElementsScript.ENSURE_READY) { ready ->
                if (!isHideElementsActive) return@evaluateJavascript
                if (ready != "true") {
                    val now = System.currentTimeMillis()
                    if (now - lastHideBootstrapAttemptMs > 1500L) {
                        lastHideBootstrapAttemptMs = now
                        injectAndStartPick(web, startImmediately = true)
                    }
                    return@evaluateJavascript
                }
                web.evaluateJavascript(HideElementsScript.ENSURE_PICKING, null)
                web.evaluateJavascript(HideElementsScript.POLL_MESSAGE) { raw ->
                    decodeJsJsonString(raw)?.let(::handleHideElementsMessage)
                }
                web.evaluateJavascript(
                    "(function(){try{var i=window.__bookmerHide&&window.__bookmerHide.selectionInfo&&window.__bookmerHide.selectionInfo();return i?JSON.stringify({type:'selection',hasSelection:!!(i.selector),label:i.label||'',selector:i.selector||''}):null;}catch(e){return null;}})();"
                ) { raw ->
                    decodeJsJsonString(raw)?.let(::handleHideElementsMessage)
                }
            }
            refreshHandler.postDelayed(this, 280)
        }
    }

    private fun decodeJsJsonString(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return runCatching { JSONArray("[$raw]").getString(0) }.getOrNull()
            ?.takeIf { it.isNotBlank() && it != "null" }
    }

    fun hideSelectedElement() = beginHideElements()

    fun beginHideElements() {
        val web = activeWebView
        if (web == null) {
            notifyUser("Open a website first")
            return
        }
        if (showsCollectionHome || currentTab.url.isNullOrBlank()) {
            notifyUser("Open a website first")
            return
        }
        endFind()
        if (isImmersive) exitImmersive()
        hideSelectionLabel = ""
        hideSelectionSelector = ""
        hideHasSelection = false
        hideIsPreviewing = false
        hideConfirmDraft = null
        isHideElementsActive = true
        toolbarCollapsed = true
        toolbarStickyCollapsed = false
        val showTip = !HideElementsTipStore.hasSeen(getApplication())
        if (showTip) showHideElementsTip = true
        // Start pick immediately (iOS parity). Tip is a Compose overlay; after dismiss, taps hit the page.
        injectAndStartPick(web, startImmediately = true)
    }

    private fun notifyUser(message: String) {
        toastMessage = message
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    private fun injectAndStartPick(web: WebView, startImmediately: Boolean) {
        ensureHideElementsBootstrap(web) {
            web.evaluateJavascript(HideElementsScript.ENSURE_READY) { ready ->
                val ok = ready == "true"
                if (!ok) {
                    // Force a second inject attempt.
                    web.evaluateJavascript(HideElementsScript.ENSURE_BOOTSTRAP) {
                        if (startImmediately) web.evaluateJavascript(HideElementsScript.START_PICK, null)
                        startHideSelectionPolling()
                    }
                } else {
                    if (startImmediately) web.evaluateJavascript(HideElementsScript.START_PICK, null)
                    startHideSelectionPolling()
                }
            }
        }
    }

    private fun startHideSelectionPolling() {
        refreshHandler.removeCallbacks(hideSelectionPoll)
        refreshHandler.post(hideSelectionPoll)
    }

    private fun stopHideSelectionPolling() {
        refreshHandler.removeCallbacks(hideSelectionPoll)
    }

    fun endHideElements(cancel: Boolean) {
        if (!isHideElementsActive && hideConfirmDraft == null) {
            hideConfirmDraft = null
            return
        }
        stopHideSelectionPolling()
        val script = if (cancel) HideElementsScript.CANCEL_PICK else HideElementsScript.STOP_PICK
        activeWebView?.evaluateJavascript(script, null)
        isHideElementsActive = false
        hideIsPreviewing = false
        hideHasSelection = false
        hideSelectionLabel = ""
        hideSelectionSelector = ""
        hideConfirmDraft = null
        areHiddenElementsRevealed = false
        showHideElementsTip = false
        expandToolbar()
        applyHiddenElementsToCurrentPage()
    }

    fun hideElementsExpand() {
        if (hideIsPreviewing) return
        activeWebView?.evaluateJavascript(HideElementsScript.EXPAND, null)
    }

    fun hideElementsShrink() {
        if (hideIsPreviewing) return
        activeWebView?.evaluateJavascript(HideElementsScript.SHRINK, null)
    }

    fun previewHideCurrentSelection() {
        if (!hideHasSelection || hideSelectionSelector.isEmpty()) return
        activeWebView?.evaluateJavascript(HideElementsScript.PREVIEW_HIDE, null)
        hideIsPreviewing = true
    }

    fun undoHidePreview() {
        hideIsPreviewing = false
        activeWebView?.evaluateJavascript(HideElementsScript.CLEAR_PREVIEW, null)
    }

    fun confirmHidePreview() {
        if (!hideIsPreviewing || !hideHasSelection || hideSelectionSelector.isEmpty()) return
        val suggested = hideSelectionLabel
            .replace(Regex("""^<[^>]+>\s*"""), "")
            .trim()
            .ifEmpty { "Hidden element" }
            .take(48)
        hideConfirmDraft = HideElementConfirmDraft(suggestedTitle = suggested, selector = hideSelectionSelector)
    }

    fun confirmHideElement(scope: HiddenElementScope, title: String) {
        val draft = hideConfirmDraft
        val url = currentTab.url
        if (draft == null || url.isNullOrBlank()) {
            hideConfirmDraft = null
            return
        }
        hiddenElements.add(url, scope, draft.selector, title)
        hideConfirmDraft = null
        hideIsPreviewing = false
        hideHasSelection = false
        hideSelectionLabel = ""
        hideSelectionSelector = ""
        areHiddenElementsRevealed = false
        applyHiddenElementsToCurrentPage()
        activeWebView?.evaluateJavascript(HideElementsScript.CLEAR_PREVIEW, null)
        activeWebView?.evaluateJavascript(HideElementsScript.START_PICK, null)
    }

    fun cancelHideElementConfirm() {
        hideConfirmDraft = null
        undoHidePreview()
    }

    fun showHiddenElementsTemporarily() {
        areHiddenElementsRevealed = true
        activeWebView?.evaluateJavascript(HideElementsScript.REVEAL_ALL, null)
    }

    fun reapplyHiddenElements() {
        areHiddenElementsRevealed = false
        applyHiddenElementsToCurrentPage()
    }

    fun toggleHiddenElementsVisibility() {
        if (areHiddenElementsRevealed) reapplyHiddenElements() else showHiddenElementsTemporarily()
    }

    fun applyHiddenElementsToCurrentPage() {
        val web = activeWebView ?: return
        val url = currentTab.url
        if (showsCollectionHome || url.isNullOrBlank()) return
        if (areHiddenElementsRevealed) {
            ensureHideElementsBootstrap(web) {
                web.evaluateJavascript(HideElementsScript.REVEAL_ALL, null)
            }
            return
        }
        val selectors = hiddenElements.rulesMatching(url).map { it.selector }
        ensureHideElementsBootstrap(web) {
            if (selectors.isEmpty()) {
                web.evaluateJavascript(HideElementsScript.REVEAL_ALL, null)
            } else {
                web.evaluateJavascript(HideElementsScript.applyJavaScript(selectors), null)
            }
        }
    }

    private fun ensureHideElementsBootstrap(web: WebView, then: () -> Unit) {
        web.evaluateJavascript(HideElementsScript.ENSURE_BOOTSTRAP) { then() }
    }

    /** Public entry for native tap picking — guarantees bootstrap before pickAt. */
    fun ensureHideBootstrapThen(then: () -> Unit) {
        val web = activeWebView ?: return then()
        ensureHideElementsBootstrap(web, then)
    }

    fun handleHideElementsMessage(json: String) {
        val body = runCatching { JSONObject(json) }.getOrNull() ?: return
        when (body.optString("type")) {
            "selection" -> {
                val has = body.optBoolean("hasSelection")
                val label = body.optString("label")
                val selector = body.optString("selector")
                if (has != hideHasSelection || label != hideSelectionLabel || selector != hideSelectionSelector) {
                    hideIsPreviewing = false
                    hideHasSelection = has
                    hideSelectionLabel = label
                    hideSelectionSelector = selector
                }
            }
            "pickStarted" -> isHideElementsActive = true
            "pickStopped" -> {
                if (body.optBoolean("cancelled")) {
                    stopHideSelectionPolling()
                    isHideElementsActive = false
                    hideIsPreviewing = false
                    hideHasSelection = false
                    expandToolbar()
                }
            }
        }
    }

    fun dismissHideElementsTip() {
        HideElementsTipStore.markSeen(getApplication())
        showHideElementsTip = false
        activeWebView?.let { web ->
            ensureHideElementsBootstrap(web) {
                web.evaluateJavascript(HideElementsScript.START_PICK, null)
                startHideSelectionPolling()
            }
        }
    }

    fun openCurrentPageInWebArchive() {
        val url = currentTab.url ?: return
        val archive = com.bookmer.browser.data.BookmerPageActions.webArchiveUrl(url) ?: return
        load(archive, inNewTab = true)
    }

    fun reportCurrentPage() {
        val url = currentTab.url ?: return
        val mail = Uri.parse(com.bookmer.browser.data.BookmerPageActions.reportMailto(url))
        val intent = Intent(Intent.ACTION_SENDTO, mail).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
            .onFailure { toastMessage = "No mail app available" }
    }

    fun removeCurrentPageData() {
        val url = currentTab.url ?: return
        val host = SitePermissionStore.hostFromUrl(url)
        if (host.isEmpty()) return
        val cookieManager = CookieManager.getInstance()
        listOf("https://$host", "http://$host", "https://www.$host", "http://www.$host").forEach { origin ->
            cookieManager.getCookie(origin)?.split(';')?.forEach { part ->
                val name = part.substringBefore('=').trim()
                if (name.isNotEmpty()) cookieManager.setCookie(origin, "$name=; Max-Age=0; Path=/")
            }
        }
        cookieManager.flush()
        activeWebView?.clearCache(true)
        activeWebView?.reload()
        toastMessage = "Removed data for $host"
    }

    fun showConnectionDetails() {
        val url = currentTab.url ?: return
        val host = SitePermissionStore.hostFromUrl(url).ifBlank { Uri.parse(url).host.orEmpty() }
        connectionDetails = ConnectionDetails(host = host.ifBlank { url }, isSecure = url.startsWith("https"))
    }

    fun beginFind() { isFindOnPage = true; findQuery = ""; findMatches = 0 }
    fun find(text: String) { findQuery = text; activeWebView?.findAllAsync(text) }
    fun findNext(forward: Boolean) { activeWebView?.findNext(forward) }
    fun endFind() { activeWebView?.clearMatches(); isFindOnPage = false; findQuery = "" }

    fun collectCurrentPage() {
        val tab = currentTab
        val url = translateOriginalUrls[selectedTabId]
            ?: unwrapGoogleTranslateUrl(tab.url)
            ?: tab.url
            ?: return
        pendingCollect = PendingCollect(url, tab.title.ifBlank { url })
    }

    fun confirmCollect(title: String, url: String, parent: String, note: String?) {
        val item = bookmarks.collect(url, title, parent, note)
        sync.pushCreate(item)
        pendingCollect = null
        toastMessage = "Saved to Collection"
    }

    fun promptDownload(url: String, userAgent: String?, contentDisposition: String?, mime: String?, size: Long) {
        pendingDownload = PendingDownload(url, userAgent, contentDisposition, mime, size)
    }

    fun confirmDownload() {
        val request = pendingDownload ?: return
        val manager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val download = DownloadManager.Request(Uri.parse(request.url)).apply {
            setTitle(request.filename); setMimeType(request.mimeType); setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Bookmer/${request.filename}")
            request.userAgent?.let { addRequestHeader("User-Agent", it) }
            CookieManager.getInstance().getCookie(request.url)?.let { addRequestHeader("Cookie", it) }
        }
        val id = manager.enqueue(download)
        downloads.add(0, DownloadEntry(id, request.filename, request.url))
        pendingDownload = null
    }

    fun clearDownloads() { downloads.clear() }

    fun clearBrowsingData(clearHistory: Boolean, clearWebData: Boolean, since: Long? = null) {
        if (clearHistory) history.clear(since)
        if (clearWebData) {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
            webViews.values.forEach { it.clearCache(true); it.clearHistory() }
        }
        if (since == null) closeAllTabs() else tabs.filter { it.lastVisitedAt >= since }.map { it.id }.forEach(::closeTab)
        downloads.clear()
    }

    fun presentLogin() {
        showLoginWeb = true
    }

    fun dismissLogin() {
        showLoginWeb = false
    }

    /** After Bookmer ID / bookmer.com/login yields `_act`. */
    fun completeWebLogin(token: String, done: () -> Unit = {}) {
        if (token.isBlank()) return
        session.apply(token)
        com.bookmer.browser.ui.ensureWebAccessCookie(token)
        preferences.update { it.copy(setupCompleted = true) }
        showLoginWeb = false
        openHome()
        refreshProfile()
        sync.pull { done() }
    }

    fun signOutLocally() {
        session.clear(); CookieManager.getInstance().removeAllCookies(null); WebStorage.getInstance().deleteAllData()
        bookmarks.resetToGuestDefaults(); history.clear(); preferences.reset(); closeAllTabs()
    }

    fun refreshProfile() {
        val token = session.value.token ?: return
        api.fetchUser(token) { result -> result.getOrNull()?.let { response ->
            val user = response.optJSONObject("user") ?: response
            val extras = user.optJSONObject("extras") ?: JSONObject()
            val subscription = extras.optJSONObject("currentSubscription") ?: JSONObject()
            val accountType = user.optString("accountType").ifBlank { extras.optString("accountType") }
            val hasPro = extras.optBoolean("lifetimeDeal") || subscription.optBoolean("active") ||
                accountType in setOf("LIFETIME", "FREE_TRIAL", "PRO", "SUBSCRIPTION")
            session.updateProfile(
                user.optString("email").takeIf { it.isNotBlank() },
                user.optString("name").ifBlank { user.optString("userName") }.takeIf { it.isNotBlank() },
                user.optString("picture").ifBlank { user.optString("avatar") }.takeIf { it.isNotBlank() },
                accountType.takeIf { it.isNotBlank() }, hasPro,
            )
            BookmerServices.pro.refreshEntitlement(syncAccount = true)
        } }
    }

    fun signOutFully(done: () -> Unit = {}) {
        api.identityLogout(session.value.email) { signOutLocally(); done() }
    }

    fun onPageStarted(tabId: String, url: String) {
        val translating = translateOriginalUrls.containsKey(tabId) && isGoogleTranslateUrl(url)
        if (!translating && !isGoogleTranslateUrl(url)) translateOriginalUrls.remove(tabId)
        val displayUrl = if (translating) translateOriginalUrls[tabId] ?: unwrapGoogleTranslateUrl(url) ?: url else url
        updateTabById(tabId) {
            it.copy(
                url = url,
                isBookmerHome = false,
                isPageTranslated = translating,
                lastVisitedAt = System.currentTimeMillis(),
            )
        }
        if (tabId == selectedTabId && !overlay.occludesBrowsingChrome()) {
            isLoading = true
            pageProgress = 0
            addressText = displayUrl
            blockedPageUrl = null
            updateNavigationState()
        }
        webViews[tabId]?.let { web ->
            val prefersDesktop = tabs.firstOrNull { it.id == tabId }?.prefersDesktopWebsite == true
            if (!alias.isOff) web.evaluateJavascript(alias.injectionScript(prefersDesktop), null)
        }
    }

    fun onPageFinished(tabId: String, url: String, title: String) {
        val translating = translateOriginalUrls.containsKey(tabId) && isGoogleTranslateUrl(url)
        updateTabById(tabId) {
            it.copy(
                url = url,
                title = title.ifBlank { it.title },
                isBookmerHome = false,
                isPageTranslated = translating || it.isPageTranslated && translating,
            )
        }
        if (!isGoogleTranslateUrl(url)) history.record(url, title)
        webViews[tabId]?.let { web ->
            val prefersDesktop = tabs.firstOrNull { it.id == tabId }?.prefersDesktopWebsite == true
            BrowserWebConfigurator.injectPageRules(web, settings, prefersDesktop)
        }
        if (tabId == selectedTabId) {
            if (!overlay.occludesBrowsingChrome()) {
                isLoading = false
                pageProgress = 100
                if (translating) {
                    addressText = translateOriginalUrls[tabId] ?: unwrapGoogleTranslateUrl(url) ?: addressText
                    scheduleEndTranslateFeedback()
                } else if (isTranslating) {
                    endTranslateFeedbackNow()
                }
                updateNavigationState()
            }
            if (isHideElementsActive) {
                webViews[tabId]?.let { injectAndStartPick(it, startImmediately = true) }
            } else {
                applyHiddenElementsToCurrentPage()
            }
            if (!overlay.occludesBrowsingChrome()) {
                captureActiveTabPreview()
            }
        }
        persistSession()
    }

    fun onProgress(tabId: String, value: Int) {
        if (tabId != selectedTabId) return
        if (overlay.occludesBrowsingChrome()) return
        pageProgress = value
        isLoading = value < 100
        if (isTranslating && value >= 92) scheduleEndTranslateFeedback()
    }
    fun onFindResult(count: Int) { findMatches = count }
    fun collapseToolbarFromScroll() {
        if (!canAutoHideToolbar()) return
        // Strip mode: page scroll must not touch chrome.
        if (toolbarStickyCollapsed) return
        toolbarCollapsed = true
    }

    fun enterImmersive() {
        if (showsCollectionHome || currentTab.url.isNullOrBlank()) return
        endFind()
        if (isHideElementsActive) endHideElements(cancel = true)
        readerContent = null
        isImmersive = true
        resetImmersiveEdges()
        toolbarStickyCollapsed = false
        toolbarCollapsed = true
        if (!ImmersiveTipStore.hasSeen(getApplication())) showImmersiveTip = true
    }

    fun exitImmersive() {
        isImmersive = false
        showImmersiveTip = false
        resetImmersiveEdges()
        expandToolbar()
    }

    fun dismissImmersiveTip() {
        ImmersiveTipStore.markSeen(getApplication())
        showImmersiveTip = false
    }

    private fun resetImmersiveEdges() {
        immersiveTopExitLatched = false
        immersiveBottomExitLatched = false
        isNearPageTop = false
        isNearPageBottom = false
    }

    fun onImmersiveOverscroll(web: WebView, atTop: Boolean, atBottom: Boolean) {
        if (web != activeWebView) return
        refreshImmersiveEdgeReveal(web, pullingTop = atTop, pullingBottom = atBottom)
    }

    fun onPageScrolled(web: WebView, scrollY: Int, oldScrollY: Int) {
        if (web != activeWebView) return
        if (overlay.occludesBrowsingChrome()) return
        val delta = scrollY - oldScrollY
        when {
            !isImmersive && scrollY <= 8 -> expandToolbarFromScroll()
            !isImmersive && delta > 18 -> collapseToolbarFromScroll()
            !isImmersive && delta < -18 -> expandToolbarFromScroll()
        }
        if (!isImmersive) return
        // Use public scroll APIs — computeVerticalScroll* is protected on WebView.
        val range = (web.contentHeight * web.scale).toInt().coerceAtLeast(web.height)
        val max = (range - web.height).coerceAtLeast(0)
        val pullingTop = scrollY <= 0 && delta < -8
        val pullingBottom = (max <= 8 && delta != 0) || (max > 0 && scrollY >= max - 1 && delta > 8)
        refreshImmersiveEdgeReveal(web, pullingTop, pullingBottom)
    }

    private fun refreshImmersiveEdgeReveal(web: WebView, pullingTop: Boolean, pullingBottom: Boolean) {
        if (!isImmersive) {
            resetImmersiveEdges()
            return
        }
        val y = web.scrollY
        val range = (web.contentHeight * web.scale).toInt().coerceAtLeast(web.height)
        val max = (range - web.height).coerceAtLeast(0)
        if (pullingTop) immersiveTopExitLatched = true
        else if (y > 36) immersiveTopExitLatched = false
        isNearPageTop = immersiveTopExitLatched

        if (max <= 8) {
            if (pullingTop || pullingBottom) immersiveBottomExitLatched = true
            else if (y > 36) immersiveBottomExitLatched = false
            isNearPageBottom = immersiveBottomExitLatched
            return
        }
        if (pullingBottom) immersiveBottomExitLatched = true
        else if (max - y > 36) immersiveBottomExitLatched = false
        isNearPageBottom = immersiveBottomExitLatched
    }

    fun expandToolbarFromScroll() {
        if (!canAutoHideToolbar()) return
        // Sticky strip stays until the user taps it.
        if (toolbarStickyCollapsed) return
        toolbarCollapsed = false
    }

    /** Swipe down on the address bar — thin black strip with the tab title. */
    fun collapseToolbarFromChrome() {
        if (!canAutoHideToolbar()) return
        toolbarStickyCollapsed = true
        toolbarCollapsed = true
    }

    fun expandToolbar() {
        toolbarStickyCollapsed = false
        toolbarCollapsed = false
    }

    private fun canAutoHideToolbar(): Boolean =
        settings.hideToolbar && !showsCollectionHome && !isImmersive && !isFindOnPage && !isHideElementsActive && readerContent == null

    /** True while full chrome is on screen (not scroll-hidden, not strip-pinned). */
    val showsToolbar: Boolean
        get() = when {
            isFindOnPage || isHideElementsActive || isImmersive || readerContent != null -> false
            toolbarStickyCollapsed -> false
            !settings.hideToolbar -> true
            else -> !toolbarCollapsed
        }

    fun registerWebView(tabId: String, webView: WebView) {
        webViews[tabId] = webView
        val tab = tabs.firstOrNull { it.id == tabId } ?: return
        if (!tab.isBookmerHome && webView.url == null && tab.url != null) webView.loadUrl(tab.url)
    }

    fun destroyWebViews() { webViews.values.forEach(WebView::destroy); webViews.clear() }

    fun updateNavigationState() {
        canGoBack = if (showsCollectionHome) bookmarks.folderStack.isNotEmpty() else true
        canGoForward = if (showsCollectionHome) currentTab.url != null else activeWebView?.canGoForward() == true
    }

    private fun rememberFolderPath() {
        if (tabs.isEmpty() || selectedTabId.isEmpty()) return
        updateTab(persist = false) { it.copy(folderPath = bookmarks.folderStack.toList()) }
    }

    private fun updateTab(persist: Boolean = true, block: (BrowserTab) -> BrowserTab) {
        val index = tabs.indexOfFirst { it.id == selectedTabId }
        if (index >= 0) tabs[index] = block(tabs[index])
        if (persist) persistSession()
    }

    private fun updateTabById(id: String, block: (BrowserTab) -> BrowserTab) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) tabs[index] = block(tabs[index])
    }

    private fun resolveAddress(raw: String): String? {
        return AddressResolver.resolve(raw, settings)
    }

    private fun googleTranslateUrl(pageUrl: String, lang: String): String =
        Uri.parse("https://translate.google.com/translate").buildUpon()
            .appendQueryParameter("hl", lang)
            .appendQueryParameter("sl", "auto")
            .appendQueryParameter("tl", lang)
            .appendQueryParameter("u", pageUrl)
            .build()
            .toString()

    private fun isGoogleTranslateUrl(url: String): Boolean {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull().orEmpty()
        return host.contains("translate.google") && (url.contains("u=") || url.contains("/translate"))
    }

    private fun unwrapGoogleTranslateUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        if (!host.contains("translate.google")) return null
        return uri.getQueryParameter("u")?.takeIf { it.startsWith("http") }
    }

    private fun blockedDestination(url: String): String? {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return null
        val rule = settings.blockedSites.firstOrNull { host == it.host || host.endsWith(".${it.host}") } ?: return null
        return rule.redirectUrl ?: "blocked"
    }

    private fun closeInactiveTabsIfNeeded() {
        val days = settings.closeTabsAfterDays
        if (days <= 0 || tabs.size <= 1) return
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        tabs.filter { it.id != selectedTabId && it.lastVisitedAt < cutoff }.map { it.id }.forEach(::closeTab)
    }

    private fun persistSession() {
        if (tabs.isEmpty()) return
        val payload = JSONObject().put("selectedTabID", selectedTabId).put("tabs", JSONArray(tabs.map { it.toJson() })).toString()
        getApplication<Application>().getSharedPreferences("bookmer.tabs", Context.MODE_PRIVATE).edit().putString("tabs.v1", payload).apply()
    }

    private fun restoreSession() {
        val raw = getApplication<Application>().getSharedPreferences("bookmer.tabs", Context.MODE_PRIVATE).getString("tabs.v1", null)
        runCatching {
            val json = JSONObject(raw ?: error("missing")); val array = json.getJSONArray("tabs")
            tabs.addAll((0 until array.length()).map { BrowserTab.fromJson(array.getJSONObject(it)) })
            selectedTabId = json.optString("selectedTabID").takeIf { id -> tabs.any { it.id == id } } ?: tabs.first().id
        }
        if (tabs.isEmpty()) { val tab = BrowserTab(prefersDesktopWebsite = settings.desktopByDefault); tabs.add(tab); selectedTabId = tab.id }
        bookmarks.folderStack.clear(); bookmarks.folderStack.addAll(currentTab.folderPath)
        addressText = if (currentTab.isBookmerHome) "" else currentTab.url.orEmpty()
        // Warm tab-switcher thumbs from disk so cards aren't empty after cold start.
        TabPreviewStore.loadAll(tabs.map { it.id }).forEach { (id, bitmap) -> tabPreviews[id] = bitmap }
        TabPreviewStore.retainOnly(tabs.map { it.id }.toSet())
    }

    override fun onCleared() {
        refreshTasks.values.forEach(refreshHandler::removeCallbacks)
        refreshTasks.clear()
        refreshHandler.removeCallbacks(endTranslateFeedback)
        destroyWebViews()
        super.onCleared()
    }
}
