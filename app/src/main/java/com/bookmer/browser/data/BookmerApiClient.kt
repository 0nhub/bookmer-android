package com.bookmer.browser.data

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors

class BookmerApiException(message: String, val status: Int = 0) : Exception(message)

class BookmerApiClient {
    private val executor = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    fun login(email: String, password: String, callback: (Result<BookmerSession>) -> Unit) = execute(callback) {
        val json = request("POST", "/user/access", body = JSONObject().put("email", email).put("password", password))
        val data = json.optJSONObject("data") ?: json
        val token = json.optString("token").ifBlank { json.optString("accessToken") }.ifBlank { data.optString("token") }
        if (token.isBlank()) throw BookmerApiException("The server did not return a session token")
        BookmerSession(token = token, email = email)
    }

    fun fetchUser(token: String, callback: (Result<JSONObject>) -> Unit) = execute(callback) {
        request("GET", "/user", token)
    }

    fun fetchLibrary(token: String, callback: (Result<List<BookmerItem>>) -> Unit) = execute(callback) {
        val result = linkedMapOf<String, BookmerItem>()
        fun walk(archive: Boolean, foldersOnly: Boolean) {
            val queue = ArrayDeque<String>()
            queue.add(if (archive) BookmerUrls.ARCHIVE else BookmerUrls.ROOT)
            val visited = mutableSetOf<String>()
            while (queue.isNotEmpty() && visited.size < 500) {
                val parent = queue.removeFirst()
                if (!visited.add(parent)) continue
                val folderQuery = if (foldersOnly) "&only_folders=1&sidebar_tree=3" else ""
                val path = if (archive && parent == BookmerUrls.ARCHIVE) {
                    "/object/archive?only_folders=${if (foldersOnly) "1" else "0"}${if (foldersOnly) "&sidebar_tree=3" else ""}"
                } else if (archive) {
                    "/object/archive?path=${encode(parent)}$folderQuery"
                } else {
                    "/object?path=${encode(parent)}$folderQuery"
                }
                val response = request("GET", path, token)
                val archiveRoot = archive && parent == BookmerUrls.ARCHIVE
                // iOS forceParent: children belong to the path we queried — API `parent` is often blank/wrong.
                parseObjects(response, parent, archiveRoot).forEach { item ->
                    val correctedParent = when {
                        archiveRoot && item.kind == ItemKind.BOOKMARK -> BookmerUrls.HIDDEN
                        archiveRoot -> BookmerUrls.ARCHIVE
                        else -> BookmerItem.normalizedParent(parent)
                    }
                    val corrected = item.copy(parentId = correctedParent)
                    // Prefer richer bookmark rows over folder-only stubs from earlier passes.
                    val existing = result[corrected.id]
                    if (existing == null ||
                        (existing.kind == ItemKind.FOLDER && corrected.kind == ItemKind.FOLDER) ||
                        corrected.kind == ItemKind.BOOKMARK
                    ) {
                        result[corrected.id] = if (existing != null && corrected.kind == ItemKind.FOLDER) {
                            corrected.copy(
                                contentView = corrected.contentView ?: existing.contentView,
                                iconUrl = corrected.iconUrl ?: existing.iconUrl,
                            )
                        } else corrected
                    }
                    if (corrected.kind == ItemKind.FOLDER) queue.add(corrected.id)
                }
            }
        }
        // Folder tree first (platform SideFolderNav), then full listings for bookmarks/views.
        walk(archive = false, foldersOnly = true)
        walk(archive = false, foldersOnly = false)
        runCatching { walk(archive = true, foldersOnly = true) }
        runCatching { walk(archive = true, foldersOnly = false) }
        result.values.toList()
    }

    fun hasTags(token: String, callback: (Boolean) -> Unit) {
        execute({ callback(it.getOrDefault(false)) }) {
            val json = request("GET", "/object/note_tags", token)
            val data = json.optJSONObject("data") ?: json
            val tags = data.optJSONArray("tags") ?: JSONArray()
            (0 until tags.length()).any { tags.optJSONObject(it)?.optInt("count") ?: 0 > 0 }
        }
    }

    fun createBookmark(token: String, item: BookmerItem, callback: (Result<String?>) -> Unit) = execute(callback) {
        val json = request("POST", "/bookmark?path=${encode(item.parentId)}", token,
            JSONObject().put("title", item.title).put("target", item.targetUrl).put("note", item.note.orEmpty()))
        extractCreatedId(json, "bookmark")
    }

    fun createFolder(token: String, item: BookmerItem, callback: (Result<String?>) -> Unit) = execute(callback) {
        val parent = if (item.parentId == BookmerUrls.ARCHIVE) BookmerUrls.ROOT else item.parentId
        val json = request("POST", "/folder?path=${encode(parent)}", token, JSONObject().put("title", item.title).put("note", item.note.orEmpty()))
        extractCreatedId(json, "folder")
    }

