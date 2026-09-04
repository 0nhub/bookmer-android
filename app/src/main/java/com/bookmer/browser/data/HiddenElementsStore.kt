package com.bookmer.browser.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class HiddenElementScope(val label: String, val footer: String) {
    PAGE("This page", "Only this URL path."),
    SITE("Similar on site", "Same kind of element on other pages of this website."),
}

data class HiddenElementRule(
    val id: String = UUID.randomUUID().toString(),
    val host: String,
    val scope: HiddenElementScope,
    val path: String? = null,
    val selector: String,
    val label: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class HideElementConfirmDraft(
    val suggestedTitle: String,
    val selector: String,
)

/** User-picked DOM hides (Safari-like Distraction Control). Persisted locally. */
class HiddenElementsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    var rules by mutableStateOf<List<HiddenElementRule>>(emptyList())
        private set

    init {
        load()
    }

    fun rulesMatching(url: String?): List<HiddenElementRule> {
        val host = normalizedHostFromUrl(url) ?: return emptyList()
        val path = normalizedPathFromUrl(url)
        return rules.filter { rule ->
            if (rule.host != host) return@filter false
            when (rule.scope) {
                HiddenElementScope.SITE -> true
                HiddenElementScope.PAGE -> (rule.path ?: "/") == path
            }
        }.sortedByDescending { it.createdAt }
    }

    fun hasRulesMatching(url: String?): Boolean = rulesMatching(url).isNotEmpty()

    fun add(url: String, scope: HiddenElementScope, selector: String, label: String): HiddenElementRule? {
        val host = normalizedHostFromUrl(url) ?: return null
        val trimmedSelector = selector.trim()
        if (trimmedSelector.isEmpty()) return null
        val path = if (scope == HiddenElementScope.PAGE) normalizedPathFromUrl(url) else null
        val cleanedLabel = label.trim().ifEmpty { "Hidden element" }
        val rule = HiddenElementRule(
            host = host,
            scope = scope,
            path = path,
            selector = trimmedSelector,
            label = cleanedLabel,
        )
        rules = listOf(rule) + rules.filterNot {
            it.host == rule.host && it.scope == rule.scope && it.path == rule.path && it.selector == rule.selector
        }
        save()
        return rule
    }

    fun rename(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        rules = rules.map { if (it.id == id) it.copy(label = trimmed) else it }
        save()
    }

    fun remove(id: String) {
        rules = rules.filterNot { it.id == id }
        save()
    }

    fun remove(ids: Set<String>) {
        if (ids.isEmpty()) return
        rules = rules.filterNot { it.id in ids }
        save()
    }

    fun removeRulesMatching(url: String?) {
        remove(rulesMatching(url).map { it.id }.toSet())
    }

    companion object {
        private const val PREFS = "bookmer.hiddenElements"
        private const val KEY = "v1"

        fun normalizedHostFromUrl(url: String?): String? {
            val host = SitePermissionStore.hostFromUrl(url)
            return host.takeIf { it.isNotEmpty() }
        }

        fun normalizedPathFromUrl(url: String?): String {
            var path = runCatching { Uri.parse(url).path }.getOrNull().orEmpty()
            if (path.isEmpty()) path = "/"
            if (path.length > 1 && path.endsWith("/")) path = path.dropLast(1)
            return path
        }
    }

    private fun load() {
        val raw = preferences.getString(KEY, null) ?: run {
            rules = emptyList()
            return
        }
        rules = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val scope = runCatching {
                        HiddenElementScope.valueOf(obj.optString("scope", "SITE").uppercase())
                    }.getOrDefault(HiddenElementScope.SITE)
                    add(
                        HiddenElementRule(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            host = obj.optString("host"),
                            scope = scope,
                            path = obj.optString("path").takeIf { it.isNotBlank() },
                            selector = obj.optString("selector"),
                            label = obj.optString("label").ifBlank { "Hidden element" },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save() {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(
                JSONObject().apply {
                    put("id", rule.id)
                    put("host", rule.host)
                    put("scope", rule.scope.name)
                    if (rule.path != null) put("path", rule.path) else put("path", JSONObject.NULL)
                    put("selector", rule.selector)
                    put("label", rule.label)
                    put("createdAt", rule.createdAt)
                },
            )
        }
        preferences.edit().putString(KEY, array.toString()).apply()
    }
}

object HideElementsTipStore {
    private const val PREFS = "bookmer.hideElementsTip"
    private const val KEY = "seen"

    fun hasSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}

object ImmersiveTipStore {
    private const val PREFS = "bookmer.immersiveTip"
    private const val KEY = "seen"

    fun hasSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}
