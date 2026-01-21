package com.elcavz.elcavzflixter

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var isFullscreen = false
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    companion object {
        // Static variable to track if website is loaded
        var isWebsiteLoaded = false

        // Function to reset for new app launches
        fun resetLoadingState() {
            isWebsiteLoaded = false
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )

        super.onCreate(savedInstanceState)
        resetLoadingState()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView()
        webView.loadUrl("https://elcavzflixter.vercel.app")
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false

        // Add JavaScript interface
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // Set WebChromeClient with fullscreen handling
        webView.webChromeClient = createWebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return handleUrl(url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Website started loading
                isWebsiteLoaded = false
            }

            @Deprecated("Deprecated in API 24")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUrl(url ?: "")
            }

            private fun handleUrl(url: String): Boolean {

                // Handle Facebook links
                if (url.contains("facebook.com") || url.contains("fb://")) {
                    openFacebookUrl(url)
                    return true
                }

                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (url.contains("elcavzflixter.vercel.app")) {
                        return false
                    }
                    openInBrowser(url)
                    return true
                }

                // Handle tel:, mailto:, etc.
                if (url.startsWith("tel:") || url.startsWith("mailto:") ||
                    url.startsWith("sms:") || url.startsWith("whatsapp:")) {
                    openInExternalApp(url)
                    return true
                }

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Website completely loaded!
                isWebsiteLoaded = true
                // You could also send a broadcast here
                sendWebsiteLoadedBroadcast()
                // Re-inject WebChromeClient for each page load
                webView.webChromeClient = createWebChromeClient()
                injectFacebookLinkFix()
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                // Even on error, consider it "loaded" to proceed
                isWebsiteLoaded = true
                sendWebsiteLoadedBroadcast()
            }
        }
    }

    private fun sendWebsiteLoadedBroadcast() {
        // Send broadcast to notify splash activity
        val intent = Intent("WEBSITE_LOADED")
        sendBroadcast(intent)
    }

    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                if (view != null) {
                    // Hide WebView
                    webView.visibility = View.GONE

                    // Add custom view to window
                    val decorView = window.decorView as ViewGroup
                    decorView.addView(view, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))

                    // Enter fullscreen
                    isFullscreen = true
                    enterFullscreenMode()

                    // Auto-rotate to landscape
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            override fun onHideCustomView() {
                if (customView == null || customViewCallback == null) {
                    return
                }

                // Restore portrait orientation
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                // Remove custom view
                val decorView = window.decorView as ViewGroup
                decorView.removeView(customView)

                // Notify callback
                customViewCallback?.onCustomViewHidden()

                // Clear references
                customView = null
                customViewCallback = null

                // Exit fullscreen
                exitFullscreenMode()
                isFullscreen = false

                // Show WebView
                webView.visibility = View.VISIBLE

                // Force WebView redraw
                webView.invalidate()
                webView.requestLayout()

                // IMPORTANT: Create a new WebChromeClient for next video
                webView.webChromeClient = createWebChromeClient()
            }
        }
    }

    fun openFacebookUrl(url: String) {
        try {
            val facebookUrl = if (url.contains("facebook.com")) {
                val pattern = "facebook.com/([^/?]+)".toRegex()
                val match = pattern.find(url)
                if (match != null) {
                    val username = match.groupValues[1]
                    "fb://profile/$username"
                } else {
                    url
                }
            } else {
                url
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl))
            val packageManager = packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)
            val isFacebookAppInstalled = activities.isNotEmpty()

            if (isFacebookAppInstalled) {
                startActivity(intent)
            } else {
                openInBrowser(url)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            openInBrowser(url)
        }
    }

    fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInExternalApp(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun injectFacebookLinkFix() {
        val javascript = """
        (function() {
            // Fix Facebook links to work in WebView
            document.addEventListener('click', function(e) {
                var target = e.target;
                while (target && target.tagName !== 'A') {
                    target = target.parentElement;
                }
                
                if (target && target.tagName === 'A') {
                    var href = target.getAttribute('href');
                    if (href && href.includes('facebook.com')) {
                        e.preventDefault();
                        e.stopPropagation();
                        
                        // Open via Android interface
                        if (window.Android && Android.openFacebookLink) {
                            Android.openFacebookLink(href);
                        } else {
                            // Fallback
                            window.open(href, '_system');
                        }
                        return false;
                    }
                }
            }, true);
            
            // Override window.open for Facebook links
            var originalOpen = window.open;
            window.open = function(url, target, features) {
                if (url && url.includes('facebook.com')) {
                    if (window.Android && Android.openFacebookLink) {
                        Android.openFacebookLink(url);
                        return null;
                    }
                }
                return originalOpen.call(this, url, target, features);
            };
            
            console.log('Facebook link fix injected');
        })();
    """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }

    private fun restoreWebView() {
        // This ensures WebView is properly restored
        webView.clearFocus()
        webView.requestFocus()

        // If still having issues, try reloading the page
        // webView.reload()

        // Or evaluate JavaScript to restore video state
        webView.evaluateJavascript("""
            if (document.pictureInPictureElement) {
                document.exitPictureInPicture();
            }
            if (document.fullscreenElement) {
                document.exitFullscreen();
            }
            if (document.webkitFullscreenElement) {
                document.webkitExitFullscreen();
            }
            
            // Pause all videos
            var videos = document.getElementsByTagName('video');
            for (var i = 0; i < videos.length; i++) {
                videos[i].pause();
                videos[i].currentTime = 0;
            }
        """.trimIndent(), null)
    }

    private fun enterFullscreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()
    }

    private fun exitFullscreenMode() {
        // Show system UI
        showSystemUI()

        // Allow screen to turn off
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Show action bar
        supportActionBar?.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBackPressed() {
        if (isFullscreen) {
            // Manually trigger exit fullscreen
            if (customView != null && customViewCallback != null) {
                (webView.webChromeClient as? WebChromeClient)?.onHideCustomView()
            }
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // Handle system back button in fullscreen
    private fun onHideCustomView() {
        if (customView == null || customViewCallback == null) return

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (customView != null) {
            val decorView = window.decorView as android.view.ViewGroup
            decorView.removeView(customView)
        }

        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null

        exitFullscreenMode()
        webView.visibility = View.VISIBLE
        isFullscreen = false
        restoreWebView()
    }

    override fun onPause() {
        super.onPause()
        if (isFullscreen) {
            exitFullscreenMode()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        if (isFullscreen) {
            enterFullscreenMode()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (isFullscreen) {
            // Maintain fullscreen in landscape
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                enterFullscreenMode()
            }
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.loadData("", "text/html", "utf-8")
            webView.clearHistory()
            webView.clearCache(true)
            webView.destroy()
        }
        super.onDestroy()
        resetLoadingState()
    }

    private fun cleanupFullscreen() {
        if (customView != null) {
            val decorView = window.decorView as ViewGroup
            decorView.removeView(customView)
            customView = null
        }
        if (customViewCallback != null) {
            customViewCallback = null
        }
        isFullscreen = false
        webView.visibility = View.VISIBLE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun injectStatusBarColorDetection() {
        val javascript = """
        (function() {
            // Function to detect background color
            function getBackgroundColor(element) {
                var bg = window.getComputedStyle(element).backgroundColor;
                if (bg === 'rgba(0, 0, 0, 0)' || bg === 'transparent') {
                    if (element.tagName === 'HTML') return 'white';
                    return getBackgroundColor(element.parentElement);
                }
                return bg;
            }
            
            // Send color to Android
            var bgColor = getBackgroundColor(document.body);
            if (window.Android && Android.setStatusBarColor) {
                Android.setStatusBarColor(bgColor);
            }
            
            // Listen for page changes
            var observer = new MutationObserver(function() {
                var newColor = getBackgroundColor(document.body);
                if (window.Android && Android.setStatusBarColor) {
                    Android.setStatusBarColor(newColor);
                }
            });
            
            observer.observe(document.body, {
                attributes: true,
                attributeFilter: ['style', 'class']
            });
        })();
    """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }
}