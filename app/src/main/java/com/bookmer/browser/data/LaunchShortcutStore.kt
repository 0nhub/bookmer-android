package com.bookmer.browser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads launch shortcuts from the same prefs blob as [PreferencesRepository]
 * so widgets / Quick Settings tiles work without a live ViewModel.
 */
object LaunchShortcutStore {
    private const val PREFS = "bookmer.settings"
    private const val KEY = "settings.v2"

    fun all(context: Context): List<LaunchShortcut> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        val array = JSONObject(raw).optJSONArray("shortcuts") ?: JSONArray()
        (0 until array.length()).mapNotNull { index ->
            val o = array.optJSONObject(index) ?: return@mapNotNull null
            val kind = runCatching {
                LaunchShortcutKind.valueOf(o.optString("kind", LaunchShortcutKind.WIDGET.name))
            }.getOrDefault(LaunchShortcutKind.WIDGET)
            LaunchShortcut(
                id = o.optString("id"),
                name = o.optString("name"),
                url = o.optString("url"),
                color = o.optString("color", "#111112"),
                immersive = o.optBoolean("immersive"),
                kind = kind,
            ).takeIf { it.id.isNotBlank() && it.url.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    fun ofKind(context: Context, kind: LaunchShortcutKind): List<LaunchShortcut> =
        all(context).filter { it.kind == kind }

    fun byId(context: Context, id: String?): LaunchShortcut? {
        if (id.isNullOrBlank()) return null
        return all(context).firstOrNull { it.id == id }
    }
}

/** Per home-screen widget instance → which widget-shortcut it opens. */
object WidgetShortcutBindings {
    private const val PREFS = "bookmer.widget.bindings"

    fun get(context: Context, appWidgetId: Int): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(appWidgetId.toString(), null)

    fun set(context: Context, appWidgetId: Int, shortcutId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(appWidgetId.toString(), shortcutId).apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(appWidgetId.toString()).apply()
    }
}

/** Quick Settings tile slot → control-shortcut id (Android Control Center analogue). */
object ControlTileBindings {
    private const val PREFS = "bookmer.control.tiles"

    fun get(context: Context, slot: Int): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("slot.$slot", null)

    fun set(context: Context, slot: Int, shortcutId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("slot.$slot", shortcutId).apply()
    }

    fun resolve(context: Context, slot: Int): LaunchShortcut? {
        val bound = byId(context, get(context, slot))
        if (bound != null && bound.kind == LaunchShortcutKind.CONTROL) return bound
        return LaunchShortcutStore.ofKind(context, LaunchShortcutKind.CONTROL).getOrNull(slot)
    }

    private fun byId(context: Context, id: String?) = LaunchShortcutStore.byId(context, id)
}
