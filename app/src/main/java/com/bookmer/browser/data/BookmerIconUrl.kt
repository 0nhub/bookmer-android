package com.bookmer.browser.data

import android.net.Uri
import java.util.regex.Pattern

/**
 * Platform / iOS [BookmerIconURL] parity — resolve Collection icon & wallpaper asset URLs.
 */
object BookmerIconUrl {
    private val mongoNavIcon = Pattern.compile("^[0-9a-f]{24}/[^/]+$", Pattern.CASE_INSENSITIVE)

    fun resolveBookmarkIcon(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.length <= 1) return null
        if (value.startsWith("native-emoji:", ignoreCase = true)) return null
        if (value.startsWith("bookmer-custom:") || value.startsWith("file:") ||
            value.startsWith("content:") || value.startsWith("data:") || value.startsWith("blob:")
        ) {
            return value
        }
        if (value.startsWith("//")) return "https:$value"

        legacyIconsCollectionSlug(value)?.let {
            return "${BookmerUrls.API}/iconscollection/$it"
        }

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return rewriteAbsoluteApiAsset(value) ?: value
        }

        when {
            value.startsWith("/api/bookmer-backend/") ->
                return "${BookmerUrls.API}${value.removePrefix("/api/bookmer-backend")}"
            value.startsWith("api/bookmer-backend/") ->
                return "${BookmerUrls.API}/${value.removePrefix("api/bookmer-backend/")}"
            value.startsWith("/iconscollection/") || value.startsWith("/favicon-cache/") ||
                value.startsWith("/icons/") || value.startsWith("/user/picture/") ||
                value.startsWith("/user/wallpaper/") || value.startsWith("/object/navigation_icons/") ||
                value.startsWith("/link-meta/") ->
                return "${BookmerUrls.API}$value"
            value.startsWith("iconscollection/") || value.startsWith("favicon-cache/") ||
                value.startsWith("icons/") ->
                return "${BookmerUrls.API}/$value"
            value.startsWith("/imgs/folders_icons") ->
                return "https://bookmer.com$value"
            value.startsWith("/") ->
                return "${BookmerUrls.API}$value"
            mongoNavIcon.matcher(value).matches() ->
                return "${BookmerUrls.API}/object/navigation_icons/$value"
            value.contains("/") ->
                return "${BookmerUrls.API}/$value"
            else ->
                return "${BookmerUrls.API}/icons/${Uri.encode(value)}"
        }
    }

    fun resolveFolderIcon(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (nativeEmoji(value) != null) return null
        if (value.startsWith("bookmer-custom:") || value.startsWith("file:") || value.startsWith("content:")) {
            return value
        }
        if (value.startsWith("//")) return "https:$value"
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return rewriteAbsoluteApiAsset(value) ?: value
        }
        when {
            value.startsWith("/icons/") || value.startsWith("icons/") -> {
                val path = if (value.startsWith("/")) value else "/$value"
                return "${BookmerUrls.API}$path"
            }
            value.startsWith("/object/navigation_icons/") -> return "${BookmerUrls.API}$value"
            value.startsWith("/imgs/folders_icons") -> return "https://bookmer.com$value"
            value.startsWith("/") -> return "${BookmerUrls.API}$value"
            else -> return "${BookmerUrls.API}/object/navigation_icons/$value"
        }
    }

    fun nativeEmoji(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val prefix = "native-emoji:"
        if (value.startsWith(prefix, ignoreCase = true)) {
            val decoded = Uri.decode(value.substring(prefix.length)).trim()
            return decoded.takeIf { it.isNotEmpty() }
        }
        if (value.contains("/") || value.contains(".") || value.startsWith("http")) return null
        if (value.toByteArray(Charsets.UTF_8).size > 16) return null
        val hasEmoji = value.codePoints().anyMatch { cp ->
            Character.getType(cp) == Character.OTHER_SYMBOL.toInt() ||
                (cp in 0x1F300..0x1FAFF) || (cp in 0x2600..0x27BF)
        }
        return if (hasEmoji) value else null
    }

    /** Candidate URLs for a Collection tile (auth-aware loader tries in order). */
    fun candidates(item: BookmerItem): List<String> {
        val out = linkedSetOf<String>()
        fun add(url: String?) {
            val resolved = url?.trim()?.takeIf { it.isNotEmpty() } ?: return
            out += resolved
        }
        if (item.kind == ItemKind.FOLDER) {
            if (nativeEmoji(item.iconUrl) == null) {
                add(resolveFolderIcon(item.iconUrl))
                add(resolveBookmarkIcon(item.iconUrl))
            }
        } else {
            add(resolveBookmarkIcon(item.iconUrl))
            add(BookmerItem.iconFor(item.targetUrl.orEmpty()))
        }
        return out.toList()
    }

    fun displayUrl(item: BookmerItem): String? = candidates(item).firstOrNull()

    /** iOS: api.bookmer.com assets need Bearer; public bookmer.com `/imgs/` must not. */
    fun needsAuthentication(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (!host.contains("bookmer.com")) return false
        if (host == "api.bookmer.com" || host.endsWith(".api.bookmer.com")) return true
        val path = uri.path.orEmpty()
        return path.startsWith("/api/bookmer-backend/") ||
            path.startsWith("/object/navigation_icons/") ||
            path.startsWith("/icons/") ||
            path.startsWith("/iconscollection/") ||
            path.startsWith("/favicon-cache/") ||
            path.startsWith("/user/picture/") ||
            path.startsWith("/user/wallpaper/")
    }

    fun resolveAny(raw: String?): String? =
        resolveBookmarkIcon(raw) ?: resolveFolderIcon(raw)

    private fun legacyIconsCollectionSlug(raw: String): String? {
        // e.g. "google.com.png" leftovers from older clients
        if (raw.contains("/") || raw.startsWith("http")) return null
        if (!raw.contains('.') || raw.length > 80) return null
        val slug = raw.removeSuffix(".png").removeSuffix(".PNG")
        return slug.takeIf { it.contains('.') && !it.contains(' ') }
            ?.let { Uri.encode(it) }
    }

    private fun rewriteAbsoluteApiAsset(raw: String): String? {
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val path = uri.path.orEmpty()
        val query = uri.query?.let { "?$it" }.orEmpty()
        val keep = listOf(
            "/icons/", "/iconscollection/", "/favicon-cache/",
            "/user/picture/", "/user/wallpaper/", "/object/navigation_icons/", "/link-meta/",
        )
        if (path.contains("/link-meta/preview-file/") || keep.any { path.startsWith(it) }) {
            return "${BookmerUrls.API}$path$query"
        }
        if (path.startsWith("/imgs/folders_icons")) {
            return "https://bookmer.com$path$query"
        }
        if (path.contains("/api/bookmer-backend/")) {
            val stripped = path.replace("/api/bookmer-backend", "")
            return "${BookmerUrls.API}$stripped$query"
        }
        return null
    }
}
