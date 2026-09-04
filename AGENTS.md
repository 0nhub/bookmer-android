# Bookmer Browser (Android) — AI-Übergabe

**Lies diese Datei zuerst.** Sie ist die Landkarte für jede neue Session.

Vertiefung (vollständig für andere Plattformen/KIs): Ordner **[`docs/`](./docs/)**  
→ Start dort mit [`docs/HANDOFF.md`](./docs/HANDOFF.md).

Wenn etwas unklar ist: hier nachschlagen, dann die genannte Datei öffnen — nicht raten.

---

## 1. Was ist das?

Native **Android-Browser-App** für Bookmer.

| | |
|--|--|
| Workspace-Root | `Bookmer/android` |
| Package | `com.bookmer.browser` |
| UI | Jetpack Compose + Material 3 |
| Engine | Android `WebView` |
| Startseite | **Native Collection** (Lesezeichen-Grid) — **nicht** bookmer.com Marketing |
| Version | `1.0.2` (`versionCode` 3) — siehe `app/build.gradle.kts` |

---

## 2. Wichtige Pfade

| Was | Pfad |
|-----|------|
| **Dieses Projekt (einzig richtige Edit-Root)** | `…/Bookmer/android` |
| Vollständige Doku | `…/Bookmer/android/docs/` |
| Backups (Ordner + `.tar.gz`) | `…/Bookmer/android/backups/` |
| Lokales Mirror (optional, non-iCloud) | `~/Bookmer/browser-android` |
| iOS-Parität (Verhalten/UI) | `…/Bookmer/Code/browser/iOS` |
| Platform Collection (Optik + Datenlogik) | `…/Bookmer/Code/bookmer-platform` |
| Alter Android-Pfad (nicht mehr editieren) | `…/Bookmer/Code/browser/android-MOVED-TO-Bookmer-android` |

---

## 3. Dokumentation (docs/)

| Datei | Inhalt |
|-------|--------|
| [`docs/HANDOFF.md`](./docs/HANDOFF.md) | Pflicht-Leseorder für fremde Agents |
| [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) | Schichten, Prozess, Persistenz, Theme, Chrome |
| [`docs/DATA-AND-SYNC.md`](./docs/DATA-AND-SYNC.md) | API, Guest-Seed, Icons, Session |
| [`docs/UI-AND-MENUS.md`](./docs/UI-AND-MENUS.md) | Collection/Web-Menüs, Overlays, Settings |
| [`docs/BILLING.md`](./docs/BILLING.md) | Google Play PRO + `/pay/google` |
| [`docs/PLATFORM-PARITY.md`](./docs/PLATFORM-PARITY.md) | iOS/Platform-Regeln, Pfad-Hygiene |
| [`docs/FILE-INDEX.md`](./docs/FILE-INDEX.md) | Alle Kotlin-Dateien |

---

## 4. Architektur in 30 Sekunden

```
MainActivity
└─ ui/BookmerApp.kt
     ├─ Setup (Dialogs.kt) falls setup unvollständig
     ├─ Browser-Shell
     │    ├─ CollectionScreen     → Start / Ordner
     │    ├─ BrowserWebView       → Website
     │    ├─ BrowserChrome        → Adressleiste + Toolbar
     │    └─ Overlays (Tabs, Settings, History, …)
     └─ LoginWebSheet             → Login über bookmer.com/login

browser/BrowserViewModel.kt       → gesamter Browse-Zustand
BookmerApplication.kt
└─ BookmerServices                → Bookmarks, History, API, Sync, Alias, Pro, …
```

**Regel:** Zustand → `BrowserViewModel`. Persistenz/API → `BookmerServices` / `data/*`. UI → `ui/*`.

---

## 5. Dateimap (exakte Dateinamen)

Kotlin-Root: `app/src/main/java/com/bookmer/browser/`

Vollständige Tabelle: [`docs/FILE-INDEX.md`](./docs/FILE-INDEX.md).

