package com.bookmer.browser.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.ItemKind
import com.bookmer.browser.data.SearchEngine
import com.bookmer.browser.data.BookmerApiClient
import com.bookmer.browser.R
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

@Composable
fun SetupWelcomeScreen(model: BrowserViewModel) {
    var step by remember { mutableStateOf(0) }
    var engine by remember { mutableStateOf(model.settings.searchEngine) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 28.dp)) {
        if (step == 0) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.bookmer_logo),
                    contentDescription = "Bookmer",
                    modifier = Modifier.width(220.dp).height(65.dp),
                )
                Spacer(Modifier.height(56.dp))
                Button(onClick = { model.presentLogin() }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text("Log in") }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Continue without signing in") }
                Spacer(Modifier.weight(2f))
            }
        } else {
            Column(
                Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("Choose a Search Engine", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text("You can change this later in Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Column(Modifier.height(340.dp).verticalScroll(rememberScrollState())) {
                    SearchEngine.entries.sortedBy { it.label }.forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable { engine = option }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(option.label, Modifier.weight(1f)); if (engine == option) Icon(Icons.Rounded.Check, null)
                        }
                    }
                }
                Button(onClick = { model.preferences.update { it.copy(setupCompleted = true, searchEngine = engine) } }, Modifier.fillMaxWidth()) { Text("Start Browsing") }
            }
        }
    }
}

@Composable
fun BlockedPage(url: String, goHome: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.Block, null, Modifier.size(58.dp), tint = MaterialTheme.colorScheme.error)
            Text("This Site Is Blocked", style = MaterialTheme.typography.headlineSmall)
            Text(url, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = goHome) { Text("Back to Collection") }
        }
    }
}

@Composable
fun BookmerDialogs(model: BrowserViewModel) {
    model.pendingCollect?.let { collect ->
        var title by remember(collect) { mutableStateOf(collect.title) }
        var url by remember(collect) { mutableStateOf(collect.url) }
        var note by remember(collect) { mutableStateOf("") }
        var parent by remember(collect) { mutableStateOf(model.bookmarks.currentFolderId) }
        var folders by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { model.pendingCollect = null },
            title = { Text("Collect") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("URL") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { folders = true }, Modifier.fillMaxWidth()) {
                        Text(model.bookmarks.items.firstOrNull { it.id == parent }?.title ?: "Collection", Modifier.weight(1f)); Icon(Icons.Rounded.ExpandMore, null)
                    }
                    BookmerMenu(expanded = folders, onDismissRequest = { folders = false }, minWidth = 240.dp) {
                        BookmerMenuItem(
                            "Collection",
                            { parent = BookmerUrls.ROOT; folders = false },
                            selected = parent == BookmerUrls.ROOT,
                        )
                        model.bookmarks.items.filter { it.kind == ItemKind.FOLDER }.sortedBy { it.title }.forEach { folder ->
                            BookmerMenuItem(
                                folder.title,
                                { parent = folder.id; folders = false },
                                selected = parent == folder.id,
                            )
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Notes") }, minLines = 2)
            } },
            confirmButton = { TextButton(onClick = { model.confirmCollect(title, url, parent, note.takeIf { it.isNotBlank() }) }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { model.pendingCollect = null }) { Text("Cancel") } },
        )
    }

    model.pendingDownload?.let { download ->
        AlertDialog(
            onDismissRequest = { model.pendingDownload = null },
            title = { Text("Download ${download.filename}?") },
            text = { Text(buildString { append(download.url.substringAfter("://").substringBefore('/')); if (download.size > 0) append("\n${download.size / 1024} KB") }) },
            confirmButton = { TextButton(onClick = model::confirmDownload) { Text("Download") } },
            dismissButton = { TextButton(onClick = { model.pendingDownload = null }) { Text("Cancel") } },
        )
    }
    if (model.sharingFolder) ShareFolderDialog(model)
    model.connectionDetails?.let { details ->
        AlertDialog(
            onDismissRequest = { model.connectionDetails = null },
            title = { Text("Connection Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Website: ${details.host}")
                    Text("Connection: ${if (details.isSecure) "Encrypted" else "Not Encrypted"}")
                }
            },
            confirmButton = { TextButton(onClick = { model.connectionDetails = null }) { Text("Done") } },
        )
    }
}

@Composable
private fun ShareFolderDialog(model: BrowserViewModel) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<BookmerApiClient.SharePageState?>(null) }
    var title by remember { mutableStateOf(model.bookmarks.currentFolderTitle) }
    var summary by remember { mutableStateOf("") }
    var public by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val token = model.session.value.token
    LaunchedEffect(model.bookmarks.currentFolderId) {
        if (token == null) { error = "Sign in required"; loading = false }
        else model.api.ensureSharePage(token, model.bookmarks.currentFolderId) { result ->
            loading = false
            result.onSuccess { loaded -> state = loaded; title = loaded.title.ifBlank { title }; summary = loaded.summary; public = loaded.isPublic }
                .onFailure { error = it.message }
        }
    }
    AlertDialog(onDismissRequest = { model.sharingFolder = false }, title = { Text("Share Folder") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when { loading -> Text("Loading share page…"); error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error); else -> {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(summary, { summary = it }, label = { Text("Summary") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Switch(public, { public = it }); Text("  Public page") }
                state?.publicUrl?.let { url -> TextButton(onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url), "Share Folder"))
                }) { Text("Share Link") } }
            } }
        }
    }, confirmButton = { TextButton(enabled = !loading && error == null, onClick = {
        val loaded = state ?: return@TextButton
        token?.let { model.api.updateSharePage(it, loaded.token, title, summary, public) { result ->
            result.onSuccess { model.sharingFolder = false }.onFailure { error = it.message }
        } }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = { model.sharingFolder = false }) { Text("Cancel") } })
}
