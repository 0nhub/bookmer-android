# Platform & iOS parity

## Non-negotiable product rules

1. **Start page = native Collection**, never bookmer.com marketing landing.
2. Collection **looks and behaves** like bookmer-platform dashboard Collection (wallpaper, theme, icon radius/pan/zoom/background, 66px tiles, 4-col phone grid, folder nav, settings for wallpaper/view/theme).
3. **Local-first** data; guest seed from bundled `global_list.json` (platform default list).
4. Signed-in sync via `api.bookmer.com` (`/object`, `/user`, wallpaper fields).
5. **WebView only** for browsing user-opened websites.
6. Before inventing UI tokens, read platform SCSS/components (`UserDashboard`, Bookmarks, wallpaper, sizes, colors) and iOS browser sources.

## Where to look for “how it should work”

| Concern | Prefer reading |
|---------|----------------|
| Tile size, grid, wallpaper | `bookmer-platform` Collection / dashboard SCSS + iOS Collection views |
| Overflow menus (Folder / Page) | iOS browser chrome menus |
| Sync walk / parent forcing | iOS library sync + this app’s `BookmerApiClient.fetchLibrary` |
| Icon URL resolution | Platform + `BookmerIconUrl.kt` |
| PRO unlock | iOS StoreKit + shared `/user` PRO fields; Android `/pay/google` |

## Android-specific surfaces (OK to differ)

- Material 3 / Compose chrome chrome (not UIKit).
- Android widgets, share target, Quick Settings tiles.
- Google Play Billing (vs StoreKit).
- System permission prompts for camera/mic/location.

Behavior and information architecture should still match iOS where both products offer the same feature.

## Path hygiene

| Path | Status |
|------|--------|
| `…/Bookmer/android` | **Canonical live tree** — edit here |
| `…/Bookmer/android/backups/` | Snapshot copies + `.tar.gz` |
| `…/Bookmer/Code/browser/android-MOVED-TO-Bookmer-android` | Abandoned stub — do not revive |
| `~/Bookmer/browser-android` | Optional local mirror (non-iCloud) for tooling |

Agents must state clearly after moves where the live tree and latest backup are.
