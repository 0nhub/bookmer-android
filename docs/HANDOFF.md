# Übergabe

Dieses Dokument dient der Übergabe des Bookmer-Android-Projekts ohne Vorwissen aus einem Chat.

## Was ist das?

Native Android-Browser-App für **Bookmer**: Collection-Startseite, WebView-Browsing, Account-Sync, Google-Play-PRO.

- Package: `com.bookmer.browser`
- Kanonischer Root: `Bookmer/android` im Bookmer-iCloud-Baum
- Einstieg: **`AGENTS.md`** (pflicht), danach `docs/*`

## Leseorder

1. [`../AGENTS.md`](../AGENTS.md) — Landkarte, Overlays, harte Regeln, Cheat Sheet  
2. [`ARCHITECTURE.md`](./ARCHITECTURE.md) — Schichten, Prozess, Persistenz  
3. [`DATA-AND-SYNC.md`](./DATA-AND-SYNC.md) — API, Guest-Seed, Icons  
4. [`UI-AND-MENUS.md`](./UI-AND-MENUS.md) — Collection-/Web-Menüs, Overlays  
5. [`BILLING.md`](./BILLING.md) — Play PRO  
6. [`PLATFORM-PARITY.md`](./PLATFORM-PARITY.md) — iOS-/Platform-Regeln  
7. [`FILE-INDEX.md`](./FILE-INDEX.md) — alle Kotlin-Dateien  

## Do / Don’t

**Do**

- Collection und Chrome-IA an iOS + bookmer-platform angleichen.
- Kleine Diffs; Zustand in `BrowserViewModel`, I/O in `data/*`.
- Tab-Previews **vor** `Overlay.TABS` erfassen.
- Theme über `bookmerIsDarkTheme()`.

**Don’t**

- Marketing-bookmer.com als Startseite laden.
- Den verlassenen Pfad `Code/browser/android*` bearbeiten.
- `local.properties`, `keystore.properties` oder Keystores committen.
- Bei Tab History `Overlay.HISTORY` öffnen.
- Collection-Layout neu erfinden.

## Build (macOS)

```bash
cd "/Users/gabriel/Library/Mobile Documents/com~apple~CloudDocs/Bookmer/android"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

## Backup-Orte

- Zeitstempel-Ordner + Tarballs: `backups/bookmer-android-YYYYMMDD-HHMMSS/`
- Jedes Backup enthält `BACKUP_MANIFEST.txt`
- Optional-Mirror: `/Users/gabriel/Bookmer/browser-android`
- GitHub: https://github.com/0nhub/bookmer-android

## Feature-Checkliste

- [x] Native Collection (4 Spalten, 66-dp-Tiles, Wallpaper, Sort, View-Styles)
- [x] WebView mit Chrome, Suchen, Reader, Immersive
- [x] Verschachteltes Collection-Folder-Menü + Web-Page-Menü
- [x] Toolbar Scroll-Hide + Swipe-down Sticky-Strip
- [x] Login-Sheet + Sync Pull/Push
- [x] Site-Permissions (Kamera/Mic/Ort)
- [x] Hide Elements
- [x] Alias-Spoofing
- [x] History / Tab History / Downloads / Navigate / Bookmark-Tools
- [x] Widgets, Share, QS-Tiles, Deep Links
- [x] Google-Play-Jahres-PRO + `/pay/google`
- [ ] Play Console + Server-Env produktionsfertig (Ops, nicht Code)

## Wo ändern?

| Anliegen | Start in |
|----------|----------|
| Menü / Chrome / Gesten | `ui/BrowserChrome.kt` |
| Collection-Grid | `ui/CollectionScreen.kt` |
| Tabs-Deck | `ui/TabsSwitcherScreen.kt` |
| Settings | `ui/SettingsScreen.kt` |
| Browse-Zustand | `browser/BrowserViewModel.kt` |
| WebView | `browser/BrowserWebView.kt` |
| Sync/API | `data/BookmerApiClient.kt` |
| PRO | `data/BookmerProStore.kt` |
| Theme | `ui/theme/Theme.kt` |