    fun rename(token: String, item: BookmerItem, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        request("PATCH", "/${if (item.kind == ItemKind.FOLDER) "folder" else "bookmark"}?id=${encode(item.remoteId ?: item.id)}", token,
            JSONObject().put("title", item.title))
    }

    fun move(token: String, item: BookmerItem, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        val body = if (item.parentId in setOf(BookmerUrls.ARCHIVE, BookmerUrls.HIDDEN)) JSONObject().put("attributes", "Archived") else JSONObject().put("path", item.parentId)
        request("PATCH", "/${if (item.kind == ItemKind.FOLDER) "folder" else "bookmark"}?id=${encode(item.remoteId ?: item.id)}", token, body)
    }

    fun delete(token: String, item: BookmerItem, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        request("DELETE", "/object/trash?id=${encode(item.remoteId ?: item.id)}&t=${if (item.kind == ItemKind.FOLDER) "f" else "b"}", token)
    }

    fun updateOrder(token: String, items: List<BookmerItem>, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        val array = JSONArray(items.map { JSONObject().put("id", it.remoteId ?: it.id).put("order", it.order) })
        request("PATCH", "/object/update_order", token, JSONObject().put("objects", array))
    }

    fun setFolderView(token: String, folderId: String, mode: ContentViewMode, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        if (folderId == BookmerUrls.ROOT || folderId == BookmerUrls.HIDDEN || folderId == BookmerUrls.TAGS) {
            request("PATCH", "/user", token, JSONObject().put("contentView", mode.name.lowercase().replaceFirstChar { it.uppercase() }))
        } else request("PATCH", "/folder?id=${encode(folderId)}", token, JSONObject().put("view", mode.name.lowercase().replaceFirstChar { it.uppercase() }))
    }

    /** `POST /pay/google` — server verifies the Play purchase token and writes the PRO period. */
    fun registerGoogleSubscription(
        token: String,
        productId: String,
        purchaseToken: String,
        packageName: String,
        orderId: String? = null,
        callback: (Result<GoogleSubscriptionRegistration>) -> Unit,
    ) = execute(callback) {
        val body = JSONObject()
            .put("productId", productId)
            .put("purchaseToken", purchaseToken)
            .put("packageName", packageName)
        if (!orderId.isNullOrBlank()) body.put("orderId", orderId)
        val json = request("POST", "/pay/google", token, body)
        GoogleSubscriptionRegistration(
            active = json.optBoolean("active", true),
            expiresMs = json.optLong("expiresMs"),
        )
    }

    data class SharePageState(val token: String, val title: String, val summary: String, val isPublic: Boolean, val publicUrl: String?)
    data class ToolRow(val id: String, val title: String, val subtitle: String, val url: String? = null, val isFolder: Boolean = false)

    fun ensureSharePage(token: String, folderId: String, callback: (Result<SharePageState>) -> Unit) = execute(callback) {
        val existing = request("GET", "/page/exist?id=${encode(folderId)}", token)
        if (existing.optBoolean("exist")) mapSharePage(existing)
        else mapSharePage(request("POST", "/page?path=${encode(folderId)}", token, JSONObject()))
    }

    fun updateSharePage(token: String, pageToken: String, title: String, summary: String, isPublic: Boolean, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        request("PATCH", "/page?id=${encode(pageToken)}", token,
            JSONObject().put("title", title).put("description", summary).put("isPublic", isPublic))
    }

    private fun mapSharePage(json: JSONObject): SharePageState {
        val page = json.optJSONObject("page") ?: json
        val token = page.optString("url").ifBlank { page.optString("id") }.ifBlank { page.optString("_id") }.ifBlank { json.optString("page_token") }
        if (token.isBlank()) throw BookmerApiException("Invalid share page response")
        val alias = page.optString("alias").takeIf(String::isNotBlank)
        val short = page.optString("shortUrl").ifBlank { page.optString("short_url") }.takeIf(String::isNotBlank)
        return SharePageState(token, page.optString("title"), page.optString("description").ifBlank { page.optString("summary") }, page.optBoolean("isPublic"),
            short ?: alias?.let { "https://bookmer.com/$it" } ?: "https://bookmer.com/$token")
    }

