package com.example.safezone

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class CampusAboutSafeZoneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_about_safezone)

        setupButtons()
    }

    private fun setupButtons() {

        // Back arrow
        findViewById<View>(R.id.tvBack).setOnClickListener {
            finish()
        }

        // Back to Campus Settings
        findViewById<View>(R.id.btnBackToSettings).setOnClickListener {
            finish()
        }
    }
}