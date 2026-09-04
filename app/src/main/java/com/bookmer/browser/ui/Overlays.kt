package com.bookmer.browser.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.data.BookmerItem
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.ItemKind
import com.bookmer.browser.data.BookmerApiClient
import java.text.DateFormat
import java.util.Date

@Composable
private fun OverlayHeader(title: String, close: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = close) { Icon(Icons.Rounded.Check, "Done") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun HistoryScreen(model: BrowserViewModel) {
    var query by remember { mutableStateOf("") }
    var clear by remember { mutableStateOf(false) }
    val entries = if (query.isBlank()) model.history.entries else model.history.search(query)
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            OverlayHeader("History", model::dismissOverlay) { IconButton(onClick = { clear = true }) { Icon(Icons.Rounded.Delete, "Clear History") } }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search History") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Row(Modifier.fillMaxWidth().clickable { model.load(entry.url); model.dismissOverlay() }.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.visitedAt)), style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { model.history.remove(entry.id) }) { Icon(Icons.Rounded.Delete, "Delete") }
                    }
                    HorizontalDivider(Modifier.padding(start = 58.dp))
                }
            }
        }
    }
    if (clear) ClearDataDialog(onDismiss = { clear = false }) { _, _ -> model.history.clear(); clear = false }
}

@Composable
fun TabHistoryScreen(model: BrowserViewModel) {
    val entries = remember(model.selectedTabId, model.currentTab.url, model.canGoBack, model.canGoForward, model.isLoading) {
        model.tabHistoryEntries()
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            OverlayHeader("Tab History", model::dismissOverlay)
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("No Tab History", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pages you open in this tab will show up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp)) {
                    items(entries, key = { "${it.index}:${it.url}" }) { entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { model.goToTabHistoryEntry(entry) }
                                .background(
                                    if (entry.isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent,
                                )
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                null,
                                tint = if (entry.isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    entry.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (entry.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                Text(
                                    entry.url,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (entry.isCurrent) {
                                    Text(
                                        "Current page",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 58.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen(model: BrowserViewModel) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            OverlayHeader("Downloads", model::dismissOverlay) {
                if (model.downloads.isNotEmpty()) TextButton(onClick = model::clearDownloads) { Text("Clear") }
            }
            LazyColumn {
                items(model.downloads, key = { it.id }) { download ->
                    Row(Modifier.fillMaxWidth().clickable {
                        val context = model.getApplication<android.app.Application>()
                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        manager.getUriForDownloadedFile(download.id)?.let { uri ->
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                        }
                    }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Download, null)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(download.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(download.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 54.dp))
                }
            }
        }
    }
}

@Composable
fun NavigateScreen(model: BrowserViewModel) {
    var query by remember { mutableStateOf("") }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderTitle by remember { mutableStateOf("Folder") }
    val searchFocused = remember { mutableStateOf(false) }
    val q = query.trim()
    val searching = q.isNotEmpty()
    val store = model.bookmarks
    // Observe expand set so chevrons recompose.
    val expanded = store.expandedFolderNavIds

    LaunchedEffect(Unit) {
        store.expandFolderNavPathToCurrent()
        if (model.session.value.isSignedIn) model.sync.pull()
    }

    data class NavRow(
        val id: String,
        val title: String,
        val depth: Int,
        val hasChildren: Boolean,
        val kind: String, // collection | tags | hidden | folder
    )

    fun appendChildren(parentId: String, depth: Int, into: MutableList<NavRow>) {
        store.childFolders(parentId).forEach { folder ->
            val kids = store.childFolders(folder.id)
            into += NavRow(folder.id, folder.title, depth, kids.isNotEmpty(), "folder")
            if (expanded.contains(folder.id)) appendChildren(folder.id, depth + 1, into)
        }
    }

    val rows: List<NavRow> = remember(store.items.toList(), expanded, q, store.remoteHasNoteTags, store.hasHiddenNavContent) {
        if (searching) {
            store.foldersInLibrary()
                .filter { it.title.contains(q, ignoreCase = true) || (it.note?.contains(q, ignoreCase = true) == true) }
                .map { NavRow(it.id, it.title, 0, store.childFolders(it.id).isNotEmpty(), "folder") }
        } else {
            buildList {
                add(NavRow(BookmerUrls.ROOT, "Collection", 0, false, "collection"))
                if (store.remoteHasNoteTags) add(NavRow(BookmerUrls.TAGS, "Tags", 0, false, "tags"))
                if (store.hasHiddenNavContent) add(NavRow(BookmerUrls.HIDDEN, "Hidden", 0, false, "hidden"))
                store.collectionSidebarRootFolders().forEach { folder ->
                    val kids = store.childFolders(folder.id)
                    add(NavRow(folder.id, folder.title, 0, kids.isNotEmpty(), "folder"))
                    if (expanded.contains(folder.id)) appendChildren(folder.id, 1, this)
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            searchFocused.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                inner()
                            }
                        },
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (searchFocused.value || query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            query = ""
                            searchFocused.value = false
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(Icons.Rounded.Close, "Close search")
                    }
                } else {
                    IconButton(
                        onClick = {
                            newFolderTitle = "Folder"
                            showNewFolder = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, "Add folder")
                    }
                }
                IconButton(onClick = model::dismissOverlay) {
                    Icon(Icons.Rounded.Check, "Done")
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            ) {
                if (searching && rows.isEmpty()) {
                    item {
                        Text(
                            "No folders found",
                            Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(rows, key = { "${it.kind}:${it.id}:${it.depth}" }) { row ->
                    val icon = when (row.kind) {
                        "collection" -> Icons.Rounded.Home
                        "tags" -> Icons.Rounded.Label
                        "hidden" -> Icons.Rounded.VisibilityOff
                        else -> Icons.Rounded.Folder
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clickable {
                                    store.navigateToFolder(row.id)
                                    model.dismissOverlay()
                                    model.updateNavigationState()
                                }
                                .padding(start = (14 + row.depth * 18).dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, null, Modifier.size(22.dp))
                            Text(row.title, Modifier.padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (row.hasChildren && !searching) {
                            IconButton(onClick = { store.toggleFolderNavExpanded(row.id) }) {
                                Icon(
                                    if (expanded.contains(row.id)) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                    if (expanded.contains(row.id)) "Collapse" else "Expand",
                                    tint = if (expanded.contains(row.id)) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else if (row.hasChildren) {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                null,
                                Modifier.padding(end = 14.dp).size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderTitle,
                    onValueChange = { newFolderTitle = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val parent = store.currentFolderId.let {
                        if (it == BookmerUrls.TAGS) BookmerUrls.ROOT else it
                    }
                    val created = store.addFolder(newFolderTitle.ifBlank { "Folder" }, parent)
                    model.sync.pushCreate(created)
                    store.ensureFolderNavExpanded(parent)
                    store.folderStack.forEach { store.ensureFolderNavExpanded(it) }
                    showNewFolder = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun BookmarkToolsScreen(model: BrowserViewModel) {
    var kind by remember { mutableStateOf(model.bookmarkToolsKind) }
    var rows by remember { mutableStateOf<List<BookmerApiClient.ToolRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val token = model.session.value.token
    LaunchedEffect(kind, reload) {
        if (token == null) { error = "Sign in required"; return@LaunchedEffect }
        loading = true; error = null
        model.api.fetchToolRows(token, kind) { result -> loading = false; result.onSuccess { rows = it }.onFailure { error = it.message } }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            OverlayHeader("Bookmarks", model::dismissOverlay)
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("broken" to "Broken links", "recover" to "Recover", "shared" to "Shared folders").forEach { (value, label) ->
                    TextButton(onClick = { kind = value }, Modifier.weight(1f)) { Text(if (kind == value) "✓ $label" else label, maxLines = 1) }
                }
            }
            if (kind == "broken") Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(enabled = !loading && token != null, onClick = { token?.let { model.api.scanBrokenLinks(it) { reload++ } } }) { Text("Scan") }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading…") }
                error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
                rows.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("No items found") }
                else -> LazyColumn {
                    items(rows, key = { it.id }) { row ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = row.url?.startsWith("http") == true) { row.url?.let { model.load(it); model.dismissOverlay() } }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (row.isFolder) Icons.Rounded.Folder else Icons.Rounded.OpenInNew, null)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(row.title); Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            if (kind == "recover") TextButton(onClick = { token?.let { model.api.restoreTrash(it, row) { model.sync.pull(); reload++ } } }) { Text("Restore") }
                            if (kind == "shared") TextButton(onClick = { token?.let { model.api.leaveShared(it, row) { model.sync.pull(); reload++ } } }) { Text("Leave") }
                        }
                        HorizontalDivider(Modifier.padding(start = 54.dp))
                    }
                }
            }
        }
    }
}
