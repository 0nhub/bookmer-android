package com.bookmer.browser.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject

enum class SitePermissionKind(val label: String) {
    CAMERA("Camera"),
    MICROPHONE("Microphone"),
    LOCATION("Location"),
}

enum class SitePermissionPolicy(val label: String) {
    ASK("Ask"),
    DENY("Deny"),
    ALLOW("Allow"),
}

/** Per-site Camera / Microphone / Location (Page menu). Default Ask. */
class SitePermissionStore(context: Context) {
    private val preferences = context.getSharedPreferences("bookmer.sitePermissions", Context.MODE_PRIVATE)
    private var policies: MutableMap<String, MutableMap<String, String>> = load()

    fun policy(kind: SitePermissionKind, host: String): SitePermissionPolicy {
        val site = normalizedHost(host)
        val raw = policies[site]?.get(kind.name.lowercase()) ?: return SitePermissionPolicy.ASK
        return runCatching { SitePermissionPolicy.valueOf(raw.uppercase()) }.getOrDefault(SitePermissionPolicy.ASK)
    }

    fun set(policy: SitePermissionPolicy, kind: SitePermissionKind, host: String) {
        val site = normalizedHost(host)
        if (site.isEmpty()) return
        val sitePolicies = policies.getOrPut(site) { mutableMapOf() }
        sitePolicies[kind.name.lowercase()] = policy.name.lowercase()
        persist()
    }

    fun decision(kind: SitePermissionKind, host: String): SitePermissionPolicy = policy(kind, host)

    companion object {
        fun normalizedHost(value: String?): String {
            var host = value?.lowercase()?.trim().orEmpty()
            if (host.startsWith(".")) host = host.drop(1)
            if (host.startsWith("www.")) host = host.drop(4)
            return host
        }

        fun hostFromUrl(url: String?): String = normalizedHost(runCatching { Uri.parse(url).host }.getOrNull())
    }

    private fun load(): MutableMap<String, MutableMap<String, String>> {
        val raw = preferences.getString("v1", null) ?: return mutableMapOf()
        return runCatching {
            val root = JSONObject(raw)
            val map = mutableMapOf<String, MutableMap<String, String>>()
            root.keys().forEach { host ->
                val site = root.optJSONObject(host) ?: return@forEach
                val policies = mutableMapOf<String, String>()
                site.keys().forEach { key -> policies[key] = site.optString(key) }
                map[host] = policies
            }
            map
        }.getOrDefault(mutableMapOf())
    }

    private fun persist() {
        val root = JSONObject()
        policies.forEach { (host, site) ->
            root.put(host, JSONObject().apply { site.forEach { (k, v) -> put(k, v) } })
        }
        preferences.edit().putString("v1", root.toString()).apply()
    }
}

object BookmerPageActions {
    fun webArchiveUrl(pageUrl: String): String? {
        if (!pageUrl.startsWith("http")) return null
        return "https://web.archive.org/web/*/$pageUrl"
    }

    fun reportMailto(pageUrl: String): String {
        val host = SitePermissionStore.hostFromUrl(pageUrl).ifBlank { pageUrl }
        return "mailto:support@mail.bookmer.com?subject=${Uri.encode("Report $host")}"
    }

    fun pdfFilename(title: String, host: String): String {
        val raw = title.trim()
        val base = if (raw.isEmpty() || raw == "Bookmer") host else raw
        val cleaned = base.replace('/', '-').replace(':', '-').trim()
        return "${cleaned.ifBlank { "Page" }}.pdf"
    }
}
