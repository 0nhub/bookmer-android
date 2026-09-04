package com.bookmer.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.data.BookmerIconUrl
import com.bookmer.browser.data.BookmerItem
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.ContentViewMode
import com.bookmer.browser.data.ItemKind
import com.bookmer.browser.ui.theme.bookmerIsDarkTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CollectionScreen(model: BrowserViewModel, modifier: Modifier = Modifier) {
    val repository = model.bookmarks
    val settings = model.preferences.settings
    val background = if (bookmerIsDarkTheme()) Color(0xFF19191B) else Color(0xFFF7F7F9)
    val labelColor = if (settings.wallpaper != null) Color.fromHex(settings.wallpaperTextColor, Color.White) else MaterialTheme.colorScheme.onBackground
    Box(modifier.background(background)) {
        settings.wallpaper?.let { wallpaper ->
            RemoteImage(wallpaper, Modifier.fillMaxSize().blur(settings.wallpaperBlur.dp), ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = settings.wallpaperDim.coerceIn(0f, .8f))))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (repository.folderStack.isNotEmpty()) Breadcrumbs(model, labelColor)
            when (repository.currentContentView(settings.collectionViewMode)) {
                ContentViewMode.LIST -> CollectionList(model, Modifier.weight(1f))
                ContentViewMode.THUMBNAIL -> CollectionThumbnails(model, Modifier.weight(1f))
                else -> CollectionGrid(model, labelColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CollectionList(model: BrowserViewModel, modifier: Modifier) {
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(model.bookmarks.visibleItems, key = { it.id }) { item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = .88f)).clickable {
                if (item.kind == ItemKind.FOLDER) model.bookmarks.navigateToFolder(item.id) else item.targetUrl?.let(model::load)
                model.updateNavigationState()
            }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BookmerTileIcon(item, model, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(item.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    item.targetUrl?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun CollectionThumbnails(model: BrowserViewModel, modifier: Modifier) {
    LazyVerticalGrid(GridCells.Fixed(2), modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(model.bookmarks.visibleItems, key = { _, item -> item.id }) { _, item ->
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = .9f)).clickable {
                if (item.kind == ItemKind.FOLDER) model.bookmarks.navigateToFolder(item.id) else item.targetUrl?.let(model::load)
                model.updateNavigationState()
            }) {
                val preview = item.customPreviewImage ?: item.previewImage ?: item.iconUrl
                Box(Modifier.fillMaxWidth().aspectRatio(1.45f).background(Color.fromHex(item.customPreviewBackground ?: item.iconBackground, Color(0xFFEFEFF2))), contentAlignment = Alignment.Center) {
                    if (item.kind == ItemKind.FOLDER && preview.isNullOrBlank()) BookmerTileIcon(item, model, Modifier.size(66.dp).clip(RoundedCornerShape(16.dp)))
                    else RemoteImage(preview, Modifier.fillMaxSize(), ContentScale.Crop)
                }
                Text(item.title, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
            }
        }
    }
}

@Composable
private fun Breadcrumbs(model: BrowserViewModel, color: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { model.bookmarks.navigateToFolder(BookmerUrls.ROOT); model.updateNavigationState() }, Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Home, "Collection", tint = color)
        }
        model.bookmarks.folderStack.forEach { id ->
            Text("›", color = color.copy(alpha = .6f), modifier = Modifier.padding(horizontal = 3.dp))
            TextButton(onClick = { model.bookmarks.navigateToFolder(id); model.updateNavigationState() }) {
                Text(if (id == BookmerUrls.HIDDEN) "Hidden" else if (id == BookmerUrls.TAGS) "Tags" else model.bookmarks.items.firstOrNull { it.id == id }?.title ?: "Folder", color = color)
            }
        }
    }
}

@Composable
private fun CollectionGrid(model: BrowserViewModel, labelColor: Color, modifier: Modifier) {
    val visible = model.bookmarks.visibleItems
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
            CollectionTile(model, item, index, labelColor)
        }
    }
}

