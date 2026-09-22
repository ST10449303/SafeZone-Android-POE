package com.example.safezone

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat

class CampusEmergencyPreferencesActivity : AppCompatActivity() {

    // =========================================================
    // UI COMPONENTS
    // =========================================================

    private lateinit var switchEmergencyServices: SwitchCompat
    private lateinit var switchEmergencyLocation: SwitchCompat
    private lateinit var switchTrustedContactAlerts: SwitchCompat
    private lateinit var etEmergencyNumber: EditText

    private lateinit var btnSavePreferences: AppCompatButton
    private lateinit var btnBackToSettings: AppCompatButton


    // =========================================================
    // SHARED PREFERENCES
    // =========================================================

    private val preferencesName = "SafeZoneCampusPreferences"


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_emergency_preferences
        )

        initializeViews()

        loadPreferences()

        setupButtons()
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        switchEmergencyServices =
            findViewById(R.id.switchEmergencyServices)

        switchEmergencyLocation =
            findViewById(R.id.switchEmergencyLocation)

        switchTrustedContactAlerts =
            findViewById(R.id.switchTrustedContactAlerts)

        etEmergencyNumber =
            findViewById(R.id.etEmergencyNumber)

        btnSavePreferences =
            findViewById(R.id.btnSavePreferences)

        btnBackToSettings =
            findViewById(R.id.btnBackToSettings)
    }


    // =========================================================
    // LOAD SAVED PREFERENCES
    // =========================================================

    private fun loadPreferences() {

        val preferences = getSharedPreferences(
            preferencesName,
            MODE_PRIVATE
        )

        // Emergency Services
        switchEmergencyServices.isChecked =
            preferences.getBoolean(
                "emergencyServices",
                true
            )

        // Emergency Location
        switchEmergencyLocation.isChecked =
            preferences.getBoolean(
                "emergencyLocation",
                true
            )

        // Trusted Contacts
        switchTrustedContactAlerts.isChecked =
            preferences.getBoolean(
                "trustedContactAlerts",
                true
            )

        // Emergency Number
        etEmergencyNumber.setText(
            preferences.getString(
                "emergencyNumber",
                "112"
            )
        )
    }


    // =========================================================
    // SETUP BUTTONS
    // =========================================================

    private fun setupButtons() {

        // -----------------------------------------------------
        // BACK ARROW
        // -----------------------------------------------------

        findViewById<View>(R.id.tvBack).setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // SAVE PREFERENCES
        // -----------------------------------------------------

        btnSavePreferences.setOnClickListener {

            savePreferences()
        }


        // -----------------------------------------------------
        // BACK TO SETTINGS
        // -----------------------------------------------------

        btnBackToSettings.setOnClickListener {

            finish()
        }
    }


    // =========================================================
    // SAVE PREFERENCES
    // =========================================================

    private fun savePreferences() {

        val emergencyNumber =
            etEmergencyNumber.text
                .toString()
                .trim()


        // -----------------------------------------------------
        // VALIDATE EMERGENCY NUMBER
        // -----------------------------------------------------

        if (emergencyNumber.isEmpty()) {

            etEmergencyNumber.error =
                "Please enter an emergency number"

            etEmergencyNumber.requestFocus()

            return
        }


        if (emergencyNumber.length < 3) {

            etEmergencyNumber.error =
                "Please enter a valid emergency number"

            etEmergencyNumber.requestFocus()

            return
        }


        // -----------------------------------------------------
        // SAVE TO DEVICE
        // -----------------------------------------------------

        val preferences = getSharedPreferences(
            preferencesName,
            MODE_PRIVATE
        )

        preferences.edit()
            .putBoolean(
                "emergencyServices",
                switchEmergencyServices.isChecked
            )
            .putBoolean(
                "emergencyLocation",
                switchEmergencyLocation.isChecked
            )
            .putBoolean(
                "trustedContactAlerts",
                switchTrustedContactAlerts.isChecked
            )
            .putString(
                "emergencyNumber",
                emergencyNumber
            )
            .apply()


        // -----------------------------------------------------
        // SUCCESS MESSAGE
        // -----------------------------------------------------

        Toast.makeText(
            this,
            "Emergency preferences saved successfully.",
            Toast.LENGTH_SHORT
        ).show()
    }
}