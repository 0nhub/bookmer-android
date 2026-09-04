# Kotlin file index

Root package: `com.bookmer.browser`  
Path prefix: `app/src/main/java/com/bookmer/browser/`

| File | Responsibility |
|------|----------------|
| `BookmerApplication.kt` | Application; `BookmerServices` DI-less init |
| `MainActivity.kt` | Compose host; WebView permission + file chooser hosts |
| `browser/AddressResolver.kt` | Address bar → URL or search engine URL |
| `browser/BrowserViewModel.kt` | Tabs, overlays, nav, translate, immersive, downloads, collect, previews |
| `browser/BrowserWebView.kt` | WebView Compose, clients, JS bridges, UA/desktop |
| `browser/HideElementsScript.kt` | JS injected for element picker / hide |
| `data/AliasStore.kt` | Spoof OS/language/TZ/geo metadata |
| `data/BookmerApiClient.kt` | HTTP API + `BookmerSyncService` |
| `data/BookmerIconUrl.kt` | Resolve Collection icon/wallpaper URLs |
| `data/BookmerProStore.kt` | Google Play Billing + `/pay/google` |
| `data/HiddenElementsStore.kt` | Persist hide rules; tip stores |
| `data/LaunchShortcutStore.kt` | Widget / control tile bindings |
| `data/Models.kt` | URLs, items, tabs, settings enums |
| `data/PreferencesRepository.kt` | `AppSettings` + `SecureSessionStore` |
| `data/Repositories.kt` | Bookmarks + history + guest seed |
| `data/SitePermissions.kt` | Per-site camera/mic/location policies; page actions helpers |
| `data/TabPreviewStore.kt` | Disk cache for tab thumbnails |
| `integration/AndroidIntegrations.kt` | Share, widgets, QS tiles, deep links, shortcuts |
| `ui/BookmerApp.kt` | Theme shell, overlay routing, chrome visibility |
| `ui/BrowserChrome.kt` | Address bar, toolbars, nested menus, find, immersive tip |
| `ui/CollectionScreen.kt` | Collection home grid |
| `ui/Common.kt` | `RemoteImage` (+ auth), color helpers |
| `ui/Dialogs.kt` | Setup welcome, blocked page, shared dialogs |
| `ui/HideElementsUi.kt` | Pick bar, confirm, manage, settings section |
| `ui/LoginWebSheet.kt` | Web login + token/cookie bridge |
| `ui/ModernMenu.kt` | Shared overflow menu primitives |
| `ui/Overlays.kt` | History, tab history, downloads, navigate, bookmark tools |
| `ui/ReaderScreen.kt` | Reader mode |
| `ui/SettingsScreen.kt` | Full settings UI |
| `ui/TabsSwitcherScreen.kt` | Tab manager deck |
| `ui/theme/Color.kt` | Color tokens |
| `ui/theme/Theme.kt` | Material schemes + `bookmerIsDarkTheme()` |
| `ui/theme/Type.kt` | Typography |

## Notable non-Kotlin assets

| Path | Purpose |
|------|---------|
| `app/src/main/assets/global_list.json` | Guest Collection seed |
| `app/src/main/AndroidManifest.xml` | Permissions, activities, widget, tiles, deep links |
| `app/src/main/res/` | Launcher, widget layouts, themes, strings |
| `gradle/libs.versions.toml` | Dependency versions (AGP, Compose BOM, Billing, …) |
| `keystore.properties` | **Local only — never commit** |
| `local.properties` | **SDK path — never commit** |
