package com.bookmer.browser.browser

import com.bookmer.browser.data.AppSettings
import com.bookmer.browser.data.BookmerUrls
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object AddressResolver {
    fun resolve(raw: String, settings: AppSettings): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (text == BookmerUrls.HOME) return text
        val scheme = runCatching { URI(text).scheme?.lowercase() }.getOrNull()
        if (scheme == "http" || scheme == "https") return text
        if (!text.contains(' ') && (text.contains('.') || text.startsWith("localhost"))) return "https://$text"
        val encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.name()).replace("+", "%20")
        val custom = settings.customSearchEngines.firstOrNull { it.id == settings.selectedCustomSearchEngineId }
        return (custom?.template ?: settings.searchEngine.template).replace("@@@", encoded)
    }
}
