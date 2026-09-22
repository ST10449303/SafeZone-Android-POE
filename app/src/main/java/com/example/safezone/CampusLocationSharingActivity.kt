package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class CampusLocationSharingActivity : AppCompatActivity() {

    // =========================================================
    // UI
    // =========================================================

    private lateinit var switchLocationSharing: SwitchCompat
    private lateinit var tvLocationStatus: TextView
    private lateinit var tvSystemLocationStatus: TextView
    private lateinit var btnLocationSettings: AppCompatButton
    private lateinit var btnBackToSettings: AppCompatButton


    // =========================================================
    // PREFERENCES
    // =========================================================

    private val preferencesName =
        "SafeZoneCampusPreferences"

    private val locationSharingKey =
        "campusLocationSharing"


    // =========================================================
    // LOCATION PERMISSION REQUEST
    // =========================================================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                    ?: false

            val coarseLocation =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION]
                    ?: false

            if (fineLocation || coarseLocation) {

                saveLocationSharing(true)

                updateLocationStatus()

                Toast.makeText(
                    this,
                    "Location sharing enabled.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                switchLocationSharing.isChecked = false

                saveLocationSharing(false)

                updateLocationStatus()

                Toast.makeText(
                    this,
                    "Location permission was not granted.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_location_sharing
        )

        initializeViews()

        loadLocationPreference()

        setupButtons()

        updateLocationStatus()
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        switchLocationSharing =
            findViewById(R.id.switchLocationSharing)

        tvLocationStatus =
            findViewById(R.id.tvLocationStatus)

        tvSystemLocationStatus =
            findViewById(R.id.tvSystemLocationStatus)

        btnLocationSettings =
            findViewById(R.id.btnLocationSettings)

        btnBackToSettings =
            findViewById(R.id.btnBackToSettings)
    }


    // =========================================================
    // LOAD SAVED PREFERENCE
    // =========================================================

    private fun loadLocationPreference() {

        val preferences =
            getSharedPreferences(
                preferencesName,
                MODE_PRIVATE
            )

        val enabled =
            preferences.getBoolean(
                locationSharingKey,
                false
            )

        switchLocationSharing.isChecked =
            enabled
    }


    // =========================================================
    // SETUP BUTTONS
    // =========================================================

    private fun setupButtons() {

        // -----------------------------------------------------
        // BACK ARROW
        // -----------------------------------------------------

        findViewById<View>(R.id.tvBack)
            .setOnClickListener {

                finish()
            }


        // -----------------------------------------------------
        // LOCATION SWITCH
        // -----------------------------------------------------

        switchLocationSharing.setOnCheckedChangeListener {

                _, isChecked ->

            if (isChecked) {

                requestLocationPermission()

            } else {

                saveLocationSharing(false)

                updateLocationStatus()

                Toast.makeText(
                    this,
                    "Location sharing disabled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // -----------------------------------------------------
        // LOCATION SETTINGS
        // -----------------------------------------------------

        btnLocationSettings.setOnClickListener {

            openLocationSettings()
        }


        // -----------------------------------------------------
        // BACK TO SETTINGS
        // -----------------------------------------------------

        btnBackToSettings.setOnClickListener {

            finish()
        }
    }


    // =========================================================
    // REQUEST LOCATION PERMISSION
    // =========================================================

    private fun requestLocationPermission() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        val hasPermission =
            finePermission == PackageManager.PERMISSION_GRANTED ||
                    coarsePermission == PackageManager.PERMISSION_GRANTED


        if (hasPermission) {

            saveLocationSharing(true)

            updateLocationStatus()

            Toast.makeText(
                this,
                "Location sharing enabled.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // Ask Android for location permission
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


    // =========================================================
    // SAVE LOCATION SHARING
    // =========================================================

    private fun saveLocationSharing(
        enabled: Boolean
    ) {

        val preferences =
            getSharedPreferences(
                preferencesName,
                MODE_PRIVATE
            )

        preferences.edit()
            .putBoolean(
                locationSharingKey,
                enabled
            )
            .apply()
    }


    // =========================================================
    // UPDATE LOCATION STATUS
    // =========================================================

    private fun updateLocationStatus() {

        val sharingEnabled =
            switchLocationSharing.isChecked


        if (sharingEnabled) {

            tvLocationStatus.text =
                "Location sharing is ON"

            tvLocationStatus.setTextColor(
                getColor(android.R.color.holo_green_dark)
            )

        } else {

            tvLocationStatus.text =
                "Location sharing is OFF"

            tvLocationStatus.setTextColor(
                getColor(android.R.color.holo_red_dark)
            )
        }


        // Check the phone's location services
        val locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager


        val gpsEnabled =
            try {

                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )

            } catch (e: Exception) {

                false
            }


        val networkEnabled =
            try {

                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )

            } catch (e: Exception) {

                false
            }


        if (gpsEnabled || networkEnabled) {

            tvSystemLocationStatus.text =
                "Device location services are ON."

            tvSystemLocationStatus.setTextColor(
                getColor(android.R.color.holo_green_dark)
            )

        } else {

            tvSystemLocationStatus.text =
                "Device location services are OFF. Turn them on to use location features."

            tvSystemLocationStatus.setTextColor(
                getColor(android.R.color.holo_red_dark)
            )
        }
    }


    // =========================================================
    // OPEN ANDROID LOCATION SETTINGS
    // =========================================================

    private fun openLocationSettings() {

        try {

            val intent =
                Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open location settings.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (::switchLocationSharing.isInitialized) {

            updateLocationStatus()
        }
    }
}