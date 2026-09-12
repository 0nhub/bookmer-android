package com.bookmer.browser.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BookmerUrls {
    const val HOME = "bookmer://collection"
    const val API = "https://api.bookmer.com"
    const val LOGIN = "https://www.bookmer.com/login"
    const val ACCOUNT = "https://id.bookmer.com"
    const val HELP = "https://help.bookmer.com"
    const val ROOT = "/"
    const val ARCHIVE = "archive"
    const val TAGS = "__bm_tags"
    const val HIDDEN = "Undefined"
}

enum class ItemKind { BOOKMARK, FOLDER }

data class BookmerItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: ItemKind,
    val title: String,
    val targetUrl: String? = null,
    val parentId: String = BookmerUrls.ROOT,
    val iconUrl: String? = null,
    val iconBackground: String? = null,
    val iconX: Double = 0.0,
    val iconY: Double = 0.0,
    val iconZoom: Double = if (kind == ItemKind.BOOKMARK) -22.0 else 0.0,
    val order: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val remoteId: String? = null,
    val note: String? = null,
    val previewImage: String? = null,
    val customPreviewImage: String? = null,
    val customPreviewX: Double? = null,
    val customPreviewY: Double? = null,
    val customPreviewZoom: Double? = null,
    val customPreviewBackground: String? = null,
    val contentView: String? = null,
) {
    val iconScale: Float get() = ((iconZoom + 100.0) / 100.0).toFloat()

    fun toJson() = JSONObject().apply {
        put("id", id); put("kind", kind.name.lowercase()); put("title", title)
        putNullable("targetURL", targetUrl); put("parentID", normalizedParent(parentId))
        putNullable("iconURL", iconUrl); putNullable("iconBackground", iconBackground)
        put("iconX", iconX); put("iconY", iconY); put("iconZoom", iconZoom); put("order", order)
        put("updatedAt", updatedAt); put("createdAt", createdAt); putNullable("remoteID", remoteId)
        putNullable("note", note); putNullable("previewImage", previewImage)
        putNullable("customPreviewImage", customPreviewImage); putNullable("customPreviewX", customPreviewX)
        putNullable("customPreviewY", customPreviewY); putNullable("customPreviewZoom", customPreviewZoom)
        putNullable("customPreviewBackground", customPreviewBackground); putNullable("contentView", contentView)
    }

    companion object {
        fun normalizedParent(value: String?): String {
            val raw = value?.trim().orEmpty()
            return if (raw.isEmpty() || raw in setOf("collection", "root", "null")) BookmerUrls.ROOT else raw
        }

        fun iconFor(url: String): String? {
            val host = runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: return null
            return "${BookmerUrls.API}/iconscollection/${Uri.encode(host)}"
        }

        fun bookmark(title: String, url: String, parent: String = BookmerUrls.ROOT, order: Int = 0) =
            BookmerItem(kind = ItemKind.BOOKMARK, title = title, targetUrl = url,
                parentId = parent, iconUrl = iconFor(url), order = order)

        fun folder(title: String, parent: String = BookmerUrls.ROOT, order: Int = 0) =
            BookmerItem(kind = ItemKind.FOLDER, title = title, parentId = parent,
                iconBackground = "#F2F2F7", order = order)

        fun fromJson(json: JSONObject): BookmerItem = BookmerItem(
            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
            kind = if (json.optString("kind").equals("folder", true)) ItemKind.FOLDER else ItemKind.BOOKMARK,
            title = json.optString("title", "Untitled"),
            targetUrl = json.nullableString("targetURL"),
            parentId = normalizedParent(json.optString("parentID", BookmerUrls.ROOT)),
            iconUrl = json.nullableString("iconURL"), iconBackground = json.nullableString("iconBackground"),
            iconX = json.optDouble("iconX", 0.0), iconY = json.optDouble("iconY", 0.0),
            iconZoom = json.optDouble("iconZoom", if (json.optString("kind") == "folder") 0.0 else -22.0),
            order = json.optInt("order"), updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            createdAt = json.optLong("createdAt", json.optLong("updatedAt", System.currentTimeMillis())),
            remoteId = json.nullableString("remoteID"), note = json.nullableString("note"),
            previewImage = json.nullableString("previewImage"), customPreviewImage = json.nullableString("customPreviewImage"),
            customPreviewX = json.nullableDouble("customPreviewX"), customPreviewY = json.nullableDouble("customPreviewY"),
            customPreviewZoom = json.nullableDouble("customPreviewZoom"),
            customPreviewBackground = json.nullableString("customPreviewBackground"),
            contentView = json.nullableString("contentView"),
        )
    }
}

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Bookmer",
    val url: String? = null,
    val isBookmerHome: Boolean = true,
    val prefersDesktopWebsite: Boolean = false,
    val pageZoom: Int = 100,
    val folderPath: List<String> = emptyList(),
    val lastVisitedAt: Long = System.currentTimeMillis(),
    val autoRefreshSeconds: Int = 0,
    val isPageTranslated: Boolean = false,
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("title", title); putNullable("url", url); put("isBookmerHome", isBookmerHome)
        put("prefersDesktopWebsite", prefersDesktopWebsite); put("pageZoom", pageZoom)
        put("folderPath", JSONArray(folderPath)); put("lastVisitedAt", lastVisitedAt)
        put("autoRefreshSeconds", autoRefreshSeconds)
    }

    companion object {
        fun fromJson(json: JSONObject) = BrowserTab(
            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = json.optString("title", "Bookmer"), url = json.nullableString("url"),
            isBookmerHome = json.optBoolean("isBookmerHome", true),
            prefersDesktopWebsite = json.optBoolean("prefersDesktopWebsite"),
            pageZoom = json.optInt("pageZoom", 100), folderPath = json.optJSONArray("folderPath").toStringList(),
            lastVisitedAt = json.optLong("lastVisitedAt", System.currentTimeMillis()),
            autoRefreshSeconds = json.optInt("autoRefreshSeconds"),
        )
    }
}

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis(),
) {
    fun toJson() = JSONObject().apply { put("id", id); put("url", url); put("title", title); put("visitedAt", visitedAt) }
    companion object {
        fun fromJson(json: JSONObject) = HistoryEntry(json.optString("id"), json.optString("url"),
            json.optString("title"), json.optLong("visitedAt"))
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ContentViewMode {
    DEFAULT, GRID, LIST, THUMBNAIL;

    val label: String get() = when (this) {
        DEFAULT -> "Default"
        GRID -> "Grid"
        LIST -> "List"
        THUMBNAIL -> "Thumbnail"
    }
}

/** Platform one-shot `Sort by` — rewrites `order`, then display stays custom. */
enum class SortMode {
    NAME_AZ, NAME_ZA, NEWEST, OLDEST;

    val label: String get() = when (this) {
        NAME_AZ -> "A → Z"
        NAME_ZA -> "Z → A"
        NEWEST -> "New → Old"
        OLDEST -> "Old → New"
    }
}
enum class StartNavigationAction {
    FOLDER_NAVIGATOR, TABS, TAB_HISTORY;

    val label: String get() = when (this) {
        FOLDER_NAVIGATOR -> "Folder Navigator"
        TABS -> "Tabs"
        TAB_HISTORY -> "Tab History"
    }
}

enum class WebNavigationAction {
    BACK, NAVIGATE, TABS, TAB_HISTORY;

    val label: String get() = when (this) {
        BACK -> "Back"
        NAVIGATE -> "Navigate"
        TABS -> "Tabs"
        TAB_HISTORY -> "Tab History"
    }
}

enum class ToolbarAction {
    FULL_SCREEN, READER, SEARCH, ZOOM, DESKTOP_VIEW;

    val label: String get() = when (this) {
        FULL_SCREEN -> "Full Screen"
        READER -> "Reader"
        SEARCH -> "Search"
        ZOOM -> "Zoom"
        DESKTOP_VIEW -> "Desktop View"
    }
}
enum class SearchEngine(val label: String, val template: String) {
    BING("Bing", "https://www.bing.com/search?q=@@@"),
    BRAVE("Brave", "https://search.brave.com/search?q=@@@"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=@@@"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=@@@"),
    GOOGLE("Google", "https://www.google.com/search?q=@@@"),
    KAGI("Kagi", "https://kagi.com/search?q=@@@"),
    PERPLEXITY("Perplexity", "https://www.perplexity.ai/search?q=@@@"),
    QWANT("Qwant", "https://www.qwant.com/?q=@@@"),
    STARTPAGE("Startpage", "https://www.startpage.com/sp/search?query=@@@"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=@@@"),
    YANDEX("Yandex", "https://yandex.com/search/?text=@@@");

    fun url(query: String) = template.replace("@@@", Uri.encode(query))

    /** Same host icons as Collection bookmarks (`/iconscollection/{host}`). */
    fun iconUrl(): String? = BookmerItem.iconFor(template.replace("@@@", "q"))
}

data class CustomSearchEngine(val id: String = UUID.randomUUID().toString(), val name: String, val template: String) {
    fun toJson() = JSONObject().apply { put("id", id); put("name", name); put("template", template) }
}

data class BlockedSite(val host: String, val redirectUrl: String? = null) {
    fun toJson() = JSONObject().apply { put("host", host); putNullable("redirectUrl", redirectUrl) }
}

data class LaunchShortcut(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val color: String = "#111112",
    val immersive: Boolean = false,
    val kind: LaunchShortcutKind = LaunchShortcutKind.WIDGET,
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("url", url); put("color", color)
        put("immersive", immersive); put("kind", kind.name)
    }

    val displayName: String
        get() = name.trim().ifBlank {
            when (kind) {
                LaunchShortcutKind.WIDGET -> "Widget"
                LaunchShortcutKind.CONTROL -> "Control"
            }
        }
}

enum class LaunchShortcutKind {
    WIDGET,
    CONTROL,
}

data class AppSettings(
    val setupCompleted: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val wallpaper: String? = null,
    val wallpaperBlur: Float = 0f,
    val wallpaperDim: Float = 0f,
    val wallpaperTextColor: String = "#111111",
    val hideTitles: Boolean = false,
    val hideToolbar: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val customSearchEngines: List<CustomSearchEngine> = emptyList(),
    val selectedCustomSearchEngineId: String? = null,
    val blockCookies: Boolean = false,
    val blockTrackers: Boolean = true,
    val blockPopups: Boolean = true,
    val blockAppBanners: Boolean = true,
    val blockYouTubeAds: Boolean = true,
    val blockedSites: List<BlockedSite> = emptyList(),
    val closeTabsAfterDays: Int = 0,
    val translateLanguage: String = "en",
    val metadataOs: String = "Default",
    val metadataLanguage: String = "Default",
    val metadataTimeZone: String = "Default",
    val desktopByDefault: Boolean = false,
    val shortcuts: List<LaunchShortcut> = emptyList(),
    val startNavigationAction: StartNavigationAction = StartNavigationAction.FOLDER_NAVIGATOR,
    val webNavigationAction: WebNavigationAction = WebNavigationAction.BACK,
    val toolbarAction: ToolbarAction = ToolbarAction.FULL_SCREEN,
    val openActionMenuOnLongPress: Boolean = false,
    val autoRefreshIntervals: List<Int> = listOf(2, 5, 10, 20, 30, 60),
    val collectionViewMode: ContentViewMode = ContentViewMode.GRID,
)

internal fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }
internal fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
internal fun JSONObject.nullableDouble(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key)
internal fun JSONArray?.toStringList(): List<String> = if (this == null) emptyList() else (0 until length()).map { optString(it) }
