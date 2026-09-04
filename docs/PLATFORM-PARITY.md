# Platform- & iOS-Parität

## Harte Produktregeln

1. **Startseite = native Collection**, nie bookmer.com-Marketing.
2. Collection **sieht aus und verhält sich** wie die bookmer-platform-Dashboard-Collection (Wallpaper, Theme, Icon-Radius/Pan/Zoom/Hintergrund, 66-px-Tiles, 4-Spalten-Phone-Grid, Ordnernav, Settings für Wallpaper/View/Theme).
3. **Local-first**; Guest-Seed aus gebündeltem `global_list.json`.
4. Sync angemeldet über `api.bookmer.com` (`/object`, `/user`, Wallpaper-Felder).
5. **WebView nur** für vom Nutzer geöffnete Websites.
6. Bevor UI-Tokens erfunden werden: Platform-SCSS/Komponenten und iOS-Browser lesen.

## Wo nachschauen

| Thema | Bevorzugt lesen |
|-------|-----------------|
| Tile-Größe, Grid, Wallpaper | bookmer-platform Collection/Dashboard-SCSS + iOS Collection |
| Overflow-Menüs (Folder / Page) | iOS Browser-Chrome-Menüs |
| Sync-Walk / Parent-Erzwingen | iOS Library-Sync + `BookmerApiClient.fetchLibrary` |
| Icon-URL-Auflösung | Platform + `BookmerIconUrl.kt` |
| PRO | iOS StoreKit + gemeinsame `/user`-PRO-Felder; Android `/pay/google` |

## Android-spezifisch (Unterschiede ok)

- Material 3 / Compose-Chrome (nicht UIKit).
- Android-Widgets, Share-Target, Quick-Settings-Tiles.
- Google Play Billing (statt StoreKit).
- System-Permission-Dialoge für Kamera/Mic/Ort.

Informationsarchitektur soll dort, wo beide Produkte dasselbe Feature haben, iOS entsprechen.

## Pfad-Hygiene

| Pfad | Status |
|------|--------|
| `…/Bookmer/android` | **Kanonischer Live-Tree** — hier editieren |
| `…/Bookmer/android/backups/` | Snapshot-Kopien + `.tar.gz` |
| `…/Bookmer/Code/browser/android-MOVED-TO-Bookmer-android` | Verlassen — nicht wiederbeleben |
| `~/Bookmer/browser-android` | Optional-Mirror (non-iCloud) |

Nach Verschiebungen klar sagen, wo Live-Tree und aktuelles Backup liegen.
