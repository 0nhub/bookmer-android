package com.bookmer.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookmer.browser.data.SearchEngine

private val YahooPurple = Color(0xFF6001D1)

@Composable
fun SearchEngineBrandIcon(
    engine: SearchEngine,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    Box(
        modifier
            .size(32.dp)
            .then(
                if (engine == SearchEngine.STARTPAGE) {
                    Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        when (engine) {
            SearchEngine.YAHOO -> {
                Text(
                    text = "y!",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = YahooPurple,
                        letterSpacing = (-0.5).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
            }
            else -> {
                Icon(
                    Icons.Rounded.Public,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                )
                RemoteImage(
                    url = engine.iconUrl(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    background = Color.White,
                )
            }
        }
    }
}
