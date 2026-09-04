# Bookmer Browser (Android)

Live-Projektroot: dieses Verzeichnis (`Bookmer/android`).

| | |
|--|--|
| Package | `com.bookmer.browser` |
| UI | Jetpack Compose |
| Engine | Android WebView |
| Start | Native Collection (`bookmer://collection`) — keine Marketing-Website |

## Dokumentation

| Dokument | Zweck |
|----------|--------|
| **[AGENTS.md](./AGENTS.md)** | Landkarte, Regeln, Cheat Sheet — zuerst lesen |
| [docs/HANDOFF.md](./docs/HANDOFF.md) | Vollständige Übergabe |
| [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) | Architektur |
| [docs/DATA-AND-SYNC.md](./docs/DATA-AND-SYNC.md) | Daten, API, Sync |
| [docs/UI-AND-MENUS.md](./docs/UI-AND-MENUS.md) | Oberfläche und Menüs |
| [docs/BILLING.md](./docs/BILLING.md) | Google Play PRO |
| [docs/PLATFORM-PARITY.md](./docs/PLATFORM-PARITY.md) | iOS- und Platform-Parität |
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