### Einstieg

| Datei | Aufgabe |
|-------|---------|
| `BookmerApplication.kt` | App-Start; initialisiert `BookmerServices`, `TabPreviewStore` |
| `MainActivity.kt` | Compose-Host; Permissions + File-Chooser für WebView |

### `browser/` — Engine & State

| Datei | Aufgabe |
|-------|---------|
| `BrowserViewModel.kt` | Tabs, Overlays, Navigation, Translate, Immersive, Hide Elements, Downloads, Previews |
| `BrowserWebView.kt` | WebView-Composable, Clients, JS, UA |
| `AddressResolver.kt` | Adresszeile → URL oder Suche |
| `HideElementsScript.kt` | JS für Element-Picker / Ausblenden |

### `data/` — Modelle, Speicher, API

| Datei | Aufgabe |
|-------|---------|
| `Models.kt` | `BookmerUrls`, `BookmerItem`, `BrowserTab`, Settings-Enums |
| `Repositories.kt` | `BookmarkRepository`, `HistoryRepository` |
| `PreferencesRepository.kt` | Settings + `SecureSessionStore` |
| `BookmerApiClient.kt` | HTTP + `BookmerSyncService` |
| `BookmerIconUrl.kt` | Icon-URL-Auflösung |
| `AliasStore.kt` | Spoof: Land, Sprache, TZ, Location, OS/UA |
| `HiddenElementsStore.kt` | Persistierte Hide-Regeln + Tip-Stores |
| `SitePermissions.kt` | Kamera/Mic/Location pro Site |
| `TabPreviewStore.kt` | Disk-Cache für Tab-Vorschaubilder |
| `LaunchShortcutStore.kt` | Shortcuts / Widget / Tile-Bindings |
| `BookmerProStore.kt` | Play Billing / Pro |

### `ui/` — Screens

| Datei | Aufgabe |
|-------|---------|
| `BookmerApp.kt` | Theme, Shell, Overlay-Routing, Chrome-Sichtbarkeit |
| `CollectionScreen.kt` | Collection-Startseite |
| `BrowserChrome.kt` | Chrome, Menüs, Find-Bar, Immersive-Exit |
| `TabsSwitcherScreen.kt` | Tab-Manager (Deck) |
| `SettingsScreen.kt` | Settings |
| `Overlays.kt` | `HistoryScreen`, `TabHistoryScreen`, Downloads, Navigate, Bookmark-Tools |
| `LoginWebSheet.kt` | Web-Login + Token |
| `HideElementsUi.kt` | Pick-Bar, Confirm, Manage |
| `ReaderScreen.kt` | Reader Mode |
| `Dialogs.kt` | Setup-Welcome, Blocked Page, Dialoge |
| `Common.kt` | `RemoteImage` (inkl. Auth für api.bookmer.com) |
| `ModernMenu.kt` | Gemeinsame Menü-UI |
| `theme/` | Theme, Farben, Typo |

### `integration/`

| Datei | Aufgabe |
|-------|---------|
| `AndroidIntegrations.kt` | Share, Widget, Quick-Settings-Tiles, Deep Links, Shortcuts |

---

## 6. Overlays — nicht verwechseln

Definiert in `BrowserViewModel.kt` als `enum class Overlay`.

| Overlay | Bedeutung | UI-Composable (in `Overlays.kt` / Tabs / Settings) |
|---------|-----------|-----------------------------------------------------|
| `TABS` | Tab-Manager | `TabsSwitcherScreen` |
| `SETTINGS` | Einstellungen | `SettingsScreen` |
| `HISTORY` | **Globaler** Verlauf (alles) | `HistoryScreen` |
| `TAB_HISTORY` | **Nur aktueller Tab** (WebView Back-Forward) | `TabHistoryScreen` |
| `DOWNLOADS` | Downloads | `DownloadsScreen` |
| `NAVIGATE` | Ordner-Navigator | `NavigateScreen` |
| `BOOKMARK_TOOLS` | Bookmark-Werkzeuge | `BookmarkToolsScreen` |

