package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CampusIncidentReportActivity : AppCompatActivity() {

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var tvBack: ImageView

    private lateinit var spIncidentType: Spinner
    private lateinit var spCampus: Spinner

    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText

    private lateinit var tvCharacterCount: TextView

    private lateinit var ivCurrentLocation: ImageView

    private lateinit var btnAddPhoto: LinearLayout
    private lateinit var btnAddVideo: LinearLayout
    private lateinit var btnAddAudio: LinearLayout
    private lateinit var btnAddFile: LinearLayout

    private lateinit var btnSubmitCampusReport: AppCompatButton


    // =========================================================
    // LOCATION
    // =========================================================

    private lateinit var locationManager: LocationManager

    private var currentLocation: Location? = null


    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private val locationPermissionRequest =
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

                getCurrentLocation()

            } else {

                Toast.makeText(
                    this,
                    "Location permission is required to use your current location.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    // =========================================================
    // SELECTED EVIDENCE FILE PATHS
    // =========================================================

    /*
     * IMPORTANT:
     *
     * We no longer store content:// URIs in Firestore.
     *
     * These variables contain paths to actual files
     * copied into the SafeZone app's private storage.
     */

    private var selectedPhotoPath: String? = null
    private var selectedVideoPath: String? = null
    private var selectedAudioPath: String? = null
    private var selectedFilePath: String? = null


    // =========================================================
    // PHOTO PICKER
    // =========================================================

    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                val savedPath =
                    copyUriToInternalStorage(
                        uri,
                        "photo"
                    )

                if (savedPath != null) {

                    selectedPhotoPath =
                        savedPath

                    Toast.makeText(
                        this,
                        "Photo saved successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Unable to save the selected photo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


    // =========================================================
    // VIDEO PICKER
    // =========================================================

    private val videoPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                val savedPath =
                    copyUriToInternalStorage(
                        uri,
                        "video"
                    )

                if (savedPath != null) {

                    selectedVideoPath =
                        savedPath

                    Toast.makeText(
                        this,
                        "Video saved successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Unable to save the selected video.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


    // =========================================================
    // AUDIO PICKER
    // =========================================================

    private val audioPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                val savedPath =
                    copyUriToInternalStorage(
                        uri,
                        "audio"
                    )

                if (savedPath != null) {

                    selectedAudioPath =
                        savedPath

                    Toast.makeText(
                        this,
                        "Audio saved successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Unable to save the selected audio.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


    // =========================================================
    // FILE PICKER
    // =========================================================

    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                val savedPath =
                    copyUriToInternalStorage(
                        uri,
                        "file"
                    )

                if (savedPath != null) {

                    selectedFilePath =
                        savedPath

                    Toast.makeText(
                        this,
                        "File saved successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Unable to save the selected file.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


    // =========================================================
    // LOCATION LISTENER
    // =========================================================

    private val locationListener =
        object : LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {

                currentLocation =
                    location

                val latitude =
                    location.latitude

                val longitude =
                    location.longitude

                etLocation.setText(
                    String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        latitude,
                        longitude
                    )
                )

                stopLocationUpdates()
            }

            override fun onProviderEnabled(
                provider: String
            ) {
            }

            override fun onProviderDisabled(
                provider: String
            ) {
            }
        }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_incident_report
        )

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()

        initializeViews()

        setupIncidentTypeSpinner()

        setupCampusSpinner()

        setupCharacterCounter()

        setupButtons()
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        tvBack =
            findViewById(R.id.tvBack)

        spIncidentType =
            findViewById(R.id.spIncidentType)

        spCampus =
            findViewById(R.id.spCampus)

        etLocation =
            findViewById(R.id.etLocation)

        etDescription =
            findViewById(R.id.etDescription)

        tvCharacterCount =
            findViewById(R.id.tvCharacterCount)

        ivCurrentLocation =
            findViewById(R.id.ivCurrentLocation)

        btnAddPhoto =
            findViewById(R.id.btnAddPhoto)

        btnAddVideo =
            findViewById(R.id.btnAddVideo)

        btnAddAudio =
            findViewById(R.id.btnAddAudio)

        btnAddFile =
            findViewById(R.id.btnAddFile)

        btnSubmitCampusReport =
            findViewById(R.id.btnSubmitCampusReport)

        locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager
    }


    // =========================================================
    // INCIDENT TYPE SPINNER
    // =========================================================

    private fun setupIncidentTypeSpinner() {

        val incidentTypes =
            arrayOf(
                "Select incident type",
                "Medical Emergency",
                "Assault",
                "Harassment",
                "Theft",
                "Fire",
                "Accident",
                "Suspicious Activity",
                "Unsafe Area",
                "Other"
            )

        val adapter =
            android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                incidentTypes
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spIncidentType.adapter =
            adapter
    }


    // =========================================================
    // CAMPUS SPINNER
    // =========================================================

    private fun setupCampusSpinner() {

        val campuses =
            arrayOf(
                "Select campus",
                "Pretoria Campus",
                "Johannesburg Campus",
                "Polokwane Campus",
                "Durban Campus",
                "Bloemfontein Campus"
            )

        val adapter =
            android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                campuses
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCampus.adapter =
            adapter
    }


    // =========================================================
    // CHARACTER COUNTER
    // =========================================================

    private fun setupCharacterCounter() {

        etDescription.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val length =
                        s?.length ?: 0

                    tvCharacterCount.text =
                        "$length/500"
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
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
        // CURRENT LOCATION
        // -----------------------------------------------------

        ivCurrentLocation.setOnClickListener {

            requestCurrentLocation()
        }


        // -----------------------------------------------------
        // PHOTO
        // -----------------------------------------------------

        btnAddPhoto.setOnClickListener {

            photoPicker.launch(
                arrayOf("image/*")
            )
        }


        // -----------------------------------------------------
        // VIDEO
        // -----------------------------------------------------

        btnAddVideo.setOnClickListener {

            videoPicker.launch(
                arrayOf("video/*")
            )
        }


        // -----------------------------------------------------
        // AUDIO
        // -----------------------------------------------------

        btnAddAudio.setOnClickListener {

            audioPicker.launch(
                arrayOf("audio/*")
            )
        }


        // -----------------------------------------------------
        // FILE
        // -----------------------------------------------------

        btnAddFile.setOnClickListener {

            filePicker.launch(
                arrayOf("*/*")
            )
        }


        // -----------------------------------------------------
        // SUBMIT
        // -----------------------------------------------------

        btnSubmitCampusReport.setOnClickListener {

            submitCampusReport()
        }
    }


    // =========================================================
    // COPY SELECTED FILE TO SAFEZONE STORAGE
    // =========================================================

    private fun copyUriToInternalStorage(
        uri: Uri,
        prefix: String
    ): String? {

        return try {

            // -------------------------------------------------
            // CREATE EVIDENCE DIRECTORY
            // -------------------------------------------------

            val evidenceDirectory =
                File(
                    filesDir,
                    "evidence"
                )

            if (!evidenceDirectory.exists()) {

                evidenceDirectory.mkdirs()
            }


            // -------------------------------------------------
            // GET ORIGINAL FILE NAME
            // -------------------------------------------------

            var originalFileName =
                getFileName(uri)

            if (
                originalFileName.isNullOrBlank()
            ) {

                originalFileName =
                    "$prefix"
            }


            // -------------------------------------------------
            // GET FILE EXTENSION
            // -------------------------------------------------

            val mimeType =
                contentResolver.getType(uri)

            val extension =
                getFileExtension(
                    originalFileName,
                    mimeType
                )


            // -------------------------------------------------
            // CREATE UNIQUE FILE NAME
            // -------------------------------------------------

            val fileName =
                "${prefix}_${System.currentTimeMillis()}$extension"


            // -------------------------------------------------
            // CREATE DESTINATION FILE
            // -------------------------------------------------

            val destinationFile =
                File(
                    evidenceDirectory,
                    fileName
                )


            // -------------------------------------------------
            // OPEN ORIGINAL FILE
            // -------------------------------------------------

            val inputStream =
                contentResolver.openInputStream(uri)
                    ?: return null


            // -------------------------------------------------
            // COPY FILE
            // -------------------------------------------------

            inputStream.use { input ->

                FileOutputStream(
                    destinationFile
                ).use { output ->

                    input.copyTo(output)
                }
            }


            // -------------------------------------------------
            // RETURN REAL FILE PATH
            // -------------------------------------------------

            destinationFile.absolutePath

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }


    // =========================================================
    // GET ORIGINAL FILE NAME
    // =========================================================

    private fun getFileName(
        uri: Uri
    ): String? {

        var fileName: String? = null

        try {

            contentResolver.query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val nameIndex =
                        cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )

                    if (nameIndex >= 0) {

                        fileName =
                            cursor.getString(
                                nameIndex
                            )
                    }
                }
            }

        } catch (_: Exception) {
        }

        return fileName
    }


    // =========================================================
    // GET FILE EXTENSION
    // =========================================================

    private fun getFileExtension(
        fileName: String?,
        mimeType: String?
    ): String {

        // -----------------------------------------------------
        // Try original file name
        // -----------------------------------------------------

        if (!fileName.isNullOrBlank()) {

            val dotPosition =
                fileName.lastIndexOf(".")

            if (dotPosition >= 0) {

                return fileName.substring(
                    dotPosition
                )
            }
        }


        // -----------------------------------------------------
        // Try MIME type
        // -----------------------------------------------------

        if (!mimeType.isNullOrBlank()) {

            val extension =
                MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(
                        mimeType
                    )

            if (!extension.isNullOrBlank()) {

                return ".$extension"
            }
        }

        return ""
    }


    // =========================================================
    // REQUEST LOCATION
    // =========================================================

    private fun requestCurrentLocation() {

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

        if (!fineGranted && !coarseGranted) {

            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            return
        }

        getCurrentLocation()
    }


    // =========================================================
    // GET CURRENT LOCATION
    // =========================================================

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

        if (!fineGranted && !coarseGranted) {

            return
        }

        try {

            val gpsEnabled =
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )

            val networkEnabled =
                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )


            if (!gpsEnabled && !networkEnabled) {

                Toast.makeText(
                    this,
                    "Please turn on Location Services.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }


            etLocation.setText(
                "Getting current location..."
            )


            // -------------------------------------------------
            // GPS
            // -------------------------------------------------

            if (gpsEnabled) {

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    locationListener
                )
            }


            // -------------------------------------------------
            // NETWORK
            // -------------------------------------------------

            if (networkEnabled) {

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    locationListener
                )
            }


            // -------------------------------------------------
            // LAST KNOWN GPS LOCATION
            // -------------------------------------------------

            var lastLocation: Location? =
                null

            try {

                lastLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

            } catch (_: SecurityException) {
            }


            // -------------------------------------------------
            // LAST KNOWN NETWORK LOCATION
            // -------------------------------------------------

            if (lastLocation == null) {

                try {

                    lastLocation =
                        locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                        )

                } catch (_: SecurityException) {
                }
            }


            // -------------------------------------------------
            // DISPLAY LAST LOCATION
            // -------------------------------------------------

            if (lastLocation != null) {

                currentLocation =
                    lastLocation

                etLocation.setText(
                    String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        lastLocation.latitude,
                        lastLocation.longitude
                    )
                )
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to get current location.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // STOP LOCATION UPDATES
    // =========================================================

    private fun stopLocationUpdates() {

        try {

            locationManager.removeUpdates(
                locationListener
            )

        } catch (_: SecurityException) {
        }
    }


    // =========================================================
    // SUBMIT CAMPUS REPORT
    // =========================================================

    private fun submitCampusReport() {

        val currentUser =
            auth.currentUser


        // =====================================================
        // LOGIN CHECK
        // =====================================================

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please login to submit a campus report.",
                Toast.LENGTH_LONG
            ).show()

            val intent =
                Intent(
                    this,
                    CampusLoginActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            return
        }


        // =====================================================
        // INCIDENT TYPE
        // =====================================================

        val incidentType =
            spIncidentType.selectedItem
                ?.toString()
                ?: ""


        // =====================================================
        // CAMPUS
        // =====================================================

        val campus =
            spCampus.selectedItem
                ?.toString()
                ?: ""


        // =====================================================
        // LOCATION
        // =====================================================

        val location =
            etLocation.text
                .toString()
                .trim()


        // =====================================================
        // DESCRIPTION
        // =====================================================

        val description =
            etDescription.text
                .toString()
                .trim()


        // =====================================================
        // VALIDATION
        // =====================================================

        if (
            incidentType.isEmpty() ||
            incidentType == "Select incident type"
        ) {

            Toast.makeText(
                this,
                "Please select an incident type.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (
            campus.isEmpty() ||
            campus == "Select campus"
        ) {

            Toast.makeText(
                this,
                "Please select a campus.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (location.isEmpty()) {

            Toast.makeText(
                this,
                "Please enter or select the incident location.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (description.isEmpty()) {

            Toast.makeText(
                this,
                "Please describe what happened.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (description.length < 10) {

            Toast.makeText(
                this,
                "Please provide more details about the incident.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (description.length > 500) {

            Toast.makeText(
                this,
                "Description cannot exceed 500 characters.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =====================================================
        // DATE AND TIME
        // =====================================================

        val calendar =
            Calendar.getInstance()

        val dateFormat =
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )

        val timeFormat =
            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            )

        val dateValue =
            dateFormat.format(
                calendar.time
            )

        val timeValue =
            timeFormat.format(
                calendar.time
            )

        val dateTimeValue =
            "$dateValue, $timeValue"


        // =====================================================
        // CREATE FIRESTORE DOCUMENT
        // =====================================================

        val reportReference =
            db.collection(
                "IncidentReports"
            ).document()

        val reportId =
            reportReference.id


        // =====================================================
        // DISABLE SUBMIT BUTTON
        // =====================================================

        btnSubmitCampusReport.isEnabled =
            false

        btnSubmitCampusReport.text =
            "Submitting..."


        // =====================================================
        // REPORT DATA
        // =====================================================

        val reportData =
            hashMapOf<String, Any>(

                "reportId" to reportId,

                "userId" to currentUser.uid,

                "incidentType" to incidentType,

                "campus" to campus,

                "location" to location,

                "description" to description,

                "date" to dateValue,

                "time" to timeValue,

                "dateTime" to dateTimeValue,

                "status" to "Submitted",

                "campusSafetyNotified" to true,

                "createdAt" to System.currentTimeMillis()
            )


        // =====================================================
        // GPS COORDINATES
        // =====================================================

        currentLocation?.let { locationData ->

            reportData["latitude"] =
                locationData.latitude

            reportData["longitude"] =
                locationData.longitude
        }


        // =====================================================
        // PHOTO EVIDENCE
        // =====================================================

        selectedPhotoPath?.let { path ->

            reportData["photoEvidence"] =
                path
        }


        // =====================================================
        // VIDEO EVIDENCE
        // =====================================================

        selectedVideoPath?.let { path ->

            reportData["videoEvidence"] =
                path
        }


        // =====================================================
        // AUDIO EVIDENCE
        // =====================================================

        selectedAudioPath?.let { path ->

            reportData["audioEvidence"] =
                path
        }


        // =====================================================
        // FILE EVIDENCE
        // =====================================================

        selectedFilePath?.let { path ->

            reportData["fileEvidence"] =
                path
        }


        // =====================================================
        // SAVE REPORT TO FIRESTORE
        // =====================================================

        reportReference
            .set(reportData)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Campus report submitted successfully.",
                    Toast.LENGTH_SHORT
                ).show()


                // =============================================
                // OPEN CONFIRMATION SCREEN
                // =============================================

                val intent =
                    Intent(
                        this,
                        CampusIncidentConfirmationActivity::class.java
                    )


                intent.putExtra(
                    "reportId",
                    reportId
                )


                intent.putExtra(
                    "incidentType",
                    incidentType
                )


                intent.putExtra(
                    "campus",
                    campus
                )


                intent.putExtra(
                    "location",
                    location
                )


                intent.putExtra(
                    "date",
                    dateValue
                )


                intent.putExtra(
                    "time",
                    timeValue
                )


                intent.putExtra(
                    "dateTime",
                    dateTimeValue
                )


                startActivity(intent)

                finish()
            }


            .addOnFailureListener { exception ->

                btnSubmitCampusReport.isEnabled =
                    true

                btnSubmitCampusReport.text =
                    "Submit Campus Report"


                Toast.makeText(
                    this,
                    "Failed to submit report: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // ON DESTROY
    // =========================================================

    override fun onDestroy() {

        stopLocationUpdates()

        super.onDestroy()
    }
}