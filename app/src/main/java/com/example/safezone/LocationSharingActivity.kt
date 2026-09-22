package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class LocationSharingActivity : AppCompatActivity() {

    private lateinit var switchLocationSharing: SwitchCompat
    private lateinit var tvLocationStatus: TextView
    private lateinit var btnLocationSettings: AppCompatButton
    private lateinit var btnBackToSettings: AppCompatButton
    private lateinit var tvBack: TextView

    // =====================================================
    // LOCATION PERMISSION RESULT
    // =====================================================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {

                // Permission has been granted.
                // Now check whether Android Location Services are enabled.
                if (isLocationEnabled()) {

                    switchLocationSharing.isChecked = true

                    tvLocationStatus.text =
                        "Location sharing is enabled."

                    Toast.makeText(
                        this,
                        "Location sharing enabled.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    // Permission is granted, but the phone's
                    // Location Services are switched off.
                    switchLocationSharing.isChecked = false

                    tvLocationStatus.text =
                        "Location services are turned off. Please enable Location."

                    Toast.makeText(
                        this,
                        "Please enable Location Services.",
                        Toast.LENGTH_LONG
                    ).show()

                    openLocationSettings()
                }

            } else {

                switchLocationSharing.isChecked = false

                tvLocationStatus.text =
                    "Location permission is required."

                Toast.makeText(
                    this,
                    "Location permission was not granted.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_location_sharing)

        // ==============================
        // FIND VIEWS
        // ==============================

        tvBack = findViewById(R.id.tvBack)

        switchLocationSharing =
            findViewById(R.id.switchLocationSharing)

        tvLocationStatus =
            findViewById(R.id.tvLocationStatus)

        btnLocationSettings =
            findViewById(R.id.btnLocationSettings)

        btnBackToSettings =
            findViewById(R.id.btnBackToSettings)


        // ==============================
        // INITIAL STATUS
        // ==============================

        updateLocationStatus()


        // ==============================
        // BACK BUTTON
        // ==============================

        tvBack.setOnClickListener {
            finish()
        }

        btnBackToSettings.setOnClickListener {
            finish()
        }


        // ==============================
        // LOCATION SWITCH
        // ==============================

        switchLocationSharing.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {

                enableLocationSharing()

            } else {

                // This prototype does not revoke Android's
                // permission automatically. We simply update
                // the application's sharing state.
                switchLocationSharing.isChecked = false

                tvLocationStatus.text =
                    "Location sharing is disabled."

                Toast.makeText(
                    this,
                    "Location sharing disabled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // ==============================
        // LOCATION SETTINGS BUTTON
        // ==============================

        btnLocationSettings.setOnClickListener {
            openLocationSettings()
        }
    }


    // =====================================================
    // ENABLE LOCATION SHARING
    // =====================================================

    private fun enableLocationSharing() {

        // First check Android permission.
        if (!hasLocationPermission()) {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            return
        }


        // Permission exists, so check whether
        // Android Location Services are actually ON.
        if (!isLocationEnabled()) {

            switchLocationSharing.isChecked = false

            tvLocationStatus.text =
                "Location services are turned off. Please enable Location."

            Toast.makeText(
                this,
                "Please enable Location Services.",
                Toast.LENGTH_LONG
            ).show()

            openLocationSettings()

            return
        }


        // Permission + Location Services are both available.
        switchLocationSharing.isChecked = true

        tvLocationStatus.text =
            "Location sharing is enabled."

        Toast.makeText(
            this,
            "Location sharing is enabled.",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =====================================================
    // CHECK LOCATION PERMISSION
    // =====================================================

    private fun hasLocationPermission(): Boolean {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }


    // =====================================================
    // CHECK ANDROID LOCATION SERVICES
    // =====================================================

    private fun isLocationEnabled(): Boolean {

        val locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        return try {

            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) ||
                    locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    )

        } catch (e: Exception) {

            false
        }
    }


    // =====================================================
    // UPDATE SCREEN STATUS
    // =====================================================

    private fun updateLocationStatus() {

        if (!hasLocationPermission()) {

            switchLocationSharing.isChecked = false

            tvLocationStatus.text =
                "Location access is currently disabled."

            return
        }


        if (!isLocationEnabled()) {

            switchLocationSharing.isChecked = false

            tvLocationStatus.text =
                "Permission granted, but Location Services are off."

            return
        }


        // Everything is available.
        switchLocationSharing.isChecked = true

        tvLocationStatus.text =
            "Location sharing is enabled."
    }


    // =====================================================
    // OPEN ANDROID LOCATION SETTINGS
    // =====================================================

    private fun openLocationSettings() {

        try {

            val intent =
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open Location Settings.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =====================================================
    // CHECK AGAIN WHEN RETURNING TO THE APP
    // =====================================================

    override fun onResume() {
        super.onResume()

        // The user may have enabled Location Services
        // while inside Android Settings.
        updateLocationStatus()
    }
}