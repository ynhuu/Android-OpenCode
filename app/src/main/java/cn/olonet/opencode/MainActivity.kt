package cn.olonet.opencode

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var root: LinearLayout
    private var restoreWebFocus = false
    private lateinit var store: FrontendStore
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var progressLabel: TextView
    private var progressDialog: AlertDialog? = null
    private lateinit var empty: TextView
    private val worker = Executors.newSingleThreadExecutor()
    private var busy = false
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var updateDialog: AlertDialog? = null
    private var notifications: NotificationBridge? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (WebView.getCurrentWebViewPackage() == null) {
            Toast.makeText(this, "请安装或更新 Android System WebView", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        store = FrontendStore(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isFocusableInTouchMode = true
            setBackgroundColor(getColor(R.color.shell_background))
        }
        web = WebView(this)
        empty = TextView(this).apply {
            text = "尚未安装前端\n\n确认安装后才会加载 OpenCode。\n点击此处重新检查更新。"
            setTextColor(getColor(R.color.shell_text))
            gravity = android.view.Gravity.CENTER
            setOnClickListener { checkUpdate() }
        }
        root.addView(empty, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            root.setOnApplyWindowInsetsListener { view, insets ->
                val padding = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
                insets
            }
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        CookieManager.getInstance().setAcceptCookie(true)
        notifications = NotificationBridge(this, web).also {
            if (!it.install()) Toast.makeText(this, "当前 WebView 不支持通知桥接，请更新 Android System WebView", Toast.LENGTH_LONG).show()
        }
        val content = LocalContent(store)
        ServiceWorkerController.getInstance().setServiceWorkerClient(object : ServiceWorkerClient() {
            override fun shouldInterceptRequest(request: WebResourceRequest) = content.respond(request.url, request.method, false)
        })
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (LocalContent.owns(Uri.parse(url))) {
                    notifications?.refresh()
                    notifications?.requestPermission()
                }
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) = content.respond(request.url, request.method, request.isForMainFrame)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame || LocalContent.owns(request.url)) return false
                if (request.url.scheme in listOf("http", "https", "mailto")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    catch (_: ActivityNotFoundException) { Toast.makeText(this@MainActivity, "没有可打开链接的应用", Toast.LENGTH_SHORT).show() }
                }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                try { startActivityForResult(params.createIntent(), 1) }
                catch (_: ActivityNotFoundException) {
                    fileCallback?.onReceiveValue(null); fileCallback = null
                    Toast.makeText(this@MainActivity, "没有可用文件选择器", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }
        if (store.active == null) {
            web.visibility = android.view.View.GONE
        } else {
            empty.visibility = android.view.View.GONE
            if (state == null || web.restoreState(state) == null) {
                if (notifications?.open(intent) != true) web.loadUrl(LocalContent.HOME)
            }
        }
        // Check only on a fresh launch, not foreground resumes or configuration recreation.
        if (state == null) checkUpdate()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::store.isInitialized && store.active != null) notifications?.open(intent)
    }

    override fun onResume() {
        super.onResume()
        notifications?.foreground(true)
        if (restoreWebFocus && ::web.isInitialized) {
            web.requestFocus()
            restoreWebFocus = false
        }
    }

    override fun onPause() {
        notifications?.foreground(false)
        if (::web.isInitialized && ::root.isInitialized) {
            restoreWebFocus = web.hasFocus()
            // WebView can retain document focus after Home; release it so the
            // unchanged frontend does not suppress background notifications.
            root.requestFocus()
        }
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NotificationBridge.REQUEST_PERMISSION) notifications?.permissionResult()
    }
    private fun ui(action: () -> Unit) = runOnUiThread { if (!isFinishing && !isDestroyed) action() }

    private fun darkTheme() = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun accentColor() = if (darkTheme()) 0xffa5b4fc.toInt() else 0xff4f46e5.toInt()

    private fun updateCard(title: String, subtitle: String): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        content.addView(TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(getColor(R.color.shell_text))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(getColor(R.color.shell_text))
            alpha = 0.6f
            setPadding(0, dp(6), 0, dp(24))
        })
        return content
    }

    private fun showCard(content: LinearLayout): AlertDialog =
        AlertDialog.Builder(this).setView(content).setCancelable(false).create().apply {
            show()
            window?.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
                setColor(getColor(R.color.shell_background))
                cornerRadius = dp(20).toFloat()
            })
            window?.setLayout(minOf(resources.displayMetrics.widthPixels - dp(48), dp(360)), -2)
        }

    private fun cardButton(label: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        minHeight = 0
        minimumHeight = 0
        setTextColor(if (primary && !darkTheme()) android.graphics.Color.WHITE else getColor(R.color.shell_text))
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22777777),
            android.graphics.drawable.GradientDrawable().apply {
                setColor(if (primary) accentColor() else if (darkTheme()) 0xff292929.toInt() else 0xffeeeeee.toInt())
                cornerRadius = dp(12).toFloat()
            }, null,
        )
        setOnClickListener { updateDialog?.dismiss(); action() }
    }

    private fun showDecision(title: String, subtitle: String, message: String,
                             primary: String, secondary: String, accept: () -> Unit, decline: () -> Unit) {
        val content = updateCard(title, subtitle)
        val details = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(getColor(R.color.shell_text))
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        content.addView(ScrollView(this).apply { addView(details) }, LinearLayout.LayoutParams(-1, dp(144)))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(cardButton(secondary, false, decline), LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(cardButton(primary, true, accept), LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(12) })
        content.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        updateDialog = showCard(content)
    }

    private fun showProgress(release: FrontendRelease) {
        val dark = darkTheme()
        val textColor = getColor(R.color.shell_text)
        val accent = accentColor()
        val content = updateCard("正在更新前端", release.label)
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(textColor)
            text = "准备安装…"
        }
        content.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            isIndeterminate = true
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(if (dark) 0xff353545.toInt() else 0xffe0e7ff.toInt())
        }
        content.addView(progress, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(16) })
        progressLabel = TextView(this).apply {
            text = "请稍候"
            textSize = 12f
            setTextColor(textColor)
            alpha = 0.6f
            gravity = android.view.Gravity.END
            setPadding(0, dp(8), 0, 0)
        }
        content.addView(progressLabel)
        content.addView(TextView(this).apply {
            text = "更新完成后将自动打开页面"
            textSize = 12f
            setTextColor(textColor)
            alpha = 0.5f
            setPadding(0, dp(20), 0, 0)
        })
        progressDialog = showCard(content)
    }

    private fun renderProgress(update: UpdateProgress) {
        status.text = update.message
        progress.isIndeterminate = update.percent == null
        update.percent?.let { progress.setProgress(it, true) }
        progressLabel.text = update.percent?.let { "$it%" } ?: "请稍候"
    }

    private fun closeProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showError(message: String) {
        closeProgress()
        showDecision("更新失败", "当前前端 · ${store.label}", message,
            "重试", "关闭", { checkUpdate() }, {})
    }

    private fun checkUpdate() {
        if (busy) return
        busy = true
        worker.execute {
            try {
                val release = store.checkUpdate()
                ui {
                    busy = false
                    closeProgress()
                    if (release == null) {
                        return@ui
                    }
                    busy = true
                    showDecision(
                        if (store.active == null) "安装前端" else "发现前端更新",
                        release.label,
                        "当前：${store.label}\n\n${release.notes.take(4000)}\n\n更新会重新加载页面，请先保存未发送内容。",
                        "立即更新", "暂不更新", { install(release) }, { busy = false },
                    )
                }
            } catch (error: Exception) {
                ui {
                    busy = false
                    if (store.active == null) showError("检查失败，尚未安装前端\n${error.message}")
                    // Installed frontends remain usable when a background check fails.
                }
            }
        }
    }

    private fun install(release: FrontendRelease) {
        showProgress(release)
        worker.execute {
            try {
                store.install(release) { update -> ui { renderProgress(update) } }
                ui {
                    web.stopLoading()
                    web.clearCache(true)
                    empty.visibility = android.view.View.GONE
                    web.visibility = android.view.View.VISIBLE
                    web.loadUrl(LocalContent.HOME)
                    busy = false
                    closeProgress()
                    Toast.makeText(this, "前端已更新", Toast.LENGTH_SHORT).show()
                }
            } catch (error: Exception) {
                ui { busy = false; showError("更新失败，当前前端：${store.label}\n${error.message}") }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            fileCallback = null
        }
    }
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        if (::web.isInitialized) web.saveState(state)
    }
    override fun onDestroy() {
        notifications?.destroy()
        updateDialog?.dismiss()
        closeProgress()
        worker.shutdownNow()
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        if (::web.isInitialized) { web.stopLoading(); web.destroy() }
        super.onDestroy()
    }
}
