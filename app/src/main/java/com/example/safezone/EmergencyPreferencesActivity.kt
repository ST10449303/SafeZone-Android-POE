
package com.example.safezone

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat

class EmergencyPreferencesActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView

    private lateinit var switchEmergencyServices: SwitchCompat
    private lateinit var switchEmergencyLocation: SwitchCompat
    private lateinit var switchTrustedContactAlerts: SwitchCompat

    private lateinit var etEmergencyNumber: EditText

    private lateinit var btnSavePreferences: AppCompatButton
    private lateinit var btnBackToSettings: AppCompatButton

    // Name of the local preferences file.
    private val preferencesName = "SafeZonePreferences"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_preferences)

        // Connect XML views.
        tvBack = findViewById(R.id.tvBack)

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

        // Return to the previous screen.
        tvBack.setOnClickListener {
            finish()
        }

        btnBackToSettings.setOnClickListener {
            finish()
        }

        // Restore previously saved preferences.
        loadPreferences()

        // Save changes when the user taps the save button.
        btnSavePreferences.setOnClickListener {
            savePreferences()
        }
    }

    // =====================================================
    // SAVE EMERGENCY PREFERENCES
    // =====================================================

    private fun savePreferences() {

        val emergencyNumber =
            etEmergencyNumber.text.toString().trim()

        // Ensure an emergency number has been entered.
        if (emergencyNumber.isBlank()) {
            etEmergencyNumber.error =
                getString(
                    R.string.emergency_preferences_number_required
                )
            etEmergencyNumber.requestFocus()
            return
        }

        // Check the minimum number length.
        if (emergencyNumber.length < 3) {
            etEmergencyNumber.error =
                getString(
                    R.string.emergency_preferences_number_invalid
                )
            etEmergencyNumber.requestFocus()
            return
        }

        // Allow digits, spaces, plus signs, hyphens and parentheses.
        if (!emergencyNumber.matches(Regex("[0-9+() -]+"))) {
            etEmergencyNumber.error =
                getString(
                    R.string.emergency_preferences_number_format
                )
            etEmergencyNumber.requestFocus()
            return
        }

        // Read the current switch values.
        val emergencyServicesEnabled =
            switchEmergencyServices.isChecked

        val locationEnabled =
            switchEmergencyLocation.isChecked

        val trustedContactsEnabled =
            switchTrustedContactAlerts.isChecked

        // Save locally. No internet connection is required.
        val preferences = getSharedPreferences(
            preferencesName,
            MODE_PRIVATE
        )

        val saved = preferences.edit()
            .putBoolean(
                "emergencyServices",
                emergencyServicesEnabled
            )
            .putBoolean(
                "emergencyLocation",
                locationEnabled
            )
            .putBoolean(
                "trustedContactAlerts",
                trustedContactsEnabled
            )
            .putString(
                "emergencyNumber",
                emergencyNumber
            )
            .commit()

        // Display a localized result.
        if (saved) {
            Toast.makeText(
                this,
                getString(
                    R.string.emergency_preferences_saved
                ),
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                getString(
                    R.string.emergency_preferences_save_failed
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // LOAD SAVED EMERGENCY PREFERENCES
    // =====================================================

    private fun loadPreferences() {

        val preferences = getSharedPreferences(
            preferencesName,
            MODE_PRIVATE
        )

        // Default to enabled if no preference has been saved.
        switchEmergencyServices.isChecked =
            preferences.getBoolean("emergencyServices", true)

        switchEmergencyLocation.isChecked =
            preferences.getBoolean("emergencyLocation", true)

        switchTrustedContactAlerts.isChecked =
            preferences.getBoolean("trustedContactAlerts", true)

        // Default emergency number is 112.
        etEmergencyNumber.setText(
            preferences.getString("emergencyNumber", "112")
        )
    }
}
