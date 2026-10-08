package cn.olonet.opencode

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class FrontendRelease(val code: Int, val label: String, val zip: String, val sha256: String, val notes: String)
data class UpdateProgress(val message: String, val percent: Int? = null)

/** All downloaded files stay in private app storage, never executable native code. */
class FrontendStore(context: Context) {
    private val preferences = context.getSharedPreferences("frontend", Context.MODE_PRIVATE)
    private val directory = File(context.filesDir, "frontends").apply { mkdirs() }
    @Volatile var active: File? = restore()
        private set

    companion object {
        const val MANIFEST_URL = "https://github.com/ynhuu/Android-OpenCode/releases/latest/download/manifest.json"
        const val ZIP_URL = "https://github.com/ynhuu/Android-OpenCode/releases/latest/download/dist.zip"
    }

    val version: Int get() = if (active == null) 0 else preferences.getInt("version", 0)
    val label: String get() = if (active == null) "未安装" else preferences.getString("label", "未知") ?: "未知"

    private fun restore(): File? {
        val name = preferences.getString("active", null) ?: return null
        if (!name.matches(Regex("[a-f0-9]{64}"))) return null
        return File(directory, name).takeIf { File(it, "index.html").isFile }
    }

    fun validateUrl(value: String): URL {
        val url = URL(value)
        require(url.protocol in listOf("http", "https") && url.host.isNotEmpty() && url.userInfo == null) { "更新地址必须是 HTTP(S)，且不能包含用户名或密码" }
        return url
    }

    private fun connect(value: String): HttpURLConnection {
        var url = validateUrl(value)
        repeat(6) { hop ->
            require(url.protocol == "https") { "更新下载必须使用 HTTPS" }
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept-Encoding", "identity")
            try {
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    require(hop < 5) { "更新下载重定向次数过多" }
                    val location = connection.getHeaderField("Location")
                    require(!location.isNullOrBlank()) { "更新下载重定向地址为空" }
                    url = validateUrl(URL(url, location).toExternalForm())
                    require(url.protocol == "https") { "更新下载不能重定向到非 HTTPS 地址" }
                    connection.disconnect()
                    return@repeat
                }
                require(code == 200) { "服务器返回 HTTP $code" }
                return connection
            } catch (error: Exception) {
                connection.disconnect()
                throw error
            }
        }
        error("更新下载重定向次数过多")
    }

    fun checkUpdate(): FrontendRelease? {
        val connection = connect(MANIFEST_URL)
        val text = try {
            connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 256 * 1024) { "更新清单过大" }
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }
        } finally { connection.disconnect() }
        val json = JSONObject(text)
        val code = json.getInt("versionCode")
        require(code > 0) { "版本号无效" }
        val zip = json.getString("zipUrl")
        require(zip == ZIP_URL) { "清单 ZIP 地址与固定更新地址不符" }
        validateUrl(zip)
        val sha = json.getString("sha256").lowercase()
        require(sha.matches(Regex("[a-f0-9]{64}"))) { "SHA-256 无效" }
        val release = FrontendRelease(code, json.getString("version"), zip, sha, json.optString("notes"))
        val currentHash = if (active == null) null else preferences.getString("active", null)
        if (!FrontendUpdates.needsUpdate(version, currentHash, release)) {
            // Adopt manifest metadata for a ZIP already installed by the old direct-download mode.
            if (currentHash == sha && code > version) {
                check(preferences.edit().putInt("version", code).putString("label", release.label).commit())
            }
            return null
        }
        return release
    }

    private fun download(url: String, archive: File, status: (UpdateProgress) -> Unit): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val connection = connect(url)
            try {
                val size = connection.contentLengthLong
                require(size <= 64L * 1024 * 1024) { "ZIP 超过 64 MiB" }
                connection.inputStream.use { input ->
                    archive.outputStream().use { output ->
                        val buffer = ByteArray(32768)
                        var total = 0L
                        var last = -1L
                        while (true) {
                            check(!Thread.currentThread().isInterrupted) { "更新已取消" }
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 64L * 1024 * 1024) { "ZIP 超过 64 MiB" }
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            if (total / 262144 != last) {
                                last = total / 262144
                                 status(UpdateProgress(
                                     "正在下载前端 · ${total / 1024} KiB",
                                     if (size > 0) (total * 100 / size).toInt().coerceIn(0, 100) else null,
                                 ))
                            }
                        }
                        require(size < 0 || total == size) { "下载不完整" }
                        output.fd.sync()
                    }
                }
            } finally { connection.disconnect() }
            status(UpdateProgress("正在校验文件完整性"))
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            return hash
    }

    fun install(release: FrontendRelease, status: (UpdateProgress) -> Unit) {
        val archive = File.createTempFile("download-", ".zip", directory)
        val staging = File(directory, "staging-${archive.name}").apply { mkdirs() }
        try {
            val hash = download(release.zip, archive, status)
            require(hash == release.sha256) { "ZIP 校验失败，已保留旧版本" }
            status(UpdateProgress("正在解压前端资源"))
            FrontendArchive.extract(archive, staging)
            require(File(staging, "index.html").isFile && File(staging, "_assets").isDirectory) { "ZIP 根目录必须包含 index.html 和 _assets" }
            check(!Thread.currentThread().isInterrupted) { "更新已取消" }
            val installed = File(directory, release.sha256)
            if (!installed.exists()) check(staging.renameTo(installed)) { "无法保存新版本" }
            status(UpdateProgress("正在安装新版本"))
            check(preferences.edit().putString("active", release.sha256).putInt("version", release.code)
                .putString("label", release.label).commit()) { "无法保存版本设置" }
            active = installed
            status(UpdateProgress("正在清理旧版资源"))
            // Cleanup happens only after the new active version has been committed.
            // A cleanup failure must not turn a successfully installed update into a failure.
            runCatching { FrontendResources.cleanup(directory, installed) }
                .onSuccess { complete ->
                    if (!complete) android.util.Log.w("FrontendStore", "部分旧版资源未能删除")
                }
                .onFailure { android.util.Log.w("FrontendStore", "旧版资源清理失败", it) }
        } finally {
            archive.delete()
            staging.deleteRecursively()
        }
    }

}

