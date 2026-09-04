package com.bookmer.browser.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.browser.Overlay
import com.bookmer.browser.data.AliasNamedItem
import com.bookmer.browser.data.AliasOs
import com.bookmer.browser.data.AliasPlaceSuggestion
import com.bookmer.browser.data.AliasStore
import com.bookmer.browser.data.BlockedSite
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.CustomSearchEngine
import com.bookmer.browser.data.LaunchShortcut
import com.bookmer.browser.data.LaunchShortcutKind
import com.bookmer.browser.data.SearchEngine
import com.bookmer.browser.data.StartNavigationAction
import com.bookmer.browser.data.ThemeMode
import com.bookmer.browser.data.ToolbarAction
import com.bookmer.browser.data.WebNavigationAction
import com.bookmer.browser.integration.ShortcutPublisher
import com.bookmer.browser.ui.theme.bookmerIsDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay

private enum class SettingsPage(val title: String) {
    ROOT("Settings"), ACCOUNT("Account"), SUBSCRIPTION("Subscription"),
    METADATA("Metadata"), METADATA_OS("Operating System"), METADATA_COUNTRY("Country"),
    METADATA_LOCATION("Location"), METADATA_LANGUAGE("Language"), METADATA_TIMEZONE("Time Zone"),
    COOKIES("Cookies"), BLOCKED_SITES("Websites"),
    HIDDEN_ELEMENTS("Hidden Elements"), CLOSE_TABS("Close Tabs"), SEARCH_ENGINE("Search Engine"),
    TRANSLATE("Translate"), WALLPAPER("Wallpaper"), THEME("Theme"), TOOLBAR("Toolbar"),
    TOOLBAR_START_PAGE("Start Page"), TOOLBAR_ON_WEBSITES("On Websites"),
    TOOLBAR_AUTO_REFRESH("Auto Refresh"), TOOLBAR_ADD_REFRESH("Add Interval"),
    WIDGETS("Widgets"), CONTROL_CENTER("Control Center"),
}

private val SettingsCardShape = RoundedCornerShape(26.dp)
private val IconWellShape = CircleShape

private object SettingsAccent {
    val blue = Color(0xFF2F6BFF)
    val blueSoft = Color(0xFF5B9BFF)
    val teal = Color(0xFF0F9C8A)
    val red = Color(0xFFE5484D)
    val orange = Color(0xFFF5A524)
    val purple = Color(0xFF7C5CFC)
    val indigo = Color(0xFF5B6CFF)
    val gray = Color(0xFF6B7280)
    val green = Color(0xFF30A46C)
}

@Composable
private fun settingsCanvas(): Color =
    if (bookmerIsDarkTheme()) Color(0xFF121214) else Color(0xFFF2F2F7)

@Composable
private fun settingsCard(): Color =
    if (bookmerIsDarkTheme()) Color(0xFF1C1C1E) else Color.White

@Composable
fun SettingsScreen(model: BrowserViewModel) {
    var stack by remember { mutableStateOf(listOf(SettingsPage.ROOT)) }
    val page = stack.last()
    val canvas = settingsCanvas()
    Surface(Modifier.fillMaxSize(), color = canvas) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (page == SettingsPage.ROOT) {
                SettingsRootChrome(onClose = model::dismissOverlay)
            } else {
                SettingsDetailHeader(page.title) { stack = stack.dropLast(1) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    SettingsPage.ROOT -> SettingsRoot(model) { stack = stack + it }
                    SettingsPage.ACCOUNT -> AccountSettings(model) { stack = stack + it }
                    SettingsPage.SUBSCRIPTION -> SubscriptionSettings(model)
                    SettingsPage.METADATA -> MetadataSettings(model) { stack = stack + it }
                    SettingsPage.METADATA_OS -> MetadataOsPage(model)
                    SettingsPage.METADATA_COUNTRY -> MetadataCatalogPage(
                        items = AliasStore.countries,
                        selectedCode = model.alias.countryCode,
                        onSelect = { model.alias.chooseCountry(it); model.destroyWebViews() },
                    )
                    SettingsPage.METADATA_LOCATION -> MetadataLocationPage(model)
                    SettingsPage.METADATA_LANGUAGE -> MetadataCatalogPage(
                        items = AliasStore.languages,
                        selectedCode = model.alias.languageCode,
                        onSelect = { model.alias.chooseLanguage(it); model.destroyWebViews() },
                    )
                    SettingsPage.METADATA_TIMEZONE -> MetadataCatalogPage(
                        items = AliasStore.timeZones,
                        selectedCode = model.alias.timeZoneIdentifier,
                        onSelect = { model.alias.chooseTimeZone(it); model.destroyWebViews() },
                    )
                    SettingsPage.COOKIES -> CookieSettings(model)
                    SettingsPage.BLOCKED_SITES -> BlockedSitesSettings(model)
                    SettingsPage.HIDDEN_ELEMENTS -> Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        HiddenElementsSettings(model)
                    }
                    SettingsPage.CLOSE_TABS -> CloseTabsSettings(model)
                    SettingsPage.SEARCH_ENGINE -> SearchEngineSettings(model)
                    SettingsPage.TRANSLATE -> TranslateSettings(model)
                    SettingsPage.WALLPAPER -> WallpaperSettings(model)
                    SettingsPage.THEME -> ThemeSettings(model)
                    SettingsPage.TOOLBAR -> ToolbarSettings(model) { stack = stack + it }
                    SettingsPage.TOOLBAR_START_PAGE -> ToolbarStartPageSettings(model)
                    SettingsPage.TOOLBAR_ON_WEBSITES -> ToolbarOnWebsitesSettings(model)
                    SettingsPage.TOOLBAR_AUTO_REFRESH -> ToolbarAutoRefreshSettings(model) { stack = stack + it }
                    SettingsPage.TOOLBAR_ADD_REFRESH -> ToolbarAddRefreshInterval(model) { stack = stack.dropLast(1) }
                    SettingsPage.WIDGETS, SettingsPage.CONTROL_CENTER -> ShortcutEditor(model, page)
                }
            }
        }
    }
}

