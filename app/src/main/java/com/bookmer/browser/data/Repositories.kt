package com.bookmer.browser.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

private class AtomicJsonFile(context: Context, name: String) {
    private val file = File(context.filesDir, "bookmer/$name")
    fun read(): String? = runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()
    fun write(value: String) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(value)
        if (!temporary.renameTo(file)) {
            file.writeText(value)
            temporary.delete()
        }
    }
    fun clear() = file.delete()
}

class BookmarkRepository(private val context: Context) {
    private val storage = AtomicJsonFile(context, "library.v2.json")
    private val io = Executors.newSingleThreadExecutor()
    val items = mutableStateListOf<BookmerItem>()
    val folderStack = mutableStateListOf<String>()
    val deletedIds = mutableSetOf<String>()
    var remoteHasNoteTags by mutableStateOf(false)
        private set

    val currentFolderId: String get() = folderStack.lastOrNull() ?: BookmerUrls.ROOT
    val currentFolderTitle: String get() = when (val id = currentFolderId) {
        BookmerUrls.ROOT -> "Collection"; BookmerUrls.TAGS -> "Tags"; BookmerUrls.HIDDEN -> "Hidden"
        else -> items.firstOrNull { it.id == id }?.title ?: "Collection"
    }
    val visibleItems: List<BookmerItem> get() = items
        .filter { BookmerItem.normalizedParent(it.parentId) == currentFolderId }
        .sortedWith(compareBy<BookmerItem> { it.order }.thenBy { it.title.lowercase() })

    init {
        load()
        if (items.isEmpty()) seedDefaults()
    }

    fun navigateToFolder(id: String) {
        val normalized = BookmerItem.normalizedParent(id)
        if (normalized == BookmerUrls.ROOT) { folderStack.clear(); return }
        if (normalized == BookmerUrls.TAGS || normalized == BookmerUrls.HIDDEN) {
            folderStack.clear(); folderStack.add(normalized); return
        }
        val path = mutableListOf<String>()
        var current: String? = normalized
        val seen = mutableSetOf<String>()
        while (current != null && current != BookmerUrls.ROOT && seen.add(current)) {
            val folder = items.firstOrNull { it.id == current && it.kind == ItemKind.FOLDER } ?: break
            path.add(0, folder.id)
            current = if (folder.parentId == BookmerUrls.ARCHIVE) BookmerUrls.ROOT else folder.parentId
        }
        folderStack.clear(); folderStack.addAll(path)
    }

    fun back(): Boolean {
        if (folderStack.isEmpty()) return false
        folderStack.removeAt(folderStack.lastIndex)
        return true
    }

    fun addFolder(title: String, parent: String = currentFolderId): BookmerItem {
        val item = BookmerItem.folder(title.trim().ifBlank { "New Folder" }, parent, nextOrder(parent))
        items.add(item); persist(); return item
    }

    fun childFolders(parentId: String): List<BookmerItem> {
        val parent = BookmerItem.normalizedParent(parentId)
        return items
            .filter { it.kind == ItemKind.FOLDER && BookmerItem.normalizedParent(it.parentId) == parent }
            .sortedWith(compareBy<BookmerItem> { it.order }.thenBy { it.title.lowercase() })
    }

    /** Root sidebar folders: Collection children + archived folders, merged by order. */
    fun collectionSidebarRootFolders(): List<BookmerItem> {
        val collection = childFolders(BookmerUrls.ROOT)
        val archived = childFolders(BookmerUrls.ARCHIVE).filter { it.id != BookmerUrls.HIDDEN }
        return (collection + archived).sortedWith(compareBy<BookmerItem> { it.order }.thenBy { it.title.lowercase() })
    }

    fun foldersInLibrary(): List<BookmerItem> =
        items.filter { it.kind == ItemKind.FOLDER && it.id !in setOf(BookmerUrls.ROOT, BookmerUrls.TAGS, BookmerUrls.HIDDEN, BookmerUrls.ARCHIVE) }
            .sortedBy { it.title.lowercase() }

    val hasHiddenNavContent: Boolean
        get() = items.any {
            it.parentId == BookmerUrls.ARCHIVE || it.parentId == BookmerUrls.HIDDEN || it.id == BookmerUrls.HIDDEN
        }

    private val folderNavPrefs = context.getSharedPreferences("bookmer.folderNav", Context.MODE_PRIVATE)
    var expandedFolderNavIds by mutableStateOf(folderNavPrefs.getStringSet("expandedIDs", emptySet()).orEmpty())
        private set

    fun isFolderNavExpanded(id: String): Boolean = expandedFolderNavIds.contains(id)

