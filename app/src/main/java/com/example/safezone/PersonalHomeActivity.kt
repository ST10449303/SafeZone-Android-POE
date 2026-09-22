package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalHomeActivity : AppCompatActivity() {

    // ==========================================
    // HEADER
    // ==========================================

    private lateinit var tvNotification: TextView
    private lateinit var tvProfile: TextView
    private lateinit var tvLogout: TextView
    private lateinit var tvUserName: TextView

    // ==========================================
    // QUICK ACTIONS
    // ==========================================

    private lateinit var btnSOS: Button
    private lateinit var btnEmergencyAssistance: Button
    private lateinit var btnSafetyCheckIn: Button
    private lateinit var btnTrustedContacts: Button
    private lateinit var btnFindHelp: Button
    private lateinit var btnReportIncident: Button
    private lateinit var btnSettings: Button

    // ==========================================
    // BOTTOM NAVIGATION
    // ==========================================

    private lateinit var navHome: TextView
    private lateinit var navMap: TextView
    private lateinit var navReports: TextView
    private lateinit var navNotifications: TextView
    private lateinit var navProfile: TextView

    // ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_personal_home
        )

        // ==========================================
        // FIREBASE INITIALIZATION
        // ==========================================

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()

        // ==========================================
        // HEADER
        // ==========================================

        tvNotification =
            findViewById(R.id.tvNotification)

        tvProfile =
            findViewById(R.id.tvProfile)

        tvLogout =
            findViewById(R.id.tvLogout)

        tvUserName =
            findViewById(R.id.tvUserName)

        // ==========================================
        // QUICK ACTIONS
        // ==========================================

        btnSOS =
            findViewById(R.id.btnSOS)

        btnEmergencyAssistance =
            findViewById(R.id.btnEmergencyAssistance)

        btnSafetyCheckIn =
            findViewById(R.id.btnSafetyCheckIn)

        btnTrustedContacts =
            findViewById(R.id.btnTrustedContacts)

        btnFindHelp =
            findViewById(R.id.btnFindHelp)

        btnReportIncident =
            findViewById(R.id.btnReportIncident)

        btnSettings =
            findViewById(R.id.btnSettings)

        // ==========================================
        // BOTTOM NAVIGATION
        // ==========================================

        navHome =
            findViewById(R.id.navHome)

        navMap =
            findViewById(R.id.navMap)

        navReports =
            findViewById(R.id.navReports)

        navNotifications =
            findViewById(R.id.navNotifications)

        navProfile =
            findViewById(R.id.navProfile)

        // ==========================================
        // LOGIN CHECK
        // ==========================================

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            goToChoosePanel()

            return
        }

        // ==========================================
        // LOAD USER NAME
        // ==========================================

        loadUserName(
            currentUser.uid
        )

        // ==========================================
        // SOS
        // ==========================================

        btnSOS.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SOSConfirmationActivity::class.java
                )
            )
        }

        // ==========================================
        // EMERGENCY ASSISTANCE
        // ==========================================

        btnEmergencyAssistance.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    EmergencyAssistanceActivity::class.java
                )
            )
        }

        // ==========================================
        // SAFETY CHECK-IN
        // ==========================================

        btnSafetyCheckIn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SafetyCheckInActivity::class.java
                )
            )
        }

        // ==========================================
        // TRUSTED CONTACTS
        // ==========================================

        btnTrustedContacts.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TrustedContactsActivity::class.java
                )
            )
        }

        // ==========================================
        // FIND HELP
        // ==========================================

        btnFindHelp.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FindHelpActivity::class.java
                )
            )
        }

        // ==========================================
        // REPORT INCIDENT
        // ==========================================

        btnReportIncident.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalIncidentReportActivity::class.java
                )
            )
        }

        // ==========================================
        // SETTINGS
        // ==========================================

        btnSettings.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalSettingsActivity::class.java
                )
            )
        }

        // ==========================================
        // NOTIFICATIONS / ALERTS
        // ==========================================

        /*
         * The notification icon now opens the
         * Personal Safety Alerts screen.
         */

        tvNotification.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalAlertsActivity::class.java
                )
            )
        }

        // ==========================================
        // PROFILE
        // ==========================================

        tvProfile.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalProfileActivity::class.java
                )
            )
        }

        // ==========================================
        // LOGOUT
        // ==========================================

        tvLogout.setOnClickListener {

            logoutUser()
        }

        // ==========================================
        // HOME
        // ==========================================

        navHome.setOnClickListener {

            Toast.makeText(
                this,
                "You are already on Home.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // MAP
        // ==========================================

        navMap.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FindHelpActivity::class.java
                )
            )
        }

        // ==========================================
        // REPORTS
        // ==========================================

        navReports.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalReportsActivity::class.java
                )
            )
        }

        // ==========================================
        // ALERTS
        // ==========================================

        /*
         * The bottom navigation previously displayed
         * a Toast saying there were no alerts.
         *
         * It now opens the actual Personal Alerts screen.
         */

        navNotifications.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalAlertsActivity::class.java
                )
            )
        }

        // ==========================================
        // PROFILE
        // ==========================================

        navProfile.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalProfileActivity::class.java
                )
            )
        }
    }

    // ==========================================
    // LOAD USER NAME
    // ==========================================

    private fun loadUserName(
        userId: String
    ) {

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val fullName =
                        document.getString(
                            "fullName"
                        )

                    tvUserName.text =
                        if (!fullName.isNullOrEmpty()) {

                            fullName

                        } else {

                            "SafeZone User"
                        }

                } else {

                    tvUserName.text =
                        "SafeZone User"
                }
            }
            .addOnFailureListener {

                tvUserName.text =
                    "SafeZone User"
            }
    }

    // ==========================================
    // LOGOUT
    // ==========================================

    private fun logoutUser() {

        auth.signOut()

        Toast.makeText(
            this,
            "You have been logged out.",
            Toast.LENGTH_SHORT
        ).show()

        goToChoosePanel()
    }

    // ==========================================
    // CHOOSE PANEL
    // ==========================================

    private fun goToChoosePanel() {

        val intent =
            Intent(
                this,
                ChoosePanelActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(
            intent
        )

        finish()
    }
}