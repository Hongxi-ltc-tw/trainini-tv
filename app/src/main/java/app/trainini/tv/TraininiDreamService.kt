package app.trainini.tv

import android.graphics.Color
import android.service.dreams.DreamService
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout

class TraininiDreamService : DreamService() {

    private var webView: WebView? = null
    private var oledShiftController: TraininiWebViewHelper.OledShiftController? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val interactive = PrefsManager.isScreensaverInteractive(this)
        val dimPercent = PrefsManager.getDimLevel(this)
        val zoomPercent = PrefsManager.getZoomPercent(this)
        val muted = PrefsManager.isScreensaverMuted(this)
        val targetUrl = PrefsManager.getTargetUrl(this)

        isInteractive = interactive
        isFullscreen = true
        isScreenBright = dimPercent < 30

        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val wv = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isFocusable = interactive
            isFocusableInTouchMode = interactive
        }

        TraininiWebViewHelper.setupWebView(
            webView = wv,
            initialZoomPercent = zoomPercent,
            initialMuted = muted
        )

        container.addView(wv)

        // Add optional night-mode dimming overlay
        if (dimPercent > 0) {
            val alpha = ((dimPercent / 100f) * 255).toInt().coerceIn(0, 200)
            val dimOverlay = View(this).apply {
                setBackgroundColor(Color.argb(alpha, 0, 0, 0))
                isClickable = false
                isFocusable = false
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            container.addView(dimOverlay)
        }

        setContentView(container)

        webView = wv
        oledShiftController = TraininiWebViewHelper.OledShiftController(wv) {
            PrefsManager.isOledProtectionEnabled(this)
        }.also { it.start() }

        wv.loadUrl(targetUrl)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        webView?.onResume()
        webView?.let {
            TraininiWebViewHelper.applyMute(it, PrefsManager.isScreensaverMuted(this))
        }
    }

    override fun onDreamingStopped() {
        webView?.let {
            TraininiWebViewHelper.applyMute(it, true)
            it.onPause()
        }
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        oledShiftController?.stop()
        oledShiftController = null
        webView?.let { wv ->
            try {
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.onPause()
                wv.removeAllViews()
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.destroy()
            } catch (_: Exception) {
            }
        }
        webView = null
        super.onDetachedFromWindow()
    }
}