    fun toggleFolderNavExpanded(id: String) {
        expandedFolderNavIds = if (expandedFolderNavIds.contains(id)) {
            expandedFolderNavIds - id
        } else {
            expandedFolderNavIds + id
        }
        folderNavPrefs.edit().putStringSet("expandedIDs", HashSet(expandedFolderNavIds)).apply()
    }

    fun ensureFolderNavExpanded(id: String) {
        if (id == BookmerUrls.ROOT || id == BookmerUrls.ARCHIVE || id == BookmerUrls.TAGS ||
            id == BookmerUrls.HIDDEN || expandedFolderNavIds.contains(id)
        ) return
        expandedFolderNavIds = expandedFolderNavIds + id
        folderNavPrefs.edit().putStringSet("expandedIDs", HashSet(expandedFolderNavIds)).apply()
    }

    /** Expand every ancestor so the current folder path is visible in SideFolderNav. */
    fun expandFolderNavPathToCurrent() {
        folderStack.forEach { ensureFolderNavExpanded(it) }
        var cursor = currentFolderId
        val visited = mutableSetOf<String>()
        while (cursor !in setOf(BookmerUrls.ROOT, BookmerUrls.TAGS, BookmerUrls.HIDDEN, BookmerUrls.ARCHIVE) &&
            visited.add(cursor)
        ) {
            val parent = items.firstOrNull { it.id == cursor }?.parentId ?: break
            val normalized = BookmerItem.normalizedParent(parent)
            if (normalized !in setOf(BookmerUrls.ROOT, BookmerUrls.ARCHIVE, BookmerUrls.TAGS, BookmerUrls.HIDDEN)) {
                ensureFolderNavExpanded(normalized)
            }
            cursor = normalized
        }
    }

    fun collect(url: String, title: String, parent: String = currentFolderId, note: String? = null): BookmerItem {
        val item = BookmerItem.bookmark(title.trim().ifBlank { UriTitles.fromUrl(url) }, url, parent, nextOrder(parent)).copy(note = note)
        items.add(item); persist(); return item
    }

