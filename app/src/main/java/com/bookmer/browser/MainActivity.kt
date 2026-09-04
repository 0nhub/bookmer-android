package com.bookmer.browser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bookmer.browser.browser.BrowserFileChooserHost
import com.bookmer.browser.browser.BrowserPermissionHost
import com.bookmer.browser.browser.BrowserViewModel
import com.bookmer.browser.browser.PendingCollect
import com.bookmer.browser.data.SitePermissionKind
import com.bookmer.browser.data.SitePermissionPolicy
import com.bookmer.browser.data.SitePermissionStore
import com.bookmer.browser.ui.BookmerApp

class MainActivity : ComponentActivity(), BrowserPermissionHost, BrowserFileChooserHost {
    private val browserModel: BrowserViewModel by viewModels()
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var webPermissionRequest: PermissionRequest? = null
    private var geolocationRequest: Pair<String, GeolocationPermissions.Callback>? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        webPermissionRequest?.let { if (grants.values.all { allowed -> allowed }) it.grant(it.resources) else it.deny() }
        webPermissionRequest = null
        geolocationRequest?.let { (origin, callback) -> callback.invoke(origin, grants.values.all { it }, false) }
        geolocationRequest = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BookmerApp(browserModel) }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val shared = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (intent?.action == Intent.ACTION_SEND && !shared.isNullOrBlank()) {
            val url = Regex("https?://\\S+").find(shared)?.value?.trimEnd('.', ',', ')') ?: shared
            browserModel.pendingCollect = PendingCollect(url, shared.removeSuffix(url).trim().ifBlank { url })
            return
        }
        intent?.data?.toString()?.let { url ->
            when {
                url == "bookmer://collection" -> browserModel.openHome()
                url.startsWith("bookmer://open") -> Uri.parse(url).getQueryParameter("url")?.let {
                    browserModel.load(it, immersive = Uri.parse(url).getBooleanQueryParameter("immersive", false))
                }
                url.startsWith("http") -> browserModel.load(url)
            }
        }
    }

    override fun handleWebPermission(request: PermissionRequest) {
        val host = SitePermissionStore.hostFromUrl(request.origin.toString())
        val cameraNeeded = PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources
        val micNeeded = PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources
        val cameraPolicy = if (cameraNeeded) BookmerServices.sitePermissions.policy(SitePermissionKind.CAMERA, host) else SitePermissionPolicy.ALLOW
        val micPolicy = if (micNeeded) BookmerServices.sitePermissions.policy(SitePermissionKind.MICROPHONE, host) else SitePermissionPolicy.ALLOW
        if (cameraPolicy == SitePermissionPolicy.DENY || micPolicy == SitePermissionPolicy.DENY) {
            request.deny(); return
        }
        if (cameraPolicy == SitePermissionPolicy.ALLOW && micPolicy == SitePermissionPolicy.ALLOW) {
            val permissions = buildList {
                if (cameraNeeded) add(Manifest.permission.CAMERA)
                if (micNeeded) add(Manifest.permission.RECORD_AUDIO)
            }
            if (permissions.isEmpty() || permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
                request.grant(request.resources)
            } else {
                webPermissionRequest = request
                permissionLauncher.launch(permissions.toTypedArray())
            }
            return
        }
        // Ask — prompt OS permission if needed, otherwise grant after user-facing OS dialog.
        val permissions = buildList {
            if (cameraNeeded) add(Manifest.permission.CAMERA)
            if (micNeeded) add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) request.grant(request.resources)
        else { webPermissionRequest = request; permissionLauncher.launch(permissions.toTypedArray()) }
    }

    override fun handleGeolocation(origin: String, callback: GeolocationPermissions.Callback) {
        val host = SitePermissionStore.hostFromUrl(origin)
        when (BookmerServices.sitePermissions.policy(SitePermissionKind.LOCATION, host)) {
            SitePermissionPolicy.DENY -> { callback.invoke(origin, false, false); return }
            SitePermissionPolicy.ALLOW, SitePermissionPolicy.ASK -> Unit
        }
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) callback.invoke(origin, true, false)
        else { geolocationRequest = origin to callback; permissionLauncher.launch(arrayOf(permission)) }
    }

    override fun openWebFileChooser(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean {
        fileCallback?.onReceiveValue(null)
        fileCallback = callback
        return runCatching { filePicker.launch(params.createIntent()); true }.getOrElse { fileCallback = null; false }
    }
}
