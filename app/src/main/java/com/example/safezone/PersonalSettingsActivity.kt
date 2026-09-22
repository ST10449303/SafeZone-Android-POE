package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth

class PersonalSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var btnEditProfile: AppCompatButton
    private lateinit var btnChangePassword: AppCompatButton
    private lateinit var btnLogout: AppCompatButton

    private lateinit var btnTrustedContacts: AppCompatButton
    private lateinit var btnLocationSharing: AppCompatButton
    private lateinit var btnEmergencyPreferences: AppCompatButton

    private lateinit var switchPushNotifications: SwitchCompat
    private lateinit var switchSafetyAlerts: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_personal_settings)

        auth = FirebaseAuth.getInstance()

        // ==============================
        // FIND VIEWS
        // ==============================

        val tvBack =
            findViewById<TextView>(R.id.tvBack)

        btnEditProfile =
            findViewById(R.id.btnEditProfile)

        btnChangePassword =
            findViewById(R.id.btnChangePassword)

        btnLogout =
            findViewById(R.id.btnLogout)

        btnTrustedContacts =
            findViewById(R.id.btnTrustedContacts)

        btnLocationSharing =
            findViewById(R.id.btnLocationSharing)

        btnEmergencyPreferences =
            findViewById(R.id.btnEmergencyPreferences)

        switchPushNotifications =
            findViewById(R.id.switchPushNotifications)

        switchSafetyAlerts =
            findViewById(R.id.switchSafetyAlerts)


        // ==============================
        // BACK
        // ==============================

        tvBack.setOnClickListener {
            finish()
        }


        // ==============================
        // EDIT PROFILE
        // ==============================

        btnEditProfile.setOnClickListener {

            try {

                startActivity(
                    Intent(
                        this,
                        PersonalProfileActivity::class.java
                    )
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Profile screen is not available yet.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // ==============================
        // CHANGE PASSWORD
        // ==============================

        btnChangePassword.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ChangePasswordActivity::class.java
                )
            )
        }


        // ==============================
        // LOGOUT
        // ==============================

        btnLogout.setOnClickListener {

            auth.signOut()

            Toast.makeText(
                this,
                "You have been logged out.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(
                this,
                ChoosePanelActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }


        // ==============================
        // TRUSTED CONTACTS
        // ==============================

        btnTrustedContacts.setOnClickListener {

            try {

                startActivity(
                    Intent(
                        this,
                        TrustedContactsActivity::class.java
                    )
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Trusted Contacts screen is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // ==============================
        // LOCATION SHARING
        // ==============================

        btnLocationSharing.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LocationSharingActivity::class.java
                )
            )
        }


        // ==============================
        // EMERGENCY PREFERENCES
        // ==============================

        btnEmergencyPreferences.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    EmergencyPreferencesActivity::class.java
                )
            )
        }


        // ==============================
        // PUSH NOTIFICATIONS
        // ==============================

        switchPushNotifications.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {

                Toast.makeText(
                    this,
                    "Push notifications enabled.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Push notifications disabled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // ==============================
        // SAFETY CHECK ALERTS
        // ==============================

        switchSafetyAlerts.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {

                Toast.makeText(
                    this,
                    "Safety check alerts enabled.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Safety check alerts disabled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}