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

    private val locationListener =
        object : LocationListener {

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
                    "Location services are turned off"
            }
        }


    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_find_help
        )


        // ==========================================
        // CONNECT XML
        // ==========================================

        tvBack =
            findViewById(R.id.tvBack)

        tvCurrentLocation =
            findViewById(R.id.tvCurrentLocation)

        tvLocationStatus =
            findViewById(R.id.tvLocationStatus)

        tvHospitalDistance =
            findViewById(R.id.tvHospitalDistance)

        btnRefreshLocation =
            findViewById(R.id.btnRefreshLocation)

        btnHospitalDirections =
            findViewById(R.id.btnHospitalDirections)

        btnHospitalCall =
            findViewById(R.id.btnHospitalCall)

        btnEmergencyDirections =
            findViewById(R.id.btnEmergencyDirections)

        btnEmergencyCall =
            findViewById(R.id.btnEmergencyCall)

        btnCampusDirections =
            findViewById(R.id.btnCampusDirections)


        // ==========================================
        // LOCATION MANAGER
        // ==========================================

        locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager


        // ==========================================
        // BACK
        // ==========================================

        tvBack.setOnClickListener {

            finish()
        }


        // ==========================================
        // REFRESH LOCATION
        // ==========================================

        btnRefreshLocation.setOnClickListener {

            getCurrentLocation()
        }


        // ==========================================
        // HOSPITAL DIRECTIONS
        // ==========================================

        btnHospitalDirections.setOnClickListener {

            openNearbySearch(
                "hospital"
            )
        }


        // ==========================================
        // HOSPITAL CALL
        // ==========================================

        btnHospitalCall.setOnClickListener {

            callNumber(
                "10177"
            )
        }


        // ==========================================
        // EMERGENCY DIRECTIONS
        // ==========================================

        btnEmergencyDirections.setOnClickListener {

            openNearbySearch(
                "emergency services"
            )
        }


        // ==========================================
        // EMERGENCY CALL
        // ==========================================

        btnEmergencyCall.setOnClickListener {

            callNumber(
                "112"
            )
        }


        // ==========================================
        // CAMPUS SECURITY DIRECTIONS
        // ==========================================

        btnCampusDirections.setOnClickListener {

            openNearbySearch(
                "campus security"
            )
        }


        // ==========================================
        // INITIAL LOCATION
        // ==========================================

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


        // ==========================================
        // REQUEST PERMISSION
        // ==========================================

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


        if (!gpsEnabled && !networkEnabled) {

            tvCurrentLocation.text =
                "Location unavailable"

            tvLocationStatus.text =
                "Turn on Location Services to find nearby help."

            return
        }


        tvLocationStatus.text =
            "Finding your current location..."


        // ==========================================
        // REQUEST LOCATION UPDATES
        // ==========================================

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


            // ==========================================
            // GET LAST KNOWN LOCATION
            // ==========================================

            var bestLocation: Location? = null


            if (gpsEnabled) {

                val gpsLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

                if (gpsLocation != null) {

                    bestLocation =
                        gpsLocation
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
                        networkLocation.accuracy <
                        bestLocation.accuracy
                    ) {

                        bestLocation =
                            networkLocation
                    }
                }
            }


            if (bestLocation != null) {

                currentLocation =
                    bestLocation

                updateLocationOnScreen(
                    bestLocation
                )
            }

        } catch (e: SecurityException) {

            tvLocationStatus.text =
                "Location permission is required."
        }
    }


    // ==========================================
    // UPDATE LOCATION ON SCREEN
    // ==========================================

    private fun updateLocationOnScreen(
        location: Location
    ) {

        val latitude =
            location.latitude

        val longitude =
            location.longitude


        tvCurrentLocation.text =
            getLocationName(
                latitude,
                longitude
            )


        tvLocationStatus.text =
            "Live location • Updated just now"


        // ==========================================
        // EXAMPLE DISTANCE CALCULATION
        // ==========================================

        /*
         * We don't invent a hospital distance.
         *
         * The actual nearby hospital is selected
         * by Google Maps when the user requests
         * directions.
         */

        tvHospitalDistance.text =
            "Distance will be calculated from your current location."
    }


    // ==========================================
    // CONVERT COORDINATES TO ADDRESS
    // ==========================================

    private fun getLocationName(
        latitude: Double,
        longitude: Double
    ): String {

        return try {

            val geocoder =
                Geocoder(
                    this,
                    Locale.getDefault()
                )


            val addresses: List<Address>? =
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1
                )


            if (
                !addresses.isNullOrEmpty()
            ) {

                val address =
                    addresses[0]


                val parts =
                    mutableListOf<String>()


                if (
                    !address.subThoroughfare.isNullOrEmpty() &&
                    !address.thoroughfare.isNullOrEmpty()
                ) {

                    parts.add(
                        "${address.subThoroughfare} ${address.thoroughfare}"
                    )
                } else if (
                    !address.thoroughfare.isNullOrEmpty()
                ) {

                    parts.add(
                        address.thoroughfare
                    )
                }


                if (
                    !address.locality.isNullOrEmpty()
                ) {

                    parts.add(
                        address.locality
                    )
                }


                if (
                    !address.adminArea.isNullOrEmpty()
                ) {

                    parts.add(
                        address.adminArea
                    )
                }


                if (parts.isNotEmpty()) {

                    parts.joinToString(
                        ", "
                    )

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

    private fun openNearbySearch(
        searchTerm: String
    ) {

        val location =
            currentLocation


        if (location == null) {

            Toast.makeText(

                this,

                "Your current location is not available yet.",

                Toast.LENGTH_SHORT

            ).show()

            getCurrentLocation()

            return
        }


        val latitude =
            location.latitude

        val longitude =
            location.longitude


        val uri =
            Uri.parse(

                "geo:$latitude,$longitude?q=" +
                        Uri.encode(searchTerm)
            )


        val intent =
            Intent(
                Intent.ACTION_VIEW,
                uri
            )


        intent.setPackage(
            "com.google.android.apps.maps"
        )


        try {

            startActivity(intent)

        } catch (e: Exception) {

            // Google Maps isn't installed.
            // Open any available map application.

            val fallbackIntent =
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )


            try {

                startActivity(
                    fallbackIntent
                )

            } catch (ex: Exception) {

                Toast.makeText(

                    this,

                    "No map application is available.",

                    Toast.LENGTH_LONG

                ).show()
            }
        }
    }


    // ==========================================
    // CALL NUMBER
    // ==========================================

    private fun callNumber(
        number: String
    ) {

        val intent =
            Intent(
                Intent.ACTION_DIAL,
                Uri.parse(
                    "tel:$number"
                )
            )


        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(

                this,

                "Unable to open the phone dialler.",

                Toast.LENGTH_SHORT

            ).show()
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


        if (
            requestCode ==
            LOCATION_PERMISSION_REQUEST
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it ==
                            PackageManager.PERMISSION_GRANTED
                }
            ) {

                getCurrentLocation()

            } else {

                tvCurrentLocation.text =
                    "Location permission required"

                tvLocationStatus.text =
                    "Allow SafeZone to access your location."
            }
        }
    }


    // ==========================================
    // STOP LOCATION UPDATES
    // ==========================================

    override fun onPause() {

        super.onPause()

        try {

            locationManager.removeUpdates(
                locationListener
            )

        } catch (e: Exception) {

            // Ignore
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
                Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
            )

        } catch (e: Exception) {

            // Ignore
        }
    }
}