# Bookmer Browser (Android)

Live-Projektroot: dieses Verzeichnis (`Bookmer/android`).

**Package:** `com.bookmer.browser` · **UI:** Jetpack Compose · **Engine:** Android WebView  
**Start:** native Collection (`bookmer://collection`) — nicht die Marketing-Website.

## Dokumentation (Menschen + KI)

| Dokument | Zweck |
|----------|--------|
| **[AGENTS.md](./AGENTS.md)** | Landkarte, Hard Rules, Cheat Sheet — **zuerst lesen** |
| **[docs/HANDOFF.md](./docs/HANDOFF.md)** | Vollständige Übergabe für andere Systeme/Agents |
| [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) | Architektur |
| [docs/DATA-AND-SYNC.md](./docs/DATA-AND-SYNC.md) | Daten, API, Sync |
| [docs/UI-AND-MENUS.md](./docs/UI-AND-MENUS.md) | UI & Menüs |
| [docs/BILLING.md](./docs/BILLING.md) | Google Play PRO |
| [docs/PLATFORM-PARITY.md](./docs/PLATFORM-PARITY.md) | iOS / Platform-Parität |
| [docs/FILE-INDEX.md](./docs/FILE-INDEX.md) | Dateiindex |

## Backups

Sicherheitskopien (ohne Secrets): `backups/bookmer-android-<timestamp>/` und `.tar.gz`.  
Optional-Mirror: `~/Bookmer/browser-android`.

Nicht versionieren: `local.properties`, `keystore.properties`, Keystores.

## Build

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```