    fun update(item: BookmerItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) { items[index] = item.copy(updatedAt = System.currentTimeMillis()); persist() }
    }

    fun move(id: String, parent: String): Boolean {
        if (!canMove(id, parent)) return false
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return false
        items[index] = items[index].copy(parentId = parent, order = nextOrder(parent), updatedAt = System.currentTimeMillis())
        normalizeOrders(items[index].parentId); persist(); return true
    }

    fun canMove(id: String, parent: String): Boolean {
        if (id == parent) return false
        var cursor: String? = parent
        val visited = mutableSetOf<String>()
        while (cursor != null && visited.add(cursor)) {
            if (cursor == id) return false
            cursor = items.firstOrNull { it.id == cursor }?.parentId
        }
        return true
    }

    fun reorder(id: String, destination: Int) {
        val siblings = visibleItems.toMutableList()
        val old = siblings.indexOfFirst { it.id == id }
        if (old < 0) return
        val moving = siblings.removeAt(old)
        siblings.add(destination.coerceIn(0, siblings.size), moving)
        siblings.forEachIndexed { order, item ->
            val index = items.indexOfFirst { it.id == item.id }
            if (index >= 0) items[index] = item.copy(order = order, updatedAt = System.currentTimeMillis())
        }
        persist()
    }

    fun sortCurrent(mode: SortMode) {
        val sorted = when (mode) {
            SortMode.NAME_AZ -> visibleItems.sortedBy { it.title.lowercase() }
            SortMode.NAME_ZA -> visibleItems.sortedByDescending { it.title.lowercase() }
            SortMode.NEWEST -> visibleItems.sortedWith(compareByDescending<BookmerItem> { it.createdAt }.thenBy { it.id })
            SortMode.OLDEST -> visibleItems.sortedWith(compareBy<BookmerItem> { it.createdAt }.thenBy { it.id })
        }
        sorted.forEachIndexed { order, item ->
            val index = items.indexOfFirst { it.id == item.id }
            if (index >= 0) items[index] = item.copy(order = order)
        }
        persist()
    }

    fun delete(id: String) {
        val ids = subtree(id)
        deletedIds.addAll(ids)
        items.removeAll { it.id in ids }
        persist()
    }

    fun search(query: String): List<BookmerItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return items.filter { it.title.lowercase().contains(q) || it.targetUrl.orEmpty().lowercase().contains(q) || it.note.orEmpty().lowercase().contains(q) }
    }

    fun replaceFromRemote(remote: List<BookmerItem>) {
        val clean = remote.filterNot { it.id in deletedIds || it.remoteId in deletedIds }
        items.clear(); items.addAll(clean.distinctBy { it.remoteId ?: it.id }); persist()
    }

    fun setRemoteTags(present: Boolean) { remoteHasNoteTags = present }

    fun setCurrentContentView(mode: ContentViewMode) {
        val id = currentFolderId
        if (id == BookmerUrls.ROOT || id == BookmerUrls.HIDDEN || id == BookmerUrls.TAGS) return
        val index = items.indexOfFirst { it.id == id && it.kind == ItemKind.FOLDER }
        if (index >= 0) { items[index] = items[index].copy(contentView = mode.name.lowercase().replaceFirstChar { it.uppercase() }); persist() }
    }

    fun currentContentView(default: ContentViewMode): ContentViewMode {
        val raw = items.firstOrNull { it.id == currentFolderId }?.contentView ?: return default
        return when (raw.lowercase()) { "list" -> ContentViewMode.LIST; "thumbnail", "card" -> ContentViewMode.THUMBNAIL; "grid" -> ContentViewMode.GRID; else -> default }
    }

    fun resetToGuestDefaults() { items.clear(); folderStack.clear(); deletedIds.clear(); seedDefaults() }

    private fun subtree(id: String): Set<String> {
        val result = mutableSetOf(id)
        var changed: Boolean
        do {
            changed = false
            items.filter { it.parentId in result && it.id !in result }.forEach { result.add(it.id); changed = true }
        } while (changed)
        return result
    }

    private fun nextOrder(parent: String) = (items.filter { it.parentId == parent }.maxOfOrNull { it.order } ?: -1) + 1
    private fun normalizeOrders(parent: String) {
        items.filter { it.parentId == parent }.sortedBy { it.order }.forEachIndexed { order, item ->
            val index = items.indexOfFirst { it.id == item.id }; items[index] = item.copy(order = order)
        }
    }

    private fun load() {
        val json = storage.read() ?: return
        runCatching {
            val root = JSONObject(json)
            val array = root.optJSONArray("items") ?: JSONArray()
            items.addAll((0 until array.length()).map { BookmerItem.fromJson(array.getJSONObject(it)) })
            root.optJSONArray("deletedIDs")?.toStringList()?.let(deletedIds::addAll)
        }
    }

    private fun seedDefaults() {
        runCatching {
            val text = context.assets.open("global_list.json").bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            for (index in 0 until array.length()) {
                val wrapper = array.getJSONObject(index)
                val bookmark = wrapper.optJSONObject("bookmark") ?: continue
                val target = bookmark.optString("target").takeIf { it.startsWith("http") } ?: continue
                items.add(BookmerItem.bookmark(bookmark.optString("title", UriTitles.fromUrl(target)), target,
                    BookmerItem.normalizedParent(wrapper.optString("parent", BookmerUrls.ROOT)), wrapper.optInt("order", index))
                    .copy(id = bookmark.optString("id").ifBlank { UUID.randomUUID().toString() }, note = wrapper.nullableString("note")))
            }
        }
        persist()
    }

    private fun persist() {
        val snapshot = items.map { it.toJson() }
        val deleted = deletedIds.toList()
        io.execute { storage.write(JSONObject().put("items", JSONArray(snapshot)).put("deletedIDs", JSONArray(deleted)).toString()) }
    }
}

class HistoryRepository(context: Context) {
    private val storage = AtomicJsonFile(context, "history.v1.json")
    private val io = Executors.newSingleThreadExecutor()
    val entries = mutableStateListOf<HistoryEntry>()
    init { load() }

    fun record(url: String, title: String) {
        if (!url.startsWith("http")) return
        entries.removeAll { it.url == url && System.currentTimeMillis() - it.visitedAt < 30_000 }
        entries.add(0, HistoryEntry(url = url, title = title.ifBlank { UriTitles.fromUrl(url) }))
        while (entries.size > 5000) entries.removeAt(entries.lastIndex)
        persist()
    }
    fun remove(id: String) { entries.removeAll { it.id == id }; persist() }
    fun clear(since: Long? = null) { if (since == null) entries.clear() else entries.removeAll { it.visitedAt >= since }; persist() }
    fun search(query: String) = entries.filter { it.title.contains(query, true) || it.url.contains(query, true) }
    private fun load() { storage.read()?.let { text -> runCatching { JSONArray(text) }.getOrNull()?.let { a -> entries.addAll((0 until a.length()).map { HistoryEntry.fromJson(a.getJSONObject(it)) }) } } }
    private fun persist() { val snapshot = JSONArray(entries.map { it.toJson() }).toString(); io.execute { storage.write(snapshot) } }
}

object UriTitles {
    fun fromUrl(url: String): String = runCatching { android.net.Uri.parse(url).host?.removePrefix("www.") }.getOrNull() ?: url
}