@Composable
private fun CollectionTile(model: BrowserViewModel, item: BookmerItem, index: Int, labelColor: Color) {
    var menu by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var hoverFolder by remember { mutableStateOf<String?>(null) }
    var hoverStarted by remember { mutableLongStateOf(0L) }
    val visible = model.bookmarks.visibleItems

    Column(
        modifier = Modifier.padding(horizontal = 3.dp).zIndex(if (dragging) 4f else 0f)
            .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
            .scale(if (dragging) 1.08f else 1f)
            .clickable {
                if (item.kind == ItemKind.FOLDER) { model.bookmarks.navigateToFolder(item.id); model.updateNavigationState() }
                else item.targetUrl?.let(model::load)
            }
            .pointerInput(item.id, visible.size) {
                val dragSlop = viewConfiguration.touchSlop
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragging = true; dragX = 0f; dragY = 0f },
                    onDragCancel = { dragging = false; dragX = 0f; dragY = 0f; hoverFolder = null },
                    onDragEnd = {
                        val moved = abs(dragX) > dragSlop || abs(dragY) > dragSlop
                        if (!moved) {
                            menu = true
                        } else {
                            val folder = hoverFolder
                            if (folder != null && System.currentTimeMillis() - hoverStarted >= 500) {
                                model.bookmarks.move(item.id, folder)
                                model.bookmarks.items.firstOrNull { it.id == item.id }?.let(model.sync::pushMove)
                            } else {
                                val columnShift = (dragX / (size.width.coerceAtLeast(1))).roundToInt()
                                val rowShift = (dragY / (size.height.coerceAtLeast(1))).roundToInt()
                                model.bookmarks.reorder(item.id, index + columnShift + rowShift * 4)
                                model.sync.pushOrder(model.bookmarks.visibleItems)
                            }
                        }
                        dragging = false; dragX = 0f; dragY = 0f; hoverFolder = null
                    },
                    onDrag = { change, amount ->
                        change.consume(); dragX += amount.x; dragY += amount.y
                        val target = index + (dragX / size.width.coerceAtLeast(1)).roundToInt() + (dragY / size.height.coerceAtLeast(1)).roundToInt() * 4
                        val targetItem = visible.getOrNull(target)
                        if (targetItem?.kind == ItemKind.FOLDER && targetItem.id != item.id) {
                            if (hoverFolder != targetItem.id) { hoverFolder = targetItem.id; hoverStarted = System.currentTimeMillis() }
                        } else hoverFolder = null
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            BookmerTileIcon(item, model, Modifier.size(66.dp).clip(RoundedCornerShape(33.dp)).then(
                if (dragging) Modifier.alpha(.92f) else Modifier
            ))
            BookmerMenu(expanded = menu, onDismissRequest = { menu = false }, minWidth = 220.dp) {
                BookmerMenuItem(
                    if (item.kind == ItemKind.FOLDER) "Open" else "Open in New Tab",
                    {
                        menu = false
                        if (item.kind == ItemKind.FOLDER) model.bookmarks.navigateToFolder(item.id)
                        else item.targetUrl?.let { model.load(it, inNewTab = true) }
                        model.updateNavigationState()
                    },
                    icon = if (item.kind == ItemKind.FOLDER) Icons.Rounded.Folder else Icons.AutoMirrored.Rounded.OpenInNew,
                )
                BookmerMenuItem("Edit", { menu = false; edit = true }, icon = Icons.Rounded.Edit)
                BookmerMenuItem("Delete", { menu = false; confirmDelete = true }, icon = Icons.Rounded.Delete, danger = true)
            }
        }
        if (!model.settings.hideTitles) {
            Text(
                item.title,
                color = labelColor,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp).padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (edit) EditItemDialog(item, onDismiss = { edit = false }, onSave = { model.bookmarks.update(it); model.sync.pushRename(it); edit = false })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete ${item.title}?") },
        text = { if (item.kind == ItemKind.FOLDER) Text("Everything inside this folder will also be deleted.") },
        confirmButton = { TextButton(onClick = { model.sync.pushDelete(item); model.bookmarks.delete(item.id); confirmDelete = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
}

@Composable
private fun BookmerTileIcon(item: BookmerItem, model: BrowserViewModel, modifier: Modifier) {
    val background = Color.fromHex(item.iconBackground, Color(0xFFF1F1F4))
    val emoji = BookmerIconUrl.nativeEmoji(item.iconUrl)
    Box(modifier.background(background), contentAlignment = Alignment.Center) {
        when {
            emoji != null -> Text(emoji, fontSize = 28.sp, textAlign = TextAlign.Center)
            item.kind == ItemKind.FOLDER && item.iconUrl.isNullOrBlank() -> {
                val children = model.bookmarks.items.filter { it.parentId == item.id }.sortedBy { it.order }.take(4)
                if (children.isEmpty()) Icon(Icons.Rounded.Folder, null, tint = Color(0xFF55555A), modifier = Modifier.size(38.dp))
                else Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    children.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            row.forEach { child ->
                                RemoteImage(
                                    BookmerIconUrl.candidates(child),
                                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)),
                                    ContentScale.Fit,
                                    Color.White,
                                )
                            }
                        }
                    }
                }
            }
            else -> RemoteImage(
                BookmerIconUrl.candidates(item),
                Modifier.fillMaxSize().offset(x = item.iconX.dp, y = item.iconY.dp).scale(item.iconScale),
                ContentScale.Fit,
                background,
            )
        }
    }
}

@Composable
private fun EditItemDialog(item: BookmerItem, onDismiss: () -> Unit, onSave: (BookmerItem) -> Unit) {
    var title by remember { mutableStateOf(item.title) }
    var url by remember { mutableStateOf(item.targetUrl.orEmpty()) }
    var note by remember { mutableStateOf(item.note.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.kind == ItemKind.FOLDER) "Edit Folder" else "Edit Bookmark") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
            if (item.kind == ItemKind.BOOKMARK) {
                OutlinedTextField(url, { url = it }, label = { Text("URL") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Notes") }, minLines = 2)
            }
        } },
        confirmButton = { Button(onClick = { onSave(item.copy(title = title.trim().ifBlank { item.title }, targetUrl = url.trim().ifBlank { item.targetUrl }, note = note.trim().takeIf(String::isNotEmpty))) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
