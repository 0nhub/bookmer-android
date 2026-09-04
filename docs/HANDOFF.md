# AI / cross-platform handoff

This document is for **other agents, platforms, or humans** picking up Bookmer Android without prior chat context.

## What this project is

Native Android browser app for **Bookmer**: Collection home + WebView browsing + Bookmer account sync + Google Play PRO.

- Package: `com.bookmer.browser`
- Canonical root: `Bookmer/android` under the user’s Bookmer iCloud tree
- Entry docs: **`AGENTS.md`** (required), then `docs/*`

## Read order (mandatory)

1. [`../AGENTS.md`](../AGENTS.md) — map, overlays, hard rules, cheat sheet  
2. [`ARCHITECTURE.md`](./ARCHITECTURE.md) — layers, process, persistence  
3. [`DATA-AND-SYNC.md`](./DATA-AND-SYNC.md) — API, guest seed, icons  
4. [`UI-AND-MENUS.md`](./UI-AND-MENUS.md) — Collection/web menus, overlays  
5. [`BILLING.md`](./BILLING.md) — Play PRO  
6. [`PLATFORM-PARITY.md`](./PLATFORM-PARITY.md) — iOS/platform rules  
7. [`FILE-INDEX.md`](./FILE-INDEX.md) — every Kotlin file in one table  

## Do / don’t

**Do**

- Match iOS + bookmer-platform for Collection and chrome IA.
- Prefer small diffs; keep state in `BrowserViewModel`, I/O in `data/*`.
- Capture tab previews before showing `Overlay.TABS`.
- Use `bookmerIsDarkTheme()` for theme-dependent UI.

**Don’t**

- Load marketing bookmer.com as start page.
- Edit the abandoned `Code/browser/android*` path.
- Commit `local.properties`, `keystore.properties`, or keystores.
- Open `Overlay.HISTORY` when the user wants Tab History.
- Invent Collection layout from scratch.

## Build (macOS)

```bash
cd "/Users/gabriel/Library/Mobile Documents/com~apple~CloudDocs/Bookmer/android"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

## Backup locations

- Timestamped folders + tarballs: `backups/bookmer-android-YYYYMMDD-HHMMSS/`
- Each backup includes `BACKUP_MANIFEST.txt`
- Optional local mirror: `/Users/gabriel/Bookmer/browser-android`
- Cursor-hosted private copy: see README / save URL after `origin` backup (not public)

## Feature checklist (current product surface)

- [x] Native Collection (4-col, 66dp tiles, wallpaper, sort, view styles)
- [x] WebView browsing with chrome, find, reader, immersive
- [x] Nested Collection Folder menu + web Page menu
- [x] Toolbar scroll hide + swipe-down sticky strip
- [x] Login sheet + sync pull/push
- [x] Site permissions (camera/mic/location)
- [x] Hide elements
- [x] Alias spoofing
- [x] History / tab history / downloads / navigate / bookmark tools
- [x] Widgets, share, QS tiles, deep links
- [x] Google Play yearly PRO + `/pay/google` registration
- [ ] Play Console + server env fully production-wired (ops, not code)

## Contact points for change requests

| Request type | Start in |
|--------------|----------|
| Menu / chrome / gestures | `ui/BrowserChrome.kt` |
| Collection grid | `ui/CollectionScreen.kt` |
| Tabs deck | `ui/TabsSwitcherScreen.kt` |
| Settings | `ui/SettingsScreen.kt` |
| Browse state | `browser/BrowserViewModel.kt` |
| WebView | `browser/BrowserWebView.kt` |
| Sync/API | `data/BookmerApiClient.kt` |
| PRO | `data/BookmerProStore.kt` |
| Theme fix | `ui/theme/Theme.kt` |
