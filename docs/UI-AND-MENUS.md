# UI, Chrome & Menüs

Paritätsziel: **iOS Bookmer Browser** + **bookmer-platform Collection**.

## Shell (`BookmerApp`)

```
BookmerBrowserTheme
└─ SetupWelcomeScreen          falls !setupCompleted
└─ sonst Browser-Shell
     ├─ CollectionScreen ODER BrowserWebView (+ Reader / Blocked)
     ├─ BrowserChrome (Adresse + Toolbars + Menüs)
     ├─ FindBar / HideElementsPickBar / ImmersiveExit
     └─ Overlay-Host (Tabs, Settings, History, …)
└─ LoginWebSheet bei Login-Anfrage
└─ BookmerDialogs
```

## Collection (`CollectionScreen`)

- Wallpaper-Ebene (Blur + Dim) aus `AppSettings`.
- Grid: `GridCells.Fixed(4)`, Icons ~66.dp, Rundungen je Setting/Ordnerstil.
- Drag-Reorder → `bookmarks.reorder` → Sync `pushOrder`.
- Ordnernavigation über `folderStack` / Navigate-Overlay.
- Overflow (⋯) auf Collection nutzt das **Folder**-Untermenü (nicht das Web-Page-Menü).

## Collection-⋯-Menü (Home)

Root (ca.): Settings · Clear Data · **Folder ›** · Bookmarks › (wenn angemeldet) · New Tab.

**Folder ›**

| Eintrag | Verhalten |
|---------|-----------|
| New Folder | Dialog → lokal + `sync.pushCreate` |
| Share | Ordner-Share-Seite (angemeldet) |
| Sort by › | A→Z, Z→A, Neu→Alt, Alt→Neu (`SortMode`) — einmaliges Umschreiben von `order` |
| View style › | Default / Grid / List / Thumbnail (`ContentViewMode`) |

**Bookmarks ›** (angemeldet): Broken links · Recover · Shared folder → `Overlay.BOOKMARK_TOOLS`.

## Web-⋯-Menü

Root: Settings · **Page ›** · Hide Element · Translate · Share · Collect · New Tab.

**Page ›**

- Print · Create PDF · Open in WebArchive · Report Page · Remove Data · Connection Details  
- Camera › · Microphone › · Location › — Ask / Deny / Allow (`SitePermissionStore`)

## Weitere Chrome-Menüs

- **Navigate:** Navigate · Tabs · Tab History · Forward (Web).
- **Action:** Full Screen · Reader · Search · Zoom · Desktop View (Defaults über Settings `ToolbarAction`).

## Overlays (`enum Overlay`)

| Wert | Screen |
|------|--------|
| `TABS` | `TabsSwitcherScreen` |
| `SETTINGS` | `SettingsScreen` |
| `HISTORY` | `HistoryScreen` (global) |
| `TAB_HISTORY` | `TabHistoryScreen` |
| `DOWNLOADS` | `DownloadsScreen` |
| `NAVIGATE` | `NavigateScreen` |
| `BOOKMARK_TOOLS` | `BookmarkToolsScreen` |

## Menü-Bausteine

`ModernMenu.kt`: `BookmerMenu`, `BookmerMenuItem`, `BookmerSubmenuHeader`, `BookmerMenuDivider`.  
Verschachtelte Sheets im Chrome: `ChromeSheet` (FOLDER, SORT, VIEW, PAGE, CAMERA, …).

## Settings (Auszug)

- Theme (`bookmerIsDarkTheme()` für UI).
- Wallpaper / Blur / Dim / Label-Farbe.
- Hide titles · Hide Toolbar (Scroll + Sticky-Collapse).
- Suchmaschine (+ eigene Engines).
- Privacy-Blocker (Cookies, Tracker, Popups, App-Banner, YouTube-Ads).
- Blockierte Sites.
- Translate-Sprache.
- Alias / Metadata-Spoof (`AliasStore`).
- Shortcuts für Widgets & QS-Tiles.
- Start-/Web-Navigationsbutton-Aktionen.
- Abo / PRO (`BookmerProStore`).
- Hidden-Elements-Verwaltung.

## Integrationen (`AndroidIntegrations.kt`)

- `ShareReceiverActivity` — URL in die App teilen.
- `BookmerWidgetProvider` + Configure-Activity.
- Quick-Settings-Tiles A/B + Configure.
- Deep Links: `http`/`https` VIEW + Schema `bookmer://`.
