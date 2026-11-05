package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ChoosePanelActivity : AppCompatActivity() {

    private lateinit var cardPersonalSafety: CardView
    private lateinit var cardCampusSafety: CardView
    private lateinit var tvEmergency: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_choose_panel)

        // Connect XML components
        cardPersonalSafety = findViewById(R.id.cardPersonalSafety)
        cardCampusSafety = findViewById(R.id.cardCampusSafety)
        tvEmergency = findViewById(R.id.tvEmergency)

        // ================================
        // PERSONAL SAFETY
        // ================================

        cardPersonalSafety.setOnClickListener {
            startActivity(
                Intent(this, PersonalLoginActivity::class.java)
            )
        }

        // ================================
        // CAMPUS SAFETY
        // ================================

        cardCampusSafety.setOnClickListener {
            startActivity(
                Intent(this, CampusLoginActivity::class.java)
            )
        }

        // ================================
        // EMERGENCY ASSISTANCE
        // ================================

        tvEmergency.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.emergency_assistance_selected),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}