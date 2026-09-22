package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampusSafetyActivity : AppCompatActivity() {

    // ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // ==========================================
    // HEADER
    // ==========================================

    private lateinit var tvNotification: TextView
    private lateinit var tvProfile: TextView

    // ==========================================
    // CAMPUS INFORMATION
    // ==========================================

    private lateinit var tvWelcome: TextView
    private lateinit var tvCampusName: TextView
    private lateinit var tvCampusStatus: TextView

    // ==========================================
    // MAIN ACTION BUTTONS
    // ==========================================

    private lateinit var btnReportCampusIncident: AppCompatButton
    private lateinit var btnCampusEmergency: AppCompatButton
    private lateinit var btnCampusSafetyMap: AppCompatButton
    private lateinit var btnCampusAlerts: AppCompatButton
    private lateinit var btnMyCampusReports: AppCompatButton

    // ==========================================
    // BOTTOM NAVIGATION
    // ==========================================

    private lateinit var navHome: TextView
    private lateinit var navMap: TextView
    private lateinit var navReports: TextView
    private lateinit var navNotifications: TextView
    private lateinit var navProfile: TextView

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_home
        )

        // ==========================================
        // INITIALISE FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()

        db = FirebaseFirestore.getInstance()

        // ==========================================
        // CONNECT HEADER
        // ==========================================

        tvNotification =
            findViewById(R.id.tvNotification)

        tvProfile =
            findViewById(R.id.tvProfile)

        // ==========================================
        // CONNECT CAMPUS INFORMATION
        // ==========================================

        tvWelcome =
            findViewById(R.id.tvWelcome)

        tvCampusName =
            findViewById(R.id.tvCampusName)

        tvCampusStatus =
            findViewById(R.id.tvCampusStatus)

        // ==========================================
        // CONNECT MAIN BUTTONS
        // ==========================================

        btnReportCampusIncident =
            findViewById(R.id.btnReportCampusIncident)

        btnCampusEmergency =
            findViewById(R.id.btnCampusEmergency)

        btnCampusSafetyMap =
            findViewById(R.id.btnCampusSafetyMap)

        btnCampusAlerts =
            findViewById(R.id.btnCampusAlerts)

        btnMyCampusReports =
            findViewById(R.id.btnMyCampusReports)

        // ==========================================
        // CONNECT BOTTOM NAVIGATION
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
        // LOAD CAMPUS USER
        // ==========================================

        loadCampusUser()

        // ==========================================
        // HEADER - NOTIFICATIONS
        // ==========================================

        tvNotification.setOnClickListener {

            Toast.makeText(
                this,
                "Campus alerts will appear here.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // HEADER - PROFILE
        // ==========================================

        tvProfile.setOnClickListener {

            Toast.makeText(
                this,
                "Campus profile.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // REPORT CAMPUS INCIDENT
        // ==========================================

        btnReportCampusIncident.setOnClickListener {

            Toast.makeText(
                this,
                "Campus incident reporting selected.",
                Toast.LENGTH_SHORT
            ).show()

            /*
             * We will connect this to the Campus
             * Incident Report screen once that
             * screen is created.
             */
        }

        // ==========================================
        // CAMPUS EMERGENCY
        // ==========================================

        btnCampusEmergency.setOnClickListener {

            Toast.makeText(
                this,
                "Campus Emergency selected.",
                Toast.LENGTH_SHORT
            ).show()

            /*
             * Emergency functionality can be
             * connected here.
             */
        }

        // ==========================================
        // CAMPUS SAFETY MAP
        // ==========================================

        btnCampusSafetyMap.setOnClickListener {

            Toast.makeText(
                this,
                "Campus Safety Map selected.",
                Toast.LENGTH_SHORT
            ).show()

            /*
             * We will connect this to the campus
             * map screen.
             */
        }

        // ==========================================
        // CAMPUS ALERTS
        // ==========================================

        btnCampusAlerts.setOnClickListener {

            Toast.makeText(
                this,
                "Campus Alerts selected.",
                Toast.LENGTH_SHORT
            ).show()

            /*
             * We will connect this to the Firebase
             * CampusAlerts collection.
             */
        }

        // ==========================================
        // MY CAMPUS REPORTS
        // ==========================================

        btnMyCampusReports.setOnClickListener {

            Toast.makeText(
                this,
                "My Campus Reports selected.",
                Toast.LENGTH_SHORT
            ).show()

            /*
             * We will connect this to the user's
             * campus reports.
             */
        }

        // ==========================================
        // BOTTOM NAV - HOME
        // ==========================================

        navHome.setOnClickListener {

            Toast.makeText(
                this,
                "You are already on Campus Home.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // BOTTOM NAV - MAP
        // ==========================================

        navMap.setOnClickListener {

            Toast.makeText(
                this,
                "Campus Safety Map selected.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // BOTTOM NAV - REPORTS
        // ==========================================

        navReports.setOnClickListener {

            Toast.makeText(
                this,
                "My Campus Reports selected.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // BOTTOM NAV - ALERTS
        // ==========================================

        navNotifications.setOnClickListener {

            Toast.makeText(
                this,
                "Campus Alerts selected.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // BOTTOM NAV - PROFILE
        // ==========================================

        navProfile.setOnClickListener {

            Toast.makeText(
                this,
                "Campus Profile selected.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==================================================
    // LOAD CAMPUS USER
    // ==================================================

    private fun loadCampusUser() {

        val currentUser =
            auth.currentUser

        // ==========================================
        // IF USER IS LOGGED IN
        // ==========================================

        if (currentUser != null) {

            db.collection("Users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->

                    if (document.exists()) {

                        // ------------------------------
                        // FULL NAME
                        // ------------------------------

                        val fullName =
                            document.getString("fullName")

                        if (!fullName.isNullOrEmpty()) {

                            tvWelcome.text =
                                "Welcome, $fullName"
                        }

                        // ------------------------------
                        // CAMPUS
                        // ------------------------------

                        val campus =
                            document.getString("campus")

                        if (!campus.isNullOrEmpty()) {

                            tvCampusName.text =
                                campus
                        }

                        // ------------------------------
                        // ACCOUNT TYPE
                        // ------------------------------

                        val accountType =
                            document.getString("accountType")

                        if (
                            accountType != null &&
                            accountType.equals(
                                "Campus",
                                ignoreCase = true
                            )
                        ) {

                            tvCampusStatus.text =
                                "●  Status: Available"
                        }
                    }
                }
                .addOnFailureListener {

                    // Keep default dashboard
                    // information if Firestore
                    // cannot be reached.

                    tvWelcome.text =
                        "Welcome, Learner"

                    tvCampusName.text =
                        "Pretoria Campus"

                    tvCampusStatus.text =
                        "●  Status: Available"
                }

        } else {

            // ==========================================
            // NO FIREBASE USER
            // ==========================================

            tvWelcome.text =
                "Welcome, Learner"

            tvCampusName.text =
                "Pretoria Campus"

            tvCampusStatus.text =
                "●  Status: Available"
        }
    }

    // ==================================================
    // OPEN ACTIVITY HELPER
    // ==================================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        try {

            val intent =
                Intent(
                    this,
                    activityClass
                )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open this screen.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}