# Daten, API & Sync

## URLs (`BookmerUrls`)

| Konstante | Wert | Nutzung |
|-----------|------|---------|
| `HOME` | `bookmer://collection` | Native Collection-Start |
| `API` | `https://api.bookmer.com` | REST + Icons |
| `LOGIN` | `https://www.bookmer.com/login` | Web-Login-Sheet |
| `ACCOUNT` | `https://id.bookmer.com` | Account-Verwaltung |
| `HELP` | `https://help.bookmer.com` | Hilfe |
| `ROOT` | `/` | Collection-Root-Parent |
| `ARCHIVE` | `archive` | Archiv-Ordner-ID |
| `TAGS` | `__bm_tags` | Tags-Pseudo-Ordner |
| `HIDDEN` | `Undefined` | Hidden/Archiv-Parent-Quirk |

## Gast vs. angemeldet

1. **Gast (local-first)**  
   - Leere Library → `BookmarkRepository.seedDefaults()` lädt `assets/global_list.json`.  
   - Kein Bearer-Token. Öffentliche Favicons funktionieren; private `api.bookmer.com`-Assets oft nicht.

2. **Angemeldet**  
   - Token in `SecureSessionStore` (LoginWebSheet / Cookie-Bridge oder E-Mail-Login).  
   - `BookmerSyncService.pull()` läuft den Remote-Baum und merge’t in lokale Items.  
   - Mutationen: `pushCreate` / `pushRename` / `pushMove` / `pushDelete` / `pushOrder` / `pushView`.

## API (`BookmerApiClient`)

Basis: `https://api.bookmer.com` — Bearer bei Authentifizierung.

| Methode | Pfad | Zweck |
|---------|------|-------|
| POST | `/user/access` | E-Mail/Passwort-Login |
| GET | `/user` | Profil, Wallpaper, PRO-Flags |
| PATCH | `/user` | z. B. Root-`contentView` |
| GET | `/object?path=` | Ordnerlisting |
| GET | `/object/archive?...` | Archiv-Walk |
| GET | `/object/note_tags` | Tag-Präsenz |
| POST | `/bookmark?path=` | Bookmark anlegen |
| POST | `/folder?path=` | Ordner anlegen |
| PATCH | `/bookmark?id=` / `/folder?id=` | Umbenennen / Verschieben / Archivieren |
| DELETE | `/object/trash?id=&t=` | Soft-Delete |
| PATCH | `/object/update_order` | Neuordnen |
| GET/POST | `/object/trash`, `/object/broken_links` | Bookmark-Tools |
| POST | `/pay/google` | Play-Abo-Token registrieren |
| (Icons) | `/iconscollection/{host}` | Host-Icons |
| (Assets) | `/user/wallpaper/…`, `/object/navigation_icons/…` | Auth-Bilder |

### Library-Pull

`fetchLibrary` spiegelt iOS/Platform:

1. Zuerst nur Ordner (`only_folders=1&sidebar_tree=3`), dann volle Listings.
2. Entsprechend für Archiv (best effort).
3. Parent-ID auf den abgefragten Pfad erzwingen (API-`parent` oft leer/falsch) — wie iOS `forceParent`.

## `BookmerItem` (Collection-Kachel)

- `kind`: BOOKMARK | FOLDER  
- `title`, `targetUrl`, `parentId`, `order`  
- Icon: `iconUrl`, `iconBackground`, `iconX`, `iconY`, `iconZoom`  
- Preview/Thumbnail: `previewImage`, `customPreview*`  
- `contentView` pro Ordner (Grid / List / Thumbnail)  
- `remoteId` wenn Server-ID ≠ lokal  

Standard-Bookmark-`iconZoom`: `-22`. Phone-Grid: **4 Spalten**, Icon **66.dp**.

## Icons nach Login

`BookmerIconUrl` + `RemoteImage` (`ui/Common.kt`):

- Relative API-Pfade → `https://api.bookmer.com/...`.
- Auth-Assets brauchen **Bearer** aus `SecureSessionStore`.
- Ohne Auth wirken angemeldete Collection-Icons kaputt.

## Session / PRO

`BookmerSession`: `token`, `email`, `name`, `avatarUrl`, `accountType`, `hasPro`.

`hasPro` kommt von:

- `GET /user` (lifetimeDeal / subscription.active / …), und/oder  
- Google-Play-Entitlement über `BookmerProStore` (`hasPlayEntitlement || session.hasPro`).

## History vs. Tab History

| Konzept | Speicher | Overlay |
|---------|----------|---------|
| Globaler Verlauf | `HistoryRepository` | `Overlay.HISTORY` |
| Back-Forward des aktuellen Tabs | WebView / ViewModel `TabHistoryEntry` | `Overlay.TAB_HISTORY` |

Nie den globalen Verlauf öffnen, wenn Tab History gemeint ist.
