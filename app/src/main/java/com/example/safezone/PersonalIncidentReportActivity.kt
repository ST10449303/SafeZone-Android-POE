package com.example.safezone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonalIncidentReportActivity : AppCompatActivity() {

    // ==========================================
    // VIEWS
    // ==========================================

    private lateinit var tvBack: TextView
    private lateinit var spIncidentType: Spinner
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDateTime: EditText

    private lateinit var btnAddPhoto: Button
    private lateinit var btnAddVideo: Button
    private lateinit var btnAddFile: Button

    private lateinit var tvEvidence: TextView
    private lateinit var btnSubmitReport: Button

    // ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // ==========================================
    // EVIDENCE
    // ==========================================

    private var selectedPhotoUri: Uri? = null
    private var selectedVideoUri: Uri? = null
    private var selectedFileUri: Uri? = null

    // ==========================================
    // REAL LOCATION
    // ==========================================

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    // ==========================================
    // REQUEST CODES
    // ==========================================

    companion object {

        private const val PHOTO_REQUEST = 2001
        private const val VIDEO_REQUEST = 2002
        private const val FILE_REQUEST = 2003
        private const val LOCATION_REQUEST = 2004
    }

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_personal_incident_report
        )

        // ==========================================
        // CONNECT XML
        // ==========================================

        tvBack = findViewById(R.id.tvBack)

        spIncidentType =
            findViewById(R.id.spIncidentType)

        etDescription =
            findViewById(R.id.etDescription)

        etLocation =
            findViewById(R.id.etLocation)

        etDateTime =
            findViewById(R.id.etDateTime)

        btnAddPhoto =
            findViewById(R.id.btnAddPhoto)

        btnAddVideo =
            findViewById(R.id.btnAddVideo)

        btnAddFile =
            findViewById(R.id.btnAddFile)

        tvEvidence =
            findViewById(R.id.tvEvidence)

        btnSubmitReport =
            findViewById(R.id.btnSubmitReport)

        // ==========================================
        // FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()

        db = FirebaseFirestore.getInstance()

        // ==========================================
        // BACK
        // ==========================================

        tvBack.setOnClickListener {
            finish()
        }

        // ==========================================
        // INCIDENT TYPES
        // ==========================================

        val incidentTypes = arrayOf(
            "Select incident type",
            "Theft",
            "Assault",
            "Harassment",
            "Accident",
            "Suspicious Activity",
            "Property Damage",
            "Missing Person",
            "Other"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            incidentTypes
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spIncidentType.adapter = adapter

        // ==========================================
        // DATE & TIME
        // ==========================================

        val currentDateTime =
            SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
            ).format(Date())

        etDateTime.setText(currentDateTime)

        // ==========================================
        // GET REAL LOCATION
        // ==========================================

        getCurrentLocation()

        // ==========================================
        // ADD PHOTO
        // ==========================================

        btnAddPhoto.setOnClickListener {

            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT)

            intent.type = "image/*"

            intent.addCategory(
                Intent.CATEGORY_OPENABLE
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )

            startActivityForResult(
                intent,
                PHOTO_REQUEST
            )
        }

        // ==========================================
        // ADD VIDEO
        // ==========================================

        btnAddVideo.setOnClickListener {

            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT)

            intent.type = "video/*"

            intent.addCategory(
                Intent.CATEGORY_OPENABLE
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )

            startActivityForResult(
                intent,
                VIDEO_REQUEST
            )
        }

        // ==========================================
        // ADD FILE
        // ==========================================

        btnAddFile.setOnClickListener {

            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT)

            intent.type = "*/*"

            intent.addCategory(
                Intent.CATEGORY_OPENABLE
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )

            startActivityForResult(
                intent,
                FILE_REQUEST
            )
        }

        // ==========================================
        // SUBMIT
        // ==========================================

        btnSubmitReport.setOnClickListener {

            submitReport()
        }
    }

    // ==================================================
    // GET CURRENT LOCATION
    // ==================================================

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
                LOCATION_REQUEST
            )

            return
        }

        // ==========================================
        // LOCATION MANAGER
        // ==========================================

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

        // ==========================================
        // LOCATION SERVICES OFF
        // ==========================================

        if (!gpsEnabled && !networkEnabled) {

            etLocation.setText(
                "Location services are turned off"
            )

            Toast.makeText(
                this,
                "Please turn on Location services.",
                Toast.LENGTH_LONG
            ).show()

            try {
                startActivity(
                    Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )
            } catch (e: Exception) {
                // Ignore if settings cannot be opened
            }

            return
        }

        // ==========================================
        // FIND BEST LAST KNOWN LOCATION
        // ==========================================

        var bestLocation: Location? = null

        if (gpsEnabled) {

            try {

                val gpsLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

                if (gpsLocation != null) {
                    bestLocation = gpsLocation
                }

            } catch (e: SecurityException) {
                // Permission problem
            }
        }

        if (networkEnabled) {

            try {

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

            } catch (e: SecurityException) {
                // Permission problem
            }
        }

        // ==========================================
        // SAVE REAL COORDINATES
        // ==========================================

        if (bestLocation != null) {

            currentLatitude =
                bestLocation.latitude

            currentLongitude =
                bestLocation.longitude

            val latitude =
                bestLocation.latitude

            val longitude =
                bestLocation.longitude

            etLocation.setText(
                "Latitude: $latitude\nLongitude: $longitude"
            )

        } else {

            etLocation.setText(
                "Current location unavailable"
            )
        }
    }

    // ==================================================
    // LOCATION PERMISSION RESULT
    // ==================================================

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
            LOCATION_REQUEST
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {

                getCurrentLocation()

            } else {

                etLocation.setText(
                    "Location permission not granted"
                )

                Toast.makeText(
                    this,
                    "Location permission is required to record the incident location.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ==================================================
    // EVIDENCE RESULT
    // ==================================================

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            resultCode != Activity.RESULT_OK ||
            data?.data == null
        ) {
            return
        }

        val uri = data.data ?: return

        // ==========================================
        // KEEP PERMISSION FOR THE URI
        // ==========================================

        try {

            val takeFlags =
                data.flags and
                        Intent.FLAG_GRANT_READ_URI_PERMISSION

            contentResolver.takePersistableUriPermission(
                uri,
                takeFlags
            )

        } catch (e: Exception) {
            // Some providers do not support persistable permission
        }

        // ==========================================
        // SAVE SELECTED EVIDENCE
        // ==========================================

        when (requestCode) {

            PHOTO_REQUEST -> {

                selectedPhotoUri = uri

                tvEvidence.text =
                    "Photo selected:\n${uri.lastPathSegment}"
            }

            VIDEO_REQUEST -> {

                selectedVideoUri = uri

                tvEvidence.text =
                    "Video selected:\n${uri.lastPathSegment}"
            }

            FILE_REQUEST -> {

                selectedFileUri = uri

                tvEvidence.text =
                    "File selected:\n${uri.lastPathSegment}"
            }
        }
    }

    // ==================================================
    // SUBMIT REPORT
    // ==================================================

    private fun submitReport() {

        // ==========================================
        // CHECK LOGIN
        // ==========================================

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ==========================================
        // INCIDENT TYPE
        // ==========================================

        if (
            spIncidentType.selectedItemPosition == 0
        ) {

            Toast.makeText(
                this,
                "Please select an incident type.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val incidentType =
            spIncidentType.selectedItem.toString()

        // ==========================================
        // FORM VALUES
        // ==========================================

        val description =
            etDescription.text
                .toString()
                .trim()

        val location =
            etLocation.text
                .toString()
                .trim()

        val dateTime =
            etDateTime.text
                .toString()
                .trim()

        // ==========================================
        // VALIDATE DESCRIPTION
        // ==========================================

        if (description.isEmpty()) {

            etDescription.error =
                "Please describe what happened."

            etDescription.requestFocus()

            return
        }

        // ==========================================
        // VALIDATE LOCATION
        // ==========================================

        if (
            location.isEmpty() ||
            location.contains(
                "unavailable",
                ignoreCase = true
            ) ||
            location.contains(
                "turned off",
                ignoreCase = true
            )
        ) {

            Toast.makeText(
                this,
                "Please enable location services and wait for your real location to appear.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // ==========================================
        // EVIDENCE VALUES
        // ==========================================

        val photoUri =
            selectedPhotoUri?.toString() ?: ""

        val videoUri =
            selectedVideoUri?.toString() ?: ""

        val fileUri =
            selectedFileUri?.toString() ?: ""

        // ==========================================
        // REPORT DATA
        // ==========================================

        val reportData =
            hashMapOf<String, Any>(

                // User
                "userId" to currentUser.uid,

                // Incident
                "incidentType" to incidentType,

                // Description
                "description" to description,

                // Display location
                "location" to location,

                // Real GPS
                "latitude" to
                        (currentLatitude ?: 0.0),

                "longitude" to
                        (currentLongitude ?: 0.0),

                // Date/time shown to user
                "dateTime" to dateTime,

                // Status
                "status" to "Submitted",

                // Evidence
                "photoEvidence" to photoUri,

                "videoEvidence" to videoUri,

                "fileEvidence" to fileUri,

                // Evidence names
                "photoName" to
                        (
                                selectedPhotoUri
                                    ?.lastPathSegment ?: ""
                                ),

                "videoName" to
                        (
                                selectedVideoUri
                                    ?.lastPathSegment ?: ""
                                ),

                "fileName" to
                        (
                                selectedFileUri
                                    ?.lastPathSegment ?: ""
                                ),

                // Created timestamp
                "createdAt" to
                        System.currentTimeMillis()
            )

        // ==========================================
        // DISABLE BUTTON
        // ==========================================

        btnSubmitReport.isEnabled = false

        btnSubmitReport.text =
            "Submitting..."

        // ==========================================
        // SAVE TO FIRESTORE
        // ==========================================

        db.collection("IncidentReports")
            .add(reportData)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Incident report submitted successfully.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
            .addOnFailureListener { exception ->

                btnSubmitReport.isEnabled =
                    true

                btnSubmitReport.text =
                    "Submit Report"

                Toast.makeText(
                    this,
                    "Unable to submit report: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}