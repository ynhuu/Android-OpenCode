package cn.olonet.opencode

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class LocalContent(private val store: FrontendStore) {
    companion object {
        const val HOME = "http://localhost:8080/"
        fun owns(uri: Uri) = uri.scheme == "http" && uri.host == "localhost" && uri.port == 8080
    }

    fun respond(uri: Uri, method: String, navigation: Boolean): WebResourceResponse? {
        if (!owns(uri)) return null
        val path = uri.path ?: return missing()
        if (method != "GET" || path.contains('\\') || path.contains('\u0000')
            || path.split('/').any { it == ".." || it == "." } || path == "/sw.js") return missing()
        val active = store.active ?: return missing()
        fun open(name: String) = try {
            File(active, name).inputStream()
        } catch (_: IOException) { null }
        var name = if (path == "/") "index.html" else path.removePrefix("/")
        var stream = open(name)
        if (stream == null && navigation && !path.startsWith("/_assets/") && !path.startsWith("/assets/")
            && !path.startsWith("/api/") && !path.startsWith("/auth/") && !name.contains('.')) {
            name = "index.html"
            stream = open(name)
        }
        if (stream == null) return missing()
        val mime = when (name.substringAfterLast('.', "")) {
            "html" -> "text/html"
            "js" -> "text/javascript"
            "css" -> "text/css"
            "json", "webmanifest" -> "application/json"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "ico" -> "image/x-icon"
            "woff2" -> "font/woff2"
            "woff" -> "font/woff"
            "ttf" -> "font/ttf"
            "wasm" -> "application/wasm"
            "aac" -> "audio/aac"
            else -> "application/octet-stream"
        }
        return WebResourceResponse(mime, null, 200, "OK", mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff"), stream)
    }

    private fun missing() = WebResourceResponse("text/plain", "UTF-8", 404, "Not Found",
        mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(byteArrayOf()))
}
