# UI, Chrome & Menus

Parity target: **iOS Bookmer Browser** + **bookmer-platform Collection**.

## Shell composition (`BookmerApp`)

```
BookmerBrowserTheme
└─ SetupWelcomeScreen          if !setupCompleted
└─ else Browser shell
     ├─ CollectionScreen OR BrowserWebView (+ Reader / Blocked)
     ├─ BrowserChrome (address + toolbars + menus)
     ├─ FindBar / HideElementsPickBar / ImmersiveExit
     └─ Overlay host (Tabs, Settings, History, …)
└─ LoginWebSheet when login requested
└─ BookmerDialogs
```

## Collection (`CollectionScreen`)

- Wallpaper layer (blur + dim) from `AppSettings`.
- Grid: `GridCells.Fixed(4)`, icons ~66.dp, rounded corners from settings / folder style.
- Drag reorder → `bookmarks.reorder` → sync `pushOrder`.
- Folder navigation via `folderStack` / Navigate overlay.
- Overflow (⋯) on Collection uses **Folder** submenu (not the web Page menu).

## Collection ⋯ menu (home)

Root items (approx.): Settings · Clear Data · **Folder ›** · Bookmarks › (if signed in) · New Tab.

**Folder ›**

| Item | Behavior |
|------|----------|
| New Folder | Dialog → local + `sync.pushCreate` |
| Share | Share folder page (signed-in) |
| Sort by › | A→Z, Z→A, New→Old, Old→New (`SortMode`) — one-shot rewrite of `order` |
| View style › | Default / Grid / List / Thumbnail (`ContentViewMode`) |

**Bookmarks ›** (signed-in): Broken links · Recover · Shared folder → `Overlay.BOOKMARK_TOOLS`.

## Web ⋯ menu

Root: Settings · **Page ›** · Hide Element · Translate · Share · Collect · New Tab.

**Page ›**

- Print · Create PDF · Open in WebArchive · Report Page · Remove Data · Connection Details  
- Camera › · Microphone › · Location › — policies Ask / Deny / Allow (`SitePermissionStore`)

## Other chrome menus

- **Navigate** long-press / menu: Navigate · Tabs · Tab History · Forward (web).
- **Action** menu: Full Screen · Reader · Search · Zoom · Desktop View (customizable via Settings `ToolbarAction` defaults).

## Overlays (`enum Overlay`)

| Value | Screen |
|-------|--------|
| `TABS` | `TabsSwitcherScreen` |
| `SETTINGS` | `SettingsScreen` |
| `HISTORY` | `HistoryScreen` (global) |
| `TAB_HISTORY` | `TabHistoryScreen` |
| `DOWNLOADS` | `DownloadsScreen` |
| `NAVIGATE` | `NavigateScreen` |
| `BOOKMARK_TOOLS` | `BookmarkToolsScreen` |

## Menu building blocks

`ModernMenu.kt`: `BookmerMenu`, `BookmerMenuItem`, `BookmerSubmenuHeader`, `BookmerMenuDivider`.  
Nested sheets in chrome use `ChromeSheet` enum (FOLDER, SORT, VIEW, PAGE, CAMERA, …).

## Settings highlights

- Theme (use `bookmerIsDarkTheme()` for UI chrome).
- Wallpaper / blur / dim / label color.
- Hide titles · Hide Toolbar (enables scroll + sticky collapse behavior).
- Search engine (+ custom engines).
- Privacy blockers (cookies, trackers, popups, app banners, YouTube ads).
- Blocked sites.
- Translate language.
- Alias / metadata spoof (also `AliasStore`).
- Shortcuts for widgets & QS tiles.
- Start / web navigation button actions.
- Subscription / PRO (`BookmerProStore` UI section).
- Hidden elements management.

## Integrations (`AndroidIntegrations.kt`)

- `ShareReceiverActivity` — share URL into app.
- `BookmerWidgetProvider` + configure activity.
- Quick Settings tiles A/B + configure.
- Deep links: `http`/`https` VIEW + `bookmer://` scheme.
