package com.bookmer.browser.data

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("bookmer.settings", Context.MODE_PRIVATE)
    var settings by mutableStateOf(load())
        private set

    fun update(block: (AppSettings) -> AppSettings) {
        settings = block(settings)
        preferences.edit().putString("settings.v2", settings.toJson().toString()).apply()
    }

    fun reset() {
        settings = AppSettings(setupCompleted = true)
        preferences.edit().putString("settings.v2", settings.toJson().toString()).apply()
    }

    private fun load(): AppSettings = preferences.getString("settings.v2", null)?.let {
        runCatching { appSettingsFromJson(JSONObject(it)) }.getOrNull()
    } ?: AppSettings()
}

data class BookmerSession(
    val token: String? = null,
    val email: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val accountType: String? = null,
    val hasPro: Boolean = false,
) {
    val isSignedIn: Boolean get() = !token.isNullOrBlank()
}

class SecureSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("bookmer.secure", Context.MODE_PRIVATE)
    private val cipher = AndroidKeyStoreCipher()
    var value by mutableStateOf(load())
        private set

    fun apply(token: String, email: String? = null, name: String? = null, avatarUrl: String? = null,
              accountType: String? = null, hasPro: Boolean = false) {
        value = BookmerSession(token, email, name, avatarUrl, accountType, hasPro)
        save(value)
    }

    fun updateProfile(email: String?, name: String?, avatarUrl: String?, accountType: String?, hasPro: Boolean) {
        val token = value.token ?: return
        apply(token, email ?: value.email, name ?: value.name, avatarUrl ?: value.avatarUrl,
            accountType ?: value.accountType, hasPro)
    }

    fun clear() {
        value = BookmerSession()
        preferences.edit().clear().apply()
    }

    private fun save(session: BookmerSession) {
        val json = JSONObject().apply {
            putNullable("token", session.token); putNullable("email", session.email); putNullable("name", session.name)
            putNullable("avatarUrl", session.avatarUrl); putNullable("accountType", session.accountType); put("hasPro", session.hasPro)
        }.toString()
        val encrypted = runCatching { cipher.encrypt(json) }.getOrElse { json }
        preferences.edit().putString("session.v1", encrypted).apply()
    }

    private fun load(): BookmerSession {
        val stored = preferences.getString("session.v1", null) ?: return BookmerSession()
        val json = runCatching { cipher.decrypt(stored) }.getOrElse { stored }
        return runCatching {
            JSONObject(json).let {
                BookmerSession(it.nullableString("token"), it.nullableString("email"), it.nullableString("name"),
                    it.nullableString("avatarUrl"), it.nullableString("accountType"), it.optBoolean("hasPro"))
            }
        }.getOrDefault(BookmerSession())
    }
}

