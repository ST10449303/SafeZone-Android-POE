package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class EmergencyRequestActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvAssistanceType: TextView
    private lateinit var tvStatus: TextView

    private lateinit var btnEmergencyServices: Button
    private lateinit var btnShareLocation: Button
    private lateinit var btnReturnHome: Button

    // ==========================================
    // LOCATION MANAGER
    // ==========================================

    private lateinit var locationManager: LocationManager

    // Executor used for location callbacks.
    // This avoids using Context.getMainExecutor()
    // which requires API 28.
    private val locationExecutor =
        Executors.newSingleThreadExecutor()

    // ==========================================
    // LOCATION PERMISSION REQUEST
    // ==========================================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseLocation =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocation || coarseLocation) {

                shareCurrentLocation()

            } else {

                Toast.makeText(
                    this,
                    "Location permission is required to share your location.",
                    Toast.LENGTH_LONG
                ).show()

                tvStatus.text =
                    "Request Status: Location permission denied"
            }
        }

    // ==========================================
    // ACTIVITY CREATED
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_emergency_request)

        // ==========================================
        // CONNECT XML CONTROLS
        // ==========================================

        tvBack =
            findViewById(R.id.tvBack)

        tvAssistanceType =
            findViewById(R.id.tvAssistanceType)

        tvStatus =
            findViewById(R.id.tvStatus)

        btnEmergencyServices =
            findViewById(R.id.btnEmergencyServices)

        btnShareLocation =
            findViewById(R.id.btnShareLocation)

        btnReturnHome =
            findViewById(R.id.btnReturnHome)

        // ==========================================
        // INITIALISE LOCATION MANAGER
        // ==========================================

        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        // ==========================================
        // GET ASSISTANCE TYPE
        // ==========================================

        val assistanceType =
            intent.getStringExtra("assistanceType")

        if (!assistanceType.isNullOrEmpty()) {

            tvAssistanceType.text =
                "Assistance Type: $assistanceType"

        } else {

            tvAssistanceType.text =
                "Assistance Type: Emergency Assistance"
        }

        // ==========================================
        // INITIAL STATUS
        // ==========================================

        tvStatus.text =
            "Request Status: Requested"

        // ==========================================
        // BACK BUTTON
        // ==========================================

        tvBack.setOnClickListener {
            finish()
        }

        // ==========================================
        // EMERGENCY SERVICES
        // ==========================================

        btnEmergencyServices.setOnClickListener {

            try {

                val dialIntent =
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:112")
                    )

                startActivity(dialIntent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Unable to open the phone dialler.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ==========================================
        // SHARE LOCATION
        // ==========================================

        btnShareLocation.setOnClickListener {

            checkLocationPermission()
        }

        // ==========================================
        // RETURN HOME
        // ==========================================

        btnReturnHome.setOnClickListener {

            val intent =
                Intent(
                    this,
                    PersonalHomeActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }
    }

    // ==================================================
    // CHECK LOCATION PERMISSION
    // ==================================================

    private fun checkLocationPermission() {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {

            shareCurrentLocation()

        } else {

            tvStatus.text =
                "Request Status: Requesting location permission..."

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ==================================================
    // SHARE CURRENT LOCATION
    // ==================================================

    private fun shareCurrentLocation() {

        // ==========================================
        // CHECK LOCATION SERVICES
        // ==========================================

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

        // ==========================================
        // LOCATION SERVICES OFF
        // ==========================================

        if (!gpsEnabled && !networkEnabled) {

            tvStatus.text =
                "Request Status: Location Services are off"

            Toast.makeText(
                this,
                "Please turn on Location Services first.",
                Toast.LENGTH_LONG
            ).show()

            try {

                val settingsIntent =
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                startActivity(settingsIntent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Please enable Location Services in Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }

            return
        }

        // ==========================================
        // CHECK PERMISSION AGAIN
        // ==========================================

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {

            Toast.makeText(
                this,
                "Location permission is required.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // ==========================================
        // FAST METHOD
        // USE LAST KNOWN LOCATION FIRST
        // ==========================================

        val lastLocation =
            getFastLastKnownLocation()

        if (lastLocation != null) {

            shareLocation(
                lastLocation.latitude,
                lastLocation.longitude
            )

            return
        }

        // ==========================================
        // NO LAST KNOWN LOCATION
        // GET CURRENT LOCATION
        // ==========================================

        tvStatus.text =
            "Request Status: Finding current location..."

        Toast.makeText(
            this,
            "Finding your location...",
            Toast.LENGTH_SHORT
        ).show()

        requestCurrentLocation(
            gpsEnabled,
            networkEnabled
        )
    }

    // ==================================================
    // GET LAST KNOWN LOCATION
    // ==================================================

    private fun getFastLastKnownLocation(): Location? {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            return null
        }

        var gpsLocation: Location? = null
        var networkLocation: Location? = null

        try {

            gpsLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )

        } catch (e: SecurityException) {
            gpsLocation = null
        }

        try {

            networkLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                )

        } catch (e: SecurityException) {
            networkLocation = null
        }

        // ==========================================
        // USE THE NEWER LOCATION
        // ==========================================

        return when {

            gpsLocation == null && networkLocation == null ->
                null

            gpsLocation == null ->
                networkLocation

            networkLocation == null ->
                gpsLocation

            gpsLocation.time >= networkLocation.time ->
                gpsLocation

            else ->
                networkLocation
        }
    }

    // ==================================================
    // REQUEST CURRENT LOCATION
    // ==================================================

    private fun requestCurrentLocation(
        gpsEnabled: Boolean,
        networkEnabled: Boolean
    ) {

        // ==========================================
        // API 30+ CURRENT LOCATION
        // ==========================================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val provider =
                when {

                    gpsEnabled ->
                        LocationManager.GPS_PROVIDER

                    networkEnabled ->
                        LocationManager.NETWORK_PROVIDER

                    else ->
                        null
                }

            if (provider == null) {

                showLocationError()

                return
            }

            try {

                locationManager.getCurrentLocation(
                    provider,
                    null,
                    locationExecutor
                ) { location ->

                    runOnUiThread {

                        if (location != null) {

                            shareLocation(
                                location.latitude,
                                location.longitude
                            )

                        } else {

                            // ==========================================
                            // FINAL FALLBACK
                            // ==========================================

                            val fallbackLocation =
                                getFastLastKnownLocation()

                            if (fallbackLocation != null) {

                                shareLocation(
                                    fallbackLocation.latitude,
                                    fallbackLocation.longitude
                                )

                            } else {

                                showLocationError()
                            }
                        }
                    }
                }

            } catch (e: SecurityException) {

                showLocationPermissionError()

            } catch (e: Exception) {

                showLocationError()
            }

        } else {

            // ==========================================
            // OLDER ANDROID DEVICES
            // ==========================================

            val fallbackLocation =
                getFastLastKnownLocation()

            if (fallbackLocation != null) {

                shareLocation(
                    fallbackLocation.latitude,
                    fallbackLocation.longitude
                )

            } else {

                showLocationError()
            }
        }
    }

    // ==================================================
    // LOCATION ERROR
    // ==================================================

    private fun showLocationError() {

        tvStatus.text =
            "Request Status: Location unavailable"

        Toast.makeText(
            this,
            "Unable to determine your location. Please try again.",
            Toast.LENGTH_LONG
        ).show()
    }

    // ==================================================
    // LOCATION PERMISSION ERROR
    // ==================================================

    private fun showLocationPermissionError() {

        tvStatus.text =
            "Request Status: Location permission required"

        Toast.makeText(
            this,
            "Location permission is required.",
            Toast.LENGTH_LONG
        ).show()
    }

    // ==================================================
    // SHARE LOCATION THROUGH ANDROID SHARE MENU
    // ==================================================

    private fun shareLocation(
        latitude: Double,
        longitude: Double
    ) {

        // ==========================================
        // CREATE GOOGLE MAPS LINK
        // ==========================================

        val mapsLink =
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        // ==========================================
        // CREATE EMERGENCY MESSAGE
        // ==========================================

        val shareMessage =
            """
            🚨 SafeZone Emergency Location
            
            I need emergency assistance.
            
            My current location:
            $mapsLink
            
            Please use this location to find me.
            """.trimIndent()

        // ==========================================
        // CREATE SHARE INTENT
        // ==========================================

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(
                    Intent.EXTRA_TEXT,
                    shareMessage
                )
            }

        // ==========================================
        // OPEN SHARE MENU
        // ==========================================

        try {

            tvStatus.text =
                "Request Status: Location Shared"

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share Emergency Location"
                )
            )

        } catch (e: Exception) {

            tvStatus.text =
                "Request Status: Unable to share location"

            Toast.makeText(
                this,
                "Unable to open the sharing menu.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==================================================
    // CLEAN UP EXECUTOR
    // ==================================================

    override fun onDestroy() {
        super.onDestroy()

        locationExecutor.shutdown()
    }
}