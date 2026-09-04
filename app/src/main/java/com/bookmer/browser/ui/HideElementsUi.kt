package com.bookmer.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.bookmer.browser.data.HiddenElementRule
import com.bookmer.browser.data.HiddenElementScope

private val HideChrome = Color(0xD9111112)

@Composable
fun HideElementsPickBar(model: BrowserViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            model.hideIsPreviewing -> {
                Row(
                    Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(HideChrome)
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = model::undoHidePreview,
                        modifier = Modifier.weight(1f),
                    ) { Text("Undo", color = Color.White, fontWeight = FontWeight.SemiBold) }
                    Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = .22f)))
                    TextButton(
                        onClick = model::confirmHidePreview,
                        modifier = Modifier.weight(1f),
                    ) { Text("Confirm", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
            model.hideHasSelection -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (model.hideSelectionLabel.isNotBlank()) {
                        Text(
                            model.hideSelectionLabel,
                            color = Color.White.copy(alpha = .9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(HideChrome)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .widthIn(max = 280.dp),
                        )
                    }
                    Row(
                        Modifier
                            .widthIn(max = 280.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(HideChrome)
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = model::hideElementsShrink, modifier = Modifier.size(50.dp)) {
                            Icon(Icons.Rounded.Remove, "Smaller selection", tint = Color.White)
                        }
                        Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = .22f)))
                        TextButton(
                            onClick = model::previewHideCurrentSelection,
                            modifier = Modifier.weight(1f),
                        ) { Text("Preview", color = Color.White, fontWeight = FontWeight.SemiBold) }
                        Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = .22f)))
                        IconButton(onClick = model::hideElementsExpand, modifier = Modifier.size(50.dp)) {
                            Icon(Icons.Rounded.Add, "Larger selection", tint = Color.White)
                        }
                    }
                }
            }
            else -> {
                Text(
                    "Tap something on the page",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(HideChrome)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
        TextButton(
            onClick = { model.endHideElements(cancel = false) },
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(HideChrome)
                .padding(horizontal = 12.dp),
        ) {
            Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun HideElementsTipOverlay(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .46f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .clickable(enabled = false, onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Hide Element", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Tap something on the page. Use − and + to adjust the selection, then Preview. Confirm to save — or Undo to tweak it again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("Continue") }
        }
    }
}

@Composable
fun HideElementConfirmDialog(model: BrowserViewModel) {
    val draft = model.hideConfirmDraft ?: return
    var title by remember(draft) { mutableStateOf(draft.suggestedTitle) }
    var scope by remember(draft) { mutableStateOf(HiddenElementScope.SITE) }
    AlertDialog(
        onDismissRequest = model::cancelHideElementConfirm,
        title = { Text("Save Hide") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Apply to", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HiddenElementScope.entries.forEach { option ->
                        FilterChip(
                            selected = scope == option,
                            onClick = { scope = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text(scope.footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = title.trim().ifEmpty { draft.suggestedTitle }
                    model.confirmHideElement(scope, name)
                },
                enabled = title.trim().isNotEmpty() || draft.suggestedTitle.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = model::cancelHideElementConfirm) { Text("Cancel") }
        },
    )
}

@Composable
fun HideElementsManageSheet(model: BrowserViewModel) {
    val url = model.currentTab.url
    val pageRules = model.hiddenElements.rulesMatching(url)
    var renameTarget by remember { mutableStateOf<HiddenElementRule?>(null) }
    var renameText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { model.showHideElementsManage = false },
        title = { Text("Hidden Elements") },
        text = {
            Column(Modifier.height(360.dp)) {
                if (pageRules.isEmpty()) {
                    Text(
                        "Hide distracting parts of this page, then manage them here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = model::toggleHiddenElementsVisibility) {
                            Icon(
                                if (model.areHiddenElementsRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                if (model.areHiddenElementsRevealed) "Hide All" else "Show All",
                            )
                        }
                    }
                    LazyColumn {
                        items(pageRules, key = { it.id }) { rule ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(rule.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(rule.scope.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    renameText = rule.label
                                    renameTarget = rule
                                }) { Icon(Icons.Rounded.Edit, "Rename") }
                                IconButton(onClick = {
                                    model.hiddenElements.remove(rule.id)
                                    model.applyHiddenElementsToCurrentPage()
                                }) { Icon(Icons.Rounded.Delete, "Delete") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                model.showHideElementsManage = false
                model.beginHideElements()
            }) { Text("Hide Element") }
        },
        dismissButton = {
            TextButton(onClick = { model.showHideElementsManage = false }) { Text("Done") }
        },
    )

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    model.hiddenElements.rename(target.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
fun HiddenElementsSettings(model: BrowserViewModel) {
    val rules = model.hiddenElements.rules
    var query by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<HiddenElementRule?>(null) }
    var renameText by remember { mutableStateOf("") }
    val filtered = remember(rules, query) {
        val q = query.trim()
        if (q.isEmpty()) rules
        else rules.filter {
            it.label.contains(q, ignoreCase = true)
                || it.host.contains(q, ignoreCase = true)
                || it.selector.contains(q, ignoreCase = true)
        }
    }
    val hosts = filtered.map { it.host }.distinct().sorted()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search") },
                singleLine = true,
            )
        }
        when {
            rules.isEmpty() -> item {
                Text(
                    "Use Hide Element from a website’s page menu to pick elements. They’ll show up here.",
                    Modifier.padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            filtered.isEmpty() -> item {
                Text("No results", Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                hosts.forEach { host ->
                    item {
                        Text(
                            host,
                            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(filtered.filter { it.host == host }, key = { it.id }) { rule ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(rule.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    when (rule.scope) {
                                        HiddenElementScope.SITE -> "Similar on site"
                                        HiddenElementScope.PAGE -> "This page · ${rule.path ?: "/"}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                renameText = rule.label
                                renameTarget = rule
                            }) { Icon(Icons.Rounded.Edit, "Rename") }
                            IconButton(onClick = {
                                model.hiddenElements.remove(rule.id)
                                model.applyHiddenElementsToCurrentPage()
                            }) { Icon(Icons.Rounded.Delete, "Delete") }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    renameText,
                    { renameText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    model.hiddenElements.rename(target.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}