    fun fetchToolRows(token: String, kind: String, callback: (Result<List<ToolRow>>) -> Unit) = execute(callback) {
        val (path, key) = when (kind) {
            "recover" -> "/object/trash" to "trash"
            "broken" -> "/object/broken_links" to "broken"
            else -> "/team/shared_with_me" to "items"
        }
        val response = request("GET", path, token)
        val data = response.optJSONObject("data") ?: response
        val rows = data.optJSONArray(key) ?: JSONArray()
        (0 until rows.length()).mapNotNull { index ->
            val raw = rows.optJSONObject(index) ?: return@mapNotNull null
            when (kind) {
                "recover" -> {
                    val folder = raw.optJSONObject("folder"); val bookmark = raw.optJSONObject("bookmark")
                    val id = raw.optString("id").ifBlank { folder?.optString("id").orEmpty() }.ifBlank { bookmark?.optString("id").orEmpty() }
                    if (id.isBlank()) null else ToolRow(id, folder?.optString("title") ?: bookmark?.optString("title") ?: "Deleted Item",
                        "Available for recovery", bookmark?.optString("target"), folder != null)
                }
                "broken" -> {
                    val bookmark = raw.optJSONObject("bookmark")
                    val id = raw.optString("id").ifBlank { bookmark?.optString("id").orEmpty() }
                    if (id.isBlank()) null else ToolRow(id, bookmark?.optString("title") ?: raw.optString("title", "Broken Link"),
                        listOfNotNull(raw.optString("linkIssueKind").takeIf(String::isNotBlank), raw.optInt("linkCheckHttpStatus").takeIf { it > 0 }?.let { "HTTP $it" }).joinToString(" · "),
                        bookmark?.optString("target") ?: raw.optString("target"))
                }
                else -> {
                    val id = raw.optString("id"); if (id.isBlank()) null else ToolRow(id, raw.optString("folderName", "Shared Folder"),
                        "${raw.optString("ownerName", "Someone")} · ${raw.optString("role", "Member")}")
                }
            }
        }
    }

    fun scanBrokenLinks(token: String, callback: (Result<Unit>) -> Unit) = mutate(callback) { request("POST", "/object/broken_links/scan", token, JSONObject()) }
    fun restoreTrash(token: String, row: ToolRow, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        request("POST", "/object/trash?id=${encode(row.id)}&t=${if (row.isFolder) "f" else "b"}", token, JSONObject())
    }
    fun leaveShared(token: String, row: ToolRow, callback: (Result<Unit>) -> Unit) = mutate(callback) {
        request("POST", "/team/remove", token, JSONObject().put("inviteId", row.id))
    }

    fun identityLogout(email: String?, callback: () -> Unit) {
        executor.execute {
            runCatching {
                val returnTo = encode("https://www.bookmer.com/login?loggedOut=1")
                val emailQuery = email?.let { "&accountEmail=${encode(it)}" }.orEmpty()
                val url = URL("https://id.bookmer.com/api/auth/bookmer-signout?returnTo=$returnTo$emailQuery")
                (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000; readTimeout = 5_000
                    CookieManager.getInstance().getCookie("https://id.bookmer.com")?.let { setRequestProperty("Cookie", it) }
                    inputStream.use { it.readBytes() }
                    disconnect()
                }
            }
            main.post(callback)
        }
    }

