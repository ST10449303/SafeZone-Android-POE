package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampusSafetyMapActivity : AppCompatActivity() {

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // =========================================================
    // HEADER
    // =========================================================

    private lateinit var tvBack: ImageView
    private lateinit var tvNotification: ImageView

    // =========================================================
    // CAMPUS
    // =========================================================

    private lateinit var layoutCampusSelector: LinearLayout
    private lateinit var tvSelectedCampus: TextView
    private lateinit var tvCampusArrow: TextView

    // =========================================================
    // SEARCH
    // =========================================================

    private lateinit var etSearchLocation: EditText

    // =========================================================
    // MAP CONTROLS
    // =========================================================

    private lateinit var btnCompass: ImageView
    private lateinit var btnZoomIn: TextView
    private lateinit var btnZoomOut: TextView
    private lateinit var btnMyLocation: ImageView

    // =========================================================
    // DIRECTIONS
    // =========================================================

    private lateinit var btnGetDirections: AppCompatButton

    // =========================================================
    // CURRENT CAMPUS
    // =========================================================

    private var campusName = "Pretoria Campus"

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_safety_map)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupButtons()
        setupBackNavigation()
        loadCampusInformation()
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        tvBack = findViewById(R.id.tvBack)

        tvNotification =
            findViewById(R.id.tvNotification)

        layoutCampusSelector =
            findViewById(R.id.layoutCampusSelector)

        tvSelectedCampus =
            findViewById(R.id.tvSelectedCampus)

        tvCampusArrow =
            findViewById(R.id.tvCampusArrow)

        etSearchLocation =
            findViewById(R.id.etSearchLocation)

        btnCompass =
            findViewById(R.id.btnCompass)

        btnZoomIn =
            findViewById(R.id.btnZoomIn)

        btnZoomOut =
            findViewById(R.id.btnZoomOut)

        btnMyLocation =
            findViewById(R.id.btnMyLocation)

        btnGetDirections =
            findViewById(R.id.btnGetDirections)
    }

    // =========================================================
    // LOAD CAMPUS INFORMATION
    // =========================================================

    private fun loadCampusInformation() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            return
        }

        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    return@addOnSuccessListener
                }

                val campus =
                    document.getString("campus")

                if (!campus.isNullOrBlank()) {

                    campusName =
                        formatCampusName(campus)

                    tvSelectedCampus.text =
                        campusName
                }
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
    // FORMAT CAMPUS
    // =========================================================

    private fun formatCampusName(
        campus: String
    ): String {

        val cleaned =
            campus.trim()

        if (cleaned.isEmpty()) {
            return "Campus"
        }

        return if (
            cleaned.endsWith(
                "Campus",
                ignoreCase = true
            )
        ) {
            cleaned
        } else {
            "$cleaned Campus"
        }
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private fun setupButtons() {

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        tvBack.setOnClickListener {

            finish()
        }

        // -----------------------------------------------------
        // HEADER NOTIFICATIONS
        // -----------------------------------------------------

        tvNotification.setOnClickListener {

            openCampusAlerts()
        }

        // -----------------------------------------------------
        // CAMPUS SELECTOR
        // -----------------------------------------------------

        layoutCampusSelector.setOnClickListener {

            showCampusSelection()
        }

        // -----------------------------------------------------
        // SEARCH
        // -----------------------------------------------------

        etSearchLocation.setOnEditorActionListener {
                _, _, _ ->

            searchCampusLocation()

            true
        }

        // -----------------------------------------------------
        // COMPASS
        // -----------------------------------------------------

        btnCompass.setOnClickListener {

            Toast.makeText(
                this,
                "Map orientation reset.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // -----------------------------------------------------
        // ZOOM IN
        // -----------------------------------------------------

        btnZoomIn.setOnClickListener {

            Toast.makeText(
                this,
                "Zooming in.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // -----------------------------------------------------
        // ZOOM OUT
        // -----------------------------------------------------

        btnZoomOut.setOnClickListener {

            Toast.makeText(
                this,
                "Zooming out.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // -----------------------------------------------------
        // MY LOCATION
        // -----------------------------------------------------

        btnMyLocation.setOnClickListener {

            showCurrentLocation()
        }

        // -----------------------------------------------------
        // GET DIRECTIONS
        // -----------------------------------------------------

        btnGetDirections.setOnClickListener {

            openDirections()
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

            openCampusHome()
        }

        // -----------------------------------------------------
        // MAP
        // -----------------------------------------------------

        findViewById<View>(
            R.id.navMap
        ).setOnClickListener {

            // Already on Campus Safety Map
        }

        // -----------------------------------------------------
        // REPORTS
        // -----------------------------------------------------

        findViewById<View>(
            R.id.navReports
        ).setOnClickListener {

            openCampusReports()
        }

        // -----------------------------------------------------
        // ALERTS / NOTIFICATIONS
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

            openCampusProfile()
        }
    }

    // =========================================================
    // CAMPUS SELECTION
    // =========================================================

    private fun showCampusSelection() {

        val campuses = arrayOf(
            "Pretoria Campus",
            "Johannesburg Campus",
            "Polokwane Campus",
            "Durban Campus",
            "Bloemfontein Campus"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Campus")
            .setItems(campuses) { _, which ->

                campusName =
                    campuses[which]

                tvSelectedCampus.text =
                    campusName

                Toast.makeText(
                    this,
                    "$campusName selected.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    // =========================================================
    // SEARCH CAMPUS
    // =========================================================

    private fun searchCampusLocation() {

        val searchText =
            etSearchLocation.text
                .toString()
                .trim()

        if (searchText.isEmpty()) {

            Toast.makeText(
                this,
                "Enter a building or location.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            this,
            "Searching for $searchText...",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // CURRENT LOCATION
    // =========================================================

    private fun showCurrentLocation() {

        Toast.makeText(
            this,
            "Showing your current campus location.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // DIRECTIONS
    // =========================================================

    private fun openDirections() {

        val destination =
            etSearchLocation.text
                .toString()
                .trim()

        val query =
            if (destination.isNotEmpty()) {

                "$campusName $destination"

            } else {

                campusName
            }

        try {

            val mapIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "geo:0,0?q=${Uri.encode(query)}"
                )
            )

            startActivity(mapIntent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No map application is available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // CAMPUS HOME
    // =========================================================

    private fun openCampusHome() {

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

    // =========================================================
    // CAMPUS REPORTS
    // =========================================================

    private fun openCampusReports() {

        try {

            val intent =
                Intent(
                    this,
                    CampusReportsActivity::class.java
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
    // CAMPUS ALERTS
    // =========================================================

    private fun openCampusAlerts() {

        try {

            val intent =
                Intent(
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
    // CAMPUS PROFILE
    // =========================================================

    private fun openCampusProfile() {

        try {

            startActivity(
                Intent(
                    this,
                    CampusProfileActivity::class.java
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Campus profile is not available.",
                Toast.LENGTH_SHORT
            ).show()
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

                    finish()
                }
            }
        )
    }
}