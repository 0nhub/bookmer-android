package com.bookmer.browser.ui

import android.content.Context
import android.content.Intent
import android.print.PrintManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.browser.Overlay
import com.bookmer.browser.data.BookmerPageActions
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.ContentViewMode
import com.bookmer.browser.data.SitePermissionKind
import com.bookmer.browser.data.SitePermissionPolicy
import com.bookmer.browser.data.SitePermissionStore
import com.bookmer.browser.data.SortMode
import com.bookmer.browser.data.StartNavigationAction
import com.bookmer.browser.data.ToolbarAction
import com.bookmer.browser.data.WebNavigationAction

private val ChromeColor = Color(0xD9111112)
private val ChromeBorder = Color.White.copy(alpha = .16f)

/** iOS AddressBarField: first focus highlights the whole URL so typing replaces it. */
internal fun addressFieldValueOnFocus(text: String): TextFieldValue =
    if (text.isEmpty()) TextFieldValue("")
    else TextFieldValue(text, TextRange(0, text.length))

/** Custom chrome glyphs not available in Material Icons Extended. */
private object BookmerChromeIcons {
    val ReadingGlasses: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReadingGlasses",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Material-style eyeglasses (filled), used for Zoom.
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.5f, 8f)
                curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
                reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                reflectiveCurveTo(15.33f, 8f, 14.5f, 8f)
                close()
                moveTo(9.5f, 8f)
                curveTo(8.67f, 8f, 8f, 8.67f, 8f, 9.5f)
                reflectiveCurveTo(8.67f, 11f, 9.5f, 11f)
                reflectiveCurveTo(11f, 10.33f, 11f, 9.5f)
                reflectiveCurveTo(10.33f, 8f, 9.5f, 8f)
                close()
                moveTo(21.43f, 12.98f)
                curveToRelative(-0.5f, -0.63f, -1.17f, -1.05f, -1.93f, -1.22f)
                curveTo(18.7f, 9.08f, 16.04f, 7f, 12.99f, 7f)
                curveToRelative(-3.05f, 0f, -5.71f, 2.08f, -6.51f, 4.76f)
                curveTo(5.74f, 11.93f, 5.07f, 12.35f, 4.57f, 12.98f)
                curveTo(3.5f, 14.34f, 3.5f, 16.24f, 4.57f, 17.6f)
                curveTo(5.64f, 18.96f, 7.36f, 19.5f, 9f, 19.5f)
                horizontalLineToRelative(0.5f)
                curveToRelative(0.28f, 0f, 0.5f, -0.22f, 0.5f, -0.5f)
                reflectiveCurveTo(9.78f, 18.5f, 9.5f, 18.5f)
                horizontalLineTo(9f)
                curveToRelative(-1.25f, 0f, -2.5f, -0.42f, -3.25f, -1.35f)
                curveToRelative(-0.58f, -0.75f, -0.58f, -1.97f, 0f, -2.7f)
                curveToRelative(0.33f, -0.42f, 0.78f, -0.7f, 1.3f, -0.83f)
                lineToRelative(0.35f, -0.09f)
                lineToRelative(0.12f, -0.34f)
                curveTo(8.1f, 10.92f, 10.35f, 9f, 12.99f, 9f)
                curveToRelative(2.64f, 0f, 4.89f, 1.92f, 5.47f, 4.19f)
                lineToRelative(0.12f, 0.34f)
                lineToRelative(0.35f, 0.09f)
                curveToRelative(0.52f, 0.13f, 0.97f, 0.41f, 1.3f, 0.83f)
                curveToRelative(0.58f, 0.73f, 0.58f, 1.95f, 0f, 2.7f)
                curveTo(19.5f, 18.58f, 18.25f, 19f, 17f, 19f)
                horizontalLineToRelative(-0.5f)
                curveToRelative(-0.28f, 0f, -0.5f, 0.22f, -0.5f, 0.5f)
                reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                horizontalLineTo(17f)
                curveToRelative(1.64f, 0f, 3.36f, -0.54f, 4.43f, -1.9f)
                curveTo(22.5f, 16.24f, 22.5f, 14.34f, 21.43f, 12.98f)
                close()
            }
        }.build()
    }
}

