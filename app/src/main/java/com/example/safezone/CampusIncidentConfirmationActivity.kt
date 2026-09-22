package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampusIncidentConfirmationActivity : AppCompatActivity() {

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var tvBack: ImageView

    private lateinit var tvReportSubmitted: TextView

    private lateinit var tvIncidentId: TextView
    private lateinit var tvIncidentType: TextView
    private lateinit var tvCampus: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTime: TextView

    private lateinit var btnTrackReport: AppCompatButton
    private lateinit var btnBackToCampusHome: AppCompatButton

    // =========================================================
    // DATA
    // =========================================================

    private var reportId: String = ""
    private var incidentType: String = ""
    private var campus: String = ""
    private var location: String = ""
    private var dateTime: String = ""

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_incident_confirmation
        )

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize screen
        initializeViews()

        // Get submitted report information
        getReportData()

        // Setup buttons
        setupButtons()

        // Setup modern Android back navigation
        setupBackNavigation()
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        tvBack =
            findViewById(R.id.tvBack)

        tvReportSubmitted =
            findViewById(R.id.tvReportSubmitted)

        tvIncidentId =
            findViewById(R.id.tvIncidentId)

        tvIncidentType =
            findViewById(R.id.tvIncidentType)

        tvCampus =
            findViewById(R.id.tvCampus)

        tvLocation =
            findViewById(R.id.tvLocation)

        tvTime =
            findViewById(R.id.tvTime)

        btnTrackReport =
            findViewById(R.id.btnTrackReport)

        btnBackToCampusHome =
            findViewById(R.id.btnBackToCampusHome)
    }

    // =========================================================
    // GET REPORT DATA
    // =========================================================

    private fun getReportData() {

        reportId =
            intent.getStringExtra("reportId")
                ?: ""

        incidentType =
            intent.getStringExtra("incidentType")
                ?: "Unknown"

        campus =
            intent.getStringExtra("campus")
                ?: "Unknown Campus"

        location =
            intent.getStringExtra("location")
                ?: "Not available"

        dateTime =
            intent.getStringExtra("dateTime")
                ?: "Not available"

        // =====================================================
        // DISPLAY REPORT INFORMATION
        // =====================================================

        tvIncidentId.text =
            createShortIncidentId(reportId)

        tvIncidentType.text =
            incidentType

        tvCampus.text =
            campus

        tvLocation.text =
            location

        tvTime.text =
            dateTime
    }

    // =========================================================
    // CREATE SHORT INCIDENT ID
    // =========================================================

    private fun createShortIncidentId(
        firestoreId: String
    ): String {

        if (firestoreId.isEmpty()) {
            return "SZ-REPORT"
        }

        val shortId =
            if (firestoreId.length > 6) {
                firestoreId.takeLast(6)
            } else {
                firestoreId
            }

        return "SZ-${shortId.uppercase()}"
    }

    // =========================================================
    // SETUP BUTTONS
    // =========================================================

    private fun setupButtons() {

        // =====================================================
        // BACK ARROW
        // =====================================================

        tvBack.setOnClickListener {

            goBackToCampusHome()
        }

        // =====================================================
        // TRACK REPORT
        // =====================================================

        btnTrackReport.setOnClickListener {

            trackReport()
        }

        // =====================================================
        // BACK TO CAMPUS HOME
        // =====================================================

        btnBackToCampusHome.setOnClickListener {

            goBackToCampusHome()
        }
    }

    // =========================================================
    // MODERN BACK NAVIGATION
    // =========================================================

    private fun setupBackNavigation() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    goBackToCampusHome()
                }
            }
        )
    }

    // =========================================================
    // TRACK REPORT
    // =========================================================

    private fun trackReport() {

        if (reportId.isEmpty()) {

            Toast.makeText(
                this,
                "Report information is unavailable.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                this,
                PersonalReportsActivity::class.java
            )

        intent.putExtra(
            "reportId",
            reportId
        )

        intent.putExtra(
            "campusReport",
            true
        )

        startActivity(intent)
    }

    // =========================================================
    // BACK TO CAMPUS HOME
    // =========================================================

    private fun goBackToCampusHome() {

        val intent =
            Intent(
                this,
                CampusHomeActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)

        finish()
    }
}