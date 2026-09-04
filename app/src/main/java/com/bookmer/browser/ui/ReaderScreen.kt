package com.bookmer.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bookmer.browser.browser.BrowserViewModel

@Composable
fun ReaderScreen(model: BrowserViewModel, modifier: Modifier = Modifier) {
    val reader = model.readerContent ?: return
    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 70.dp)) {
            Text(reader.title, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
            Text(reader.url, Modifier.padding(top = 8.dp, bottom = 28.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(reader.text, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Serif)
        }
        IconButton(onClick = { model.readerContent = null }, Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 10.dp)) {
            Icon(Icons.Rounded.Close, "Close Reader")
        }
    }
}
