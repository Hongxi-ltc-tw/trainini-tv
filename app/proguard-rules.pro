# Keep WebView JavascriptInterface methods if added
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
