package com.bookmer.browser.integration

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.bookmer.browser.MainActivity
import com.bookmer.browser.R
import com.bookmer.browser.data.ControlTileBindings
import com.bookmer.browser.data.LaunchShortcut
import com.bookmer.browser.data.LaunchShortcutKind
import com.bookmer.browser.data.LaunchShortcutStore
import com.bookmer.browser.data.WidgetShortcutBindings
import com.bookmer.browser.ui.theme.BookmerBrowserTheme

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = intent.type
            putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT))
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        finish()
    }
}

/** Publishes home widgets + Quick Settings tiles after shortcut library changes. */
object ShortcutPublisher {
    fun publish(context: Context, shortcuts: List<LaunchShortcut> = LaunchShortcutStore.all(context)) {
        // Clear legacy long-press app shortcuts — Control Center uses QS tiles instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            runCatching {
                context.getSystemService(android.content.pm.ShortcutManager::class.java)
                    ?.removeAllDynamicShortcuts()
            }
        }
        BookmerWidgetProvider.refresh(context)
        BookmerControlTileService.requestListening(context)
    }
}

object BookmerDeepLinks {
    fun openIntent(context: Context, shortcut: LaunchShortcut): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(
                "bookmer://open?url=${Uri.encode(shortcut.url)}&immersive=${shortcut.immersive}",
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun collectionIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("bookmer://collection")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
}

class BookmerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetShortcutBindings.remove(context, it) }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, BookmerWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { update(context, manager, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val shortcut = resolveShortcut(context, id)
            val options = manager.getAppWidgetOptions(id)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
            val compact = minW < 90 || minH < 55
            val layout = if (compact) R.layout.bookmer_widget_1x1 else R.layout.bookmer_widget
            val views = RemoteViews(context.packageName, layout)
            val title = shortcut?.displayName ?: context.getString(R.string.app_name)
            if (!compact) {
                views.setTextViewText(R.id.widget_title, title)
            }
            views.setContentDescription(R.id.widget_root, title)
            applyWidgetTint(views, shortcut?.color)
            val intent = if (shortcut == null) BookmerDeepLinks.collectionIntent(context)
            else BookmerDeepLinks.openIntent(context, shortcut)
            val pending = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }

        private fun resolveShortcut(context: Context, appWidgetId: Int): LaunchShortcut? {
            val bound = WidgetShortcutBindings.get(context, appWidgetId)
            val fromBind = LaunchShortcutStore.byId(context, bound)
                ?.takeIf { it.kind == LaunchShortcutKind.WIDGET }
            if (fromBind != null) return fromBind
            return LaunchShortcutStore.ofKind(context, LaunchShortcutKind.WIDGET).firstOrNull()
        }

        private fun applyWidgetTint(views: RemoteViews, colorHex: String?) {
            val color = runCatching {
                (colorHex ?: "#111112").toColorInt()
            }.getOrDefault(Color.parseColor("#111112"))
            // Keep readable plate: darken slightly via alpha overlay on solid.
            views.setInt(R.id.widget_root, "setBackgroundColor", color)
        }
    }
}

/**
 * Home-screen widget configure: pick which Widgets-shortcut this instance opens.
 */
class BookmerWidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_CANCELED)
        setContent {
            BookmerBrowserTheme {
                ShortcutPickerScreen(
                    title = "Choose Widget",
                    emptyHint = "Create a widget shortcut in Bookmer Settings → Widgets first.",
                    shortcuts = LaunchShortcutStore.ofKind(this, LaunchShortcutKind.WIDGET),
                    onCancel = { finish() },
                    onPick = { shortcut ->
                        WidgetShortcutBindings.set(this, appWidgetId, shortcut.id)
                        val manager = AppWidgetManager.getInstance(this)
                        BookmerWidgetProvider.update(this, manager, appWidgetId)
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

/** Quick Settings tile — Android analogue of iOS Control Center controls. */
open class BookmerControlTileService : TileService() {
    protected open val slot: Int = 0

    override fun onStartListening() {
        refreshTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val shortcut = ControlTileBindings.resolve(this, slot)
        val intent = if (shortcut == null) BookmerDeepLinks.collectionIntent(this)
        else BookmerDeepLinks.openIntent(this, shortcut)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(
                this,
                slot + 100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val shortcut = ControlTileBindings.resolve(this, slot)
        tile.label = shortcut?.displayName ?: getString(R.string.control_tile_default_label)
        tile.contentDescription = tile.label
        tile.state = if (shortcut != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = shortcut?.let { Uri.parse(it.url).host?.removePrefix("www.") } ?: getString(R.string.control_tile_empty_subtitle)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_bookmer)
        tile.updateTile()
    }

    companion object {
        fun requestListening(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            listOf(
                BookmerControlTileService::class.java,
                BookmerControlTileServiceB::class.java,
            ).forEach { cls ->
                runCatching {
                    requestListeningState(context, ComponentName(context, cls))
                }
            }
        }
    }
}

/** Second Control Center slot so two controls can sit in Quick Settings. */
class BookmerControlTileServiceB : BookmerControlTileService() {
    override val slot: Int = 1
}

class BookmerControlTileConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val component = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        }
        val slot = when {
            intent.getIntExtra(EXTRA_SLOT, -1) >= 0 -> intent.getIntExtra(EXTRA_SLOT, 0)
            component?.className?.endsWith("B") == true -> 1
            else -> 0
        }
        setContent {
            BookmerBrowserTheme {
                ShortcutPickerScreen(
                    title = "Choose Control",
                    emptyHint = "Create a control in Bookmer Settings → Control Center first.",
                    shortcuts = LaunchShortcutStore.ofKind(this, LaunchShortcutKind.CONTROL),
                    onCancel = { finish() },
                    onPick = { shortcut ->
                        ControlTileBindings.set(this, slot, shortcut.id)
                        BookmerControlTileService.requestListening(this)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_SLOT = "slot"
    }
}

@Composable
private fun ShortcutPickerScreen(
    title: String,
    emptyHint: String,
    shortcuts: List<LaunchShortcut>,
    onCancel: () -> Unit,
    onPick: (LaunchShortcut) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (shortcuts.isEmpty()) {
                Text(emptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onCancel) { Text("Close") }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shortcuts, key = { it.id }) { shortcut ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .clickable { onPick(shortcut) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        runCatching {
                                            ComposeColor(shortcut.color.toColorInt())
                                        }.getOrDefault(ComposeColor(0xFF111112)),
                                    ),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(shortcut.displayName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(shortcut.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}