private enum class ChromeSheet {
    FOLDER, SORT, VIEW, BOOKMARKS,
    PAGE, CAMERA, MICROPHONE, LOCATION,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserChrome(
    model: BrowserViewModel,
    modifier: Modifier = Modifier,
    tabSlideLocked: Boolean = false,
    onTabSlideDrag: (Float) -> Unit = {},
    onTabSlideEnd: (dx: Float, predictedX: Float) -> Unit = { _, _ -> },
    onTabSlideCancel: () -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    var navMenu by remember { mutableStateOf(false) }
    var actionMenu by remember { mutableStateOf(false) }
    var refreshMenu by remember { mutableStateOf(false) }
    var clearData by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<ChromeSheet?>(null) }
    var addressFocused by remember { mutableStateOf(false) }
    var addressValue by remember { mutableStateOf(TextFieldValue(model.addressText)) }
    var selectAllOnFocus by remember { mutableStateOf(false) }
    val addressFocusRequester = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val dismissAddressEditing = {
        addressFocused = false
        focus.clearFocus()
        model.cancelEditingAddress()
        addressValue = TextFieldValue(model.addressText)
    }
    val chromeHorizontalInset = if (addressFocused) 6.dp else 16.dp
    val chromeControlGap = if (addressFocused) 6.dp else 12.dp
    LaunchedEffect(model.addressText) {
        if (!addressFocused && addressValue.text != model.addressText) {
            addressValue = TextFieldValue(model.addressText)
        }
    }
    val requestAddressFocus = {
        addressFocusRequester.requestFocus()
    }
    LaunchedEffect(model.selectedTabId) {
        menu = false
        navMenu = false
        actionMenu = false
        refreshMenu = false
        sheet = null
        if (addressFocused) {
            dismissAddressEditing()
        }
    }
    val dismissMenu = { menu = false }
    val openSheet = { target: ChromeSheet -> menu = false; sheet = target }
    // iOS AddressBarField: centered when idle (title or placeholder), left when editing
    val addressCentered = !addressFocused
    val showIdlePageButtons = !addressFocused && !model.showsCollectionHome
    val showDownloadsChrome = !addressFocused && model.downloads.isNotEmpty()
    val showAddressLeadingControl = showIdlePageButtons || showDownloadsChrome
    val latestTabSlideDrag = rememberUpdatedState(onTabSlideDrag)
    val latestTabSlideEnd = rememberUpdatedState(onTabSlideEnd)
    val latestTabSlideCancel = rememberUpdatedState(onTabSlideCancel)

