package com.elcavz.elcavzflixter

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """
            {
                "platform": "android",
                "version": "${android.os.Build.VERSION.RELEASE}",
                "model": "${android.os.Build.MODEL}",
                "manufacturer": "${android.os.Build.MANUFACTURER}",
                "sdk": "${android.os.Build.VERSION.SDK_INT}"
            }
        """.trimIndent()
    }

    @JavascriptInterface
    fun echo(message: String): String {
        return "Android received: $message"
    }

    @JavascriptInterface
    fun openFacebookLink(url: String) {
        if (context is MainActivity) {
            (context as MainActivity).runOnUiThread {
                (context as MainActivity).openFacebookUrl(url)
            }
        }
    }

    @JavascriptInterface
    fun openExternalLink(url: String) {
        if (context is MainActivity) {
            (context as MainActivity).runOnUiThread {
                (context as MainActivity).openInBrowser(url)
            }
        }
    }

    @JavascriptInterface
    fun setStatusBarColor(color: String) {
        if (context is MainActivity) {
            (context as MainActivity).runOnUiThread {
                try {
                    val parsedColor = Color.parseColor(convertToHex(color))
                    (context as MainActivity).window.statusBarColor = parsedColor

                    // Adjust icon color
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val isDark = isColorDark(parsedColor)
                        var flags = (context as MainActivity).window.decorView.systemUiVisibility
                        if (isDark) {
                            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        } else {
                            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                        }
                        (context as MainActivity).window.decorView.systemUiVisibility = flags
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun convertToHex(color: String): String {
        return when {
            color.startsWith("rgb") -> {
                val values = color.replace("rgb(", "").replace(")", "").split(",")
                val r = values[0].trim().toInt()
                val g = values[1].trim().toInt()
                val b = values[2].trim().toInt()
                String.format("#%02X%02X%02X", r, g, b)
            }
            color.startsWith("rgba") -> {
                val values = color.replace("rgba(", "").replace(")", "").split(",")
                val r = values[0].trim().toInt()
                val g = values[1].trim().toInt()
                val b = values[2].trim().toInt()
                String.format("#%02X%02X%02X", r, g, b)
            }
            else -> color
        }
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
}