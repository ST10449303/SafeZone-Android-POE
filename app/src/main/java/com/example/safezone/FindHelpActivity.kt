
package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class FindHelpActivity : AppCompatActivity() {

    // ==========================================
    // VIEWS
    // ==========================================

    private lateinit var tvBack: TextView
    private lateinit var tvCurrentLocation: TextView
    private lateinit var tvLocationStatus: TextView
    private lateinit var tvHospitalDistance: TextView

    private lateinit var btnRefreshLocation: Button
    private lateinit var btnHospitalDirections: Button
    private lateinit var btnHospitalCall: Button
    private lateinit var btnEmergencyDirections: Button
    private lateinit var btnEmergencyCall: Button
    private lateinit var btnCampusDirections: Button

    // ==========================================
    // LOCATION
    // ==========================================

    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 5001
    }

    // ==========================================
    // LOCATION LISTENER
    // ==========================================

    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            currentLocation = location
            updateLocationOnScreen(location)
        }

        override fun onProviderEnabled(provider: String) {
            if (
                provider == LocationManager.GPS_PROVIDER ||
                provider == LocationManager.NETWORK_PROVIDER
            ) {
                getCurrentLocation()
            }
        }

        override fun onProviderDisabled(provider: String) {
            tvLocationStatus.text =
                getString(R.string.find_help_location_services_off)
        }
    }

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_help)

        // Connect XML
        tvBack = findViewById(R.id.tvBack)
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)
        tvLocationStatus = findViewById(R.id.tvLocationStatus)
        tvHospitalDistance = findViewById(R.id.tvHospitalDistance)

        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)
        btnHospitalDirections = findViewById(R.id.btnHospitalDirections)
        btnHospitalCall = findViewById(R.id.btnHospitalCall)
        btnEmergencyDirections = findViewById(R.id.btnEmergencyDirections)
        btnEmergencyCall = findViewById(R.id.btnEmergencyCall)
        btnCampusDirections = findViewById(R.id.btnCampusDirections)

        // Location manager
        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        // Back
        tvBack.setOnClickListener {
            finish()
        }

        // Refresh location
        btnRefreshLocation.setOnClickListener {
            getCurrentLocation()
        }

        // Hospital directions
        btnHospitalDirections.setOnClickListener {
            openNearbySearch("hospital")
        }

        // Hospital call
        btnHospitalCall.setOnClickListener {
            callNumber("10177")
        }

        // Emergency directions
        btnEmergencyDirections.setOnClickListener {
            openNearbySearch("emergency services")
        }

        // Emergency call
        btnEmergencyCall.setOnClickListener {
            callNumber("112")
        }

        // Campus security directions
        btnCampusDirections.setOnClickListener {
            openNearbySearch("campus security")
        }

        // Initial location
        getCurrentLocation()
    }

    // ==========================================
    // GET CURRENT LOCATION
    // ==========================================

    private fun getCurrentLocation() {

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

        // Request permission
        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        // Check location services
        val gpsEnabled = try {
            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
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
            tvCurrentLocation.text =
                getString(R.string.find_help_location_unavailable)

            tvLocationStatus.text =
                getString(R.string.find_help_turn_on_location)

            return
        }

        tvLocationStatus.text =
            getString(R.string.find_help_finding_location)

        // Request location updates
        try {
            if (gpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L,
                    5f,
                    locationListener
                )
            }

            if (networkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    locationListener
                )
            }

            // Get last known location
            var bestLocation: Location? = null

            if (gpsEnabled) {
                val gpsLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

                if (gpsLocation != null) {
                    bestLocation = gpsLocation
                }
            }

            if (networkEnabled) {
                val networkLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )

                if (networkLocation != null) {
                    if (
                        bestLocation == null ||
                        networkLocation.accuracy < bestLocation.accuracy
                    ) {
                        bestLocation = networkLocation
                    }
                }
            }

            if (bestLocation != null) {
                currentLocation = bestLocation
                updateLocationOnScreen(bestLocation)
            }

        } catch (e: SecurityException) {
            tvLocationStatus.text =
                getString(R.string.find_help_permission_required)
        }
    }

    // ==========================================
    // UPDATE LOCATION ON SCREEN
    // ==========================================

    private fun updateLocationOnScreen(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        tvCurrentLocation.text =
            getLocationName(latitude, longitude)

        tvLocationStatus.text =
            getString(R.string.find_help_live_location_updated)

        // The actual nearby hospital is selected by Google Maps.
        tvHospitalDistance.text =
            getString(R.string.find_help_distance_from_location)
    }

    // ==========================================
    // CONVERT COORDINATES TO ADDRESS
    // ==========================================

    private fun getLocationName(
        latitude: Double,
        longitude: Double
    ): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())

            val addresses: List<Address>? =
                geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val parts = mutableListOf<String>()

                if (
                    !address.subThoroughfare.isNullOrEmpty() &&
                    !address.thoroughfare.isNullOrEmpty()
                ) {
                    parts.add(
                        "${address.subThoroughfare} ${address.thoroughfare}"
                    )
                } else if (!address.thoroughfare.isNullOrEmpty()) {
                    parts.add(address.thoroughfare)
                }

                if (!address.locality.isNullOrEmpty()) {
                    parts.add(address.locality)
                }

                if (!address.adminArea.isNullOrEmpty()) {
                    parts.add(address.adminArea)
                }

                if (parts.isNotEmpty()) {
                    parts.joinToString(", ")
                } else {
                    "$latitude, $longitude"
                }
            } else {
                "$latitude, $longitude"
            }
        } catch (e: Exception) {
            "$latitude, $longitude"
        }
    }

    // ==========================================
    // OPEN GOOGLE MAPS NEAR CURRENT LOCATION
    // ==========================================

    private fun openNearbySearch(searchTerm: String) {
        val location = currentLocation

        if (location == null) {
            showToast(
                getString(R.string.find_help_location_not_ready)
            )
            getCurrentLocation()
            return
        }

        val latitude = location.latitude
        val longitude = location.longitude

        val uri = Uri.parse(
            "geo:$latitude,$longitude?q=" + Uri.encode(searchTerm)
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Google Maps is unavailable; try another map application.
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)

            try {
                startActivity(fallbackIntent)
            } catch (ex: Exception) {
                showToast(
                    getString(R.string.find_help_no_map_app)
                )
            }
        }
    }

    // ==========================================
    // CALL NUMBER
    // ==========================================

    private fun callNumber(number: String) {
        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:$number")
        )

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast(
                getString(R.string.find_help_dialer_error)
            )
        }
    }

    // ==========================================
    // PERMISSION RESULT
    // ==========================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                getCurrentLocation()
            } else {
                tvCurrentLocation.text =
                    getString(R.string.find_help_permission_required)

                tvLocationStatus.text =
                    getString(R.string.find_help_allow_location)
            }
        }
    }

    // ==========================================
    // STOP LOCATION UPDATES
    // ==========================================

    override fun onPause() {
        super.onPause()

        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            // Ignore location cleanup errors.
        }
    }

    // ==========================================
    // RESUME LOCATION UPDATES
    // ==========================================

    override fun onResume() {
        super.onResume()

        if (::locationManager.isInitialized) {
            getCurrentLocation()
        }
    }

    // ==========================================
    // OPEN LOCATION SETTINGS
    // ==========================================

    private fun openLocationSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
        } catch (e: Exception) {
            // Ignore settings launch errors.
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
