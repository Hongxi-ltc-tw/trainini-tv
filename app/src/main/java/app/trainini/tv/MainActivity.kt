package app.trainini.tv

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView
    private lateinit var dimOverlay: View
    private lateinit var cursorView: CursorOverlayView
    private lateinit var controlBarContainer: FrameLayout
    private lateinit var hintBanner: TextView

    private lateinit var btnStation: Button
    private lateinit var btnCursor: Button
    private lateinit var btnZoom: Button
    private lateinit var btnMute: Button
    private lateinit var btnReload: Button
    private lateinit var btnSettings: Button

    private var oledShiftController: TraininiWebViewHelper.OledShiftController? = null
    private var isCursorMode = false
    private var cursorX = 960f
    private var cursorY = 540f
    private val cursorStep = 36f
    private var lastLoadedUrl = ""

    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideHintRunnable = Runnable {
        hintBanner.animate().alpha(0f).setDuration(400).withEndAction {
            hintBanner.visibility = View.GONE
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep TV screen awake at all times while timetable app is open
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val initialUrl = PrefsManager.getTargetUrl(this)
        val initialZoom = PrefsManager.getZoomPercent(this)
        val initialMuted = PrefsManager.isMuted(this)
        lastLoadedUrl = initialUrl

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isFocusable = false
            isFocusableInTouchMode = false
        }

        TraininiWebViewHelper.setupWebView(
            webView = webView,
            initialZoomPercent = initialZoom,
            initialMuted = initialMuted,
            onUrlChanged = { newUrl ->
                // If user changed station inside trainini.app's own gear menu, persist the updated URL!
                if (newUrl.contains("trainini.app") && newUrl != lastLoadedUrl) {
                    lastLoadedUrl = newUrl
                    PrefsManager.setTargetUrl(this, newUrl)
                }
            }
        )
        rootLayout.addView(webView)

        // Dim overlay for night viewing
        dimOverlay = View(this).apply {
            isClickable = false
            isFocusable = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout.addView(dimOverlay)
        updateDimOverlay()

        // Virtual Mouse Pointer Overlay for TV Remote
        cursorView = CursorOverlayView(this).apply {
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout.addView(cursorView)

        // Startup remote control hint banner
        hintBanner = TextView(this).apply {
            text = "💡 按遙控器【OK / 確認鍵】或【選單鍵】開啟電視控制列"
            setTextColor(Color.parseColor("#FBBF24"))
            textSize = 15f
            setPadding(36, 18, 36, 18)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC18181B"))
                cornerRadius = 999f
                setStroke(2, Color.parseColor("#F59E0B"))
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 36
            }
        }
        rootLayout.addView(hintBanner)

        // Build TV Quick Control Bar Overlay
        controlBarContainer = buildControlBar()
        rootLayout.addView(controlBarContainer)

        setContentView(rootLayout)

        oledShiftController = TraininiWebViewHelper.OledShiftController(webView) {
            PrefsManager.isOledProtectionEnabled(this)
        }.also { it.start() }

        webView.loadUrl(initialUrl)
        showBriefHint()
    }

    private fun buildControlBar(): FrameLayout {
        val wrapper = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#88000000"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 28, 40, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F218181B"))
                cornerRadius = 28f
                setStroke(3, Color.parseColor("#F59E0B"))
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 42
            }
        }

        val titleRow = TextView(this).apply {
            text = "🚂 翻牌時刻表 Google TV 控制台（按返回鍵關閉）"
            setTextColor(Color.parseColor("#FBBF24"))
            textSize = 16f
            setPadding(4, 0, 4, 18)
        }
        card.addView(titleRow)

        val scrollRow = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        btnStation = createTvButton("📍 切換車站") {
            showStationPickerDialog()
        }
        btnCursor = createTvButton("🖱️ 遙控器游標：關") {
            toggleCursorMode()
        }
        btnZoom = createTvButton("🔍 縮放：100%") {
            cycleZoomLevel()
        }
        btnMute = createTvButton("🔊 音效：開啟") {
            toggleMute()
        }
        btnReload = createTvButton("🔄 重新整理") {
            webView.reload()
            hideControlBar()
        }
        btnSettings = createTvButton("⚙️ 屏保與設定") {
            hideControlBar()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val buttons = listOf(btnStation, btnCursor, btnZoom, btnMute, btnReload, btnSettings)
        buttons.forEachIndexed { index, btn ->
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index > 0) marginStart = 16
            }
            buttonRow.addView(btn, lp)
        }

        scrollRow.addView(buttonRow)
        card.addView(scrollRow)
        wrapper.addView(card)

        return wrapper
    }

    private fun createTvButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setPadding(32, 18, 32, 18)
            isFocusable = true
            isFocusableInTouchMode = true
            setTextColor(Color.WHITE)
            background = createButtonDrawable(focused = false)
            setOnFocusChangeListener { _, hasFocus ->
                background = createButtonDrawable(focused = hasFocus)
                setTextColor(if (hasFocus) Color.BLACK else Color.WHITE)
                scaleX = if (hasFocus) 1.06f else 1.0f
                scaleY = if (hasFocus) 1.06f else 1.0f
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createButtonDrawable(focused: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 16f
            if (focused) {
                setColor(Color.parseColor("#FBBF24"))
                setStroke(3, Color.WHITE)
            } else {
                setColor(Color.parseColor("#27272A"))
                setStroke(2, Color.parseColor("#52525B"))
            }
        }
    }

    private fun updateControlBarLabels() {
        val currentUrl = PrefsManager.getTargetUrl(this)
        val matchedPreset = PrefsManager.PRESETS.find { it.url == currentUrl }
        val stationName = matchedPreset?.title?.substringBefore(" (") ?: "自訂網址"
        btnStation.text = "📍 車站：$stationName"
        btnCursor.text = if (isCursorMode) "🖱️ 遙控游標：開啟" else "🖱️ 遙控游標：關閉"
        btnZoom.text = "🔍 縮放：${PrefsManager.getZoomPercent(this)}%"
        btnMute.text = if (PrefsManager.isMuted(this)) "🔇 音效：靜音" else "🔊 音效：開啟"
    }

    private fun showControlBar() {
        updateControlBarLabels()
        controlBarContainer.visibility = View.VISIBLE
        btnStation.requestFocus()
    }

    private fun hideControlBar() {
        controlBarContainer.visibility = View.GONE
    }

    private fun toggleCursorMode() {
        isCursorMode = !isCursorMode
        if (isCursorMode) {
            cursorX = (rootLayout.width.takeIf { it > 0 } ?: 1920) / 2f
            cursorY = (rootLayout.height.takeIf { it > 0 } ?: 1080) / 2f
            cursorView.updatePosition(cursorX, cursorY)
            cursorView.visibility = View.VISIBLE
            hideControlBar()
            Toast.makeText(
                this,
                "遙控游標已開啟：方向鍵移動、OK 鍵點擊網頁（可點右上角齒輪）、返回鍵退出游標",
                Toast.LENGTH_LONG
            ).show()
        } else {
            cursorView.visibility = View.GONE
            updateControlBarLabels()
        }
    }

    private fun cycleZoomLevel() {
        val current = PrefsManager.getZoomPercent(this)
        val options = PrefsManager.ZOOM_OPTIONS
        val nextIndex = (options.indexOf(current) + 1) % options.size
        val nextZoom = options[nextIndex]
        PrefsManager.setZoomPercent(this, nextZoom)
        TraininiWebViewHelper.applyZoom(webView, nextZoom)
        updateControlBarLabels()
    }

    private fun toggleMute() {
        val newMuted = !PrefsManager.isMuted(this)
        PrefsManager.setMuted(this, newMuted)
        TraininiWebViewHelper.applyMute(webView, newMuted)
        updateControlBarLabels()
    }

    private fun showStationPickerDialog() {
        val titles = PrefsManager.PRESETS.map { it.title }.toTypedArray()
        val currentUrl = PrefsManager.getTargetUrl(this)
        val checkedItem = PrefsManager.PRESETS.indexOfFirst { it.url == currentUrl }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("選擇翻牌時刻表車站")
            .setSingleChoiceItems(titles, checkedItem) { dialog, which ->
                val selected = PrefsManager.PRESETS[which]
                PrefsManager.setTargetUrl(this, selected.url)
                lastLoadedUrl = selected.url
                webView.loadUrl(selected.url)
                updateControlBarLabels()
                dialog.dismiss()
                hideControlBar()
            }
            .setNeutralButton("自訂網址 / 更多設定") { _, _ ->
                hideControlBar()
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateDimOverlay() {
        val dimPercent = PrefsManager.getDimLevel(this)
        if (dimPercent <= 0) {
            dimOverlay.visibility = View.GONE
        } else {
            val alpha = ((dimPercent / 100f) * 255).toInt().coerceIn(0, 200)
            dimOverlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
            dimOverlay.visibility = View.VISIBLE
        }
    }

    private fun showBriefHint() {
        hintBanner.alpha = 1f
        hintBanner.visibility = View.VISIBLE
        uiHandler.removeCallbacks(hideHintRunnable)
        uiHandler.postDelayed(hideHintRunnable, 5000L)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode

            // 1. If Control Bar is open, let it handle D-Pad navigation; BACK or MENU closes it
            if (controlBarContainer.visibility == View.VISIBLE) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                    hideControlBar()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }

            // 2. If Virtual Mouse Cursor mode is active, D-Pad moves cursor & OK clicks on WebView
            if (isCursorMode) {
                val maxW = (rootLayout.width.takeIf { it > 0 } ?: 1920).toFloat()
                val maxH = (rootLayout.height.takeIf { it > 0 } ?: 1080).toFloat()
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        cursorY = max(10f, cursorY - cursorStep)
                        cursorView.updatePosition(cursorX, cursorY)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        cursorY = min(maxH - 10f, cursorY + cursorStep)
                        cursorView.updatePosition(cursorX, cursorY)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        cursorX = max(10f, cursorX - cursorStep)
                        cursorView.updatePosition(cursorX, cursorY)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        cursorX = min(maxW - 10f, cursorX + cursorStep)
                        cursorView.updatePosition(cursorX, cursorY)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        cursorView.animateClick()
                        TraininiWebViewHelper.simulateClickAt(webView, cursorX, cursorY)
                        return true
                    }
                    KeyEvent.KEYCODE_MENU -> {
                        isCursorMode = false
                        cursorView.visibility = View.GONE
                        showControlBar()
                        return true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        isCursorMode = false
                        cursorView.visibility = View.GONE
                        Toast.makeText(this, "已關閉遙控游標", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }

            // 3. Normal Fullscreen Timetable Mode: OK / Enter / Menu opens the TV Control Bar
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_SETTINGS -> {
                    showControlBar()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    showControlBar()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        webView.onResume()

        val savedUrl = PrefsManager.getTargetUrl(this)
        if (savedUrl != lastLoadedUrl) {
            lastLoadedUrl = savedUrl
            webView.loadUrl(savedUrl)
        } else {
            TraininiWebViewHelper.applyZoom(webView, PrefsManager.getZoomPercent(this))
            TraininiWebViewHelper.applyMute(webView, PrefsManager.isMuted(this))
        }
        updateDimOverlay()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(hideHintRunnable)
        oledShiftController?.stop()
        oledShiftController = null
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    private class CursorOverlayView(context: android.content.Context) : View(context) {
        private var cx = 960f
        private var cy = 540f
        private var radius = 16f

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DDFA9E0B")
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        fun updatePosition(x: Float, y: Float) {
            cx = x
            cy = y
            invalidate()
        }

        fun animateClick() {
            radius = 10f
            invalidate()
            postDelayed({
                radius = 16f
                invalidate()
            }, 140L)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawCircle(cx, cy, radius, fillPaint)
            canvas.drawCircle(cx, cy, radius, strokePaint)
            canvas.drawLine(cx - 8f, cy, cx + 8f, cy, crosshairPaint)
            canvas.drawLine(cx, cy - 8f, cx, cy + 8f, crosshairPaint)
        }
    }
}
