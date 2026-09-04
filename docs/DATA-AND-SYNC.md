# Data, API & Sync

## URLs (`BookmerUrls`)

| Constant | Value | Use |
|----------|-------|-----|
| `HOME` | `bookmer://collection` | Native Collection start |
| `API` | `https://api.bookmer.com` | REST + icons |
| `LOGIN` | `https://www.bookmer.com/login` | Web login sheet |
| `ACCOUNT` | `https://id.bookmer.com` | Account management |
| `HELP` | `https://help.bookmer.com` | Help |
| `ROOT` | `/` | Collection root parent id |
| `ARCHIVE` | `archive` | Archive folder id |
| `TAGS` | `__bm_tags` | Tags pseudo-folder |
| `HIDDEN` | `Undefined` | Hidden/archived bookmark parent quirk |

## Guest vs signed-in

1. **Guest (local-first)**  
   - On empty library → `BookmarkRepository.seedDefaults()` loads `assets/global_list.json`.  
   - No Bearer token. Icons that are public CDN/host favicons still work; private `api.bookmer.com` assets may not.

2. **Signed-in**  
   - Token in `SecureSessionStore` (from `LoginWebSheet` / cookie bridge or email login).  
   - `BookmerSyncService.pull()` walks the remote tree and replaces/merges into local items.  
   - Mutations call `pushCreate` / `pushRename` / `pushMove` / `pushDelete` / `pushOrder` / `pushView`.

## API surface used by the app (`BookmerApiClient`)

Base: `https://api.bookmer.com` — Bearer token when authenticated.

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/user/access` | Email/password login |
| GET | `/user` | Profile, wallpaper fields, PRO flags |
| PATCH | `/user` | e.g. root `contentView` |
| GET | `/object?path=` | Folder listing |
| GET | `/object/archive?...` | Archive walk |
| GET | `/object/note_tags` | Tag presence |
| POST | `/bookmark?path=` | Create bookmark |
| POST | `/folder?path=` | Create folder |
| PATCH | `/bookmark?id=` / `/folder?id=` | Rename / move / archive |
| DELETE | `/object/trash?id=&t=` | Soft delete |
| PATCH | `/object/update_order` | Reorder |
| GET/POST | `/object/trash`, `/object/broken_links` | Bookmark tools |
| POST | `/pay/google` | Register Play subscription token |
| (icons) | `/iconscollection/{host}` | Default host icons |
| (assets) | `/user/wallpaper/…`, `/object/navigation_icons/…`, etc. | Authenticated images |

### Library pull algorithm

`fetchLibrary` mirrors iOS/platform:

1. Walk folders-only (`only_folders=1&sidebar_tree=3`) for main tree, then full listings.
2. Same for archive (best-effort).
3. Force parent id to the path being queried (API `parent` is often blank/wrong) — same as iOS `forceParent`.

## `BookmerItem` (Collection tile)

Shared with platform/iOS conceptual model:

- `kind`: BOOKMARK | FOLDER  
- `title`, `targetUrl`, `parentId`, `order`  
- Icon framing: `iconUrl`, `iconBackground`, `iconX`, `iconY`, `iconZoom`  
- Preview / thumbnail mode: `previewImage`, `customPreview*`  
- `contentView` per folder (grid / list / thumbnail)  
- `remoteId` for server id when local id differs  

Default bookmark `iconZoom` is `-22` (platform parity). Phone Collection grid: **4 columns**, tile icon size **66.dp**.

## Icons after login

`BookmerIconUrl` + `RemoteImage` (`ui/Common.kt`):

- Resolve relative API paths to absolute `https://api.bookmer.com/...`.
- Requests to authenticated asset paths must send **Bearer** from `SecureSessionStore`.
- Without auth headers, signed-in Collection icons look broken.

## Session / PRO flags

`BookmerSession`: `token`, `email`, `name`, `avatarUrl`, `accountType`, `hasPro`.

`hasPro` is updated from:

- `GET /user` (lifetimeDeal / subscription.active / … — see ViewModel profile sync), and/or  
- Google Play entitlement via `BookmerProStore` (`hasPlayEntitlement || session.hasPro`).

## History vs Tab History

| Concept | Storage | Overlay |
|---------|---------|---------|
| Global history | `HistoryRepository` | `Overlay.HISTORY` |
| Current tab back-forward list | WebView / ViewModel `TabHistoryEntry` | `Overlay.TAB_HISTORY` |

Never open global history when the user asked for Tab History.
