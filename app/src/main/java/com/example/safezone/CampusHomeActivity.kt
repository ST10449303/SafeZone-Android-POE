package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampusHomeActivity : AppCompatActivity() {

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // =========================================================
    // USER INFORMATION
    // =========================================================

    private lateinit var tvWelcome: TextView
    private lateinit var tvCampusName: TextView
    private lateinit var tvCampusStatus: TextView

    // =========================================================
    // HEADER ICONS
    // =========================================================

    private lateinit var tvNotification: ImageView
    private lateinit var tvProfile: ImageView

    // =========================================================
    // CAMPUS HOME BUTTONS
    // =========================================================

    private lateinit var btnReportCampusIncident: AppCompatButton
    private lateinit var btnCampusEmergency: AppCompatButton
    private lateinit var btnCampusSafetyMap: AppCompatButton
    private lateinit var btnCampusAlerts: AppCompatButton
    private lateinit var btnMyCampusReports: AppCompatButton
    private lateinit var btnLogout: AppCompatButton

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_home)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Setup buttons
        setupButtons()

        // Modern Android back navigation
        setupBackNavigation()

        // Load campus user
        loadCampusUser()
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        // -----------------------------------------------------
        // USER INFORMATION
        // -----------------------------------------------------

        tvWelcome = findViewById(R.id.tvWelcome)

        tvCampusName = findViewById(R.id.tvCampusName)

        tvCampusStatus = findViewById(R.id.tvCampusStatus)

        // -----------------------------------------------------
        // HEADER ICONS
        // -----------------------------------------------------

        tvNotification = findViewById(R.id.tvNotification)

        tvProfile = findViewById(R.id.tvProfile)

        // -----------------------------------------------------
        // CAMPUS HOME ACTIONS
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // LOGOUT
        // -----------------------------------------------------

        btnLogout =
            findViewById(R.id.btnLogout)
    }

    // =========================================================
    // CHECK LOGIN
    // =========================================================

    private fun checkLogin(): Boolean {

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please login to continue.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(
                this,
                CampusLoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()

            return false
        }

        return true
    }

    // =========================================================
    // LOAD CAMPUS USER
    // =========================================================

    private fun loadCampusUser() {

        if (!checkLogin()) {
            return
        }

        val currentUser =
            auth.currentUser ?: return

        // -----------------------------------------------------
        // DEFAULT INFORMATION
        // -----------------------------------------------------

        tvWelcome.text =
            "Welcome, Learner"

        tvCampusName.text =
            "Campus"

        tvCampusStatus.text =
            "Status: Available"

        // -----------------------------------------------------
        // FIRESTORE
        // -----------------------------------------------------

        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Campus profile information not found.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                // -------------------------------------------------
                // FULL NAME
                // -------------------------------------------------

                val fullName =
                    document.getString("fullName")

                if (!fullName.isNullOrBlank()) {

                    val firstName =
                        getFirstName(fullName)

                    tvWelcome.text =
                        "Welcome, $firstName"
                }

                // -------------------------------------------------
                // CAMPUS
                // -------------------------------------------------

                val campus =
                    document.getString("campus")

                if (!campus.isNullOrBlank()) {

                    tvCampusName.text =
                        formatCampusName(campus)
                }

                // -------------------------------------------------
                // STATUS
                // -------------------------------------------------

                tvCampusStatus.text =
                    "Status: Available"
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to load campus information.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =========================================================
    // FORMAT CAMPUS NAME
    // =========================================================

    private fun formatCampusName(
        campus: String
    ): String {

        val cleanedCampus =
            campus.trim()

        if (cleanedCampus.isEmpty()) {
            return "Campus"
        }

        return if (
            cleanedCampus.endsWith(
                "Campus",
                ignoreCase = true
            )
        ) {
            cleanedCampus
        } else {
            "$cleanedCampus Campus"
        }
    }

    // =========================================================
    // GET FIRST NAME
    // =========================================================

    private fun getFirstName(
        fullName: String
    ): String {

        val cleanedName =
            fullName.trim()

        if (cleanedName.isEmpty()) {
            return "Learner"
        }

        return cleanedName
            .split("\\s+".toRegex())
            .firstOrNull()
            ?: "Learner"
    }

    // =========================================================
    // SETUP BUTTONS
    // =========================================================

    private fun setupButtons() {

        // =====================================================
        // HEADER NOTIFICATION
        // =====================================================

        tvNotification.setOnClickListener {

            openCampusAlerts()
        }

        // =====================================================
        // HEADER PROFILE
        // =====================================================

        tvProfile.setOnClickListener {

            openProfile()
        }

        // =====================================================
        // REPORT CAMPUS INCIDENT
        // =====================================================

        btnReportCampusIncident.setOnClickListener {

            openCampusIncidentReport()
        }

        // =====================================================
        // CAMPUS EMERGENCY
        // =====================================================

        btnCampusEmergency.setOnClickListener {

            openCampusEmergency()
        }

        // =====================================================
        // CAMPUS SAFETY MAP
        // =====================================================

        btnCampusSafetyMap.setOnClickListener {

            openCampusSafetyMap()
        }

        // =====================================================
        // CAMPUS ALERTS
        // =====================================================

        btnCampusAlerts.setOnClickListener {

            openCampusAlerts()
        }

        // =====================================================
        // MY CAMPUS REPORTS
        // =====================================================

        btnMyCampusReports.setOnClickListener {

            openCampusReports(false)
        }

        // =====================================================
        // LOGOUT
        // =====================================================

        btnLogout.setOnClickListener {

            logoutUser()
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        findViewById<View>(
            R.id.navHome
        ).setOnClickListener {

            // Already on Campus Home
        }

        // -----------------------------------------------------
        // MAP
        // -----------------------------------------------------

        findViewById<View>(
            R.id.navMap
        ).setOnClickListener {

            openCampusSafetyMap()
        }

        // -----------------------------------------------------
        // REPORTS
        // -----------------------------------------------------

        findViewById<View>(
            R.id.navReports
        ).setOnClickListener {

            openCampusReports(false)
        }

        // -----------------------------------------------------
        // ALERTS
        //
        // IMPORTANT:
        // Use navNotifications.
        // DO NOT use navAlerts.
        // -----------------------------------------------------

        findViewById<View>(
            R.id.navNotifications
        ).setOnClickListener {

            openCampusAlerts()
        }

        // -----------------------------------------------------
        // PROFILE
        // -----------------------------------------------------

        findViewById<View>(
            R.id.navProfile
        ).setOnClickListener {

            openProfile()
        }

        // =====================================================
        // OPTIONAL MANAGE REPORTS BUTTON
        // =====================================================
        //
        // If your Campus Home XML contains:
        //
        // @+id/btnManageReports
        //
        // this button will open CampusReportsActivity
        // in Manage Reports mode.
        //
        // If the button does not exist, nothing happens and
        // the app will continue normally.
        // =====================================================

        setupOptionalManageReportsButton()
    }

    // =========================================================
    // OPTIONAL MANAGE REPORTS BUTTON
    // =========================================================

    private fun setupOptionalManageReportsButton() {

        val possibleIds = listOf(
            "btnManageReports",
            "btnManageCampusReports"
        )

        for (idName in possibleIds) {

            val id = resources.getIdentifier(
                idName,
                "id",
                packageName
            )

            if (id != 0) {

                val manageButton =
                    findViewById<View>(id)

                manageButton?.setOnClickListener {

                    openCampusReports(true)
                }
            }
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private fun logoutUser() {

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

    // =========================================================
    // BACK NAVIGATION
    // =========================================================

    private fun setupBackNavigation() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    finish()
                }
            }
        )
    }

    // =========================================================
    // CAMPUS INCIDENT REPORT
    // =========================================================

    private fun openCampusIncidentReport() {

        try {

            val intent = Intent(
                this,
                CampusIncidentReportActivity::class.java
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Campus incident reporting is not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // CAMPUS EMERGENCY
    // =========================================================

    private fun openCampusEmergency() {

        try {

            val intent = Intent(
                this,
                CampusEmergencyActivity::class.java
            )

            startActivity(intent)

        } catch (e: Exception) {

            try {

                val dialIntent = Intent(
                    Intent.ACTION_DIAL
                )

                dialIntent.data =
                    Uri.parse("tel:112")

                startActivity(dialIntent)

            } catch (dialException: Exception) {

                Toast.makeText(
                    this,
                    "Emergency services are not available.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // CAMPUS SAFETY MAP
    // =========================================================

    private fun openCampusSafetyMap() {

        try {

            val intent = Intent(
                this,
                CampusSafetyMapActivity::class.java
            )

            startActivity(intent)

        } catch (e: Exception) {

            try {

                val mapIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "geo:0,0?q=Pretoria Campus"
                    )
                )

                startActivity(mapIntent)

            } catch (mapException: Exception) {

                Toast.makeText(
                    this,
                    "Campus safety map is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // CAMPUS ALERTS
    // =========================================================

    private fun openCampusAlerts() {

        try {

            val intent = Intent(
                this,
                CampusAlertsActivity::class.java
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Campus alerts are not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // CAMPUS REPORTS
    // =========================================================

    private fun openCampusReports(
        manageReports: Boolean
    ) {

        try {

            val intent = Intent(
                this,
                CampusReportsActivity::class.java
            )

            // -------------------------------------------------
            // false = My Campus Reports
            // true  = Manage Campus Reports
            // -------------------------------------------------

            intent.putExtra(
                "manageReports",
                manageReports
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Campus reports are not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // CAMPUS PROFILE
    // =========================================================

    private fun openProfile() {

        try {

            val intent = Intent(
                this,
                CampusProfileActivity::class.java
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Campus profile is not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (!::auth.isInitialized) {
            return
        }

        if (auth.currentUser == null) {

            checkLogin()

        } else {

            loadCampusUser()
        }
    }
}