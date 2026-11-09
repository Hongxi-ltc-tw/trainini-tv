package app.trainini.tv

import android.content.Context
import android.content.SharedPreferences

data class StationPreset(
    val title: String,
    val url: String
)

object PrefsManager {
    private const val PREFS_NAME = "trainini_tv_prefs"

    private const val KEY_TARGET_URL = "target_url"
    private const val KEY_ZOOM_PERCENT = "zoom_percent"
    private const val KEY_MUTE_AUDIO = "mute_audio"
    private const val KEY_SCREENSAVER_MUTE = "screensaver_mute"
    private const val KEY_SCREENSAVER_INTERACTIVE = "screensaver_interactive"
    private const val KEY_OLED_PROTECTION = "oled_protection"
    private const val KEY_DIM_LEVEL = "dim_level"

    const val DEFAULT_URL = "https://trainini.app/?tra=7380"

    val PRESETS = listOf(
        StationPreset("台鐵 四腳亭 (tra=7380 預設)", "https://trainini.app/?tra=7380"),
        StationPreset("台鐵 瑞芳 (tra=7360)", "https://trainini.app/?tra=7360"),
        StationPreset("台鐵 暖暖 (tra=7390)", "https://trainini.app/?tra=7390"),
        StationPreset("台鐵 八堵 (tra=0920)", "https://trainini.app/?tra=0920"),
        StationPreset("台鐵 七堵 (tra=0930)", "https://trainini.app/?tra=0930"),
        StationPreset("台鐵 基隆 (tra=0900)", "https://trainini.app/?tra=0900"),
        StationPreset("台鐵 臺北 (tra=1000)", "https://trainini.app/?tra=1000"),
        StationPreset("台鐵 松山 (tra=0980)", "https://trainini.app/?tra=0980"),
        StationPreset("台鐵 南港 (tra=0970)", "https://trainini.app/?tra=0970"),
        StationPreset("台鐵 板橋 (tra=1020)", "https://trainini.app/?tra=1020"),
        StationPreset("台鐵 桃園 (tra=1080)", "https://trainini.app/?tra=1080"),
        StationPreset("台鐵 新竹 (tra=1210)", "https://trainini.app/?tra=1210"),
        StationPreset("台鐵 臺中 (tra=3300)", "https://trainini.app/?tra=3300"),
        StationPreset("台鐵 彰化 (tra=3360)", "https://trainini.app/?tra=3360"),
        StationPreset("台鐵 嘉義 (tra=4080)", "https://trainini.app/?tra=4080"),
        StationPreset("台鐵 臺南 (tra=4220)", "https://trainini.app/?tra=4220"),
        StationPreset("台鐵 高雄 (tra=4400)", "https://trainini.app/?tra=4400"),
        StationPreset("台鐵 宜蘭 (tra=7190)", "https://trainini.app/?tra=7190"),
        StationPreset("台鐵 羅東 (tra=7160)", "https://trainini.app/?tra=7160"),
        StationPreset("台鐵 花蓮 (tra=7000)", "https://trainini.app/?tra=7000"),
        StationPreset("台鐵 臺東 (tra=6000)", "https://trainini.app/?tra=6000")
    )

    val ZOOM_OPTIONS = listOf(75, 90, 100, 115, 125, 150, 175, 200)
    val DIM_OPTIONS = listOf(0, 15, 30, 50)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTargetUrl(context: Context): String =
        prefs(context).getString(KEY_TARGET_URL, DEFAULT_URL)?.takeIf { it.isNotBlank() } ?: DEFAULT_URL

    fun setTargetUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_TARGET_URL, url.trim().ifEmpty { DEFAULT_URL }).apply()
    }

    fun getZoomPercent(context: Context): Int =
        prefs(context).getInt(KEY_ZOOM_PERCENT, 100)

    fun setZoomPercent(context: Context, zoomPercent: Int) {
        prefs(context).edit().putInt(KEY_ZOOM_PERCENT, zoomPercent).apply()
    }

    fun isMuted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MUTE_AUDIO, false)

    fun setMuted(context: Context, muted: Boolean) {
        prefs(context).edit().putBoolean(KEY_MUTE_AUDIO, muted).apply()
    }

    fun isScreensaverMuted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SCREENSAVER_MUTE, false)

    fun setScreensaverMuted(context: Context, muted: Boolean) {
        prefs(context).edit().putBoolean(KEY_SCREENSAVER_MUTE, muted).apply()
    }

    fun isScreensaverInteractive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SCREENSAVER_INTERACTIVE, false)

    fun setScreensaverInteractive(context: Context, interactive: Boolean) {
        prefs(context).edit().putBoolean(KEY_SCREENSAVER_INTERACTIVE, interactive).apply()
    }

    fun isOledProtectionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OLED_PROTECTION, true)

    fun setOledProtectionEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_OLED_PROTECTION, enabled).apply()
    }

    fun getDimLevel(context: Context): Int =
        prefs(context).getInt(KEY_DIM_LEVEL, 0)

    fun setDimLevel(context: Context, dimPercent: Int) {
        prefs(context).edit().putInt(KEY_DIM_LEVEL, dimPercent.coerceIn(0, 70)).apply()
    }
}
