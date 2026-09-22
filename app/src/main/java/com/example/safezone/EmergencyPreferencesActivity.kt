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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_emergency_preferences
        )


        // ==============================
        // FIND VIEWS
        // ==============================

        tvBack =
            findViewById(R.id.tvBack)

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


        // ==============================
        // BACK
        // ==============================

        tvBack.setOnClickListener {
            finish()
        }

        btnBackToSettings.setOnClickListener {
            finish()
        }


        // ==============================
        // SAVE PREFERENCES
        // ==============================

        btnSavePreferences.setOnClickListener {

            val emergencyNumber =
                etEmergencyNumber.text.toString().trim()


            // ==============================
            // VALIDATE NUMBER
            // ==============================

            if (emergencyNumber.isEmpty()) {

                etEmergencyNumber.error =
                    "Please enter an emergency number"

                etEmergencyNumber.requestFocus()

                return@setOnClickListener
            }


            if (emergencyNumber.length < 3) {

                etEmergencyNumber.error =
                    "Please enter a valid emergency number"

                etEmergencyNumber.requestFocus()

                return@setOnClickListener
            }


            // ==============================
            // READ SETTINGS
            // ==============================

            val emergencyServicesEnabled =
                switchEmergencyServices.isChecked

            val locationEnabled =
                switchEmergencyLocation.isChecked

            val trustedContactsEnabled =
                switchTrustedContactAlerts.isChecked


            // ==============================
            // SAVE
            // ==============================

            val preferences =
                getSharedPreferences(
                    "SafeZonePreferences",
                    MODE_PRIVATE
                )

            preferences.edit()
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
                .apply()


            // ==============================
            // SUCCESS
            // ==============================

            Toast.makeText(
                this,
                "Emergency preferences saved.",
                Toast.LENGTH_LONG
            ).show()
        }


        // ==============================
        // LOAD SAVED PREFERENCES
        // ==============================

        loadPreferences()
    }


    private fun loadPreferences() {

        val preferences =
            getSharedPreferences(
                "SafeZonePreferences",
                MODE_PRIVATE
            )


        switchEmergencyServices.isChecked =
            preferences.getBoolean(
                "emergencyServices",
                true
            )


        switchEmergencyLocation.isChecked =
            preferences.getBoolean(
                "emergencyLocation",
                true
            )


        switchTrustedContactAlerts.isChecked =
            preferences.getBoolean(
                "trustedContactAlerts",
                true
            )


        etEmergencyNumber.setText(
            preferences.getString(
                "emergencyNumber",
                "112"
            )
        )
    }
}