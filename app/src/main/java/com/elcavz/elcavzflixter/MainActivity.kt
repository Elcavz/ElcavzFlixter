package com.elcavz.elcavzflixter

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var isFullscreen = false
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make status bar transparent
        makeStatusBarTransparent()

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView()
        webView.loadUrl("https://elcavzflixter.vercel.app")
    }

    private fun makeStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
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

        // Set custom WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                // Store references
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                // Hide the WebView
                webView.visibility = View.GONE

                // Add custom view to layout
                if (view != null) {
                    val decorView = window.decorView as android.view.ViewGroup
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
                // Check if we're in fullscreen
                if (customView == null || customViewCallback == null) {
                    return
                }

                // Restore portrait orientation FIRST
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                // Remove custom view from layout
                if (customView != null) {
                    val decorView = window.decorView as android.view.ViewGroup
                    decorView.removeView(customView)
                }

                // Call callback to notify WebView
                customViewCallback?.onCustomViewHidden()

                // Clear references
                customView = null
                customViewCallback = null

                // Exit fullscreen mode
                exitFullscreenMode()

                // Show WebView again
                webView.visibility = View.VISIBLE
                isFullscreen = false

                // IMPORTANT: Reload the WebView state
                restoreWebView()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Add padding for status bar
                addStatusBarPadding()
            }
        }
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
        // Hide system bars
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
    }

    private fun exitFullscreenMode() {
        // Show system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }

        // Allow screen to turn off
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Restore status bar
        makeStatusBarTransparent()
    }

    private fun addStatusBarPadding() {
        val statusBarHeight = getStatusBarHeight()
        webView.setPadding(0, statusBarHeight, 0, 0)
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            // Manually trigger exit fullscreen
            if (customView != null && customViewCallback != null) {
                onHideCustomView()
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
            onHideCustomView()
        }
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        if (isFullscreen) {
            enterFullscreenMode()
        }
    }

    override fun onDestroy() {
        if (webView != null) {
            webView.loadData("", "text/html", "utf-8")
            webView.clearHistory()
            webView.clearCache(true)
            webView.destroy()
        }
        super.onDestroy()
    }
}