    val runNavigation = {
        if (model.showsCollectionHome) when (model.settings.startNavigationAction) {
            StartNavigationAction.FOLDER_NAVIGATOR -> model.showOverlay(Overlay.NAVIGATE)
            StartNavigationAction.TABS -> model.showOverlay(Overlay.TABS)
            StartNavigationAction.TAB_HISTORY -> model.showOverlay(Overlay.TAB_HISTORY)
        } else when (model.settings.webNavigationAction) {
            WebNavigationAction.BACK -> model.goBack()
            WebNavigationAction.NAVIGATE -> model.showOverlay(Overlay.NAVIGATE)
            WebNavigationAction.TABS -> model.showOverlay(Overlay.TABS)
            WebNavigationAction.TAB_HISTORY -> model.showOverlay(Overlay.TAB_HISTORY)
        }
        Unit
    }
    val runAction = {
        when (model.settings.toolbarAction) {
            ToolbarAction.FULL_SCREEN -> model.enterImmersive()
            ToolbarAction.READER -> model.enterReader()
            ToolbarAction.SEARCH -> model.beginFind()
            ToolbarAction.ZOOM -> model.setZoom(if (model.currentTab.pageZoom >= 150) 100 else model.currentTab.pageZoom + 10)
            ToolbarAction.DESKTOP_VIEW -> model.toggleDesktop()
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = chromeHorizontalInset, vertical = 10.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(chromeControlGap),
    ) {
        if (!addressFocused) {
            Box {
                Box(
                    Modifier.size(50.dp).clip(CircleShape).background(ChromeColor).border(.6.dp, ChromeBorder, CircleShape)
                        .combinedClickable(onClick = runNavigation, onLongClick = { navMenu = true }),
                    contentAlignment = Alignment.Center,
                ) {
                    val navIcon = if (model.showsCollectionHome) when (model.settings.startNavigationAction) {
                        StartNavigationAction.FOLDER_NAVIGATOR -> Icons.Rounded.Menu
                        StartNavigationAction.TABS -> Icons.Rounded.ContentCopy
                        StartNavigationAction.TAB_HISTORY -> Icons.Rounded.History
                    } else when (model.settings.webNavigationAction) {
                        WebNavigationAction.BACK -> Icons.AutoMirrored.Rounded.ArrowBack
                        WebNavigationAction.NAVIGATE -> Icons.Rounded.Menu
                        WebNavigationAction.TABS -> Icons.Rounded.ContentCopy
                        WebNavigationAction.TAB_HISTORY -> Icons.Rounded.History
                    }
                    Icon(navIcon, "Navigation", tint = Color.White)
                }
                BookmerMenu(expanded = navMenu, onDismissRequest = { navMenu = false }) {
                    BookmerMenuItem("Navigate", { navMenu = false; model.showOverlay(Overlay.NAVIGATE) }, icon = Icons.Rounded.Menu)
                    BookmerMenuItem("Tabs", { navMenu = false; model.showOverlay(Overlay.TABS) }, icon = Icons.Rounded.ContentCopy)
                    BookmerMenuItem("Tab History", { navMenu = false; model.showOverlay(Overlay.TAB_HISTORY) }, icon = Icons.Rounded.History)
                    if (model.canGoForward) {
                        BookmerMenuItem("Forward", { navMenu = false; model.goForward() }, icon = Icons.Rounded.ArrowForward)
                    }
                }
            }
        }

        Box(Modifier.weight(1f)) {
            val capsuleHorizontalPad = when {
                addressFocused -> 12.dp
                showAddressLeadingControl -> 2.dp
                else -> 16.dp
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(ChromeColor)
                    .border(.6.dp, ChromeBorder, RoundedCornerShape(25.dp))
                    .then(
                        if (!addressFocused) {
                            addressBarIdleGestures(
                                tabSlideLocked = { tabSlideLocked },
                                onTabSlideDrag = { latestTabSlideDrag.value(it) },
                                onTabSlideEnd = { dx, predicted -> latestTabSlideEnd.value(dx, predicted) },
                                onTabSlideCancel = { latestTabSlideCancel.value() },
                                onSwipeUp = { model.showOverlay(Overlay.TABS) },
                                onSwipeDown = {
                                    if (model.settings.hideToolbar && !model.showsCollectionHome) {
                                        model.collapseToolbarFromChrome()
                                    }
                                },
                            ) {
                                model.beginEditingAddress()
                                addressValue = addressFieldValueOnFocus(model.addressText)
                                selectAllOnFocus = model.addressText.isNotEmpty()
                                requestAddressFocus()
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(
                        start = capsuleHorizontalPad,
                        end = if (addressFocused) 12.dp else capsuleHorizontalPad,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showAddressLeadingControl) {
                    IconButton(
                        onClick = {
                            if (model.downloads.isNotEmpty()) model.showOverlay(Overlay.DOWNLOADS)
                            else if (model.settings.openActionMenuOnLongPress) runAction()
                            else actionMenu = true
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        val actionIcon = if (model.downloads.isNotEmpty()) Icons.Rounded.Download else when (model.settings.toolbarAction) {
                            ToolbarAction.FULL_SCREEN -> Icons.Rounded.Fullscreen
                            ToolbarAction.READER -> Icons.Rounded.TextFields
                            ToolbarAction.SEARCH -> Icons.Rounded.Search
                            ToolbarAction.ZOOM -> BookmerChromeIcons.ReadingGlasses
                            ToolbarAction.DESKTOP_VIEW -> Icons.Rounded.Computer
                        }
                        Icon(actionIcon, "Page Action", tint = Color.White.copy(alpha = .68f), modifier = Modifier.size(20.dp))
                    }
                }
                Box(
                    Modifier.weight(1f),
                    contentAlignment = if (addressCentered) Alignment.Center else Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = addressValue,
                        onValueChange = { next ->
                            if (!addressFocused) return@BasicTextField
                            if (selectAllOnFocus && next.text == addressValue.text) {
                                selectAllOnFocus = false
                                addressValue = next.copy(selection = TextRange(0, next.text.length))
                                return@BasicTextField
                            }
                            selectAllOnFocus = false
                            addressValue = next
                            if (model.addressText != next.text) {
                                model.addressText = next.text
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(addressFocusRequester)
                            .onFocusChanged { state ->
                                val nowFocused = state.isFocused
                                if (nowFocused) {
                                    if (!addressFocused) {
                                        model.beginEditingAddress()
                                        val text = model.addressText.ifEmpty { addressValue.text }
                                        selectAllOnFocus = text.isNotEmpty()
                                        addressValue = if (text.isNotEmpty()) {
                                            addressFieldValueOnFocus(text)
                                        } else {
                                            TextFieldValue("")
                                        }
                                    }
                                    addressFocused = true
                                } else if (addressFocused) {
                                    addressFocused = false
                                    model.cancelEditingAddress()
                                    addressValue = TextFieldValue(model.addressText)
                                }
                            }
                            .graphicsLayer { alpha = if (addressFocused) 1f else 0f },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start,
                        ),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            model.submitAddress()
                            focus.clearFocus()
                        }),
                        decorationBox = { field ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (addressFocused && addressValue.text.isEmpty()) {
                                    Text(
                                        "Search or URL",
                                        color = Color.White.copy(alpha = .68f),
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                    )
                                }
                                field()
                            }
                        },
                    )
                    if (!addressFocused) {
                        val idleLabel = when {
                            model.showsCollectionHome && model.addressText.isEmpty() -> "Search or URL"
                            model.showsCollectionHome -> model.addressText
                            model.currentTab.title.isNotBlank() -> model.currentTab.title
                            model.addressText.isNotBlank() -> model.addressText
                            else -> "Search or URL"
                        }
                        Text(
                            idleLabel,
                            color = Color.White.copy(alpha = if (model.showsCollectionHome && model.addressText.isEmpty()) .68f else 1f),
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = if (addressCentered) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showIdlePageButtons) {
                    Box {
                        Box(
                            Modifier
                                .size(44.dp)
                                .combinedClickable(
                                    onClick = model::reloadOrStop,
                                    onLongClick = { refreshMenu = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Refresh, "Reload", tint = Color.White.copy(alpha = .68f), modifier = Modifier.size(20.dp))
                        }
                        BookmerMenu(expanded = refreshMenu, onDismissRequest = { refreshMenu = false }) {
                            val current = model.currentTab.autoRefreshSeconds
                            BookmerMenuItem(
                                "Off",
                                { refreshMenu = false; model.setAutoRefresh(0) },
                                selected = current <= 0,
                            )
                            val intervals = model.settings.autoRefreshIntervals
                                .let { list -> if (current > 0 && current !in list) (list + current).sorted() else list }
                            intervals.forEach { seconds ->
                                val label = when {
                                    seconds % 3600 == 0 -> {
                                        val h = seconds / 3600
                                        if (h == 1) "1 hour" else "$h hours"
                                    }
                                    seconds % 60 == 0 -> {
                                        val m = seconds / 60
                                        if (m == 1) "1 minute" else "$m minutes"
                                    }
                                    seconds == 1 -> "1 second"
                                    else -> "$seconds seconds"
                                }
                                BookmerMenuItem(
                                    label,
                                    { refreshMenu = false; model.setAutoRefresh(seconds) },
                                    selected = current == seconds,
                                )
                            }
                        }
                    }
                } else if (model.showsCollectionHome && model.addressText.isNotEmpty() && !addressFocused) {
                    IconButton(onClick = { model.addressText = "" }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Close, "Clear", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else if (addressFocused && model.addressText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            model.addressText = ""
                            addressValue = TextFieldValue("")
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Rounded.Close, "Clear", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            BookmerMenu(expanded = actionMenu, onDismissRequest = { actionMenu = false }) {
                BookmerMenuItem("Full Screen", { actionMenu = false; model.enterImmersive() }, icon = Icons.Rounded.Fullscreen)
                BookmerMenuItem("Reader", { actionMenu = false; model.enterReader() }, icon = Icons.Rounded.TextFields)
                BookmerMenuItem("Search", { actionMenu = false; model.beginFind() }, icon = Icons.Rounded.Search)
                BookmerMenuItem("Zoom", { actionMenu = false; model.setZoom(model.currentTab.pageZoom + 10) }, icon = BookmerChromeIcons.ReadingGlasses)
                BookmerMenuItem("Desktop View", { actionMenu = false; model.toggleDesktop() }, icon = Icons.Rounded.Computer)
            }
        }

        Box {
            if (addressFocused) {
                IconButton(
                    onClick = dismissAddressEditing,
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(ChromeColor).border(.6.dp, ChromeBorder, CircleShape),
                ) {
                    Icon(Icons.Rounded.Close, "Close search", tint = Color.White)
                }
            } else {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(ChromeColor).border(.6.dp, ChromeBorder, CircleShape),
                ) {
                    Icon(Icons.Rounded.MoreHoriz, "Menu", tint = Color.White)
                }
            }
            // Root menus stay stable — never swap menu children while open (ANR).
            key(model.selectedTabId) {
            BookmerMenu(expanded = menu, onDismissRequest = dismissMenu) {
                if (model.showsCollectionHome) {
                    BookmerMenuItem("Settings", { dismissMenu(); model.showOverlay(Overlay.SETTINGS) }, icon = Icons.Rounded.Settings)
                    BookmerMenuItem("Clear Data", { dismissMenu(); clearData = true }, icon = Icons.Rounded.Delete)
                    BookmerMenuDivider()
                    BookmerMenuItem("Folder", { openSheet(ChromeSheet.FOLDER) }, icon = Icons.Rounded.Folder, showsChevron = true)
                    if (model.session.value.isSignedIn) {
                        BookmerMenuItem("Bookmarks", { openSheet(ChromeSheet.BOOKMARKS) }, icon = Icons.Rounded.Star, showsChevron = true)
                    }
                    BookmerMenuDivider()
                    BookmerMenuItem("New Tab", {
                        menu = false
                        sheet = null
                        model.createTab()
                    }, icon = Icons.Rounded.Add)
                } else {
                    val hasPage = !model.currentTab.url.isNullOrBlank()
                    val hasHides = model.hiddenElements.hasRulesMatching(model.currentTab.url)
                    BookmerMenuItem("Settings", { dismissMenu(); model.showOverlay(Overlay.SETTINGS) }, icon = Icons.Rounded.Settings)
                    BookmerMenuDivider()
                    BookmerMenuItem("Page", { openSheet(ChromeSheet.PAGE) }, icon = Icons.Rounded.Article, showsChevron = true)
                    BookmerMenuItem(
                        "Hide Element",
                        { if (hasPage) { dismissMenu(); model.beginHideElements() } },
                        icon = Icons.Rounded.VisibilityOff,
                        enabled = hasPage,
                    )
                    if (hasHides) {
                        BookmerMenuItem(
                            "Hidden Elements",
                            { dismissMenu(); model.showHideElementsManage = true },
                            icon = Icons.Rounded.List,
                        )
                    }
                    BookmerMenuItem(
                        when {
                            model.isTranslating -> "Translating…"
                            model.currentTab.isPageTranslated -> "Show Original"
                            else -> "Translate"
                        },
                        {
                            if (hasPage && !model.isTranslating) {
                                dismissMenu()
                                model.translatePage()
                            }
                        },
                        icon = Icons.Rounded.Translate,
                        enabled = hasPage && !model.isTranslating,
                    )
                    BookmerMenuItem(
                        "Share",
                        { if (hasPage) { dismissMenu(); sharePage(model) } },
                        icon = Icons.Rounded.Share,
                        enabled = hasPage,
                    )
                    BookmerMenuItem(
                        "Collect",
                        { if (hasPage) { dismissMenu(); model.collectCurrentPage() } },
                        icon = Icons.Rounded.Star,
                        enabled = hasPage,
                    )
                    BookmerMenuDivider()
                    BookmerMenuItem("New Tab", {
                        menu = false
                        sheet = null
                        model.createTab()
                    }, icon = Icons.Rounded.Add)
                }
            }
            }
            OverflowSubmenus(
                model = model,
                sheet = sheet,
                onSheet = { sheet = it },
                onNewFolder = { sheet = null; newFolder = true },
                reopenRoot = { sheet = null; menu = true },
            )
        }
    }

    if (clearData) ClearDataDialog(onDismiss = { clearData = false }) { history, web ->
        model.clearBrowsingData(history, web); clearData = false
    }
    if (newFolder) TextEntryDialog("New Folder", "Title", onDismiss = { newFolder = false }) {
        model.sync.pushCreate(model.bookmarks.addFolder(it)); newFolder = false
    }
}

@Composable
private fun OverflowSubmenus(
    model: BrowserViewModel,
    sheet: ChromeSheet?,
    onSheet: (ChromeSheet?) -> Unit,
    onNewFolder: () -> Unit,
    reopenRoot: () -> Unit,
) {
    val dismiss = { onSheet(null) }
    val hasPage = !model.currentTab.url.isNullOrBlank()
    val host = SitePermissionStore.hostFromUrl(model.currentTab.url)

    BookmerMenu(expanded = sheet == ChromeSheet.FOLDER, onDismissRequest = dismiss) {
        BookmerSubmenuHeader("Folder", reopenRoot)
        BookmerMenuItem("New Folder", onNewFolder, icon = Icons.Rounded.Folder)
        if (model.session.value.isSignedIn) {
            BookmerMenuItem("Share", { dismiss(); model.sharingFolder = true }, icon = Icons.Rounded.Share)
        }
        BookmerMenuItem("Sort by", { onSheet(ChromeSheet.SORT) }, icon = Icons.Rounded.SwapVert, showsChevron = true)
        BookmerMenuItem("View style", { onSheet(ChromeSheet.VIEW) }, icon = Icons.Rounded.GridView, showsChevron = true)
        BookmerMenuDivider()
        BookmerMenuItem(
            "Title",
            { model.preferences.update { it.copy(hideTitles = !it.hideTitles) } },
            selected = !model.settings.hideTitles,
        )
    }
    BookmerMenu(expanded = sheet == ChromeSheet.SORT, onDismissRequest = dismiss) {
        BookmerSubmenuHeader("Sort by") { onSheet(ChromeSheet.FOLDER) }
        SortMode.entries.forEach { mode ->
            BookmerMenuItem(mode.label, {
                model.bookmarks.sortCurrent(mode)
                model.sync.pushOrder(model.bookmarks.visibleItems)
                dismiss()
            })
        }
    }
    BookmerMenu(expanded = sheet == ChromeSheet.VIEW, onDismissRequest = dismiss) {
        BookmerSubmenuHeader("View style") { onSheet(ChromeSheet.FOLDER) }
        val current = model.bookmarks.currentContentView(model.settings.collectionViewMode)
        listOf(ContentViewMode.GRID, ContentViewMode.LIST, ContentViewMode.THUMBNAIL).forEach { mode ->
            BookmerMenuItem(mode.label, { setCollectionView(model, mode) }, selected = current == mode)
        }
    }
    BookmerMenu(expanded = sheet == ChromeSheet.BOOKMARKS, onDismissRequest = dismiss) {
        BookmerSubmenuHeader("Bookmarks", reopenRoot)
        BookmerMenuItem("Broken links", { dismiss(); model.showBookmarkTools("broken") }, icon = Icons.Rounded.Link)
        BookmerMenuItem("Recover", { dismiss(); model.showBookmarkTools("recover") }, icon = Icons.Rounded.Undo)
        BookmerMenuItem("Shared folder", { dismiss(); model.showBookmarkTools("shared") }, icon = Icons.Rounded.Folder)
    }
    BookmerMenu(expanded = sheet == ChromeSheet.PAGE, onDismissRequest = dismiss) {
        BookmerSubmenuHeader("Page", reopenRoot)
        BookmerMenuItem("Print", { dismiss(); printPage(model) }, icon = Icons.Rounded.Print, enabled = hasPage)
        BookmerMenuItem("Create PDF", { dismiss(); createPdf(model) }, icon = Icons.Rounded.Description, enabled = hasPage)
        BookmerMenuItem("Open in WebArchive", { dismiss(); model.openCurrentPageInWebArchive() }, icon = Icons.Rounded.History, enabled = hasPage)
        BookmerMenuItem("Report Page", { dismiss(); model.reportCurrentPage() }, icon = Icons.Rounded.Flag, enabled = hasPage)
        BookmerMenuItem("Remove Data", { dismiss(); model.removeCurrentPageData() }, icon = Icons.Rounded.Delete, enabled = hasPage)
        BookmerMenuItem("Connection Details", { dismiss(); model.showConnectionDetails() }, icon = Icons.Rounded.Security, enabled = hasPage)
        BookmerMenuDivider()
        BookmerMenuItem("Camera", { onSheet(ChromeSheet.CAMERA) }, icon = Icons.Rounded.CameraAlt, showsChevron = true)
        BookmerMenuItem("Microphone", { onSheet(ChromeSheet.MICROPHONE) }, icon = Icons.Rounded.Mic, showsChevron = true)
        BookmerMenuItem("Location", { onSheet(ChromeSheet.LOCATION) }, icon = Icons.Rounded.LocationOn, showsChevron = true)
    }
    PermissionSubmenu(model, SitePermissionKind.CAMERA, host, sheet == ChromeSheet.CAMERA, { onSheet(ChromeSheet.PAGE) }, dismiss)
    PermissionSubmenu(model, SitePermissionKind.MICROPHONE, host, sheet == ChromeSheet.MICROPHONE, { onSheet(ChromeSheet.PAGE) }, dismiss)
    PermissionSubmenu(model, SitePermissionKind.LOCATION, host, sheet == ChromeSheet.LOCATION, { onSheet(ChromeSheet.PAGE) }, dismiss)
}

@Composable
private fun PermissionSubmenu(
    model: BrowserViewModel,
    kind: SitePermissionKind,
    host: String,
    expanded: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(kind, host) { mutableStateOf(model.sitePermissions.policy(kind, host)) }
    BookmerMenu(expanded = expanded, onDismissRequest = onDismiss) {
        BookmerSubmenuHeader(kind.label, onBack)
        SitePermissionPolicy.entries.forEach { policy ->
            BookmerMenuItem(
                policy.label,
                {
                    model.sitePermissions.set(policy, kind, host)
                    selected = policy
                },
                enabled = host.isNotEmpty(),
                selected = selected == policy,
            )
        }
    }
}

/**
 * Idle capsule: tap edits the URL; horizontal swipe switches tabs. The label never pans.
 * Use down-relative positions — [positionChange] goes to 0 after consume and broke tab swipe.
 */
private fun addressBarIdleGestures(
    tabSlideLocked: () -> Boolean,
    onTabSlideDrag: (Float) -> Unit,
    onTabSlideEnd: (Float, Float) -> Unit,
    onTabSlideCancel: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onTap: () -> Unit,
): Modifier {
    return Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (tabSlideLocked()) return@awaitEachGesture
            val start = down.position
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var totalX = 0f
            var totalY = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                totalX = change.position.x - start.x
                totalY = change.position.y - start.y
                tracker.addPosition(change.uptimeMillis, change.position)
                if (abs(totalX) > abs(totalY) && abs(totalX) > 10f) {
                    onTabSlideDrag(totalX)
                }
                if (abs(totalX) > 10f || abs(totalY) > 10f) change.consume()
                if (!event.changes.any { it.pressed }) break
            }
            val predictedX = totalX + tracker.calculateVelocity().x * 0.16f
            when {
                abs(totalX) < 10f && abs(totalY) < 10f -> onTap()
                totalY < -48f && abs(totalY) > abs(totalX) -> {
                    onTabSlideCancel()
                    onSwipeUp()
                }
                totalY > 56f && abs(totalY) > abs(totalX) -> {
                    onTabSlideCancel()
                    onSwipeDown()
                }
                abs(totalX) > abs(totalY) -> onTabSlideEnd(totalX, predictedX)
                else -> onTabSlideCancel()
            }
        }
    }
}

@Composable
fun FindBar(model: BrowserViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(12.dp)
            .navigationBarsPadding()
            .imePadding()
            .clip(RoundedCornerShape(25.dp))
            .background(ChromeColor)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(model.findQuery, { model.find(it) }, Modifier.weight(1f), placeholder = { Text("Find on Page") }, singleLine = true)
        Text(if (model.findQuery.isEmpty()) "" else "${model.findMatches}", color = Color.White)
        IconButton(onClick = { model.findNext(false) }) {
            Icon(Icons.Rounded.ArrowDownward, "Previous", tint = Color.White, modifier = Modifier.rotateCompat(180f))
        }
        IconButton(onClick = { model.findNext(true) }) {
            Icon(Icons.Rounded.ArrowDownward, "Next", tint = Color.White)
        }
        IconButton(onClick = model::endFind) { Icon(Icons.Rounded.Close, "Done", tint = Color.White) }
    }
}

@Composable
fun ImmersiveExit(model: BrowserViewModel, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .clip(RoundedCornerShape(25.dp))
            .background(ChromeColor)
            .clickable(onClick = model::exitImmersive)
            .padding(horizontal = 20.dp)
            .height(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Exit Full Screen",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
fun ImmersiveTipOverlay(onDismiss: () -> Unit) {
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
        ) {
            Text("Exit Full Screen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pull past the top or bottom of the page, then tap Exit Full Screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("Continue") }
        }
    }
}

private fun printPage(model: BrowserViewModel) {
    val web = model.activeWebView ?: return
    val manager = web.context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    manager.print(model.currentTab.title, web.createPrintDocumentAdapter(model.currentTab.title), null)
}

private fun createPdf(model: BrowserViewModel) {
    val web = model.activeWebView ?: return
    val host = SitePermissionStore.hostFromUrl(model.currentTab.url)
    val name = BookmerPageActions.pdfFilename(model.currentTab.title, host)
    val manager = web.context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    manager.print(name, web.createPrintDocumentAdapter(name), null)
}

private fun sharePage(model: BrowserViewModel) {
    val context = model.getApplication<android.app.Application>()
    val url = model.addressText.takeIf { it.startsWith("http") } ?: model.currentTab.url
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
    context.startActivity(Intent.createChooser(intent, "Share Page").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun setCollectionView(model: BrowserViewModel, mode: ContentViewMode) {
    val folder = model.bookmarks.currentFolderId
    if (folder == BookmerUrls.ROOT || folder == BookmerUrls.HIDDEN || folder == BookmerUrls.TAGS) {
        model.preferences.update { it.copy(collectionViewMode = mode) }
    } else {
        model.bookmarks.setCurrentContentView(mode)
    }
    model.sync.pushView(folder, mode)
}

@Composable
fun ClearDataDialog(onDismiss: () -> Unit, onClear: (Boolean, Boolean) -> Unit) {
    var history by remember { mutableStateOf(true) }
    var website by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Data") },
        text = {
            Column {
                CheckboxRowCompat("History", history) { history = it }
                CheckboxRowCompat("Website Data", website) { website = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onClear(history, website) }) {
                Text("Clear", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun TextEntryDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onSave(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Modifier.rotateCompat(degrees: Float): Modifier = graphicsLayer(rotationZ = degrees)

@Composable
private fun CheckboxRowCompat(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(checked, onCheckedChange)
        Text(label)
    }
}