    private fun parseObjects(json: JSONObject, fallbackParent: String, archiveRoot: Boolean): List<BookmerItem> {
        val data = json.optJSONObject("data") ?: json
        val root = data.optJSONArray("root") ?: data.optJSONArray("archive") ?: json.optJSONArray("root") ?: json.optJSONArray("archive") ?: JSONArray()
        return (0 until root.length()).mapNotNull { index ->
            val raw = root.optJSONObject(index) ?: return@mapNotNull null
            val bookmark = raw.optJSONObject("bookmark")
            val folder = raw.optJSONObject("folder")
            val icon = raw.optJSONObject("icon")
            // Prefer non-blank API parent; empty string must not win over the path we queried.
            val apiParent = raw.opt("parent").let { value ->
                when (value) {
                    is String -> value.takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            val parent = when {
                archiveRoot && bookmark != null -> BookmerUrls.HIDDEN
                archiveRoot -> BookmerUrls.ARCHIVE
                else -> BookmerItem.normalizedParent(apiParent ?: fallbackParent)
            }
            if (bookmark != null) {
                val target = bookmark.optString("target").takeIf { it.startsWith("http") } ?: return@mapNotNull null
                val id = raw.optString("id").ifBlank { bookmark.optString("id") }.ifBlank { UUID.randomUUID().toString() }
                BookmerItem.bookmark(bookmark.optString("title", "Bookmark"), target, parent, raw.optInt("order", index)).copy(
                    id = id, remoteId = id, iconUrl = absoluteIcon(icon?.optString("url")) ?: BookmerItem.iconFor(target),
                    iconBackground = raw.optString("navigationIconBackground").takeIf(String::isNotBlank) ?: icon?.optString("background")?.takeIf(String::isNotBlank),
                    iconX = raw.optDouble("navigationIconX", icon?.optDouble("x", 0.0) ?: 0.0),
                    iconY = raw.optDouble("navigationIconY", icon?.optDouble("y", 0.0) ?: 0.0),
                    iconZoom = raw.optDouble("navigationIconZoom", icon?.optDouble("zoom", -22.0) ?: -22.0),
                    note = bookmark.optString("note").ifBlank { raw.optString("note") }.takeIf(String::isNotBlank),
                    createdAt = parseDate(raw.opt("createdAt")), updatedAt = System.currentTimeMillis(),
                    previewImage = bookmark.optString("previewImage").takeIf(String::isNotBlank),
                    customPreviewImage = bookmark.optString("customPreviewImage").takeIf(String::isNotBlank),
                )
            } else if (folder != null) {
                val id = raw.optString("id").ifBlank { folder.optString("id") }.ifBlank { UUID.randomUUID().toString() }
                val navigation = raw.optString("navigationIcon").ifBlank { folder.optString("navigationIcon") }
                    .ifBlank { icon?.optString("url").orEmpty() }.takeIf(String::isNotBlank)
                val folderIcon = when {
                    navigation != null && BookmerIconUrl.nativeEmoji(navigation) != null -> navigation
                    else -> BookmerIconUrl.resolveFolderIcon(navigation)
                        ?: BookmerIconUrl.resolveBookmarkIcon(navigation)
                }
                BookmerItem.folder(folder.optString("title", "Folder"), parent, raw.optInt("order", index)).copy(
                    id = id, remoteId = id, iconUrl = folderIcon,
                    iconBackground = raw.optString("navigationIconBackground", "#F2F2F7"),
                    iconX = raw.optDouble("navigationIconX"), iconY = raw.optDouble("navigationIconY"), iconZoom = raw.optDouble("navigationIconZoom"),
                    createdAt = parseDate(raw.opt("createdAt")), updatedAt = System.currentTimeMillis(),
                    contentView = folder.optString("view").takeIf(String::isNotBlank),
                )
            } else null
        }
    }

    private fun absoluteIcon(raw: String?): String? = BookmerIconUrl.resolveBookmarkIcon(raw)

    private fun parseDate(value: Any?): Long = when (value) {
        is Number -> if (value.toLong() > 1_000_000_000_000) value.toLong() else value.toLong() * 1000
        is String -> runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value)?.time
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value)?.time
                ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
        else -> System.currentTimeMillis()
    }

    private fun extractCreatedId(json: JSONObject, type: String): String? {
        val data = json.optJSONObject("data") ?: json
        return data.optJSONObject(type)?.optString("id")?.takeIf(String::isNotBlank)
            ?: data.optString("id").takeIf(String::isNotBlank)
    }

    private fun request(method: String, path: String, token: String? = null, body: JSONObject? = null): JSONObject {
        val connection = URL("${BookmerUrls.API}$path").openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000; connection.readTimeout = 20_000
        if (method == "PATCH") { connection.requestMethod = "POST"; connection.setRequestProperty("X-HTTP-Method-Override", "PATCH") }
        else connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        body?.let {
            connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { output -> output.write(it.toString().toByteArray()) }
        }
        val status = connection.responseCode
        val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299 && !(method == "DELETE" && status == 404)) throw BookmerApiException(text.ifBlank { "Request failed" }, status)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun <T> execute(callback: (Result<T>) -> Unit, action: () -> T) {
        executor.execute { val result = runCatching(action); main.post { callback(result) } }
    }
    private fun mutate(callback: (Result<Unit>) -> Unit, action: () -> Unit) = execute(callback, action)
    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

class BookmerSyncService(
    private val api: BookmerApiClient,
    private val bookmarks: BookmarkRepository,
    private val session: SecureSessionStore,
) {
    fun pull(callback: (Result<Unit>) -> Unit = {}) {
        val token = session.value.token ?: return
        api.fetchLibrary(token) { result ->
            result.onSuccess { remote -> bookmarks.replaceFromRemote(remote); api.hasTags(token, bookmarks::setRemoteTags) }
            callback(result.map { })
        }
    }

    fun pushCreate(item: BookmerItem) {
        val token = session.value.token ?: return
        if (item.kind == ItemKind.FOLDER) api.createFolder(token, item) { result -> result.getOrNull()?.let { remote -> bookmarks.update(item.copy(remoteId = remote)) } }
        else api.createBookmark(token, item) { result -> result.getOrNull()?.let { remote -> bookmarks.update(item.copy(remoteId = remote)) } }
    }
    fun pushRename(item: BookmerItem) { session.value.token?.let { api.rename(it, item) {} } }
    fun pushMove(item: BookmerItem) { session.value.token?.let { api.move(it, item) {} } }
    fun pushDelete(item: BookmerItem) { session.value.token?.let { api.delete(it, item) {} } }
    fun pushOrder(items: List<BookmerItem>) { session.value.token?.let { api.updateOrder(it, items) {} } }
    fun pushView(folderId: String, mode: ContentViewMode) { session.value.token?.let { api.setFolderView(it, folderId, mode) {} } }
}
