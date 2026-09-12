package com.bookmer.browser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.ui.theme.bookmerIsDarkTheme
import com.bookmer.browser.data.BookmerIconUrl
import com.bookmer.browser.data.BookmerItem
import com.bookmer.browser.data.BrowserTab
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Arc-style tab deck — mirrors iOS `TabsSwitcherView`.
 * Focused card owns the stage; older tabs fan left; swipe up closes; tap zooms open.
 */
@Composable
fun TabsSwitcherScreen(model: BrowserViewModel) {
    val scope = rememberCoroutineScope()
    val focus = remember { Animatable(model.selectedTabIndex.toFloat()) }
    val zoom = remember { Animatable(1f) }
    val closeLift = remember { Animatable(0f) }
    var closingTabId by remember { mutableStateOf<String?>(null) }
    var closingDelta by remember { mutableFloatStateOf(0f) }
    var focusOverride by remember { mutableStateOf<Float?>(null) }
    var closeLiftOverride by remember { mutableStateOf<Float?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var closeMenu by remember { mutableStateOf(false) }
    val displayFocus = focusOverride ?: focus.value
    val displayCloseLift = closeLiftOverride ?: closeLift.value

    LaunchedEffect(Unit) {
        zoom.snapTo(1f)
        focus.snapTo(model.selectedTabIndex.toFloat())
        zoom.animateTo(0f, DeckMotion.zoom)
    }

    val dismiss: () -> Unit = { model.dismissOverlay() }

    fun openTabAt(index: Int) {
        if (isBusy || index !in model.tabs.indices) return
        isBusy = true
        model.selectTab(model.tabs[index].id)
        scope.launch {
            zoom.animateTo(1f, DeckMotion.zoom)
            dismiss()
        }
    }

    fun addTab() {
        if (isBusy) return
        isBusy = true
        scope.launch {
            model.createTab()
            val target = (model.tabs.size - 1).toFloat()
            focus.animateTo(target, DeckMotion.deck)
            delay(140)
            isBusy = false
            openTabAt(target.roundToInt())
        }
    }

    fun closeAll() {
        if (isBusy) return
        model.closeAllTabs()
        dismiss()
    }

    fun closeFocused(screenHeight: Float) {
        val index = displayFocus.roundToInt().coerceIn(0, model.tabs.lastIndex)
        val id = model.tabs.getOrNull(index)?.id ?: return
        if (model.tabs.size == 1) {
            model.closeAllTabs()
            dismiss()
            return
        }
        isBusy = true
        closingTabId = id
        closingDelta = index - displayFocus
        val startLift = closeLiftOverride ?: closeLift.value
        closeLiftOverride = null
        scope.launch {
            closeLift.snapTo(startLift)
            closeLift.animateTo(-(screenHeight), tween(200))
            val surviving = min(index, max(model.tabs.size - 2, 0))
            model.closeTab(id)
            closingTabId = null
            closeLift.snapTo(0f)
            closingDelta = 0f
            focus.snapTo(surviving.toFloat())
            delay(120)
            if (model.hasOnlyStartPageTab) {
                openTabAt(0)
            } else {
                isBusy = false
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val statusTop = WindowInsets.statusBars.getTop(density).toFloat()
        val safeTop = statusTop + with(density) { 10.dp.toPx() }
        val navBottom = WindowInsets.navigationBars.getBottom(density).toFloat()
        val safeBottom = with(density) { 34.dp.toPx() } + navBottom
        val metrics = remember(widthPx, heightPx, safeTop, safeBottom) {
            DeckMetrics(widthPx, heightPx, safeTop, safeBottom)
        }

        // Opaque backdrop — never let Collection / page chrome bleed through.
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .background(Color.Black.copy(alpha = 0.06f * fade(zoom.value)))
        )

        DeckLayer(
            model = model,
            metrics = metrics,
            focus = displayFocus,
            zoom = zoom.value,
            closeLift = displayCloseLift,
            closingTabId = closingTabId,
            closingDelta = closingDelta,
            isBusy = isBusy,
            onDragStart = {
                val now = focusOverride ?: focus.value
                focusOverride = now
                scope.launch {
                    focus.stop()
                    focus.snapTo(now)
                }
            },
            onDragHorizontal = { deltaFocus ->
                val last = max(model.tabs.size - 1, 0).toFloat()
                focusOverride = rubberBand(deltaFocus, 0f, last)
            },
            onDragVertical = { dy -> closeLiftOverride = min(0f, dy) },
            onDragEnd = { phase, translation, predicted, start, dragAnchor ->
                when (phase) {
                    DeckDragPhase.CLOSE -> {
                        if (translation.y < -110f || predicted.y < -260f) {
                            closeFocused(heightPx)
                        } else {
                            closingTabId = null
                            val lift = closeLiftOverride ?: closeLift.value
                            closeLiftOverride = null
                            scope.launch {
                                closeLift.snapTo(lift)
                                closeLift.animateTo(0f, DeckMotion.closeReturn)
                            }
                        }
                    }
                    DeckDragPhase.DECK -> {
                        val current = focusOverride ?: focus.value
                        focusOverride = null
                        val last = max(model.tabs.size - 1, 0).toFloat()
                        val step = metrics.width * DeckLayout.dragStep
                        val travelX = if (predicted.x * translation.x >= 0f) predicted.x else translation.x
                        val projected = dragAnchor - travelX / step
                        val target = projected.roundToInt().toFloat().coerceIn(0f, last)
                        scope.launch {
                            focus.stop()
                            focus.snapTo(current)
                            focus.animateTo(target, DeckMotion.deck)
                        }
                    }
                    DeckDragPhase.TAP -> {
                        focusOverride = null
                        closeLiftOverride = null
                        if (zoom.value <= 0.12f && max(abs(translation.x), abs(translation.y)) < 8f) {
                            val hit = hitIndex(start, metrics, displayFocus, model.tabs.size)
                            if (hit != null) openTabAt(hit)
                        }
                    }
                    else -> {
                        focusOverride = null
                        closeLiftOverride = null
                    }
                }
            },
            onBeginClose = {
                val index = displayFocus.roundToInt().coerceIn(0, model.tabs.lastIndex)
                closingTabId = model.tabs.getOrNull(index)?.id
                closingDelta = index - displayFocus
            },
        )

        // Bottom controls — trash left, new tab right (Safari / iOS layout).
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .graphicsLayer { alpha = fade(zoom.value) }
                .zIndex(100f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                CircleControl(Icons.Rounded.Delete, "Close all tabs", enabled = !isBusy) { closeMenu = true }
                BookmerMenu(expanded = closeMenu, onDismissRequest = { closeMenu = false }, minWidth = 200.dp) {
                    val count = model.tabs.size
                    BookmerMenuItem(
                        if (count == 1) "Close 1 Tab" else "Close $count Tabs",
                        { closeMenu = false; closeAll() },
                        icon = Icons.Rounded.Delete,
                        danger = true,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            CircleControl(Icons.Rounded.Add, "New Tab", enabled = !isBusy) { addTab() }
        }
    }
}

private enum class DeckDragPhase { IDLE, UNDECIDED, DECK, CLOSE, TAP }

private object DeckMotion {
    val zoom = spring<Float>(dampingRatio = 1f, stiffness = 180f)
    val deck = spring<Float>(dampingRatio = 0.88f, stiffness = 280f)
    val closeReturn = spring<Float>(dampingRatio = 0.84f, stiffness = 380f)
}

private object DeckLayout {
    const val cardWidth = 0.76f
    const val focusLeading = 0.24f
    const val rightStep = 0.62f
    const val leftStep = 0.145f
    const val leftFalloff = 0.42f
    const val dragStep = 0.55f
    const val depthScale = 0.024f
    const val maxDepth = 3f
    const val corner = 26f
}

private data class DeckMetrics(
    val width: Float,
    val height: Float,
    val safeTop: Float,
    val safeBottom: Float,
) {
    private val headerHeight = 10f
    private val footerHeight = 80f
    private val labelHeight = 28f
    val deckTop get() = safeTop + headerHeight
    val deckBottom get() = height - safeBottom - footerHeight
    val availableHeight get() = max(deckBottom - deckTop - labelHeight, 160f)
    val aspect get() = width / max(height, 1f)
    val cardWidth get() = min(width * DeckLayout.cardWidth, availableHeight * aspect)
    val cardHeight get() = cardWidth / max(aspect, 0.001f)
    val fillScale get() = width / max(cardWidth, 1f)
    val deckCenterY get() = deckTop + labelHeight + (deckBottom - deckTop - labelHeight) / 2f
}

@Composable
private fun DeckLayer(
    model: BrowserViewModel,
    metrics: DeckMetrics,
    focus: Float,
    zoom: Float,
    closeLift: Float,
    closingTabId: String?,
    closingDelta: Float,
    isBusy: Boolean,
    onDragStart: () -> Unit,
    onDragHorizontal: (Float) -> Unit,
    onDragVertical: (Float) -> Unit,
    onDragEnd: (DeckDragPhase, Offset, Offset, Offset, Float) -> Unit,
    onBeginClose: () -> Unit,
) {
    var dragAnchor by remember { mutableFloatStateOf(focus) }
    var phase by remember { mutableStateOf(DeckDragPhase.IDLE) }
    val busyState = rememberUpdatedState(isBusy)
    val zoomState = rememberUpdatedState(zoom)
    val dragStart = rememberUpdatedState(onDragStart)
    val dragHorizontal = rememberUpdatedState(onDragHorizontal)
    val dragVertical = rememberUpdatedState(onDragVertical)
    val dragEnd = rememberUpdatedState(onDragEnd)
    val beginClose = rememberUpdatedState(onBeginClose)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(model.tabs.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (busyState.value) return@awaitEachGesture
                    val startedDuringOpen = zoomState.value > 0.12f
                    phase = DeckDragPhase.UNDECIDED
                    dragAnchor = focus
                    var total = Offset.Zero
                    var claimedClose = false
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val delta = change.positionChange()
                        total += delta
                        tracker.addPosition(change.uptimeMillis, change.position)
                        if (phase == DeckDragPhase.UNDECIDED && max(abs(total.x), abs(total.y)) > 8f) {
                            if (total.y < 0f && abs(total.y) > abs(total.x) * 1.3f) {
                                phase = DeckDragPhase.CLOSE
                                claimedClose = true
                                beginClose.value()
                            } else if (abs(total.x) >= abs(total.y) && zoomState.value < 0.12f) {
                                phase = DeckDragPhase.DECK
                                dragStart.value()
                            }
                        }
                        when (phase) {
                            DeckDragPhase.CLOSE -> dragVertical.value(total.y)
                            DeckDragPhase.DECK -> {
                                val step = metrics.width * DeckLayout.dragStep
                                dragHorizontal.value(dragAnchor - total.x / step)
                            }
                            else -> Unit
                        }
                        if (claimedClose || phase == DeckDragPhase.DECK) change.consume()
                        if (!event.changes.any { it.pressed }) break
                    }
                    val velocity = tracker.calculateVelocity()
                    val predicted = Offset(
                        total.x + velocity.x * 0.16f,
                        total.y + velocity.y * 0.16f,
                    )
                    val ended = when {
                        phase != DeckDragPhase.UNDECIDED -> phase
                        startedDuringOpen -> DeckDragPhase.IDLE
                        else -> DeckDragPhase.TAP
                    }
                    phase = DeckDragPhase.IDLE
                    dragEnd.value(ended, total, predicted, down.position, dragAnchor)
                }
            }
    ) {
        val density = LocalDensity.current
        model.tabs.forEachIndexed { index, tab ->
            val delta = if (tab.id == closingTabId) closingDelta else index - focus
            if (zoom > 0.3f && tab.id != model.selectedTabId) return@forEachIndexed
            if (delta <= -8f || delta >= 2.2f) return@forEachIndexed

            val isClosing = tab.id == closingTabId
            val isZoomTarget = tab.id == model.selectedTabId && !isClosing
            val z = if (isZoomTarget) zoom else 0f
            val lift = if (isClosing) closeLift else 0f
            val restScale = depthScale(delta)
            val scale = lerp(restScale, metrics.fillScale, z)
            val lastIndex = max(model.tabs.size - 1, 0).toFloat()
            val centerX = lerp(
                leadingEdge(delta, metrics, focus, lastIndex) + metrics.cardWidth / 2f,
                metrics.width / 2f,
                z,
            )
            val centerY = lerp(metrics.deckCenterY + lift, metrics.height / 2f, z)
            val resting = restOpacity(delta)
            val opacity = when {
                isClosing -> 1f
                isZoomTarget -> lerp(resting, 1f, z)
                else -> resting * fade(zoom)
            }
            val corner = DeckLayout.corner * (1f - z)

            val cardW = with(density) { metrics.cardWidth.toDp() }
            val cardH = with(density) { metrics.cardHeight.toDp() }
            val labelHeightPx = with(density) { 18.dp.toPx() }
            val labelGapPx = with(density) { 10.dp.toPx() }
            val visualTop = centerY - metrics.cardHeight * scale / 2f

            key(tab.id) {
            Box(
                Modifier
                    .zIndex(index + if (isZoomTarget && zoom > 0f) 10_000f else 0f)
                    .graphicsLayer {
                        this.alpha = opacity
                        scaleX = scale
                        scaleY = scale
                        translationX = centerX - metrics.cardWidth / 2f
                        translationY = centerY - metrics.cardHeight / 2f
                        transformOrigin = TransformOrigin.Center
                    }
                    .width(cardW)
                    .height(cardH)
            ) {
                TabCardContent(
                    model = model,
                    tab = tab,
                    corner = corner,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val leading = max(leadingEdge(delta, metrics, focus, lastIndex), 0f)
            val covered = if (index + 1 < model.tabs.size) {
                leadingEdge(delta + 1, metrics, focus, lastIndex)
            } else {
                metrics.width
            }
            val trailing = min(min(leading + metrics.cardWidth, covered), metrics.width)
            val slice = max(trailing - leading, 22f)
            val showsTitle = slice > metrics.width * 0.3f
            val labelOpacity = if (isClosing) 1f else resting * fade(zoom)
            val labelWidthPx = if (showsTitle) slice - 12f else 22f
            val labelTop = visualTop - labelHeightPx - labelGapPx
            val labelLeft = leading + 10f

            Row(
                Modifier
                    .zIndex(index + 0.5f)
                    .graphicsLayer {
                        alpha = labelOpacity
                        translationX = labelLeft
                        translationY = labelTop
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .width(with(density) { labelWidthPx.toDp() })
                    .height(with(density) { labelHeightPx.toDp() }),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FaviconBadge(tab)
                if (showsTitle) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (tab.isBookmerHome) "Collection" else tab.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun TabCardContent(
    model: BrowserViewModel,
    tab: BrowserTab,
    corner: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(max(corner, 0f).dp)
    val preview = model.tabPreviews[tab.id]
    val image = remember(preview) { preview?.takeIf { !it.isRecycled }?.asImageBitmap() }
    val cardColor = MaterialTheme.colorScheme.background
    Box(
        modifier
            .graphicsLayer {
                shadowElevation = 28f
                this.shape = shape
                clip = true
            }
            .background(cardColor)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), shape),
    ) {
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
            )
            tab.isBookmerHome -> CollectionTabPreviewFallback(model)
            else -> TabPreviewFallback(tab)
        }
    }
}

@Composable
private fun CollectionTabPreviewFallback(model: BrowserViewModel) {
    val settings = model.settings
    val background = if (bookmerIsDarkTheme()) Color(0xFF19191B) else Color(0xFFF7F7F9)
    Box(Modifier.fillMaxSize().background(background)) {
        settings.wallpaper?.let { RemoteImage(it, Modifier.fillMaxSize(), ContentScale.Crop) }
    }
}

@Composable
private fun TabPreviewFallback(tab: BrowserTab) {
    val hue = abs(tab.title.hashCode() % 360).toFloat()
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.hsl(hue, 0.55f, 0.42f),
                        Color.hsl((hue + 50f) % 360f, 0.62f, 0.28f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Language, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                tab.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun FaviconBadge(tab: BrowserTab) {
    if (tab.isBookmerHome) {
        Icon(Icons.Rounded.GridView, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface)
    } else {
        val icon = BookmerItem.iconFor(tab.url.orEmpty())
        if (icon != null) {
            RemoteImage(icon, Modifier.size(15.dp).clip(RoundedCornerShape(3.dp)))
        } else {
            Icon(Icons.Rounded.Language, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CircleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
    }
}

private fun slotOffset(delta: Float, width: Float): Float {
    if (delta >= 0f) return delta * width * DeckLayout.rightStep
    val depth = -delta
    val falloff = DeckLayout.leftFalloff
    return -width * DeckLayout.leftStep * (1f - falloff.pow(depth)) / (1f - falloff)
}

private fun focusLeading(metrics: DeckMetrics, focus: Float, lastIndex: Float): Float {
    val behind = -slotOffset(-min(max(focus, 0f), DeckLayout.maxDepth + 0.4f), metrics.width)
    val balanced = (metrics.width - metrics.cardWidth + behind) / 2f
    val anchored = metrics.width * DeckLayout.focusLeading
    val rightPresence = min(max(lastIndex - focus, 0f), 1f)
    return balanced + (anchored - balanced) * rightPresence
}

private fun leadingEdge(delta: Float, metrics: DeckMetrics, focus: Float, lastIndex: Float): Float =
    focusLeading(metrics, focus, lastIndex) + slotOffset(delta, metrics.width)

private fun depthScale(delta: Float): Float {
    val depth = min(max(-delta, 0f), DeckLayout.maxDepth)
    return 1f - DeckLayout.depthScale * depth
}

private fun restOpacity(delta: Float): Float {
    if (delta > 1f) return max(0f, 1f - (delta - 1f) / 0.5f)
    if (delta < -6.5f) return max(0f, 1f + (delta + 6.5f) / 0.9f)
    return 1f
}

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t.coerceIn(0f, 1f)
private fun fade(zoom: Float): Float = max(0f, 1f - zoom * 2.4f)

private fun rubberBand(value: Float, lower: Float, upper: Float): Float {
    if (value < lower) {
        val overflow = lower - value
        return lower - (1f - 1f / (overflow * 0.9f + 1f)) * 0.42f
    }
    if (value > upper) {
        val overflow = value - upper
        return upper + (1f - 1f / (overflow * 0.9f + 1f)) * 0.42f
    }
    return value
}

private fun hitIndex(point: Offset, metrics: DeckMetrics, focus: Float, count: Int): Int? {
    val lastIndex = max(count - 1, 0).toFloat()
    for (index in (count - 1) downTo 0) {
        val delta = index - focus
        if (delta <= -8f || delta >= 2.2f || restOpacity(delta) <= 0.2f) continue
        val scale = depthScale(delta)
        val width = metrics.cardWidth * scale
        val height = metrics.cardHeight * scale
        val centerX = leadingEdge(delta, metrics, focus, lastIndex) + metrics.cardWidth / 2f
        val rect = Rect(
            left = centerX - width / 2f,
            top = metrics.deckCenterY - height / 2f,
            right = centerX + width / 2f,
            bottom = metrics.deckCenterY + height / 2f,
        )
        if (rect.contains(point)) return index
    }
    return null
}
