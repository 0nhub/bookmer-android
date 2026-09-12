package com.bookmer.browser.ui

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.browser.BrowserWebView
import com.bookmer.browser.browser.Overlay
import com.bookmer.browser.data.ThemeMode
import com.bookmer.browser.ui.theme.BookmerBrowserTheme
import com.bookmer.browser.ui.theme.bookmerIsDarkTheme
import kotlin.math.min
import kotlinx.coroutines.launch

@Composable
fun BookmerApp(model: BrowserViewModel) {
    val settings = model.preferences.settings
    val dark = when (settings.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    BookmerBrowserTheme(darkTheme = dark, dynamicColor = false) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                if (!settings.setupCompleted) SetupWelcomeScreen(model) else BrowserShell(model)
                if (model.showLoginWeb) {
                    BackHandler { model.dismissLogin() }
                    LoginWebSheet(
                        onAuthenticated = { token -> model.completeWebLogin(token) },
                        onCancel = model::dismissLogin,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserShell(model: BrowserViewModel) {
    BackHandler {
        when {
            model.showLoginWeb -> model.dismissLogin()
            model.readerContent != null -> model.readerContent = null
            model.pendingCollect != null -> model.pendingCollect = null
            model.pendingDownload != null -> model.pendingDownload = null
            model.overlay != Overlay.NONE -> model.dismissOverlay()
            model.isFindOnPage -> model.endFind()
            model.isHideElementsActive -> model.endHideElements(cancel = true)
            model.isImmersive -> model.exitImmersive()
            else -> model.goBack()
        }
    }

    // Settings scroll must not compete with the full browser shell recomposing every frame.
    if (model.overlay == Overlay.SETTINGS) {
        LightOverlayStatusBar()
        SettingsScreen(model)
        return
    }

    val browsingWeb = model.readerContent == null
        && !model.showsCollectionHome
        && model.blockedPageUrl == null
        && !model.isImmersive

    val darkTheme = bookmerIsDarkTheme()
    val lightAppOverlay = when (model.overlay) {
        Overlay.SETTINGS, Overlay.HISTORY, Overlay.TAB_HISTORY, Overlay.DOWNLOADS, Overlay.NAVIGATE, Overlay.BOOKMARK_TOOLS -> true
        else -> false
    }

    LightOverlayStatusBar(browsingWeb = browsingWeb, lightAppOverlay = lightAppOverlay, darkTheme = darkTheme)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val hostView = LocalView.current
        val scope = rememberCoroutineScope()
        val tabSlide = remember { Animatable(0f) }
        var tabSlideDrag by remember { mutableFloatStateOf(0f) }
        var tabSlideDragging by remember { mutableStateOf(false) }
        var isTabSliding by remember { mutableStateOf(false) }
        val displaySlide = if (tabSlideDragging) tabSlideDrag else tabSlide.value
        val settleSlide = spring<Float>(dampingRatio = 0.76f, stiffness = 400f)
        val switchSlide = spring<Float>(dampingRatio = 0.92f, stiffness = 380f)
        fun cancelTabSlide() {
            val current = if (tabSlideDragging) tabSlideDrag else tabSlide.value
            tabSlideDragging = false
            scope.launch {
                tabSlide.snapTo(current)
                tabSlide.animateTo(0f, settleSlide)
            }
        }
        fun animateTabSlide(forward: Boolean, apply: () -> Unit) {
            if (isTabSliding) return
            isTabSliding = true
            tabSlideDragging = false
            apply()
            hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            scope.launch {
                tabSlide.snapTo(if (forward) pageWidth else -pageWidth)
                tabSlide.animateTo(0f, switchSlide)
                isTabSliding = false
            }
        }

        Box(Modifier.fillMaxSize().graphicsLayer { translationX = displaySlide }) {
        when {
            model.readerContent != null -> ReaderScreen(model, Modifier.fillMaxSize())
            model.showsCollectionHome -> {
                if (lightAppOverlay) {
                    OverlayUnderlayPlaceholder(collectionHome = true)
                } else {
                    CollectionScreen(model, Modifier.fillMaxSize())
                }
            }
            model.blockedPageUrl != null -> BlockedPage(model.blockedPageUrl.orEmpty(), model::openHome)
            lightAppOverlay -> OverlayUnderlayPlaceholder(collectionHome = false)
            else -> {
                if (model.isImmersive) {
                    BrowserWebView(model, Modifier.fillMaxSize())
                } else {
                    // Page content starts below the status bar — clock/battery never sit on the page.
                    Column(Modifier.fillMaxSize()) {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .windowInsetsTopHeight(WindowInsets.statusBars)
                                .background(Color.Black)
                        )
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            BrowserWebView(model, Modifier.fillMaxSize())
                            if (model.isLoading) {
                                LinearProgressIndicator(
                                    progress = { model.pageProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (model.isLoading && model.showsCollectionHome) {
            LinearProgressIndicator(
                progress = { model.pageProgress / 100f },
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(2.dp).align(Alignment.TopCenter),
                color = Color.White,
            )
        }

        AnimatedVisibility(
            visible = model.isTranslating && model.overlay == Overlay.NONE && model.readerContent == null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .zIndex(40f),
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            TranslatingStatusPill()
        }
        }

        when {
            model.readerContent != null -> Unit
            model.overlay != Overlay.NONE -> Unit
            model.isImmersive && model.isNearPageTop && !model.showImmersiveTip &&
                !model.isFindOnPage && !model.isHideElementsActive ->
                ImmersiveExit(model, Modifier.align(Alignment.TopCenter).statusBarsPadding())
            model.isImmersive && model.isNearPageBottom && !model.showImmersiveTip &&
                !model.isFindOnPage && !model.isHideElementsActive ->
                ImmersiveExit(model, Modifier.align(Alignment.BottomCenter))
            model.isImmersive -> Unit
            model.isHideElementsActive -> HideElementsPickBar(model, Modifier.align(Alignment.BottomCenter))
            model.isFindOnPage -> FindBar(model, Modifier.align(Alignment.BottomCenter))
            // Swipe-down on address bar only — sticky strip with tab title; scroll does nothing.
            model.toolbarStickyCollapsed -> MinimizedChromeStrip(model, Modifier.align(Alignment.BottomCenter))
            // Page scroll hide — chrome is completely gone until scroll-up.
            model.showsToolbar -> BrowserChrome(
                model,
                Modifier.align(Alignment.BottomCenter),
                tabSlideLocked = isTabSliding,
                onTabSlideDrag = { dx ->
                    if (!isTabSliding && dx > 0f && !model.canGoToPreviousTab) {
                        tabSlideDragging = true
                        val limit = min(pageWidth * 0.2f, 72f)
                        tabSlideDrag = (1f - 1f / (dx / limit + 1f)) * limit
                    }
                },
                onTabSlideEnd = { dx, predictedX ->
                    if (!isTabSliding) {
                        when {
                            kotlin.math.abs(dx) <= 50f && kotlin.math.abs(predictedX) <= 80f -> cancelTabSlide()
                            dx < 0f || predictedX < -100f -> animateTabSlide(forward = true) { model.goToNextTabOrCreate() }
                            dx > 0f || predictedX > 100f -> {
                                if (model.canGoToPreviousTab) {
                                    animateTabSlide(forward = false) { model.goToPreviousTab() }
                                } else {
                                    cancelTabSlide()
                                }
                            }
                            else -> cancelTabSlide()
                        }
                    }
                },
                onTabSlideCancel = { cancelTabSlide() },
            )
        }

        if (model.showImmersiveTip) {
            ImmersiveTipOverlay(onDismiss = model::dismissImmersiveTip)
        }

        if (model.showHideElementsTip) {
            HideElementsTipOverlay(onDismiss = model::dismissHideElementsTip)
        }

        when (model.overlay) {
            Overlay.SETTINGS -> Unit // Handled above via dedicated branch (scroll isolation).
            Overlay.TABS -> TabsSwitcherScreen(model)
            Overlay.HISTORY -> HistoryScreen(model)
            Overlay.TAB_HISTORY -> TabHistoryScreen(model)
            Overlay.DOWNLOADS -> DownloadsScreen(model)
            Overlay.NAVIGATE -> NavigateScreen(model)
            Overlay.BOOKMARK_TOOLS -> BookmarkToolsScreen(model)
            Overlay.NONE -> Unit
        }
        BookmerDialogs(model)
        if (model.hideConfirmDraft != null) HideElementConfirmDialog(model)
        if (model.showHideElementsManage) HideElementsManageSheet(model)
    }
}

@Composable
private fun LightOverlayStatusBar(
    browsingWeb: Boolean = false,
    lightAppOverlay: Boolean = true,
    darkTheme: Boolean = bookmerIsDarkTheme(),
) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val insets = WindowCompat.getInsetsController(window, view)
        val darkStatusIcons = lightAppOverlay && !darkTheme
        insets.isAppearanceLightStatusBars = darkStatusIcons
        insets.isAppearanceLightNavigationBars = darkStatusIcons
        if (browsingWeb && !lightAppOverlay) {
            window.statusBarColor = Color.Black.toArgb()
        } else if (lightAppOverlay) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}

/** Cheap backdrop while full-screen overlays (Settings, History, …) cover the shell — avoids WebView/Collection drawing underneath. */
@Composable
private fun OverlayUnderlayPlaceholder(collectionHome: Boolean) {
    val color = when {
        collectionHome && bookmerIsDarkTheme() -> Color(0xFF19191B)
        collectionHome -> Color(0xFFF7F7F9)
        else -> Color.Black
    }
    Box(Modifier.fillMaxSize().background(color))
}

/** Sticky stub after swipe-down on the address bar — tap restores chrome. Shows tab title. */
@Composable
private fun MinimizedChromeStrip(model: BrowserViewModel, modifier: Modifier = Modifier) {
    val title = model.currentTab.title.ifBlank { model.currentTab.url ?: "Bookmer" }
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable(onClick = model::expandToolbar)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            color = Color.White.copy(alpha = .86f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(horizontal = 16.dp),
        )
    }
}

/** iOS-style status chip while Google Translate loads the free web view. */
@Composable
private fun TranslatingStatusPill() {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xE6111112))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color.White,
            strokeWidth = 2.dp,
        )
        Text(
            "Translating",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