@Composable
private fun SettingsRootChrome(onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Rounded.Close, "Close")
        }
        Text(
            "Settings",
            Modifier
                .align(Alignment.Center)
                .padding(vertical = 28.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun SettingsDetailHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsRoot(model: BrowserViewModel, open: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    var clearData by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { AccountHeroCard(model) { open(SettingsPage.ACCOUNT) } }

        item { SectionLabel("Browser Data") }
        item {
            SettingsModuleCard {
                SettingsModuleRow("Metadata", Icons.Rounded.Description, SettingsAccent.teal) {
                    open(SettingsPage.METADATA)
                }
                SettingsModuleRow("History", Icons.Rounded.History, SettingsAccent.blue) {
                    model.showOverlay(Overlay.HISTORY)
                }
                SettingsModuleRow("Cookies", Icons.Rounded.Cookie, SettingsAccent.orange) {
                    open(SettingsPage.COOKIES)
                }
                SettingsModuleRow(
                    "Clear Data",
                    Icons.Rounded.Delete,
                    SettingsAccent.red,
                    danger = true,
                    showDivider = false,
                ) { clearData = true }
            }
        }

        item { SectionLabel("Blocker") }
        item {
            SettingsModuleCard {
                ToggleModuleRow("Cookies", Icons.Rounded.Cookie, SettingsAccent.orange, model.settings.blockCookies) { value ->
                    model.preferences.update { it.copy(blockCookies = value) }; model.destroyWebViews()
                }
                ToggleModuleRow("Trackers", Icons.Rounded.Security, SettingsAccent.red, model.settings.blockTrackers) { value ->
                    model.preferences.update { it.copy(blockTrackers = value) }; model.destroyWebViews()
                }
                ToggleModuleRow("Popups", Icons.Rounded.Block, SettingsAccent.purple, model.settings.blockPopups) { value ->
                    model.preferences.update { it.copy(blockPopups = value) }; model.destroyWebViews()
                }
                ToggleModuleRow("App Banners", Icons.Rounded.Smartphone, SettingsAccent.blue, model.settings.blockAppBanners) { value ->
                    model.preferences.update { it.copy(blockAppBanners = value) }
                }
                ToggleModuleRow("YouTube Ads", Icons.Rounded.BrokenImage, SettingsAccent.red, model.settings.blockYouTubeAds) { value ->
                    model.preferences.update { it.copy(blockYouTubeAds = value) }
                }
                SettingsModuleRow("Websites", Icons.Rounded.Description, SettingsAccent.teal, showDivider = false) {
                    open(SettingsPage.BLOCKED_SITES)
                }
            }
        }

        item { SectionLabel("Browsing") }
        item {
            SettingsModuleCard {
                SettingsModuleRow("Hidden Elements", Icons.Rounded.VisibilityOff, SettingsAccent.purple) {
                    open(SettingsPage.HIDDEN_ELEMENTS)
                }
                SettingsModuleRow("Close Tabs", Icons.Rounded.Tab, SettingsAccent.blue) {
                    open(SettingsPage.CLOSE_TABS)
                }
                SettingsModuleRow("Search Engine", Icons.Rounded.Search, SettingsAccent.teal) {
                    open(SettingsPage.SEARCH_ENGINE)
                }
                SettingsModuleRow("Translate", Icons.Rounded.Translate, SettingsAccent.green, showDivider = false) {
                    open(SettingsPage.TRANSLATE)
                }
            }
        }

        item { SectionLabel("Appearance") }
        item {
            SettingsModuleCard {
                SettingsModuleRow("Wallpaper", Icons.Rounded.Wallpaper, SettingsAccent.purple) {
                    open(SettingsPage.WALLPAPER)
                }
                SettingsModuleRow("Theme", Icons.Rounded.Tune, SettingsAccent.indigo) {
                    open(SettingsPage.THEME)
                }
                SettingsModuleRow("Toolbar", Icons.Rounded.PhoneAndroid, SettingsAccent.blue, showDivider = false) {
                    open(SettingsPage.TOOLBAR)
                }
            }
        }

        item { SectionLabel("Shortcuts") }
        item {
            SettingsModuleCard {
                SettingsModuleRow("Widgets", Icons.Rounded.Widgets, SettingsAccent.indigo) {
                    open(SettingsPage.WIDGETS)
                }
                SettingsModuleRow("Control Center", Icons.Rounded.ToggleOn, SettingsAccent.blue, showDivider = false) {
                    open(SettingsPage.CONTROL_CENTER)
                }
            }
        }

        item {
            SettingsModuleCard {
                SettingsModuleRow("Android Settings", Icons.Rounded.PhoneAndroid, SettingsAccent.gray, external = true) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                }
                SettingsModuleRow(
                    "Get Support",
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    SettingsAccent.green,
                    external = true,
                    showDivider = false,
                ) {
                    model.load(BookmerUrls.HELP, inNewTab = true)
                    model.dismissOverlay()
                }
            }
        }
    }

    if (clearData) {
        ClearDataDialog({ clearData = false }) { history, web ->
            model.clearBrowsingData(history, web); clearData = false
        }
    }
}

