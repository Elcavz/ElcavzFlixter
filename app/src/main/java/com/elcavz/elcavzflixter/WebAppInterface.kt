package com.elcavz.elcavzflixter

import android.content.Context
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
}