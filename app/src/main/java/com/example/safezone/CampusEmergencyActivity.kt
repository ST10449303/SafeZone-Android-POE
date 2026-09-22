package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class CampusEmergencyActivity : AppCompatActivity() {

    private lateinit var tvBack: ImageView
    private lateinit var cardEmergencyCall: android.view.View

    private lateinit var btnCallSecurity: ImageView
    private lateinit var btnCallCampusControl: ImageView
    private lateinit var btnCallHealth: ImageView
    private lateinit var btnCallStudentSupport: ImageView

    private lateinit var btnShareLocation: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_emergency)

        initializeViews()
        setupButtons()
        setupBackNavigation()
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        tvBack = findViewById(R.id.tvBack)

        cardEmergencyCall =
            findViewById(R.id.cardEmergencyCall)

        btnCallSecurity =
            findViewById(R.id.btnCallSecurity)

        btnCallCampusControl =
            findViewById(R.id.btnCallCampusControl)

        btnCallHealth =
            findViewById(R.id.btnCallHealth)

        btnCallStudentSupport =
            findViewById(R.id.btnCallStudentSupport)

        btnShareLocation =
            findViewById(R.id.btnShareLocation)
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
        // EMERGENCY 112
        // -----------------------------------------------------

        cardEmergencyCall.setOnClickListener {

            callNumber("112")
        }


        // -----------------------------------------------------
        // CAMPUS SECURITY
        // -----------------------------------------------------

        btnCallSecurity.setOnClickListener {

            callNumber("112")
        }


        // -----------------------------------------------------
        // CAMPUS CONTROL
        // -----------------------------------------------------

        btnCallCampusControl.setOnClickListener {

            callNumber("0123456789")
        }


        // -----------------------------------------------------
        // HEALTH CENTRE
        // -----------------------------------------------------

        btnCallHealth.setOnClickListener {

            callNumber("0123456790")
        }


        // -----------------------------------------------------
        // STUDENT SUPPORT
        // -----------------------------------------------------

        btnCallStudentSupport.setOnClickListener {

            callNumber("0123456791")
        }


        // -----------------------------------------------------
        // SHARE LOCATION
        // -----------------------------------------------------

        btnShareLocation.setOnClickListener {

            shareCurrentLocation()
        }


        // -----------------------------------------------------
        // BOTTOM NAVIGATION
        // -----------------------------------------------------

        findViewById<android.view.View>(
            R.id.navHome
        ).setOnClickListener {

            val intent = Intent(
                this,
                CampusHomeActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        }


        findViewById<android.view.View>(
            R.id.navMap
        ).setOnClickListener {

            openCampusMap()
        }


        findViewById<android.view.View>(
            R.id.navReports
        ).setOnClickListener {

            try {

                startActivity(
                    Intent(
                        this,
                        PersonalReportsActivity::class.java
                    )
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Reports are not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        findViewById<android.view.View>(
            R.id.navNotifications
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Campus alerts will appear here.",
                Toast.LENGTH_SHORT
            ).show()
        }


        findViewById<android.view.View>(
            R.id.navProfile
        ).setOnClickListener {

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
    }


    // =========================================================
    // CALL PHONE NUMBER
    // =========================================================

    private fun callNumber(number: String) {

        try {

            val intent = Intent(
                Intent.ACTION_DIAL
            )

            intent.data =
                Uri.parse("tel:$number")

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open phone dialer.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // SHARE CURRENT LOCATION
    // =========================================================

    private fun shareCurrentLocation() {

        val locationManager =
            getSystemService(LOCATION_SERVICE)
                    as LocationManager

        if (
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )

            return
        }


        try {

            val location =
                locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )
                    ?: locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )


            if (location != null) {

                val latitude =
                    location.latitude

                val longitude =
                    location.longitude

                val mapLink =
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

                val shareIntent =
                    Intent(Intent.ACTION_SEND)

                shareIntent.type =
                    "text/plain"

                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "My current SafeZone emergency location:\n$mapLink"
                )

                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share My Location"
                    )
                )

            } else {

                Toast.makeText(
                    this,
                    "Current location is not available. Please make sure Location is turned on.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: SecurityException) {

            Toast.makeText(
                this,
                "Location permission is required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // CAMPUS MAP
    // =========================================================

    private fun openCampusMap() {

        try {

            val mapIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "geo:0,0?q=Pretoria Campus"
                )
            )

            startActivity(mapIntent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Map is not available on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // BACK GESTURE / BACK BUTTON
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
    // LOCATION PERMISSION RESULT
    // =========================================================

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
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(
                    this,
                    "Location permission granted. Tap Share My Location again.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Location permission was not granted.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    companion object {

        private const val LOCATION_PERMISSION_REQUEST = 1001
    }
}