@Composable
private fun AccountHeroCard(model: BrowserViewModel, onClick: () -> Unit) {
    val signedIn = model.session.value.isSignedIn
    val title = if (signedIn) {
        model.session.value.name ?: model.session.value.email ?: "Account"
    } else {
        "Sign in to Bookmer"
    }
    val subtitle = if (signedIn) {
        model.session.value.email.orEmpty().ifBlank { "Manage your account and sync" }
    } else {
        "Sync your Collection across devices"
    }
    Surface(
        onClick = onClick,
        shape = SettingsCardShape,
        color = settingsCard(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = SettingsAccent.blueSoft, style = MaterialTheme.typography.bodyMedium)
            }
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SettingsAccent.blueSoft.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, null, Modifier.size(30.dp), tint = SettingsAccent.blueSoft)
            }
        }
    }
}

@Composable
private fun SettingsModuleCard(content: @Composable () -> Unit) {
    Surface(
        shape = SettingsCardShape,
        color = settingsCard(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsModuleRow(
    title: String,
    icon: ImageVector,
    accent: Color,
    subtitle: String? = null,
    value: String? = null,
    external: Boolean = false,
    danger: Boolean = false,
    showDivider: Boolean = true,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val titleColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(IconWellShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(22.dp), tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = titleColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            when {
                trailing != null -> trailing()
                else -> {
                    if (!value.isNullOrBlank()) {
                        Text(
                            value,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    when {
                        external -> Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        showChevron && onClick != null -> Icon(
                            Icons.Rounded.ChevronRight,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                Modifier.padding(start = 70.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            )
        }
    }
}

@Composable
private fun SettingsScroll(content: LazyListScope.() -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun AccountSettings(model: BrowserViewModel, open: (SettingsPage) -> Unit) {
    val signedIn = model.session.value.isSignedIn
    var logout by remember { mutableStateOf(false) }
    SettingsScroll {
        if (!signedIn) {
            item {
                SettingsModuleCard {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).background(SettingsAccent.blueSoft.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Person, null, Modifier.size(40.dp), tint = SettingsAccent.blueSoft)
                        }
                        Text("Sign in to sync your Collection across devices.", textAlign = TextAlign.Center)
                        Button(onClick = {
                            model.dismissOverlay()
                            model.presentLogin()
                        }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Log in")
                        }
                    }
                }
            }
        } else {
            item {
                SettingsModuleCard {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.AccountCircle, null, Modifier.size(72.dp), tint = SettingsAccent.blueSoft)
                        Spacer(Modifier.height(8.dp))
                        Text(model.session.value.name ?: "Bookmer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(model.session.value.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                SettingsModuleCard {
                    SettingsModuleRow("Account Hub", Icons.AutoMirrored.Rounded.OpenInNew, SettingsAccent.blue, external = true) {
                        model.load(BookmerUrls.ACCOUNT, inNewTab = true); model.dismissOverlay()
                    }
                    SettingsModuleRow("Subscription", Icons.Rounded.Star, SettingsAccent.orange) { open(SettingsPage.SUBSCRIPTION) }
                    SettingsModuleRow("Switch Account", Icons.Rounded.SwapHoriz, SettingsAccent.indigo) {
                        model.signOutFully {
                            model.dismissOverlay()
                            model.presentLogin()
                        }
                    }
                    SettingsModuleRow("Log out", Icons.AutoMirrored.Rounded.Logout, SettingsAccent.red, danger = true, showDivider = false) { logout = true }
                }
            }
            item {
                TextButton(
                    onClick = {
                        model.load("https://id.bookmer.com/dashboard?tab=accounts", inNewTab = true)
                        model.dismissOverlay()
                    },
                    Modifier.fillMaxWidth(),
                ) { Text("Remove Account", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (logout) {
        AlertDialog(
            onDismissRequest = { logout = false },
            title = { Text("Log out?") },
            text = { Text("Local browsing data and the signed-in Collection will be reset to guest defaults.") },
            confirmButton = {
                TextButton(onClick = { model.signOutFully { logout = false; model.dismissOverlay() } }) {
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { logout = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SubscriptionSettings(model: BrowserViewModel) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val pro = model.pro
    val signedIn = model.session.value.isSignedIn
    val lifetime = model.session.value.accountType == "LIFETIME"
    val isPro = pro.isPro
    LaunchedEffect(Unit) {
        pro.loadProduct()
        pro.refreshEntitlement(syncAccount = true)
        model.refreshProfile()
    }
    SettingsScroll {
        item {
            SettingsModuleCard {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(SettingsAccent.orange),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Star, null, tint = Color.White)
                    }
                    Text(
                        when {
                            lifetime -> "Lifetime"
                            isPro -> "Bookmer PRO"
                            else -> "Free Plan"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        when {
                            lifetime -> "Your account has lifetime access to all PRO features."
                            isPro && pro.accountSyncFailed ->
                                "PRO is active on this device. Syncing it to your Bookmer account failed — it retries automatically."
                            isPro && pro.storeExpiryMs != null ->
                                "PRO is active until ${formatSubscriptionDate(pro.storeExpiryMs!!)} and syncs with your Bookmer account."
                            isPro -> "PRO is active and syncs with your Bookmer account."
                            signedIn -> "Your account is on the free plan."
                            else -> "Sign in first so the subscription is tied to your Bookmer account."
                        },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!isPro) {
            item {
                SettingsModuleCard {
                    listOf(
                        "Hidden bookmarks and archive",
                        "Recover deleted bookmarks",
                        "Broken link check",
                        "Your own wallpapers",
                        "Full-text search in notes",
                        "Share folders with password and analytics",
                    ).forEachIndexed { index, label ->
                        SettingsModuleRow(
                            label,
                            Icons.Rounded.Check,
                            SettingsAccent.orange,
                            showDivider = index < 5,
                            showChevron = false,
                        )
                    }
                }
            }
            item {
                SettingsModuleCard {
                    Button(
                        onClick = { activity?.let(pro::purchase) },
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        enabled = !pro.isPurchasing && signedIn,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (pro.isPurchasing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Subscribe for ${pro.displayPrice} / year")
                        }
                    }
                    SettingsModuleRow(
                        if (pro.isRestoring) "Restoring…" else "Restore Purchases",
                        Icons.Rounded.History,
                        SettingsAccent.blue,
                        showDivider = false,
                    ) { if (!pro.isRestoring) pro.restore() }
                }
            }
            item {
                ExplanatoryCard(
                    "Bookmer PRO renews every year for ${pro.displayPrice} until you cancel. " +
                        "Payment is charged to your Google account; manage or cancel it in Google Play at least 24 hours before the period ends.",
                )
            }
        } else if (pro.hasPlayEntitlement) {
            item {
                SettingsModuleCard {
                    SettingsModuleRow(
                        "Manage Subscription",
                        Icons.AutoMirrored.Rounded.OpenInNew,
                        SettingsAccent.blue,
                        showDivider = false,
                    ) { pro.openManageSubscriptions() }
                }
            }
        }
        pro.lastError?.takeIf { it.isNotBlank() }?.let { error ->
            item { ExplanatoryCard(error) }
        }
        item {
            SettingsModuleCard {
                SettingsModuleRow("Terms of Use", Icons.AutoMirrored.Rounded.OpenInNew, SettingsAccent.blue) {
                    model.load("https://www.bookmer.com/terms", inNewTab = true); model.dismissOverlay()
                }
                SettingsModuleRow("Privacy Policy", Icons.AutoMirrored.Rounded.OpenInNew, SettingsAccent.blue, showDivider = false) {
                    model.load("https://www.bookmer.com/privacy", inNewTab = true); model.dismissOverlay()
                }
            }
        }
    }
}

private fun formatSubscriptionDate(ms: Long): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(ms))

@Composable
private fun MetadataSettings(model: BrowserViewModel, open: (SettingsPage) -> Unit) {
    val alias = model.alias
    SettingsScroll {
        item {
            ExplanatoryCard("Websites see these values instead of your real device. Default sends what this phone actually is.")
        }
        item {
            SettingsModuleCard {
                MetadataValueRow("Operating System", alias.operatingSystemLabel) { open(SettingsPage.METADATA_OS) }
                MetadataValueRow("Country", alias.countryLabel) { open(SettingsPage.METADATA_COUNTRY) }
                MetadataValueRow("Location", alias.locationLabel) { open(SettingsPage.METADATA_LOCATION) }
                MetadataValueRow("Language", alias.languageLabel) { open(SettingsPage.METADATA_LANGUAGE) }
                MetadataValueRow("Time Zone", alias.timeZoneLabel, showDivider = false) {
                    open(SettingsPage.METADATA_TIMEZONE)
                }
            }
        }
    }
}

@Composable
private fun MetadataValueRow(title: String, value: String, showDivider: Boolean = true, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.padding(end = 6.dp),
            )
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showDivider) {
            HorizontalDivider(
                Modifier.padding(start = 18.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            )
        }
    }
}

@Composable
private fun MetadataOsPage(model: BrowserViewModel) {
    SettingsScroll {
        item {
            SettingsModuleCard {
                AliasOs.entries.forEachIndexed { index, os ->
                    CheckModuleRow(
                        os.label,
                        model.alias.operatingSystem == os,
                        showDivider = index < AliasOs.entries.lastIndex,
                    ) {
                        model.alias.chooseOperatingSystem(os)
                        model.destroyWebViews()
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataCatalogPage(
    items: List<AliasNamedItem>,
    selectedCode: String?,
    onSelect: (String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) { AliasStore.filterItems(items, query) }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsModuleCard {
                    CheckModuleRow("Default", selectedCode == null) { onSelect(null) }
                    filtered.forEachIndexed { index, item ->
                        CheckModuleRow(
                            item.name,
                            selectedCode.equals(item.code, ignoreCase = true),
                            showDivider = index < filtered.lastIndex,
                        ) { onSelect(item.code) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataLocationPage(model: BrowserViewModel) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AliasPlaceSuggestion>>(emptyList()) }
    val alias = model.alias

    LaunchedEffect(query) {
        delay(280)
        if (query.trim().length < 2) {
            suggestions = emptyList()
        } else {
            alias.searchPlaces(query) { suggestions = it }
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("City") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsModuleCard {
                    CheckModuleRow("Default", !alias.hasLocation, showDivider = alias.hasLocation || suggestions.isNotEmpty()) {
                        alias.clearLocation()
                        model.destroyWebViews()
                    }
                    if (alias.hasLocation) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                            Text(alias.locationLabel, fontWeight = FontWeight.Medium)
                            Text(
                                "${alias.locationLatitude}, ${alias.locationLongitude}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (suggestions.isNotEmpty()) {
                item { SectionLabel("Results") }
                item {
                    SettingsModuleCard {
                        suggestions.forEachIndexed { index, place ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        alias.chooseLocation(place.name, place.latitude, place.longitude)
                                        query = ""
                                        suggestions = emptyList()
                                        model.destroyWebViews()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                            ) {
                                Text(place.name, fontWeight = FontWeight.Medium)
                                if (place.subtitle.isNotBlank()) {
                                    Text(
                                        place.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (index < suggestions.lastIndex) {
                                HorizontalDivider(
                                    Modifier.padding(start = 18.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                )
                            }
                        }
                    }
                }
            }
            item {
                ExplanatoryCard("Websites that ask for your location receive this place.")
            }
        }
    }
}

@Composable
private fun CookieSettings(model: BrowserViewModel) {
    SettingsScroll {
        item {
            SettingsModuleCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Website Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Android WebView keeps website cookies in its protected app storage. Bookmer login cookies are preserved until you log out or clear all website data.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { android.webkit.CookieManager.getInstance().removeAllCookies(null) },
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Clear Cookies") }
                }
            }
        }
    }
}

@Composable
private fun BlockedSitesSettings(model: BrowserViewModel) {
    var add by remember { mutableStateOf(false) }
    SettingsScroll {
        item {
            SettingsModuleCard {
                SettingsModuleRow("Add Website", Icons.Rounded.Add, SettingsAccent.blue, showDivider = model.settings.blockedSites.isNotEmpty()) {
                    add = true
                }
                model.settings.blockedSites.forEachIndexed { index, site ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(site.host, fontWeight = FontWeight.Medium)
                            site.redirectUrl?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { model.preferences.update { it.copy(blockedSites = it.blockedSites - site) } }) {
                            Icon(Icons.Rounded.Delete, "Delete", tint = SettingsAccent.red)
                        }
                    }
                    if (index < model.settings.blockedSites.lastIndex) {
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
    if (add) AddBlockedSiteDialog({ add = false }) { site ->
        model.preferences.update { it.copy(blockedSites = it.blockedSites + site) }; add = false
    }
}

@Composable
private fun CloseTabsSettings(model: BrowserViewModel) {
    val options = listOf("Manually" to 0, "After One Day" to 1, "After One Week" to 7, "After One Month" to 30)
    SettingsScroll {
        item {
            SettingsModuleCard {
                options.forEachIndexed { index, (label, days) ->
                    CheckModuleRow(label, model.settings.closeTabsAfterDays == days, showDivider = index < options.lastIndex) {
                        model.preferences.update { it.copy(closeTabsAfterDays = days) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEngineSettings(model: BrowserViewModel) {
    var add by remember { mutableStateOf(false) }
    val engines = SearchEngine.entries.sortedBy { it.label }
    SettingsScroll {
        item {
            SettingsModuleCard {
                engines.forEachIndexed { index, engine ->
                    CheckModuleRow(
                        engine.label,
                        model.settings.selectedCustomSearchEngineId == null && model.settings.searchEngine == engine,
                        showDivider = index < engines.lastIndex || model.settings.customSearchEngines.isNotEmpty(),
                    ) {
                        model.preferences.update { it.copy(searchEngine = engine, selectedCustomSearchEngineId = null) }
                    }
                }
                model.settings.customSearchEngines.forEach { engine ->
                    CheckModuleRow(engine.name, model.settings.selectedCustomSearchEngineId == engine.id) {
                        model.preferences.update { it.copy(selectedCustomSearchEngineId = engine.id) }
                    }
                }
                SettingsModuleRow("Add Search Engine", Icons.Rounded.Add, SettingsAccent.blue, showDivider = false) { add = true }
            }
        }
    }
    if (add) AddSearchEngineDialog({ add = false }) { custom ->
        model.preferences.update {
            it.copy(customSearchEngines = it.customSearchEngines + custom, selectedCustomSearchEngineId = custom.id)
        }
        add = false
    }
}

@Composable
private fun TranslateSettings(model: BrowserViewModel) {
    val languages = listOf(
        "English" to "en", "Deutsch" to "de", "Français" to "fr", "Español" to "es",
        "Italiano" to "it", "Português" to "pt", "Nederlands" to "nl", "Polski" to "pl",
        "Русский" to "ru", "Türkçe" to "tr", "العربية" to "ar", "日本語" to "ja",
        "한국어" to "ko", "中文" to "zh-CN",
    )
    SettingsScroll {
        item { ExplanatoryCard("Pages are translated with free Google Translate. The Google Translate banner may appear on the page.") }
        item {
            SettingsModuleCard {
                languages.forEachIndexed { index, (name, code) ->
                    CheckModuleRow(name, model.settings.translateLanguage == code, showDivider = index < languages.lastIndex) {
                        model.preferences.update { it.copy(translateLanguage = code) }
                    }
                }
            }
        }
    }
}

private val wallpapers = listOf(
    "https://images.unsplash.com/photo-1554147090-e1221a04a025?w=1080&fit=max&q=80",
    "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1080&fit=max&q=80",
    "https://images.unsplash.com/photo-1477346611705-65d1883cee1e?w=1080&fit=max&q=80",
    "https://images.unsplash.com/photo-1511300636408-a63a89df3482?w=1080&fit=max&q=80",
    "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1080&fit=max&q=80",
    "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1080&fit=max&q=80",
)

@Composable
private fun WallpaperSettings(model: BrowserViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            model.preferences.update { settings -> settings.copy(wallpaper = it.toString(), wallpaperTextColor = "#FFFFFF") }
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SettingsModuleCard {
                Column(Modifier.padding(12.dp)) {
                    LazyVerticalGrid(
                        GridCells.Fixed(3),
                        Modifier.fillMaxWidth().height(390.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        model.preferences.update {
                                            it.copy(
                                                wallpaper = null,
                                                wallpaperTextColor = if (it.theme == ThemeMode.DARK) "#FFFFFF" else "#111111",
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text("None") }
                        }
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { picker.launch(arrayOf("image/*")) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Add, null); Text("Choose")
                                }
                            }
                        }
                        items(wallpapers) { wallpaper ->
                            Box {
                                RemoteImage(
                                    wallpaper,
                                    Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)).background(Color.Gray),
                                    ContentScale.Crop,
                                )
                                Box(Modifier.matchParentSize().clickable {
                                    model.preferences.update { it.copy(wallpaper = wallpaper, wallpaperTextColor = "#FFFFFF") }
                                })
                                if (model.settings.wallpaper == wallpaper) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black, CircleShape).padding(4.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingsModuleCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Blur", fontWeight = FontWeight.Medium)
                    Slider(model.settings.wallpaperBlur, { value -> model.preferences.update { it.copy(wallpaperBlur = value) } }, valueRange = 0f..24f)
                    Text("Dim", fontWeight = FontWeight.Medium)
                    Slider(model.settings.wallpaperDim, { value -> model.preferences.update { it.copy(wallpaperDim = value) } }, valueRange = 0f..0.7f)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { model.preferences.update { it.copy(wallpaperTextColor = "#FFFFFF") } }) { Text("White") }
                        OutlinedButton(onClick = { model.preferences.update { it.copy(wallpaperTextColor = "#111111") } }) { Text("Black") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettings(model: BrowserViewModel) {
    SettingsScroll {
        item {
            SettingsModuleCard {
                ThemeMode.entries.forEachIndexed { index, theme ->
                    CheckModuleRow(
                        theme.name.lowercase().replaceFirstChar(Char::uppercase),
                        model.settings.theme == theme,
                        showDivider = index < ThemeMode.entries.lastIndex,
                    ) {
                        model.preferences.update {
                            it.copy(
                                theme = theme,
                                wallpaperTextColor = when (theme) {
                                    ThemeMode.DARK -> "#FFFFFF"
                                    ThemeMode.LIGHT -> "#111111"
                                    ThemeMode.SYSTEM -> it.wallpaperTextColor
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarSettings(model: BrowserViewModel, open: (SettingsPage) -> Unit) {
    SettingsScroll {
        item {
            SettingsModuleCard {
                ToggleModuleRow(
                    "Hide Toolbar",
                    Icons.Rounded.VisibilityOff,
                    SettingsAccent.purple,
                    model.settings.hideToolbar,
                    showDivider = false,
                ) { value ->
                    model.preferences.update { it.copy(hideToolbar = value) }
                    if (!value) model.expandToolbar()
                }
            }
        }
        item {
            ExplanatoryCard("Scroll down to hide completely, scroll up to show. Swipe down on the address bar for a sticky title strip — tap it to restore.")
        }
        item { SectionLabel("Navigation Button") }
        item {
            SettingsModuleCard {
                SettingsModuleRow(
                    "Start Page",
                    Icons.Rounded.Home,
                    SettingsAccent.blue,
                    value = model.settings.startNavigationAction.label,
                ) { open(SettingsPage.TOOLBAR_START_PAGE) }
                SettingsModuleRow(
                    "On Websites",
                    Icons.Rounded.Language,
                    SettingsAccent.teal,
                    value = model.settings.webNavigationAction.label,
                    showDivider = false,
                ) { open(SettingsPage.TOOLBAR_ON_WEBSITES) }
            }
        }
        item { SectionLabel("Action Button") }
        item {
            SettingsModuleCard {
                ToolbarAction.entries.forEachIndexed { index, option ->
                    CheckModuleRow(
                        option.label,
                        model.settings.toolbarAction == option,
                        showDivider = true,
                    ) {
                        model.preferences.update { it.copy(toolbarAction = option) }
                    }
                }
                ToggleModuleRow(
                    "Open Menu on Long Press",
                    Icons.Rounded.Tune,
                    SettingsAccent.indigo,
                    model.settings.openActionMenuOnLongPress,
                    showDivider = false,
                ) { value ->
                    model.preferences.update { it.copy(openActionMenuOnLongPress = value) }
                }
            }
        }
        item {
            ExplanatoryCard("When off, tap opens the menu and a long-press runs the selected action. When on, tap runs the action and a long-press opens the menu.")
        }
        item {
            SettingsModuleCard {
                SettingsModuleRow(
                    "Auto Refresh",
                    Icons.Rounded.Refresh,
                    SettingsAccent.orange,
                    showDivider = false,
                ) { open(SettingsPage.TOOLBAR_AUTO_REFRESH) }
            }
        }
    }
}

@Composable
private fun ToolbarStartPageSettings(model: BrowserViewModel) {
    SettingsScroll {
        item {
            SettingsModuleCard {
                StartNavigationAction.entries.forEachIndexed { index, option ->
                    CheckModuleRow(
                        option.label,
                        model.settings.startNavigationAction == option,
                        showDivider = index < StartNavigationAction.entries.lastIndex,
                    ) {
                        model.preferences.update { it.copy(startNavigationAction = option) }
                    }
                }
            }
        }
        item { ExplanatoryCard("Left button on Collection. Long-press for the other options.") }
    }
}

@Composable
private fun ToolbarOnWebsitesSettings(model: BrowserViewModel) {
    SettingsScroll {
        item {
            SettingsModuleCard {
                WebNavigationAction.entries.forEachIndexed { index, option ->
                    CheckModuleRow(
                        option.label,
                        model.settings.webNavigationAction == option,
                        showDivider = index < WebNavigationAction.entries.lastIndex,
                    ) {
                        model.preferences.update { it.copy(webNavigationAction = option) }
                    }
                }
            }
        }
        item { ExplanatoryCard("Left button while browsing. Long-press for the other options.") }
    }
}

private fun autoRefreshIntervalLabel(seconds: Int): String = when {
    seconds <= 0 -> "Off"
    seconds % 3600 == 0 -> {
        val hours = seconds / 3600
        if (hours == 1) "1 hour" else "$hours hours"
    }
    seconds % 60 == 0 -> {
        val minutes = seconds / 60
        if (minutes == 1) "1 minute" else "$minutes minutes"
    }
    seconds == 1 -> "1 second"
    else -> "$seconds seconds"
}

@Composable
private fun ToolbarAutoRefreshSettings(model: BrowserViewModel, open: (SettingsPage) -> Unit) {
    val intervals = model.settings.autoRefreshIntervals
    SettingsScroll {
        item {
            SettingsModuleCard {
                intervals.forEachIndexed { index, seconds ->
                    SettingsModuleRow(
                        autoRefreshIntervalLabel(seconds),
                        Icons.Rounded.Timer,
                        SettingsAccent.orange,
                        showChevron = false,
                        showDivider = true,
                    ) {
                        model.preferences.update {
                            it.copy(autoRefreshIntervals = it.autoRefreshIntervals.filterNot { s -> s == seconds })
                        }
                    }
                }
                SettingsModuleRow(
                    "Add Interval",
                    Icons.Rounded.Add,
                    SettingsAccent.blue,
                    showDivider = false,
                ) { open(SettingsPage.TOOLBAR_ADD_REFRESH) }
            }
        }
        item { ExplanatoryCard("These intervals appear when you long-press Reload. Tap an interval here to remove it.") }
    }
}

@Composable
private fun ToolbarAddRefreshInterval(model: BrowserViewModel, dismiss: () -> Unit) {
    var amount by remember { mutableIntStateOf(15) }
    var unit by remember { mutableStateOf("Seconds") }
    val maxAmount = when (unit) {
        "Minutes" -> 120
        "Hours" -> 24
        else -> 120
    }
    val resolved = when (unit) {
        "Minutes" -> amount * 60
        "Hours" -> amount * 3600
        else -> amount
    }
    SettingsScroll {
        item {
            SettingsModuleCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Amount", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { amount = (amount - 1).coerceAtLeast(1) }) {
                            Icon(Icons.Rounded.Remove, "Less")
                        }
                        Text("$amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { amount = (amount + 1).coerceAtMost(maxAmount) }) {
                            Icon(Icons.Rounded.Add, "More")
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                listOf("Seconds", "Minutes", "Hours").forEachIndexed { index, option ->
                    CheckModuleRow(
                        option,
                        unit == option,
                        showDivider = index < 2,
                    ) {
                        unit = option
                        amount = amount.coerceIn(1, when (option) {
                            "Minutes" -> 120
                            "Hours" -> 24
                            else -> 120
                        })
                    }
                }
            }
        }
        item { ExplanatoryCard(autoRefreshIntervalLabel(resolved)) }
        item {
            SettingsModuleCard {
                SettingsModuleRow(
                    "Add",
                    Icons.Rounded.Check,
                    SettingsAccent.green,
                    showChevron = false,
                    showDivider = false,
                ) {
                    val clipped = resolved.coerceIn(1, 24 * 60 * 60)
                    model.preferences.update { settings ->
                        if (settings.autoRefreshIntervals.contains(clipped)) settings
                        else settings.copy(autoRefreshIntervals = (settings.autoRefreshIntervals + clipped).sorted())
                    }
                    dismiss()
                }
            }
        }
    }
}

@Composable
private fun ShortcutEditor(model: BrowserViewModel, page: SettingsPage) {
    val context = LocalContext.current
    val kind = if (page == SettingsPage.CONTROL_CENTER) LaunchShortcutKind.CONTROL else LaunchShortcutKind.WIDGET
    val listed = model.settings.shortcuts.filter { it.kind == kind }
    var add by remember { mutableStateOf(false) }
    SettingsScroll {
        item {
            ExplanatoryCard(
                when (kind) {
                    LaunchShortcutKind.WIDGET ->
                        "On the Home Screen: long-press → Widgets → Bookmer, then pick which shortcut to show. Resize from 1×1 up. A placed widget stays on that shortcut even if you add more here."
                    LaunchShortcutKind.CONTROL ->
                        "Android Control Center = Quick Settings. Pull down the status bar → edit tiles → add Bookmer / Bookmer 2. Long-press a tile to pick which control it opens."
                },
            )
        }
        item {
            SettingsModuleCard {
                SettingsModuleRow(
                    if (kind == LaunchShortcutKind.CONTROL) "New Control" else "New Widget",
                    Icons.Rounded.Add,
                    SettingsAccent.green,
                    showChevron = false,
                    showDivider = listed.isNotEmpty(),
                ) { add = true }
                listed.forEachIndexed { index, shortcut ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    runCatching { Color(shortcut.color.toColorInt()) }
                                        .getOrDefault(SettingsAccent.indigo),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Link, null, Modifier.size(20.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shortcut.displayName, fontWeight = FontWeight.Medium)
                            Text(
                                shortcut.url,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = {
                            model.preferences.update { it.copy(shortcuts = it.shortcuts - shortcut) }
                            ShortcutPublisher.publish(context)
                        }) { Icon(Icons.Rounded.Delete, "Delete") }
                    }
                    if (index < listed.lastIndex) {
                        HorizontalDivider(
                            Modifier.padding(start = 70.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }
        }
    }
    if (add) {
        AddShortcutDialog(
            kind = kind,
            dismiss = { add = false },
        ) { shortcut ->
            model.preferences.update { it.copy(shortcuts = it.shortcuts + shortcut) }
            ShortcutPublisher.publish(context)
            add = false
        }
    }
}

@Composable
private fun ToggleModuleRow(
    title: String,
    icon: ImageVector,
    accent: Color,
    checked: Boolean,
    showDivider: Boolean = true,
    change: (Boolean) -> Unit,
) {
    SettingsModuleRow(
        title = title,
        icon = icon,
        accent = accent,
        showDivider = showDivider,
        trailing = { Switch(checked, change) },
    )
}

@Composable
private fun CheckModuleRow(title: String, selected: Boolean, showDivider: Boolean = true, action: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = action)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (selected) Icon(Icons.Rounded.Check, null, tint = SettingsAccent.blue)
        }
        if (showDivider) {
            HorizontalDivider(
                Modifier.padding(start = 18.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            )
        }
    }
}

@Composable
private fun ExplanatoryCard(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun AddBlockedSiteDialog(dismiss: () -> Unit, save: (BlockedSite) -> Unit) {
    var host by remember { mutableStateOf("") }
    var redirect by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Block Website") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    host,
                    { host = it },
                    label = { Text("URL") },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                )
                OutlinedTextField(redirect, { redirect = it }, label = { Text("Redirect URL (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (host.isNotBlank()) {
                    save(BlockedSite(host.trim().removePrefix("www."), redirect.trim().takeIf { it.isNotEmpty() }))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddSearchEngineDialog(dismiss: () -> Unit, save: (CustomSearchEngine) -> Unit) {
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Add Search Engine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(template, { template = it }, label = { Text("URL containing @@@") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && template.contains("@@@")) {
                    save(CustomSearchEngine(name = name.trim(), template = template.trim()))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddShortcutDialog(
    kind: LaunchShortcutKind,
    dismiss: () -> Unit,
    save: (LaunchShortcut) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var immersive by remember { mutableStateOf(false) }
    val palette = listOf(
        "#111112", "#2F6BFF", "#0F9C8A", "#E5484D", "#F5A524",
        "#7C5CFC", "#5B6CFF", "#30A46C", "#636366", "#111827",
    )
    var color by remember {
        mutableStateOf(if (kind == LaunchShortcutKind.CONTROL) "#636366" else "#111112")
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Text(if (kind == LaunchShortcutKind.CONTROL) "New Control" else "New Widget")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("URL") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(immersive, { immersive = it })
                    Text("Full Screen")
                }
                if (kind == LaunchShortcutKind.WIDGET) {
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex.toColorInt()))
                                    .clickable { color = hex },
                            ) {
                                if (color == hex) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        Modifier.align(Alignment.Center).size(16.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (url.isNotBlank()) {
                    save(
                        LaunchShortcut(
                            name = name.trim(),
                            url = url.trim(),
                            color = if (kind == LaunchShortcutKind.CONTROL) "#636366" else color,
                            immersive = immersive,
                            kind = kind,
                        ),
                    )
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}
