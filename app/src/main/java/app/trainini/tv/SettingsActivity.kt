package app.trainini.tv

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvCurrentUrl: TextView
    private lateinit var btnPresetStation: Button
    private lateinit var btnCustomUrl: Button
    private lateinit var btnZoom: Button
    private lateinit var btnAppMute: Button
    private lateinit var btnDreamMute: Button
    private lateinit var btnDreamInteractive: Button
    private lateinit var btnOledShift: Button
    private lateinit var btnDimLevel: Button
    private lateinit var btnOpenDreamSettings: Button
    private lateinit var btnResetDefault: Button
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121214"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 48, 80, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val header = TextView(this).apply {
            text = "🚂 翻牌時刻表 Google TV & 螢幕保護程式設定"
            setTextColor(Color.parseColor("#FBBF24"))
            textSize = 24f
            setPadding(0, 0, 0, 12)
        }
        container.addView(header)

        tvCurrentUrl = TextView(this).apply {
            setTextColor(Color.parseColor("#A1A1AA"))
            textSize = 15f
            setPadding(0, 0, 0, 28)
        }
        container.addView(tvCurrentUrl)

        btnPresetStation = createSettingButton { showPresetDialog() }
        btnCustomUrl = createSettingButton { showCustomUrlDialog() }
        btnZoom = createSettingButton {
            val current = PrefsManager.getZoomPercent(this)
            val opts = PrefsManager.ZOOM_OPTIONS
            val next = opts[(opts.indexOf(current) + 1) % opts.size]
            PrefsManager.setZoomPercent(this, next)
            refreshLabels()
        }
        btnAppMute = createSettingButton {
            PrefsManager.setMuted(this, !PrefsManager.isMuted(this))
            refreshLabels()
        }
        btnDreamMute = createSettingButton {
            PrefsManager.setScreensaverMuted(this, !PrefsManager.isScreensaverMuted(this))
            refreshLabels()
        }
        btnDreamInteractive = createSettingButton {
            PrefsManager.setScreensaverInteractive(this, !PrefsManager.isScreensaverInteractive(this))
            refreshLabels()
        }
        btnOledShift = createSettingButton {
            PrefsManager.setOledProtectionEnabled(this, !PrefsManager.isOledProtectionEnabled(this))
            refreshLabels()
        }
        btnDimLevel = createSettingButton {
            val current = PrefsManager.getDimLevel(this)
            val opts = PrefsManager.DIM_OPTIONS
            val next = opts[(opts.indexOf(current) + 1) % opts.size]
            PrefsManager.setDimLevel(this, next)
            refreshLabels()
        }
        btnOpenDreamSettings = createSettingButton {
            openSystemDreamSettings()
        }
        btnResetDefault = createSettingButton {
            PrefsManager.setTargetUrl(this, PrefsManager.DEFAULT_URL)
            PrefsManager.setZoomPercent(this, 100)
            PrefsManager.setMuted(this, false)
            PrefsManager.setScreensaverMuted(this, false)
            PrefsManager.setScreensaverInteractive(this, false)
            PrefsManager.setOledProtectionEnabled(this, true)
            PrefsManager.setDimLevel(this, 0)
            refreshLabels()
            Toast.makeText(this, "已還原為預設值 (tra=7380 四腳亭)", Toast.LENGTH_SHORT).show()
        }
        btnClose = createSettingButton {
            finish()
        }

        val allButtons = listOf(
            btnPresetStation,
            btnCustomUrl,
            btnZoom,
            btnAppMute,
            btnDreamMute,
            btnDreamInteractive,
            btnOledShift,
            btnDimLevel,
            btnOpenDreamSettings,
            btnResetDefault,
            btnClose
        )

        allButtons.forEach { btn ->
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            container.addView(btn, lp)
        }

        val adbHelp = TextView(this).apply {
            text = "💡 小提示：部分 Chromecast with Google TV 隱藏了系統內建屏保選單，若點擊上方「開啟系統螢幕保護程式設定」無反應，可使用 ADB 指令一鍵指定為 Google TV 預設屏保：\n" +
                "adb shell settings put secure screensaver_components app.trainini.tv/.TraininiDreamService"
            setTextColor(Color.parseColor("#71717A"))
            textSize = 13f
            setPadding(0, 20, 0, 20)
        }
        container.addView(adbHelp)

        scrollView.addView(container)
        setContentView(scrollView)

        refreshLabels()
        btnPresetStation.requestFocus()
    }

    private fun createSettingButton(onClick: () -> Unit): Button {
        return Button(this).apply {
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            textSize = 16f
            setPadding(36, 22, 36, 22)
            isFocusable = true
            isFocusableInTouchMode = true
            setTextColor(Color.WHITE)
            background = buttonBg(false)
            setOnFocusChangeListener { _, hasFocus ->
                background = buttonBg(hasFocus)
                setTextColor(if (hasFocus) Color.BLACK else Color.WHITE)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun buttonBg(focused: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 16f
            if (focused) {
                setColor(Color.parseColor("#FBBF24"))
                setStroke(3, Color.WHITE)
            } else {
                setColor(Color.parseColor("#27272A"))
                setStroke(2, Color.parseColor("#3F3F46"))
            }
        }
    }

    private fun refreshLabels() {
        val currentUrl = PrefsManager.getTargetUrl(this)
        val matched = PrefsManager.PRESETS.find { it.url == currentUrl }
        tvCurrentUrl.text = "目前載入網址：$currentUrl"
        btnPresetStation.text = "📍 快速選擇預設車站（目前：${matched?.title ?: "自訂網址"}）"
        btnCustomUrl.text = "🔗 輸入自訂 Trainini 網址（支援台鐵 tra / 高鐵 thsr / 輕軌參數）"
        btnZoom.text = "🔍 電視畫面縮放比例：${PrefsManager.getZoomPercent(this)}%（點擊切換）"
        btnAppMute.text = "🔊 主程式翻牌音效：${if (PrefsManager.isMuted(this)) "靜音 🔇" else "開啟 🔊"}"
        btnDreamMute.text = "🌙 螢幕保護程式翻牌音效：${if (PrefsManager.isScreensaverMuted(this)) "靜音 🔇" else "開啟 🔊"}"
        btnDreamInteractive.text =
            "🎮 螢幕保護程式遙控器行為：${if (PrefsManager.isScreensaverInteractive(this)) "可操作網頁（按返回鍵退出）" else "按任意鍵立即喚醒電視（預設）"}"
        btnOledShift.text =
            "🛡️ OLED 電視防烙印微移保護：${if (PrefsManager.isOledProtectionEnabled(this)) "開啟（每分鐘微移 3px）" else "關閉"}"
        btnDimLevel.text = "🔅 夜間亮度調暗遮罩：調暗 ${PrefsManager.getDimLevel(this)}%（點擊切換）"
        btnOpenDreamSettings.text = "📺 開啟 Google TV 系統「螢幕保護程式」設定選單"
        btnResetDefault.text = "♻️ 還原為預設值 (https://trainini.app/?tra=7380)"
        btnClose.text = "✅ 儲存並返回翻牌時刻表"
    }

    private fun showPresetDialog() {
        val titles = PrefsManager.PRESETS.map { it.title }.toTypedArray()
        val currentUrl = PrefsManager.getTargetUrl(this)
        val checked = PrefsManager.PRESETS.indexOfFirst { it.url == currentUrl }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("選擇預設車站")
            .setSingleChoiceItems(titles, checked) { dialog, which ->
                PrefsManager.setTargetUrl(this, PrefsManager.PRESETS[which].url)
                refreshLabels()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCustomUrlDialog() {
        val input = EditText(this).apply {
            setText(PrefsManager.getTargetUrl(this@SettingsActivity))
            setSelection(text.length)
            hint = "例如 https://trainini.app/?tra=7380"
        }

        AlertDialog.Builder(this)
            .setTitle("輸入自訂 Trainini 網址")
            .setMessage("可輸入任何 trainini.app 參數網址，例如：\nhttps://trainini.app/?tra=7380")
            .setView(input)
            .setPositiveButton("儲存") { _, _ ->
                val raw = input.text.toString().trim()
                val normalized = when {
                    raw.isEmpty() -> PrefsManager.DEFAULT_URL
                    raw.startsWith("http://") || raw.startsWith("https://") -> raw
                    raw.all { it.isDigit() } -> "https://trainini.app/?tra=$raw"
                    else -> "https://$raw"
                }
                PrefsManager.setTargetUrl(this, normalized)
                refreshLabels()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openSystemDreamSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                Toast.makeText(
                    this,
                    "請在系統設定 > 系統 > 待機模式 / 螢幕保護程式 中選擇「台鐵翻牌時刻表」",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this, "無法直接開啟系統設定：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
