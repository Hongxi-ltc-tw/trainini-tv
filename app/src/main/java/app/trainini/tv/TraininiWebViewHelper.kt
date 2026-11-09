package app.trainini.tv

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.cos
import kotlin.math.sin

object TraininiWebViewHelper {

    private const val DESKTOP_TV_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 TraininiGoogleTV/1.0"

    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView(
        webView: WebView,
        initialZoomPercent: Int,
        initialMuted: Boolean,
        onUrlChanged: ((String) -> Unit)? = null
    ) {
        webView.setBackgroundColor(Color.BLACK)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = DESKTOP_TV_USER_AGENT
            textZoom = 100
        }

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == null) return
                url?.let {
                    if (it.startsWith("http")) {
                        onUrlChanged?.invoke(it)
                    }
                }
                injectTvEnhancements(view, initialZoomPercent, initialMuted)
                // Dispatch a synthetic gentle click at (1, 1) to unlock Web Audio API on first load
                simulateUserGestureForAudio(view)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && view != null) {
                    // Auto-retry after 5 seconds if TV network is still waking up
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            view.reload()
                        } catch (_: Exception) {
                        }
                    }, 5000L)
                }
            }
        }
    }

    fun injectTvEnhancements(webView: WebView, zoomPercent: Int, isMuted: Boolean) {
        val zoomRatio = zoomPercent / 100.0
        val muteFlag = if (isMuted) "true" else "false"
        val js = """
            (function() {
                try {
                    // 1. Clean TV Fullscreen style (hide scrollbars, black background)
                    var styleId = 'trainini-tv-style';
                    var styleEl = document.getElementById(styleId);
                    if (!styleEl) {
                        styleEl = document.createElement('style');
                        styleEl.id = styleId;
                        document.head.appendChild(styleEl);
                    }
                    styleEl.textContent = `
                        ::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; }
                        html, body {
                            background-color: #000000 !important;
                            overflow-x: hidden !important;
                            user-select: none !important;
                        }
                    `;

                    // 2. Apply TV Zoom scale
                    document.body.style.zoom = '${zoomRatio}';

                    // 3. Hook AudioContext & HTMLMediaElement for TV Autoplay and Mute control
                    window.__traininiTvMuted = $muteFlag;
                    if (!window.__traininiAudioHooked) {
                        window.__traininiAudioHooked = true;
                        window.__traininiAudioContexts = [];
                        var OrigAudioContext = window.AudioContext || window.webkitAudioContext;
                        if (OrigAudioContext) {
                            var PatchedAudioContext = function() {
                                var ctx = new OrigAudioContext();
                                window.__traininiAudioContexts.push(ctx);
                                if (window.__traininiTvMuted && ctx.suspend) {
                                    ctx.suspend();
                                } else if (!window.__traininiTvMuted && ctx.state === 'suspended' && ctx.resume) {
                                    ctx.resume();
                                }
                                return ctx;
                            };
                            PatchedAudioContext.prototype = OrigAudioContext.prototype;
                            window.AudioContext = PatchedAudioContext;
                            if (window.webkitAudioContext) {
                                window.webkitAudioContext = PatchedAudioContext;
                            }
                        }
                    }

                    // Apply current mute state to any tracked AudioContexts and <audio>/<video> tags
                    if (window.__traininiAudioContexts) {
                        window.__traininiAudioContexts.forEach(function(ctx) {
                            try {
                                if (window.__traininiTvMuted) {
                                    if (ctx.state === 'running') ctx.suspend();
                                } else {
                                    if (ctx.state === 'suspended') ctx.resume();
                                }
                            } catch (e) {}
                        });
                    }
                    document.querySelectorAll('audio, video').forEach(function(media) {
                        media.muted = window.__traininiTvMuted;
                    });
                } catch (err) {
                    console.error('TraininiTV enhancement error:', err);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    fun applyZoom(webView: WebView, zoomPercent: Int) {
        val zoomRatio = zoomPercent / 100.0
        webView.evaluateJavascript(
            "if(document.body){document.body.style.zoom='$zoomRatio';}",
            null
        )
    }

    fun applyMute(webView: WebView, isMuted: Boolean) {
        webView.isSoundEffectsEnabled = !isMuted
        val muteFlag = if (isMuted) "true" else "false"
        val js = """
            (function() {
                window.__traininiTvMuted = $muteFlag;
                if (window.__traininiAudioContexts) {
                    window.__traininiAudioContexts.forEach(function(ctx) {
                        try {
                            if ($muteFlag) { ctx.suspend(); } else { ctx.resume(); }
                        } catch (e) {}
                    });
                }
                document.querySelectorAll('audio, video').forEach(function(m) {
                    m.muted = $muteFlag;
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    fun simulateClickAt(webView: WebView, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0)
        webView.dispatchTouchEvent(downEvent)
        webView.dispatchTouchEvent(upEvent)
        downEvent.recycle()
        upEvent.recycle()
    }

    private fun simulateUserGestureForAudio(webView: WebView) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                simulateClickAt(webView, 2f, 2f)
            } catch (_: Exception) {
            }
        }, 600L)
    }

    class OledShiftController(
        private val webView: WebView,
        private val isEnabledProvider: () -> Boolean
    ) {
        private val handler = Handler(Looper.getMainLooper())
        private var step = 0
        private val runnable = object : Runnable {
            override fun run() {
                if (isEnabledProvider()) {
                    step = (step + 1) % 12
                    val angle = step * (Math.PI / 6.0)
                    webView.translationX = (cos(angle) * 3.0).toFloat()
                    webView.translationY = (sin(angle) * 3.0).toFloat()
                } else {
                    webView.translationX = 0f
                    webView.translationY = 0f
                }
                handler.postDelayed(this, 60_000L) // Shift gently every 60 seconds
            }
        }

        fun start() {
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 60_000L)
        }

        fun stop() {
            handler.removeCallbacks(runnable)
            webView.translationX = 0f
            webView.translationY = 0f
        }
    }
}
