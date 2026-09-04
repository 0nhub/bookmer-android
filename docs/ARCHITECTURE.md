# Architecture — Bookmer Browser (Android)

Companion to root [`AGENTS.md`](../AGENTS.md). Read that first for the file map and hard rules.

## Stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Browser engine | Android `WebView` (not Chromium Custom Tabs as the shell) |
| Min / target SDK | 24 / 37 |
| App ID | `com.bookmer.browser` |
| Version | see `app/build.gradle.kts` (`versionName` / `versionCode`) |
| Billing | Google Play Billing Library (`billing-ktx`) |
| SVG icons | AndroidSVG |

## Process model

```
BookmerApplication.onCreate()
  → BookmerServices.initialize()
       bookmarks, history, preferences, session,
       sitePermissions, hiddenElements, alias,
       api, sync, pro
  → TabPreviewStore.init()
  → WebView debugging if debuggable

MainActivity (Compose host)
  → BrowserViewModel
  → BookmerApp()
```

`BookmerServices` is a process-wide singleton (not a DI framework). UI and ViewModel read it directly.

## Layering (do not invert)

| Layer | Owns | Must not own |
|-------|------|----------------|
| `ui/*` | Compose screens, menus, gestures | HTTP, file I/O for library |
| `browser/BrowserViewModel` | Tabs, overlays, navigation commands, transient UI flags | Raw SharedPreferences / API wire format |
| `data/*` | Models, JSON persistence, API, sync, billing, permissions stores | Compose |
| `integration/*` | Widgets, share target, QS tiles, deep links | Collection grid layout |

## Start URL / home

- Logical home: `bookmer://collection` (`BookmerUrls.HOME`).
- When `BrowserTab.isBookmerHome == true`, the shell shows **`CollectionScreen`**, not a WebView of bookmer.com.
- WebView is only for user-opened `http(s)` pages (and some special navigations).

## Tab model

`BrowserTab` fields that matter:

- `isBookmerHome` — Collection vs web
- `url` — current page when web
- `folderPath` — Collection folder stack for that tab’s home context
- `prefersDesktopWebsite`, `pageZoom`, `autoRefreshSeconds`, `isPageTranslated`

Tabs are held in `BrowserViewModel`; previews are bitmaps on disk via `TabPreviewStore` (capture **before** opening the tabs overlay).

## Chrome visibility

Controlled in `BookmerApp` + `BrowserChrome`:

1. **Scroll hide/show** — scrolling down collapses chrome; scrolling up expands (when `hideToolbar` setting allows).
2. **Swipe-down sticky** — swipe down on the address chrome → sticky collapsed strip (`n/m` tab indicator); tap restores.
3. **Immersive / full screen** — separate from sticky collapse (`enterImmersive` / `ImmersiveExit`).

## Theme

- App preference: `ThemeMode` = SYSTEM | LIGHT | DARK (`AppSettings.theme`).
- UI must use **`bookmerIsDarkTheme()`** (in `ui/theme/Theme.kt`), **not** raw `isSystemInDarkTheme()`, so Light mode stays light even when the OS is dark.
- Collection wallpaper can override label colors via `wallpaperTextColor`.

## Persistence files (local-first)

Typical app-private JSON (names are illustrative of intent; see repository classes):

| Store | Class | Purpose |
|-------|-------|---------|
| Bookmarks / folders | `BookmarkRepository` | Collection tree |
| History | `HistoryRepository` | Global visit history |
| Settings | `PreferencesRepository` | `AppSettings` |
| Session | `SecureSessionStore` | Token + profile + `hasPro` |
| Site permissions | `SitePermissionStore` | Camera / mic / location policies |
| Hidden elements | `HiddenElementsStore` | CSS hide rules |
| Alias | `AliasStore` | Spoof country/language/TZ/UA |
| Tab previews | `TabPreviewStore` | Preview images |
| Shortcuts | `LaunchShortcutStore` + settings.shortcuts | Widget / QS tile bindings |

Guest Collection seed: asset `app/src/main/assets/global_list.json` (platform `GET /data/default` snapshot).

## Related trees (sibling products)

| Tree | Role |
|------|------|
| `Bookmer/Code/bookmer-platform` | Source of truth for Collection visuals + API semantics |
| `Bookmer/Code/browser/iOS` | Behavioral / menu parity for the native browser |
| Backend `api.bookmer.com` | `/object`, `/user`, `/pay/google`, icons, wallpapers |

Do not invent Collection layout tokens; match platform SCSS / iOS first.