internal object FrontendUpdates {
    fun needsUpdate(code: Int, sha256: String?, release: FrontendRelease) =
        sha256 == null || (release.code > code && release.sha256 != sha256)
}

internal object FrontendResources {
    fun cleanup(directory: File, active: File): Boolean {
        require(active.parentFile == directory && active.name.matches(Regex("[a-f0-9]{64}")) &&
            File(active, "index.html").isFile) { "当前前端资源无效，未清理旧版本" }
        val versions = directory.listFiles() ?: return false
        return versions.filter { it.isDirectory && it != active && it.name.matches(Regex("[a-f0-9]{64}")) }
            .map { it.deleteRecursively() }.all { it }
    }
}

internal object FrontendArchive {
    fun extract(archive: File, staging: File) {
        val root = staging.canonicalPath + File.separator
        val names = HashSet<String>()
        var total = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                check(!Thread.currentThread().isInterrupted) { "更新已取消" }
                val entry = zip.nextEntry ?: break
                val name = entry.name
                require(names.size < 10000 && names.add(name)) { "ZIP 文件数量过多或路径重复" }
                require(!name.startsWith("/") && !name.contains('\\') && !name.contains('\u0000')
                    && name.split('/').none { it == ".." || it == "." }) { "ZIP 路径无效" }
                val file = File(staging, name)
                require(file.canonicalPath.startsWith(root)) { "ZIP 路径越界" }
                if (entry.isDirectory) { check(file.mkdirs() || file.isDirectory); continue }
                check(file.parentFile!!.mkdirs() || file.parentFile!!.isDirectory)
                file.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        check(!Thread.currentThread().isInterrupted) { "更新已取消" }
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 256L * 1024 * 1024) { "解压后超过 256 MiB" }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
                zip.closeEntry()
            }
        }
    }
}
