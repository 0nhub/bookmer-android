package com.bookmer.browser.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.concurrent.Executors

/**
 * Disk cache for tab switcher thumbnails — mirrors iOS [TabSessionStore] JPEG files.
 * Keeps previews available after process death and for background tabs whose WebView
 * is detached (Compose only attaches the selected tab).
 */
object TabPreviewStore {
    private val io = Executors.newSingleThreadExecutor()
    private var dir: File? = null

    fun init(context: Context) {
        // v2: page-only software captures (v1 window PixelCopy often included chrome / tab deck).
        val legacy = File(context.filesDir, "bookmer/tab-previews")
        if (legacy.exists()) runCatching { legacy.deleteRecursively() }
        dir = File(context.filesDir, "bookmer/tab-previews-v2").also { it.mkdirs() }
    }

    private fun file(tabId: String): File? = dir?.let { File(it, "$tabId.jpg") }

    fun load(tabId: String): Bitmap? {
        val f = file(tabId) ?: return null
        if (!f.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        }.getOrNull()
    }

    fun loadAll(tabIds: Collection<String>): Map<String, Bitmap> =
        tabIds.mapNotNull { id -> load(id)?.let { id to it } }.toMap()

    fun save(tabId: String, bitmap: Bitmap) {
        val f = file(tabId) ?: return
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        io.execute {
            runCatching {
                f.outputStream().use { out ->
                    copy.compress(Bitmap.CompressFormat.JPEG, 55, out)
                }
            }
            if (!copy.isRecycled) copy.recycle()
        }
    }

    fun remove(tabId: String) {
        io.execute { file(tabId)?.delete() }
    }

    fun retainOnly(liveIds: Set<String>) {
        val folder = dir ?: return
        io.execute {
            folder.listFiles()?.forEach { file ->
                if (file.extension != "jpg") return@forEach
                val id = file.nameWithoutExtension
                if (id !in liveIds) file.delete()
            }
        }
    }

    fun clear() {
        val folder = dir ?: return
        io.execute { folder.listFiles()?.forEach { it.delete() } }
    }
}
