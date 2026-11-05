package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class CampusIncidentConfirmationActivity : AppCompatActivity() {

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var tvBack: TextView
    private lateinit var tvReportSubmitted: TextView
    private lateinit var tvIncidentId: TextView
    private lateinit var tvIncidentType: TextView
    private lateinit var tvCampus: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTime: TextView

    private lateinit var btnTrackReport: AppCompatButton
    private lateinit var btnBackToCampusHome: AppCompatButton

    // =========================================================
    // REPORT INFORMATION
    // =========================================================

    private var reportId: String = ""
    private var incidentType: String = ""
    private var campus: String = ""
    private var location: String = ""
    private var dateTime: String = ""

    private var isOfflineReport: Boolean = false

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_incident_confirmation
        )

        // -----------------------------------------------------
        // INITIALISE VIEWS
        // -----------------------------------------------------

        tvBack = findViewById(R.id.tvBack)

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

        // -----------------------------------------------------
        // GET REPORT DETAILS
        // -----------------------------------------------------

        getReportDetails()

        // -----------------------------------------------------
        // DISPLAY CONFIRMATION
        // -----------------------------------------------------

        displayConfirmation()

        // -----------------------------------------------------
        // BACK BUTTON
        // -----------------------------------------------------

        tvBack.setOnClickListener {
            goBackToCampusHome()
        }

        // -----------------------------------------------------
        // TRACK REPORT
        // -----------------------------------------------------

        btnTrackReport.setOnClickListener {
            openCampusReports()
        }

        // -----------------------------------------------------
        // BACK TO CAMPUS HOME
        // -----------------------------------------------------

        btnBackToCampusHome.setOnClickListener {
            goBackToCampusHome()
        }
    }

    // =========================================================
    // GET REPORT DETAILS
    // =========================================================

    private fun getReportDetails() {

        reportId =
            intent.getStringExtra("reportId")
                ?: ""

        incidentType =
            intent.getStringExtra("incidentType")
                ?: "Incident Report"

        campus =
            intent.getStringExtra("campus")
                ?: "Campus not provided"

        location =
            intent.getStringExtra("location")
                ?: "Location not provided"

        dateTime =
            intent.getStringExtra("dateTime")
                ?: ""

        // -----------------------------------------------------
        // DETERMINE WHETHER THE REPORT IS OFFLINE
        // -----------------------------------------------------

        isOfflineReport =
            intent.getBooleanExtra(
                "offlineReport",
                false
            )
    }

    // =========================================================
    // DISPLAY CONFIRMATION
    // =========================================================

    private fun displayConfirmation() {

        // -----------------------------------------------------
        // REPORT ID
        // -----------------------------------------------------

        if (reportId.isNotBlank()) {

            val shortenedId =
                if (reportId.length > 6) {
                    reportId.takeLast(6)
                        .uppercase()
                } else {
                    reportId.uppercase()
                }

            tvIncidentId.text =
                "Incident ID: SZ-$shortenedId"

        } else {

            tvIncidentId.text =
                "Incident ID: Not available"
        }

        // -----------------------------------------------------
        // INCIDENT DETAILS
        // -----------------------------------------------------

        tvIncidentType.text =
            incidentType

        tvCampus.text =
            "Campus: $campus"

        tvLocation.text =
            "Location: $location"

        tvTime.text =
            if (dateTime.isNotBlank()) {
                "Date and time: $dateTime"
            } else {
                "Date and time: Not available"
            }

        // -----------------------------------------------------
        // ONLINE OR OFFLINE CONFIRMATION
        // -----------------------------------------------------

        if (isOfflineReport) {

            // ---------------------------------------------
            // OFFLINE REPORT
            // ---------------------------------------------

            tvReportSubmitted.text =
                "Report saved on this device"

            Toast.makeText(
                this,
                "Your report is saved and waiting to synchronise.",
                Toast.LENGTH_LONG
            ).show()

        } else {

            // ---------------------------------------------
            // ONLINE REPORT
            // ---------------------------------------------

            tvReportSubmitted.text =
                "Report submitted successfully"
        }
    }

    // =========================================================
    // OPEN CAMPUS REPORTS
    // =========================================================

    private fun openCampusReports() {

        val intent =
            Intent(
                this,
                CampusReportsActivity::class.java
            )

        startActivity(intent)
    }

    // =========================================================
    // RETURN TO CAMPUS HOME
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