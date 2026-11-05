package com.example.safezone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
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
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.IncidentReportDao
import com.example.safezone.data.IncidentReportEntity
import com.example.safezone.data.SafeZoneDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CampusIncidentReportActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Local database
    private lateinit var localDatabase: SafeZoneDatabase
    private lateinit var incidentReportDao: IncidentReportDao

    // Views
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

    // Location
    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null

    // Selected evidence files
    private var selectedPhotoPath: String? = null
    private var selectedVideoPath: String? = null
    private var selectedAudioPath: String? = null
    private var selectedFilePath: String? = null

    // Location permission request
    private val locationPermissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            val coarseLocation =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocation || coarseLocation) {
                getCurrentLocation()
            } else {
                showToast(R.string.cir_location_permission_required, true)
            }
        }

    // Photo picker
    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                saveSelectedEvidence(it, "photo")
            }
        }

    // Video picker
    private val videoPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                saveSelectedEvidence(it, "video")
            }
        }

    // Audio picker
    private val audioPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                saveSelectedEvidence(it, "audio")
            }
        }

    // General file picker
    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                saveSelectedEvidence(it, "file")
            }
        }

    // Location listener
    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            currentLocation = location

            etLocation.setText(
                String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    location.latitude,
                    location.longitude
                )
            )

            stopLocationUpdates()
        }

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    // ---------------------------------------------------------
    // CREATE ACTIVITY
    // ---------------------------------------------------------

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            SafeZoneLanguageManager.wrapContext(newBase)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_incident_report)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        localDatabase =
            SafeZoneDatabase.getDatabase(applicationContext)

        incidentReportDao = localDatabase.incidentReportDao()

        initializeViews()
        setupIncidentTypeSpinner()
        setupCampusSpinner()
        setupCharacterCounter()
        setupButtons()
    }

    // ---------------------------------------------------------
    // INITIALIZE VIEWS
    // ---------------------------------------------------------

    private fun initializeViews() {
        tvBack = findViewById(R.id.tvBack)
        spIncidentType = findViewById(R.id.spIncidentType)
        spCampus = findViewById(R.id.spCampus)
        etLocation = findViewById(R.id.etLocation)
        etDescription = findViewById(R.id.etDescription)
        tvCharacterCount = findViewById(R.id.tvCharacterCount)
        ivCurrentLocation = findViewById(R.id.ivCurrentLocation)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        btnAddVideo = findViewById(R.id.btnAddVideo)
        btnAddAudio = findViewById(R.id.btnAddAudio)
        btnAddFile = findViewById(R.id.btnAddFile)
        btnSubmitCampusReport =
            findViewById(R.id.btnSubmitCampusReport)

        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager
    }

    // ---------------------------------------------------------
    // INCIDENT TYPE SPINNER
    // ---------------------------------------------------------

    private fun setupIncidentTypeSpinner() {
        val incidentTypes = resources.getStringArray(
            R.array.cir_incident_types
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
    }

    // ---------------------------------------------------------
    // CAMPUS SPINNER
    // ---------------------------------------------------------

    private fun setupCampusSpinner() {
        val campuses = resources.getStringArray(
            R.array.cir_campuses
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            campuses
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCampus.adapter = adapter
    }

    // ---------------------------------------------------------
    // CHARACTER COUNTER
    // ---------------------------------------------------------

    private fun setupCharacterCounter() {
        etDescription.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    tvCharacterCount.text = getString(
                        R.string.cir_character_count,
                        s?.length ?: 0
                    )
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )
    }

    // ---------------------------------------------------------
    // BUTTONS
    // ---------------------------------------------------------

    private fun setupButtons() {

        // Back button
        tvBack.setOnClickListener {
            finish()
        }

        // Current location
        ivCurrentLocation.setOnClickListener {
            requestCurrentLocation()
        }

        // Evidence pickers
        btnAddPhoto.setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }

        btnAddVideo.setOnClickListener {
            videoPicker.launch(arrayOf("video/*"))
        }

        btnAddAudio.setOnClickListener {
            audioPicker.launch(arrayOf("audio/*"))
        }

        btnAddFile.setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }

        // Submit report
        btnSubmitCampusReport.setOnClickListener {
            submitCampusReport()
        }

        // Set up functional bottom navigation
        setupBottomNavigation()
    }

    // ---------------------------------------------------------
    // BOTTOM NAVIGATION
    // ---------------------------------------------------------

    private fun setupBottomNavigation() {

        // Home
        findViewById<LinearLayout>(R.id.navHome)
            .setOnClickListener {
                navigateToScreen(CampusHomeActivity::class.java)
            }

        // Safety map
        findViewById<LinearLayout>(R.id.navMap)
            .setOnClickListener {
                navigateToScreen(CampusSafetyMapActivity::class.java)
            }

        // Incident reports
        findViewById<LinearLayout>(R.id.navReports)
            .setOnClickListener {
                navigateToScreen(CampusReportsActivity::class.java)
            }

        // Notifications and alerts
        findViewById<LinearLayout>(R.id.navNotifications)
            .setOnClickListener {
                navigateToScreen(CampusAlertsActivity::class.java)
            }

        // Profile
        findViewById<LinearLayout>(R.id.navProfile)
            .setOnClickListener {
                navigateToScreen(CampusProfileActivity::class.java)
            }
    }

    // ---------------------------------------------------------
    // NAVIGATE TO A SCREEN
    // ---------------------------------------------------------

    private fun navigateToScreen(destination: Class<*>) {

        try {
            startActivity(
                Intent(this, destination)
            )
        } catch (exception: Exception) {

            Toast.makeText(
                this,
                getString(R.string.cir_navigation_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ---------------------------------------------------------
    // SAVE SELECTED EVIDENCE
    // ---------------------------------------------------------

    private fun saveSelectedEvidence(uri: Uri, prefix: String) {
        val savedPath = copyUriToInternalStorage(uri, prefix)

        if (savedPath == null) {
            showToast(R.string.cir_evidence_save_failed, true)
            return
        }

        when (prefix) {
            "photo" -> selectedPhotoPath = savedPath
            "video" -> selectedVideoPath = savedPath
            "audio" -> selectedAudioPath = savedPath
            "file" -> selectedFilePath = savedPath
        }

        val message = when (prefix) {
            "photo" -> R.string.cir_photo_saved
            "video" -> R.string.cir_video_saved
            "audio" -> R.string.cir_audio_saved
            else -> R.string.cir_file_saved
        }

        showToast(message)
    }

    // ---------------------------------------------------------
    // COPY FILE TO PRIVATE APP STORAGE
    // ---------------------------------------------------------

    private fun copyUriToInternalStorage(
        uri: Uri,
        prefix: String
    ): String? {
        return try {
            val evidenceDirectory = File(filesDir, "evidence")

            if (!evidenceDirectory.exists() &&
                !evidenceDirectory.mkdirs()
            ) {
                return null
            }

            val originalFileName =
                getFileName(uri).takeUnless { it.isNullOrBlank() }
                    ?: prefix

            val mimeType = contentResolver.getType(uri)
            val extension =
                getFileExtension(originalFileName, mimeType)

            val fileName =
                "${prefix}_${System.currentTimeMillis()}$extension"

            val destinationFile = File(evidenceDirectory, fileName)

            val inputStream =
                contentResolver.openInputStream(uri) ?: return null

            inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ---------------------------------------------------------
    // GET ORIGINAL FILE NAME
    // ---------------------------------------------------------

    private fun getFileName(uri: Uri): String? {
        return try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // ---------------------------------------------------------
    // GET FILE EXTENSION
    // ---------------------------------------------------------

    private fun getFileExtension(
        fileName: String?,
        mimeType: String?
    ): String {
        if (!fileName.isNullOrBlank()) {
            val dotPosition = fileName.lastIndexOf('.')

            if (dotPosition >= 0 &&
                dotPosition < fileName.length - 1
            ) {
                return fileName.substring(dotPosition)
            }
        }

        if (!mimeType.isNullOrBlank()) {
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)

            if (!extension.isNullOrBlank()) {
                return ".$extension"
            }
        }

        return ""
    }

    // ---------------------------------------------------------
    // REQUEST LOCATION PERMISSION
    // ---------------------------------------------------------

    private fun requestCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
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

    // ---------------------------------------------------------
    // GET CURRENT LOCATION
    // ---------------------------------------------------------

    private fun getCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return

        try {
            val gpsEnabled = locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

            val networkEnabled = locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )

            if (!gpsEnabled && !networkEnabled) {
                showToast(R.string.cir_enable_location_services, true)
                return
            }

            etLocation.setText(
                getString(R.string.cir_getting_location)
            )

            if (gpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    locationListener
                )
            }

            if (networkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    locationListener
                )
            }

            var lastLocation: Location? = null

            if (gpsEnabled) {
                lastLocation = try {
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )
                } catch (_: SecurityException) {
                    null
                }
            }

            if (lastLocation == null && networkEnabled) {
                lastLocation = try {
                    locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
                } catch (_: SecurityException) {
                    null
                }
            }

            if (lastLocation != null) {
                currentLocation = lastLocation

                etLocation.setText(
                    String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        lastLocation.latitude,
                        lastLocation.longitude
                    )
                )
            }
        } catch (e: SecurityException) {
            showToast(R.string.cir_location_permission_required, true)
        } catch (e: Exception) {
            showToast(R.string.cir_location_failed, true)
        }
    }

    // ---------------------------------------------------------
    // STOP LOCATION UPDATES
    // ---------------------------------------------------------

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: SecurityException) {
            // Permission may have been revoked.
        }
    }

    // ---------------------------------------------------------
    // CHECK INTERNET CONNECTIVITY
    // ---------------------------------------------------------

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    // ---------------------------------------------------------
    // SUBMIT CAMPUS REPORT - SAVE LOCALLY FIRST
    // ---------------------------------------------------------

    private fun submitCampusReport() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showToast(R.string.cir_login_required, true)

            val intent = Intent(this, CampusLoginActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            return
        }

        val incidentType =
            spIncidentType.selectedItem?.toString().orEmpty()

        val campus =
            spCampus.selectedItem?.toString().orEmpty()

        val location = etLocation.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Spinner position 0 is the placeholder in every language.
        if (spIncidentType.selectedItemPosition <= 0) {
            showToast(R.string.cir_select_incident_type)
            return
        }

        if (spCampus.selectedItemPosition <= 0) {
            showToast(R.string.cir_select_campus)
            return
        }

        if (location.isBlank() ||
            location == getString(R.string.cir_getting_location)
        ) {
            showToast(R.string.cir_enter_location)
            return
        }

        if (description.isBlank()) {
            showToast(R.string.cir_enter_description)
            return
        }

        if (description.length < 10) {
            showToast(R.string.cir_description_too_short)
            return
        }

        if (description.length > 500) {
            showToast(R.string.cir_description_too_long)
            return
        }

        val calendar = Calendar.getInstance()

        val dateValue = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        ).format(calendar.time)

        val timeValue = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(calendar.time)

        val dateTimeValue = "$dateValue, $timeValue"
        val createdAt = System.currentTimeMillis()

        val reportReference =
            db.collection("IncidentReports").document()

        val reportId = reportReference.id

        val latitude = currentLocation?.latitude ?: 0.0
        val longitude = currentLocation?.longitude ?: 0.0

        val localReport = IncidentReportEntity(
            localId = reportId,
            userId = currentUser.uid,
            campus = campus,
            incidentType = incidentType,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            dateTime = dateTimeValue,
            status = "Submitted",
            photoEvidence = selectedPhotoPath ?: "",
            videoEvidence = selectedVideoPath ?: "",
            audioEvidence = selectedAudioPath ?: "",
            fileEvidence = selectedFilePath ?: "",
            createdAt = createdAt,
            syncStatus = "Pending Sync"
        )

        btnSubmitCampusReport.isEnabled = false
        btnSubmitCampusReport.text =
            getString(R.string.cir_saving_report)

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    incidentReportDao.insert(localReport)
                }

                if (isInternetAvailable()) {
                    uploadCampusReport(
                        report = localReport,
                        reportReference = reportReference,
                        dateValue = dateValue,
                        timeValue = timeValue
                    )
                } else {
                    showToast(R.string.cir_saved_offline, true)

                    openConfirmationScreen(
                        reportId = reportId,
                        incidentType = incidentType,
                        campus = campus,
                        location = location,
                        dateValue = dateValue,
                        timeValue = timeValue,
                        dateTimeValue = dateTimeValue,
                        offline = true
                    )
                }
            } catch (exception: Exception) {
                btnSubmitCampusReport.isEnabled = true
                btnSubmitCampusReport.text =
                    getString(R.string.cir_submit_report)

                Toast.makeText(
                    this@CampusIncidentReportActivity,
                    getString(
                        R.string.cir_local_save_failed,
                        exception.message ?: ""
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------------------------------------------------
    // UPLOAD REPORT TO FIRESTORE
    // ---------------------------------------------------------

    private fun uploadCampusReport(
        report: IncidentReportEntity,
        reportReference: DocumentReference,
        dateValue: String,
        timeValue: String
    ) {
        btnSubmitCampusReport.text =
            getString(R.string.cir_synchronising)

        val reportData = hashMapOf<String, Any>(
            "reportId" to report.localId,
            "userId" to report.userId,
            "incidentType" to report.incidentType,
            "campus" to report.campus,
            "location" to report.location,
            "description" to report.description,
            "date" to dateValue,
            "time" to timeValue,
            "dateTime" to report.dateTime,
            "status" to report.status,
            "campusSafetyNotified" to true,
            "createdAt" to report.createdAt
        )

        if (report.latitude != 0.0 || report.longitude != 0.0) {
            reportData["latitude"] = report.latitude
            reportData["longitude"] = report.longitude
        }

        if (report.photoEvidence.isNotBlank()) {
            reportData["photoEvidence"] = report.photoEvidence
        }

        if (report.videoEvidence.isNotBlank()) {
            reportData["videoEvidence"] = report.videoEvidence
        }

        if (report.audioEvidence.isNotBlank()) {
            reportData["audioEvidence"] = report.audioEvidence
        }

        if (report.fileEvidence.isNotBlank()) {
            reportData["fileEvidence"] = report.fileEvidence
        }

        reportReference.set(reportData)
            .addOnSuccessListener {
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            incidentReportDao.updateSyncStatus(
                                localId = report.localId,
                                userId = report.userId,
                                syncStatus = "Synced"
                            )
                        }
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }

                    showToast(R.string.cir_submission_success)

                    openConfirmationScreen(
                        reportId = report.localId,
                        incidentType = report.incidentType,
                        campus = report.campus,
                        location = report.location,
                        dateValue = dateValue,
                        timeValue = timeValue,
                        dateTimeValue = report.dateTime,
                        offline = false
                    )
                }
            }
            .addOnFailureListener {
                showToast(R.string.cir_sync_pending, true)

                openConfirmationScreen(
                    reportId = report.localId,
                    incidentType = report.incidentType,
                    campus = report.campus,
                    location = report.location,
                    dateValue = dateValue,
                    timeValue = timeValue,
                    dateTimeValue = report.dateTime,
                    offline = true
                )
            }
    }

    // ---------------------------------------------------------
    // OPEN CONFIRMATION SCREEN
    // ---------------------------------------------------------

    private fun openConfirmationScreen(
        reportId: String,
        incidentType: String,
        campus: String,
        location: String,
        dateValue: String,
        timeValue: String,
        dateTimeValue: String,
        offline: Boolean
    ) {
        val intent = Intent(
            this,
            CampusIncidentConfirmationActivity::class.java
        )

        intent.putExtra("reportId", reportId)
        intent.putExtra("incidentType", incidentType)
        intent.putExtra("campus", campus)
        intent.putExtra("location", location)
        intent.putExtra("date", dateValue)
        intent.putExtra("time", timeValue)
        intent.putExtra("dateTime", dateTimeValue)
        intent.putExtra("offlineReport", offline)

        startActivity(intent)
        finish()
    }

    // ---------------------------------------------------------
    // TOAST HELPER
    // ---------------------------------------------------------

    private fun showToast(
        messageRes: Int,
        longDuration: Boolean = false
    ) {
        Toast.makeText(
            this,
            getString(messageRes),
            if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    // ---------------------------------------------------------
    // DESTROY ACTIVITY
    // ---------------------------------------------------------

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}
