# Kotlin-Dateiindex

Root-Package: `com.bookmer.browser`  
Pfad-Präfix: `app/src/main/java/com/bookmer/browser/`

| Datei | Aufgabe |
|-------|---------|
| `BookmerApplication.kt` | Application; `BookmerServices`-Init |
| `MainActivity.kt` | Compose-Host; WebView-Permissions + File-Chooser |
| `browser/AddressResolver.kt` | Adresszeile → URL oder Suchmaschinen-URL |
| `browser/BrowserViewModel.kt` | Tabs, Overlays, Nav, Translate, Immersive, Downloads, Collect, Previews |
| `browser/BrowserWebView.kt` | WebView-Compose, Clients, JS-Bridges, UA/Desktop |
| `browser/HideElementsScript.kt` | JS für Element-Picker / Hide |
| `data/AliasStore.kt` | Spoof OS/Sprache/TZ/Geo |
| `data/BookmerApiClient.kt` | HTTP-API + `BookmerSyncService` |
| `data/BookmerIconUrl.kt` | Collection-Icon-/Wallpaper-URLs auflösen |
| `data/BookmerProStore.kt` | Google Play Billing + `/pay/google` |
| `data/HiddenElementsStore.kt` | Hide-Regeln; Tip-Stores |
| `data/LaunchShortcutStore.kt` | Widget- / Control-Tile-Bindings |
| `data/Models.kt` | URLs, Items, Tabs, Settings-Enums |
| `data/PreferencesRepository.kt` | `AppSettings` + `SecureSessionStore` |
| `data/Repositories.kt` | Bookmarks + History + Guest-Seed |
| `data/SitePermissions.kt` | Kamera/Mic/Ort pro Site; Page-Action-Helfer |
| `data/TabPreviewStore.kt` | Disk-Cache für Tab-Thumbnails |
| `integration/AndroidIntegrations.kt` | Share, Widgets, QS-Tiles, Deep Links, Shortcuts |
| `ui/BookmerApp.kt` | Theme-Shell, Overlay-Routing, Chrome-Sichtbarkeit |
| `ui/BrowserChrome.kt` | Adressleiste, Toolbars, verschachtelte Menüs, Find, Immersive-Tip |
| `ui/CollectionScreen.kt` | Collection-Home-Grid |
| `ui/Common.kt` | `RemoteImage` (+ Auth), Farbhelfer |
| `ui/Dialogs.kt` | Setup-Welcome, Blocked Page, Dialoge |
| `ui/HideElementsUi.kt` | Pick-Bar, Confirm, Manage, Settings-Abschnitt |
| `ui/LoginWebSheet.kt` | Web-Login + Token/Cookie-Bridge |
| `ui/ModernMenu.kt` | Gemeinsame Overflow-Menü-Primitives |
| `ui/Overlays.kt` | History, Tab History, Downloads, Navigate, Bookmark-Tools |
| `ui/ReaderScreen.kt` | Reader Mode |
| `ui/SettingsScreen.kt` | Settings-UI |
| `ui/TabsSwitcherScreen.kt` | Tab-Manager-Deck |
| `ui/theme/Color.kt` | Farbtokens |
| `ui/theme/Theme.kt` | Material-Schemes + `bookmerIsDarkTheme()` |
| `ui/theme/Type.kt` | Typografie |

## Weitere Assets

| Pfad | Zweck |
|------|-------|
| `app/src/main/assets/global_list.json` | Guest-Collection-Seed |
| `app/src/main/AndroidManifest.xml` | Permissions, Activities, Widget, Tiles, Deep Links |
| `app/src/main/res/` | Launcher, Widget-Layouts, Themes, Strings |
| `gradle/libs.versions.toml` | Dependency-Versionen |
| `keystore.properties` | **Nur lokal — nie committen** |
| `local.properties` | **SDK-Pfad — nie committen** |
