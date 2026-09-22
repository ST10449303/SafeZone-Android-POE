package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class CampusSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_settings)

        // =====================================================
        // FIREBASE AUTHENTICATION
        // =====================================================

        auth = FirebaseAuth.getInstance()

        setupButtons()
    }


    // =========================================================
    // SETUP BUTTONS
    // =========================================================

    private fun setupButtons() {

        // =====================================================
        // BACK BUTTON
        // =====================================================

        findViewById<View>(R.id.tvBack).setOnClickListener {

            finish()
        }


        // =====================================================
        // ACCOUNT
        // =====================================================

        // -----------------------------------------------------
        // EDIT PROFILE
        // -----------------------------------------------------

        findViewById<View>(R.id.rowEditProfile).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    EditProfileActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Edit Profile is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // CHANGE PASSWORD
        // -----------------------------------------------------

        findViewById<View>(R.id.rowChangePassword).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusChangePasswordActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Change Password screen is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // LOGOUT
        // -----------------------------------------------------

        findViewById<View>(R.id.rowLogout).setOnClickListener {

            logoutUser()
        }


        // =====================================================
        // CAMPUS SAFETY
        // =====================================================

        // -----------------------------------------------------
        // CAMPUS ALERTS
        // -----------------------------------------------------

        findViewById<View>(R.id.rowCampusAlerts).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusAlertsActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Campus Alerts are not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // EMERGENCY PREFERENCES
        // -----------------------------------------------------

        findViewById<View>(R.id.rowEmergencyPreferences).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusEmergencyPreferencesActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Emergency Preferences are not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // LOCATION SHARING
        // -----------------------------------------------------

        findViewById<View>(R.id.rowLocationSharing).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusLocationSharingActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Location Sharing is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // =====================================================
        // SUPPORT
        // =====================================================

        // -----------------------------------------------------
        // HELP & SUPPORT
        // -----------------------------------------------------

        findViewById<View>(R.id.rowHelpSupport).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusHelpSupportActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Help & Support is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // ABOUT SAFEZONE
        // -----------------------------------------------------

        findViewById<View>(R.id.rowAboutSafeZone).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusAboutSafeZoneActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "About SafeZone is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        findViewById<View>(R.id.navHome).setOnClickListener {

            val intent = Intent(
                this,
                CampusHomeActivity::class.java
            )

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(intent)

            finish()
        }


        // -----------------------------------------------------
        // MAP
        // -----------------------------------------------------

        findViewById<View>(R.id.navMap).setOnClickListener {

            openCampusMap()
        }


        // -----------------------------------------------------
        // REPORTS
        // -----------------------------------------------------

        // IMPORTANT:
        // Campus users now use CampusReportsActivity.
        // This no longer opens PersonalReportsActivity.

        findViewById<View>(R.id.navReports).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusReportsActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Campus Reports are not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // ALERTS
        // -----------------------------------------------------

        findViewById<View>(R.id.navNotifications).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusAlertsActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Campus Alerts are not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // PROFILE
        // -----------------------------------------------------

        findViewById<View>(R.id.navProfile).setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    CampusProfileActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Profile is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    private fun logoutUser() {

        // Sign out from Firebase Authentication
        auth.signOut()

        Toast.makeText(
            this,
            "You have been logged out.",
            Toast.LENGTH_SHORT
        ).show()

        // Go back to Choose Panel
        val intent = Intent(
            this,
            ChoosePanelActivity::class.java
        )

        // Clear all previous screens
        // so the user cannot press Back and return
        // to the Campus section after logging out
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }


    // =========================================================
    // CAMPUS MAP
    // =========================================================

    private fun openCampusMap() {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "geo:0,0?q=Pretoria Campus"
                )
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Map is not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}