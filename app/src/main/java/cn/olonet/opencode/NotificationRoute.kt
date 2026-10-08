package cn.olonet.opencode

import java.net.URI

internal object NotificationRoute {
    fun validate(value: String): String? {
        if (value.length > 4096 || value.contains('\\') || value.any { it.code < 32 }) return null
        val uri = try { URI(value) } catch (_: Exception) { return null }
        return value.takeIf {
            uri.scheme == "http" && uri.host == "localhost" && uri.port == 8080 && uri.rawUserInfo == null
        }
    }
}
