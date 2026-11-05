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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safezone.data.SafeZoneDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class EmergencyRequestActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvAssistanceType: TextView
    private lateinit var tvStatus: TextView

    private lateinit var btnEmergencyServices: Button
    private lateinit var btnShareLocation: Button
    private lateinit var btnReturnHome: Button

    private lateinit var locationManager: LocationManager

    private val locationExecutor = Executors.newSingleThreadExecutor()

    // Keep the sync value supplied by the request/database.
    private var syncStatusText = ""
    private var locationStatusText = ""

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
                locationStatusText =
                    getString(R.string.emergency_request_location_permission_denied)
                updateStatusText()

                Toast.makeText(
                    this,
                    R.string.emergency_request_location_permission_required,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_request)

        tvBack = findViewById(R.id.tvBack)
        tvAssistanceType = findViewById(R.id.tvAssistanceType)
        tvStatus = findViewById(R.id.tvStatus)

        btnEmergencyServices = findViewById(R.id.btnEmergencyServices)
        btnShareLocation = findViewById(R.id.btnShareLocation)
        btnReturnHome = findViewById(R.id.btnReturnHome)

        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        val assistanceType = intent.getStringExtra("assistanceType")

        tvAssistanceType.text = getString(
            R.string.emergency_request_assistance_type_value,
            assistanceType
                ?: getString(R.string.emergency_request_default_type)
        )

        syncStatusText = intent.getStringExtra("syncStatus")
            ?: getString(R.string.emergency_request_pending_sync)

        locationStatusText =
            getString(R.string.emergency_request_location_not_shared)

        updateStatusText()

        // Observe the request's actual local synchronisation status.
        val requestId = intent.getStringExtra("requestId")
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (!requestId.isNullOrBlank() && !userId.isNullOrBlank()) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    SafeZoneDatabase.getDatabase(applicationContext)
                        .emergencyRequestDao()
                        .observeRequest(requestId, userId)
                        .collect { request ->
                            if (request != null) {
                                syncStatusText = request.syncStatus
                                updateStatusText()
                            }
                        }
                }
            }
        }

        tvBack.setOnClickListener {
            finish()
        }

        btnEmergencyServices.setOnClickListener {
            openEmergencyDialler()
        }

        btnShareLocation.setOnClickListener {
            val preferences = getSharedPreferences(
                "SafeZonePreferences",
                MODE_PRIVATE
            )

            val locationEnabled =
                preferences.getBoolean("emergencyLocation", true)

            if (!locationEnabled) {
                locationStatusText =
                    getString(R.string.emergency_request_location_disabled)
                updateStatusText()

                Toast.makeText(
                    this,
                    R.string.emergency_request_enable_location_first,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                checkLocationPermission()
            }
        }

        btnReturnHome.setOnClickListener {
            val homeIntent = Intent(
                this,
                PersonalHomeActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(homeIntent)
            finish()
        }
    }

    // Display sync and location status separately.
    private fun updateStatusText() {
        tvStatus.text = getString(
            R.string.emergency_request_status_format,
            syncStatusText,
            locationStatusText
        )
    }

    private fun openEmergencyDialler() {
        val preferences = getSharedPreferences(
            "SafeZonePreferences",
            MODE_PRIVATE
        )

        val servicesEnabled =
            preferences.getBoolean("emergencyServices", true)

        if (!servicesEnabled) {
            Toast.makeText(
                this,
                R.string.emergency_request_services_disabled,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val emergencyNumber = preferences
            .getString("emergencyNumber", "112")
            .orEmpty()
            .trim()

        if (emergencyNumber.isBlank()) {
            Toast.makeText(
                this,
                R.string.emergency_request_set_emergency_number,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            // ACTION_DIAL opens the dialler; the user must place the call.
            startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.fromParts("tel", emergencyNumber, null)
                )
            )
        } catch (e: Exception) {
            Toast.makeText(
                this,
                R.string.emergency_request_dialler_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            shareCurrentLocation()
        } else {
            locationStatusText =
                getString(R.string.emergency_request_requesting_permission)
            updateStatusText()

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun shareCurrentLocation() {
        val gpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            false
        }

        val networkEnabled = try {
            locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        } catch (e: Exception) {
            false
        }

        if (!gpsEnabled && !networkEnabled) {
            locationStatusText =
                getString(R.string.emergency_request_location_services_off)
            updateStatusText()

            Toast.makeText(
                this,
                R.string.emergency_request_turn_on_location,
                Toast.LENGTH_LONG
            ).show()

            try {
                startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    R.string.emergency_request_enable_location_in_settings,
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationStatusText =
                getString(R.string.emergency_request_location_permission_required_status)
            updateStatusText()
            return
        }

        val lastLocation = getFastLastKnownLocation()

        if (lastLocation != null) {
            shareLocation(lastLocation.latitude, lastLocation.longitude)
            return
        }

        locationStatusText =
            getString(R.string.emergency_request_finding_location)
        updateStatusText()

        Toast.makeText(
            this,
            R.string.emergency_request_finding_location_toast,
            Toast.LENGTH_SHORT
        ).show()

        requestCurrentLocation(gpsEnabled, networkEnabled)
    }

    private fun getFastLastKnownLocation(): Location? {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return null

        val gpsLocation = try {
            locationManager.getLastKnownLocation(
                LocationManager.GPS_PROVIDER
            )
        } catch (e: Exception) {
            null
        }

        val networkLocation = try {
            locationManager.getLastKnownLocation(
                LocationManager.NETWORK_PROVIDER
            )
        } catch (e: Exception) {
            null
        }

        return when {
            gpsLocation == null && networkLocation == null -> null
            gpsLocation == null -> networkLocation
            networkLocation == null -> gpsLocation
            gpsLocation.time >= networkLocation.time -> gpsLocation
            else -> networkLocation
        }
    }

    private fun requestCurrentLocation(
        gpsEnabled: Boolean,
        networkEnabled: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = when {
                gpsEnabled -> LocationManager.GPS_PROVIDER
                networkEnabled -> LocationManager.NETWORK_PROVIDER
                else -> null
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
                        if (isFinishing || isDestroyed) return@runOnUiThread

                        if (location != null) {
                            shareLocation(
                                location.latitude,
                                location.longitude
                            )
                        } else {
                            val fallback = getFastLastKnownLocation()

                            if (fallback != null) {
                                shareLocation(
                                    fallback.latitude,
                                    fallback.longitude
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
            val fallback = getFastLastKnownLocation()

            if (fallback != null) {
                shareLocation(fallback.latitude, fallback.longitude)
            } else {
                showLocationError()
            }
        }
    }

    private fun showLocationError() {
        locationStatusText =
            getString(R.string.emergency_request_location_unavailable)
        updateStatusText()

        Toast.makeText(
            this,
            R.string.emergency_request_location_try_again,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLocationPermissionError() {
        locationStatusText =
            getString(R.string.emergency_request_location_permission_required_status)
        updateStatusText()

        Toast.makeText(
            this,
            R.string.emergency_request_location_permission_required,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun shareLocation(latitude: Double, longitude: Double) {
        val mapsLink =
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        val shareMessage = getString(
            R.string.emergency_request_share_message,
            mapsLink
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        try {
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    getString(R.string.emergency_request_share_chooser_title)
                )
            )

            // Opening the chooser does not confirm that a message was sent.
            locationStatusText =
                getString(R.string.emergency_request_location_ready_to_share)
            updateStatusText()
        } catch (e: Exception) {
            locationStatusText =
                getString(R.string.emergency_request_sharing_menu_error)
            updateStatusText()

            Toast.makeText(
                this,
                R.string.emergency_request_sharing_menu_error_toast,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationExecutor.shutdown()
    }
}