private class AndroidKeyStoreCipher {
    private val alias = "bookmer.browser.session"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key(): SecretKey {
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(android.security.keystore.KeyGenParameterSpec.Builder(alias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    fun encrypt(clear: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(clear.toByteArray()), Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString()
    }
}

private fun AppSettings.toJson() = JSONObject().apply {
    put("setupCompleted", setupCompleted); put("theme", theme.name); putNullable("wallpaper", wallpaper)
    put("wallpaperBlur", wallpaperBlur.toDouble()); put("wallpaperDim", wallpaperDim.toDouble())
    put("wallpaperTextColor", wallpaperTextColor); put("hideTitles", hideTitles); put("hideToolbar", hideToolbar)
    put("searchEngine", searchEngine.name); put("customSearchEngines", JSONArray(customSearchEngines.map { it.toJson() }))
    putNullable("selectedCustomSearchEngineId", selectedCustomSearchEngineId)
    put("blockCookies", blockCookies); put("blockTrackers", blockTrackers); put("blockPopups", blockPopups)
    put("blockAppBanners", blockAppBanners); put("blockYouTubeAds", blockYouTubeAds)
    put("blockedSites", JSONArray(blockedSites.map { it.toJson() })); put("closeTabsAfterDays", closeTabsAfterDays)
    put("translateLanguage", translateLanguage); put("metadataOs", metadataOs)
    put("metadataLanguage", metadataLanguage); put("metadataTimeZone", metadataTimeZone)
    put("desktopByDefault", desktopByDefault)
    put("shortcuts", JSONArray(shortcuts.map { it.toJson() }))
    put("startNavigationAction", startNavigationAction.name); put("webNavigationAction", webNavigationAction.name)
    put("toolbarAction", toolbarAction.name); put("openActionMenuOnLongPress", openActionMenuOnLongPress)
    put("autoRefreshIntervals", JSONArray(autoRefreshIntervals))
    put("collectionViewMode", collectionViewMode.name)
}

private fun appSettingsFromJson(json: JSONObject): AppSettings {
    val custom = json.optJSONArray("customSearchEngines") ?: JSONArray()
    val blocked = json.optJSONArray("blockedSites") ?: JSONArray()
    val shortcuts = json.optJSONArray("shortcuts") ?: JSONArray()
    return AppSettings(
        setupCompleted = json.optBoolean("setupCompleted"),
        theme = runCatching { ThemeMode.valueOf(json.optString("theme", ThemeMode.SYSTEM.name)) }.getOrDefault(ThemeMode.SYSTEM),
        wallpaper = json.nullableString("wallpaper"), wallpaperBlur = json.optDouble("wallpaperBlur").toFloat(),
        wallpaperDim = json.optDouble("wallpaperDim").toFloat(), wallpaperTextColor = json.optString("wallpaperTextColor", "#111111"),
        hideTitles = json.optBoolean("hideTitles"), hideToolbar = json.optBoolean("hideToolbar", true),
        searchEngine = runCatching { SearchEngine.valueOf(json.optString("searchEngine", SearchEngine.DUCKDUCKGO.name)) }.getOrDefault(SearchEngine.DUCKDUCKGO),
        customSearchEngines = (0 until custom.length()).map { custom.getJSONObject(it) }.map {
            CustomSearchEngine(it.optString("id"), it.optString("name"), it.optString("template"))
        },
        selectedCustomSearchEngineId = json.nullableString("selectedCustomSearchEngineId"),
        blockCookies = json.optBoolean("blockCookies"), blockTrackers = json.optBoolean("blockTrackers", true),
        blockPopups = json.optBoolean("blockPopups", true), blockAppBanners = json.optBoolean("blockAppBanners", true),
        blockYouTubeAds = json.optBoolean("blockYouTubeAds", true),
        blockedSites = (0 until blocked.length()).map { blocked.getJSONObject(it) }.map {
            BlockedSite(it.optString("host"), it.nullableString("redirectUrl"))
        },
        closeTabsAfterDays = json.optInt("closeTabsAfterDays"), translateLanguage = json.optString("translateLanguage", "en"),
        metadataOs = json.optString("metadataOs", "Default"), metadataLanguage = json.optString("metadataLanguage", "Default"),
        metadataTimeZone = json.optString("metadataTimeZone", "Default"), desktopByDefault = json.optBoolean("desktopByDefault"),
        shortcuts = (0 until shortcuts.length()).map { shortcuts.getJSONObject(it) }.map {
            val kind = runCatching {
                LaunchShortcutKind.valueOf(it.optString("kind", LaunchShortcutKind.WIDGET.name))
            }.getOrDefault(LaunchShortcutKind.WIDGET)
            LaunchShortcut(
                id = it.optString("id"),
                name = it.optString("name"),
                url = it.optString("url"),
                color = it.optString("color", "#111112"),
                immersive = it.optBoolean("immersive"),
                kind = kind,
            )
        },
        startNavigationAction = runCatching { StartNavigationAction.valueOf(json.optString("startNavigationAction", StartNavigationAction.FOLDER_NAVIGATOR.name)) }.getOrDefault(StartNavigationAction.FOLDER_NAVIGATOR),
        webNavigationAction = runCatching { WebNavigationAction.valueOf(json.optString("webNavigationAction", WebNavigationAction.BACK.name)) }.getOrDefault(WebNavigationAction.BACK),
        toolbarAction = runCatching { ToolbarAction.valueOf(json.optString("toolbarAction", ToolbarAction.FULL_SCREEN.name)) }.getOrDefault(ToolbarAction.FULL_SCREEN),
        openActionMenuOnLongPress = json.optBoolean("openActionMenuOnLongPress"),
        autoRefreshIntervals = run {
            val arr = json.optJSONArray("autoRefreshIntervals")
            if (arr == null || arr.length() == 0) listOf(2, 5, 10, 20, 30, 60)
            else (0 until arr.length()).mapNotNull { i ->
                arr.optInt(i, -1).takeIf { it in 1..(24 * 60 * 60) }
            }.distinct().sorted().ifEmpty { listOf(2, 5, 10, 20, 30, 60) }
        },
        collectionViewMode = runCatching { ContentViewMode.valueOf(json.optString("collectionViewMode", ContentViewMode.GRID.name)) }.getOrDefault(ContentViewMode.GRID),
    )
}