**Hard rule:** „Tab History“ öffnet `Overlay.TAB_HISTORY` — **nie** `HISTORY`.

---

## 7. Harte Produktregeln (nicht brechen)

1. **Start = Collection**, keine Marketing-Website.
2. **Tab-Previews** = nur WebView-Seiteninhalt (Software-`draw`), **kein** Window-`PixelCopy`. Capture **bevor** `Overlay.TABS` gesetzt wird — sonst Spiegel-/Chrome-Bug.
3. **Icons nach Login:** Assets auf `api.bookmer.com` brauchen Bearer (siehe `Common.kt` / `BookmerIconUrl.kt`).
4. **Translate:** Google-Translate-URL + sofortiges UI-Feedback (`isTranslating`).
5. **Alias:** Spoofing über `AliasStore`, nicht ad-hoc im WebView streuen.
6. **iOS/Platform zuerst**, bevor Collection- oder Chrome-UX neu erfunden wird.
7. Nur in `Bookmer/android` arbeiten — alten `Code/browser/android*`-Pfad nicht wiederbeleben.
8. **Theme:** UI-Dunkelheit über `bookmerIsDarkTheme()`, nicht blind `isSystemInDarkTheme()`.
9. **PRO:** Product-ID `com.bookmer.browser.pro.yearly` → Acknowledge → `POST /pay/google`.

---

## 8. Wo ändern? (Cheat Sheet)

| Anliegen | Datei |
|----------|-------|
| Adressleiste, Toolbar, Menüs, Gesten | `ui/BrowserChrome.kt` |
| Tab-Deck | `ui/TabsSwitcherScreen.kt` |
| Startseite / Tiles | `ui/CollectionScreen.kt`, `data/Repositories.kt` |
| Settings | `ui/SettingsScreen.kt` |
| Laden, Tabs, Nav, Translate | `browser/BrowserViewModel.kt` |
| WebView / JS | `browser/BrowserWebView.kt` |
| API / Sync | `data/BookmerApiClient.kt` |
| Icons kaputt | `ui/Common.kt`, `data/BookmerIconUrl.kt` |
| Login | `ui/LoginWebSheet.kt` |
| Hide Elements | `HideElementsScript.kt`, `HideElementsUi.kt`, `HiddenElementsStore.kt` |
| Widget / Share / Tiles | `integration/AndroidIntegrations.kt` |
| Play PRO | `data/BookmerProStore.kt` |
| Light/Dark Theme | `ui/theme/Theme.kt` |

---

## 9. Bauen

```bash
cd "/Users/gabriel/Library/Mobile Documents/com~apple~CloudDocs/Bookmer/android"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

Nicht committen: `local.properties`, `keystore.properties`, `*.jks`, `*.keystore`.

---

## 10. Checkliste nach Änderungen

- [ ] Collection-Home erscheint (kein Marketing)
- [ ] Tab History ≠ Settings History
- [ ] Tab-Previews = Seiteninhalt ohne Chrome/Deck
- [ ] Icons eingeloggt laden
- [ ] Light-Theme bleibt hell wenn App=Light (auch bei OS=Dark)
- [ ] Compile grün

---

## 11. Arbeitsregeln für Agents

1. Kleine, gezielte Diffs — keine Drive-by-Refactors.
2. Bei Unklarheit: diese Datei → `docs/HANDOFF.md` → Cheat-Sheet-Datei öffnen.
3. Verhalten an iOS/Platform angleichen, wenn Collection/Chrome betroffen ist.
4. Nach Datei-Verschiebungen im Reply klar sagen: wo Live-Tree und wo Backup liegt.
5. Neue Features in `docs/` nachziehen, wenn Struktur oder API sich ändert.
