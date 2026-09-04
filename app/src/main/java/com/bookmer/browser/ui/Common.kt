package com.bookmer.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bookmer.browser.BookmerServices
import com.bookmer.browser.data.BookmerIconUrl
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

private object ImageLoader {
    private val cache = ConcurrentHashMap<String, Bitmap>()
    private val executor = Executors.newFixedThreadPool(4)
    private val main = Handler(Looper.getMainLooper())

    fun load(context: Context, urls: List<String>, token: String?, callback: (Bitmap?) -> Unit) {
        val resolved = urls.mapNotNull { raw ->
            when {
                raw.startsWith("content://") || raw.startsWith("file://") ||
                    raw.startsWith("bookmer-custom:") -> raw
                else -> BookmerIconUrl.resolveAny(raw) ?: raw.takeIf {
                    it.startsWith("http") || it.startsWith("content:") || it.startsWith("file:")
                }
            }
        }.distinct()
        if (resolved.isEmpty()) {
            callback(null)
            return
        }
        resolved.firstOrNull { cache.containsKey(cacheKey(it, token)) }?.let { hit ->
            callback(cache[cacheKey(hit, token)])
            return
        }
        executor.execute {
            var bitmap: Bitmap? = null
            for (url in resolved) {
                bitmap = fetch(context, url, token)
                if (bitmap != null) {
                    cache[cacheKey(url, token)] = bitmap
                    break
                }
            }
            main.post { callback(bitmap) }
        }
    }

    private fun cacheKey(url: String, token: String?): String =
        if (BookmerIconUrl.needsAuthentication(url) && !token.isNullOrBlank()) "$url#auth" else url

    private fun fetch(context: Context, url: String, token: String?): Bitmap? = runCatching {
        when {
            url.startsWith("content://") || url.startsWith("file://") ->
                context.contentResolver.openInputStream(android.net.Uri.parse(url))
                    ?.use(BitmapFactory::decodeStream)
            url.startsWith("bookmer-custom:") -> null
            else -> {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 7_000
                connection.readTimeout = 12_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "image/avif,image/webp,image/png,image/jpeg,image/svg+xml,*/*;q=0.8")
                connection.setRequestProperty("User-Agent", "Bookmer Browser Android")
                if (BookmerIconUrl.needsAuthentication(url) && !token.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Cookie", "_act=$token")
                }
                val status = connection.responseCode
                if (status !in 200..299) {
                    connection.disconnect()
                    return@runCatching null
                }
                val mime = connection.contentType.orEmpty().lowercase()
                val bytes = connection.inputStream.use { it.readBytes() }
                connection.disconnect()
                decodeImage(bytes, mime)
            }
        }
    }.getOrNull()

    private fun decodeImage(bytes: ByteArray, mime: String): Bitmap? {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return it }
        val head = bytes.copyOfRange(0, minOf(bytes.size, 200)).toString(Charsets.UTF_8).lowercase()
        val isSvg = mime.contains("svg") || head.contains("<svg")
        if (!isSvg) return null
        val svg = SVG.getFromInputStream(ByteArrayInputStream(bytes))
        val picture = svg.renderToPicture(128, 128)
        val bitmap = Bitmap.createBitmap(
            maxOf(picture.width, 1),
            maxOf(picture.height, 1),
            Bitmap.Config.ARGB_8888,
        )
        Canvas(bitmap).drawPicture(picture)
        return bitmap
    }
}

@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    background: Color = Color.Transparent,
) {
    RemoteImage(urls = listOfNotNull(url), modifier, contentScale, background)
}

@Composable
fun RemoteImage(
    urls: List<String?>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    background: Color = Color.Transparent,
) {
    val candidates = remember(urls) { urls.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) } }
    var loaded by remember(candidates) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val token = runCatching { BookmerServices.session.value.token }.getOrNull()
    LaunchedEffect(candidates, token) {
        loaded = null
        if (candidates.isNotEmpty()) {
            ImageLoader.load(context.applicationContext, candidates, token) { loaded = it }
        }
    }
    Box(modifier.background(background)) {
        loaded?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

fun Color.Companion.fromHex(value: String?, fallback: Color = Color.LightGray): Color = runCatching {
    Color(android.graphics.Color.parseColor(value))
}.getOrDefault(fallback)
