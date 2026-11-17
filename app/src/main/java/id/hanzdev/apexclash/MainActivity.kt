package id.hanzdev.apexclash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

/**
 * Apex Clash - game fighting Mecha vs Demi-Human.
 * Game web (HTML/JS) dimuat dari folder assets/www lewat WebView, sepenuhnya offline.
 *
 * Developer : Hanz Dev Official
 * Kontak    : hanzzcodee@gmail.com
 * Dukung    : https://saweria.co/hanzreally
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var lastBackPress = 0L

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#101923"))
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isLongClickable = false
            setOnLongClickListener { true }
            isHapticFeedbackEnabled = false
        }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.parseColor("#101923"))
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
        )

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.addJavascriptInterface(Bridge(), "AndroidApp")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = serveAsset(request)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                if (request.url.host == HOST) return false
                openExternal(request.url.toString())
                return true
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                recreate()
                return true
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl("https://$HOST/index.html")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
        enterImmersive()
    }

    override fun onPause() {
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersive()
    }

    override fun onDestroy() {
        (webView.parent as? FrameLayout)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    /**
     * Tombol Back = tombol Esc di game (jeda / kembali satu layar menu).
     * Tekan Back dua kali dengan cepat untuk keluar dari aplikasi.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackPress < 700) {
            finish()
            return
        }
        lastBackPress = now
        val js = "(function(){var t=document.querySelector('iframe')&&document.querySelector('iframe').contentWindow||window;" +
            "var e=function(w){w.document.dispatchEvent(new w.KeyboardEvent('keydown',{key:'Escape',code:'Escape',bubbles:true}));};" +
            "try{e(t);}catch(x){e(window);}})();"
        webView.evaluateJavascript(js, null)
        Toast.makeText(this, "Tekan Back 2x cepat untuk keluar", Toast.LENGTH_SHORT).show()
    }

    private fun enterImmersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun openExternal(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" && uri.scheme != "mailto") return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka tautan ini", Toast.LENGTH_SHORT).show()
        }
    }

    /** Dipanggil dari JavaScript (menu About): window.AndroidApp.openUrl(url). */
    private inner class Bridge {
        @JavascriptInterface
        fun openUrl(url: String) {
            runOnUiThread { openExternal(url) }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Server aset lokal: https://appassets.androidplatform.net/<path> -> assets/www/<path>
    // Mendukung header Range (206) supaya musik dan video bisa di-seek / di-loop dengan benar.
    // ---------------------------------------------------------------------------------------

    private fun serveAsset(request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        if (uri.host != HOST) return null

        var path = Uri.decode(uri.path ?: "/").trimStart('/')
        if (path.isEmpty()) path = "index.html"
        if (path.contains("..")) return plain(404, "Not Found")

        val assetPath = "www/$path"
        val mime = mimeOf(path)
        val encoding = if (mime.startsWith("text/") || mime.contains("javascript") || mime.contains("json")) "UTF-8" else null
        val headers = hashMapOf(
            "Access-Control-Allow-Origin" to "*",
            "Accept-Ranges" to "bytes",
            "Cache-Control" to "no-cache"
        )

        return try {
            val total = assetLength(assetPath)
            val rangeHeader = request.requestHeaders?.entries
                ?.firstOrNull { it.key.equals("Range", ignoreCase = true) }?.value

            if (rangeHeader != null && total > 0) {
                val m = Regex("bytes=(\\d*)-(\\d*)").find(rangeHeader)
                val startRaw = m?.groupValues?.get(1).orEmpty()
                val endRaw = m?.groupValues?.get(2).orEmpty()
                var start: Long
                var end: Long
                if (startRaw.isEmpty() && endRaw.isNotEmpty()) {
                    start = (total - endRaw.toLong()).coerceAtLeast(0)
                    end = total - 1
                } else {
                    start = startRaw.toLongOrNull() ?: 0L
                    end = if (endRaw.isEmpty()) total - 1 else minOf(endRaw.toLong(), total - 1)
                }
                if (start >= total || start > end) {
                    headers["Content-Range"] = "bytes */$total"
                    return WebResourceResponse(mime, encoding, 416, "Range Not Satisfiable", headers, ByteArrayInputStream(ByteArray(0)))
                }
                val stream = assets.open(assetPath, AssetManager.ACCESS_STREAMING)
                skipFully(stream, start)
                val length = end - start + 1
                headers["Content-Range"] = "bytes $start-$end/$total"
                headers["Content-Length"] = length.toString()
                WebResourceResponse(mime, encoding, 206, "Partial Content", headers, BoundedInputStream(stream, length))
            } else {
                if (total > 0) headers["Content-Length"] = total.toString()
                WebResourceResponse(mime, encoding, 200, "OK", headers, assets.open(assetPath, AssetManager.ACCESS_STREAMING))
            }
        } catch (_: IOException) {
            plain(404, "Not Found")
        }
    }

    private fun plain(code: Int, reason: String) = WebResourceResponse(
        "text/plain", "UTF-8", code, reason,
        mapOf("Access-Control-Allow-Origin" to "*"),
        ByteArrayInputStream(ByteArray(0))
    )

    private fun assetLength(assetPath: String): Long {
        try {
            assets.openFd(assetPath).use { return it.declaredLength }
        } catch (_: IOException) {
            // aset terkompresi: hitung manual
        }
        var count = 0L
        assets.open(assetPath).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                count += n
            }
        }
        return count
    }

    private fun skipFully(stream: InputStream, bytes: Long) {
        var left = bytes
        while (left > 0) {
            val skipped = stream.skip(left)
            if (skipped > 0) {
                left -= skipped
            } else {
                if (stream.read() < 0) break
                left--
            }
        }
    }

    private fun mimeOf(path: String): String =
        when (path.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "html", "htm" -> "text/html"
            "js", "mjs" -> "application/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "ttf" -> "font/ttf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }

    private class BoundedInputStream(input: InputStream, private var remaining: Long) : FilterInputStream(input) {
        override fun read(): Int {
            if (remaining <= 0) return -1
            val b = super.read()
            if (b >= 0) remaining--
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val n = super.read(b, off, minOf(len.toLong(), remaining).toInt())
            if (n > 0) remaining -= n
            return n
        }

        override fun available(): Int = minOf(super.available().toLong(), remaining).toInt()
    }

    companion object {
        private const val HOST = "appassets.androidplatform.net"
    }
}
