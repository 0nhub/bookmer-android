package com.bookmer.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Material You extra-large corner (menus / dialogs). */
val BookmerMenuShape = RoundedCornerShape(28.dp)

@Composable
fun bookmerMenuSurface() = MaterialTheme.colorScheme.surfaceContainerHigh

@Composable
fun BookmerMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = 280.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(minWidth),
        shape = BookmerMenuShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        content()
    }
}

@Composable
fun BookmerMenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/** iOS-style nested menu title row with back. */
@Composable
fun BookmerSubmenuHeader(title: String, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(22.dp), tint = colors.onSurface)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            color = colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    BookmerMenuDivider()
}

/**
 * Material You row: title left, trailing icon (and submenu chevron) right-aligned.
 */
@Composable
fun BookmerMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    selected: Boolean = false,
    showsChevron: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val titleColor = when {
        danger -> colors.error
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        else -> colors.onSurface
    }
    val iconColor = when {
        danger -> colors.error
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        else -> colors.onSurface
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = titleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!showsChevron && icon != null) {
            Spacer(Modifier.width(12.dp))
            Icon(icon, null, Modifier.size(24.dp), tint = iconColor)
        }
        if (selected && icon == null && !showsChevron) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Check, null, Modifier.size(24.dp), tint = colors.onSurface)
        }
        if (showsChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                Modifier.size(20.dp),
                tint = colors.onSurfaceVariant,
            )
        }
    }
}
