# Architektur — Bookmer Browser (Android)

Begleitdokument zu [`AGENTS.md`](../AGENTS.md). Zuerst dort lesen.

## Stack

| Schicht | Wahl |
|---------|------|
| Sprache | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Browser-Engine | Android `WebView` (nicht Custom Tabs als Shell) |
| Min / Target SDK | 24 / 37 |
| App-ID | `com.bookmer.browser` |
| Version | siehe `app/build.gradle.kts` (`versionName` / `versionCode`) |
| Billing | Google Play Billing (`billing-ktx`) |
| SVG-Icons | AndroidSVG |

## Prozessmodell

```
BookmerApplication.onCreate()
  → BookmerServices.initialize()
       bookmarks, history, preferences, session,
       sitePermissions, hiddenElements, alias,
       api, sync, pro
  → TabPreviewStore.init()
  → WebView-Debugging falls debuggable

MainActivity (Compose-Host)
  → BrowserViewModel
  → BookmerApp()
```

`BookmerServices` ist ein Prozess-Singleton (kein DI-Framework). UI und ViewModel greifen direkt darauf zu.

## Schichten (nicht umkehren)

| Schicht | Besitzt | Darf nicht besitzen |
|---------|---------|---------------------|
| `ui/*` | Compose-Screens, Menüs, Gesten | HTTP, Datei-I/O für die Library |
| `browser/BrowserViewModel` | Tabs, Overlays, Navigationsbefehle, transienter UI-Zustand | Roh-SharedPreferences / API-Wire-Format |
| `data/*` | Modelle, JSON-Persistenz, API, Sync, Billing, Permission-Stores | Compose |
| `integration/*` | Widgets, Share-Target, QS-Tiles, Deep Links | Collection-Grid-Layout |

## Start-URL / Home

- Logische Home: `bookmer://collection` (`BookmerUrls.HOME`).
- Bei `BrowserTab.isBookmerHome == true` zeigt die Shell **`CollectionScreen`**, kein WebView von bookmer.com.
- WebView nur für vom Nutzer geöffnete `http(s)`-Seiten (und Spezialfälle).

## Tab-Modell

Wichtige `BrowserTab`-Felder:

- `isBookmerHome` — Collection vs. Web
- `url` — aktuelle Seite im Web
- `folderPath` — Collection-Ordnerstapel dieses Tabs
- `prefersDesktopWebsite`, `pageZoom`, `autoRefreshSeconds`, `isPageTranslated`

Tabs liegen in `BrowserViewModel`; Previews als Bitmaps auf Disk über `TabPreviewStore` (Capture **vor** Öffnen des Tab-Overlays).

## Chrome-Sichtbarkeit

Gesteuert in `BookmerApp` + `BrowserChrome`:

1. **Scroll hide/show** — nach unten scrollen klappt Chrome ein; nach oben wieder aus (wenn Setting `hideToolbar` das erlaubt).
2. **Swipe-down Sticky** — Wischen nach unten auf der Adressleiste → eingeklappter Sticky-Streifen (`n/m`); Tippen stellt wieder her.
3. **Immersive / Vollbild** — getrennt vom Sticky-Collapse (`enterImmersive` / `ImmersiveExit`).

## Theme

- App-Einstellung: `ThemeMode` = SYSTEM | LIGHT | DARK (`AppSettings.theme`).
- UI muss **`bookmerIsDarkTheme()`** nutzen (`ui/theme/Theme.kt`), **nicht** roh `isSystemInDarkTheme()`, damit Light hell bleibt, auch wenn das OS dunkel ist.
- Collection-Wallpaper kann Label-Farben über `wallpaperTextColor` überschreiben.

## Persistenz (local-first)

| Store | Klasse | Zweck |
|-------|--------|-------|
| Bookmarks / Ordner | `BookmarkRepository` | Collection-Baum |
| History | `HistoryRepository` | Globaler Besucherverlauf |
| Settings | `PreferencesRepository` | `AppSettings` |
| Session | `SecureSessionStore` | Token + Profil + `hasPro` |
| Site-Permissions | `SitePermissionStore` | Kamera / Mic / Ort |
| Hidden Elements | `HiddenElementsStore` | CSS-Hide-Regeln |
| Alias | `AliasStore` | Spoof Land/Sprache/TZ/UA |
| Tab-Previews | `TabPreviewStore` | Vorschaubilder |
| Shortcuts | `LaunchShortcutStore` + settings.shortcuts | Widget- / QS-Bindings |

Guest-Collection-Seed: Asset `app/src/main/assets/global_list.json` (Platform-Default-Liste).

## Verwandte Bäume

| Baum | Rolle |
|------|-------|
| `Bookmer/Code/bookmer-platform` | Quelle für Collection-Optik + API-Semantik |
| `Bookmer/Code/browser/iOS` | Verhaltens-/Menü-Parität |
| Backend `api.bookmer.com` | `/object`, `/user`, `/pay/google`, Icons, Wallpapers |

Collection-Layout-Tokens nicht erfinden — zuerst Platform-SCSS / iOS.
