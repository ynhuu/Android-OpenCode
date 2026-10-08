package cn.olonet.opencode

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.util.UUID

/** Origin-scoped, main-frame-only bridge. Never expose an addJavascriptInterface. */
class NotificationBridge(private val activity: MainActivity, private val web: WebView) {
    private val manager = activity.getSystemService(NotificationManager::class.java)
    private val preferences = activity.getSharedPreferences("notifications", 0)
    private val document = UUID.randomUUID().toString()
    private val permissionReplies = mutableListOf<(String) -> Unit>()
    private var requestingPermission = false
    private var destroyed = false
    private var foreground = false

    init {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "OpenCode 通知", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun install(): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false
        val origins = setOf("http://localhost:8080")
        WebViewCompat.addWebMessageListener(web, "OpenCodeNotifications", origins) { _, message, origin, mainFrame, reply ->
            if (destroyed || !mainFrame || !LocalContent.owns(origin)) return@addWebMessageListener
            val raw = message.data ?: return@addWebMessageListener
            if (raw.length > 16384) return@addWebMessageListener
            val data = try { JSONObject(raw) } catch (_: Exception) { return@addWebMessageListener }
            val id = data.optString("id").take(128)
            fun respond(value: String, result: String = "ok") = reply.postMessage(JSONObject()
                .put("id", id).put("permission", value).put("result", result).toString())
            when (data.optString("type")) {
                "status" -> respond(permission())
                "permission" -> requestPermission { respond(it) }
                "show" -> respond(permission(), show(data, id))
                "close" -> manager.cancel(notificationTag(id), 0)
            }
        }
        WebViewCompat.addDocumentStartJavaScript(web, activity.resources.openRawResource(R.raw.notification_bridge)
            .bufferedReader().use { it.readText() }, origins)
        return true
    }

    private fun permission(): String {
        if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return if (preferences.getBoolean("asked", false)) "denied" else "default"
        }
        return if (manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE) "granted" else "denied"
    }

    fun permissionResult() {
        if (destroyed) return
        requestingPermission = false
        val value = permission()
        val replies = permissionReplies.toList()
        permissionReplies.clear()
        // A page may have navigated away while the system permission dialog was open.
        replies.forEach { reply -> runCatching { reply(value) } }
        refresh()
        if (value == "denied") android.widget.Toast.makeText(activity, "通知未获允许，可在系统设置 → 应用 → OpenCode → 通知中开启", android.widget.Toast.LENGTH_LONG).show()
    }

    fun requestPermission(reply: ((String) -> Unit)? = null) {
        if (destroyed) return
        if (requestingPermission) {
            reply?.let { permissionReplies.add(it) }
            return
        }
        val value = permission()
        if (!foreground || Build.VERSION.SDK_INT < 33 || value != "default") {
            reply?.invoke(value)
            return
        }
        reply?.let { permissionReplies.add(it) }
        requestingPermission = true
        preferences.edit().putBoolean("asked", true).apply()
        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_PERMISSION)
    }

    fun refresh() {
        if (!hasLocalPage()) return
        val state = JSONObject().put("permission", permission())
        web.evaluateJavascript("window.__openCodeNotificationState?.($state)", null)
    }

    fun foreground(value: Boolean) {
        foreground = value
        refresh()
    }

    fun destroy() {
        destroyed = true
        permissionReplies.clear()
    }

    private fun hasLocalPage() = !destroyed && web.url?.let { LocalContent.owns(Uri.parse(it)) } == true

    private fun notificationTag(id: String) = "$document:$id"

    private fun show(data: JSONObject, id: String): String {
        if (permission() != "granted") return "blocked-permission"
        if (foreground) return "blocked-foreground"
        if (id.isEmpty()) return "invalid-id"
        val route = NotificationRoute.validate(data.optString("url")) ?: LocalContent.HOME
        val intent = Intent(activity, MainActivity::class.java).apply {
            action = ACTION_OPEN
            this.data = Uri.parse("opencode-notification://open/$document/$id")
            putExtra(EXTRA_ROUTE, route)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_DOCUMENT, document)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val body = data.optString("body").take(4000)
        val notification = Notification.Builder(activity, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(data.optString("title").take(200))
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pending).setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build()
        return try {
            manager.notify(notificationTag(id), 0, notification)
            "posted"
        } catch (_: SecurityException) {
            refresh()
            "blocked-permission"
        }
    }

    fun open(intent: Intent): Boolean {
        if (destroyed || intent.action != ACTION_OPEN) return false
        val route = NotificationRoute.validate(intent.getStringExtra(EXTRA_ROUTE) ?: "") ?: return false
        val id = intent.getStringExtra(EXTRA_ID) ?: ""
        if (intent.getStringExtra(EXTRA_DOCUMENT) == document && hasLocalPage()) {
            val script = "window.__openCodeNotificationClick?.(${JSONObject.quote(id)}) === true"
            web.evaluateJavascript(script) { handled -> if (!destroyed && handled != "true") web.loadUrl(route) }
        } else web.loadUrl(route)
        return true
    }

    companion object {
        const val REQUEST_PERMISSION = 2
        private const val CHANNEL = "opencode_events"
        private const val ACTION_OPEN = "cn.olonet.opencode.OPEN_NOTIFICATION"
        private const val EXTRA_ROUTE = "notification_route"
        private const val EXTRA_ID = "notification_id"
        private const val EXTRA_DOCUMENT = "notification_document"
    }
}
