package com.elcavz.elcavzflixter

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private var isWebsiteLoaded = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        startCheckingForWebsiteLoad()

        handler.postDelayed({
            if (!isWebsiteLoaded) {
                proceedToMainActivity()
            }
        }, 3000)
    }

    private fun startCheckingForWebsiteLoad() {
        // Check every 500ms if website is loaded
        handler.postDelayed({
            if (MainActivity.isWebsiteLoaded) {
                isWebsiteLoaded = true
                proceedToMainActivity()
            } else {
                startCheckingForWebsiteLoad() // Continue checking
            }
        }, 100)
    }

    private fun proceedToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onBackPressed() {
        // Disable back button during splash
         super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}