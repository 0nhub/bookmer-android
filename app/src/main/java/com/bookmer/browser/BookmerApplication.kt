package com.bookmer.browser

import android.app.Application
import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import com.bookmer.browser.data.AliasStore
import com.bookmer.browser.data.BookmarkRepository
import com.bookmer.browser.data.BookmerApiClient
import com.bookmer.browser.data.BookmerProStore
import com.bookmer.browser.data.BookmerSyncService
import com.bookmer.browser.data.HiddenElementsStore
import com.bookmer.browser.data.HistoryRepository
import com.bookmer.browser.data.PreferencesRepository
import com.bookmer.browser.data.SecureSessionStore
import com.bookmer.browser.data.SitePermissionStore
import com.bookmer.browser.data.TabPreviewStore

class BookmerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BookmerServices.initialize(this)
        TabPreviewStore.init(this)
        WebView.setWebContentsDebuggingEnabled((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
    }
}

@SuppressLint("StaticFieldLeak")
object BookmerServices {
    lateinit var bookmarks: BookmarkRepository
        private set
    lateinit var history: HistoryRepository
        private set
    lateinit var preferences: PreferencesRepository
        private set
    lateinit var session: SecureSessionStore
        private set
    lateinit var api: BookmerApiClient
        private set
    lateinit var sync: BookmerSyncService
        private set
    lateinit var sitePermissions: SitePermissionStore
        private set
    lateinit var hiddenElements: HiddenElementsStore
        private set
    lateinit var alias: AliasStore
        private set
    lateinit var pro: BookmerProStore
        private set

    fun initialize(application: Application) {
        if (::bookmarks.isInitialized) return
        preferences = PreferencesRepository(application)
        bookmarks = BookmarkRepository(application)
        history = HistoryRepository(application)
        session = SecureSessionStore(application)
        sitePermissions = SitePermissionStore(application)
        hiddenElements = HiddenElementsStore(application)
        alias = AliasStore(application)
        alias.migrateFromLegacySettings(preferences.settings)
        api = BookmerApiClient()
        sync = BookmerSyncService(api, bookmarks, session)
        pro = BookmerProStore(application, api, session)
        pro.start()
        com.bookmer.browser.integration.ShortcutPublisher.publish(application)
    }
}
