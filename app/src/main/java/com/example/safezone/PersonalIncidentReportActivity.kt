
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
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.IncidentReportRepository
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.sync.IncidentReportSyncScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonalIncidentReportActivity : AppCompatActivity() {

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

    private lateinit var auth: FirebaseAuth
    private lateinit var reportRepository: IncidentReportRepository

    private var selectedPhotoUri: Uri? = null
    private var selectedVideoUri: Uri? = null
    private var selectedFileUri: Uri? = null

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    companion object {
        private const val PHOTO_REQUEST = 2001
        private const val VIDEO_REQUEST = 2002
        private const val FILE_REQUEST = 2003
        private const val LOCATION_REQUEST = 2004
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_incident_report)

        tvBack = findViewById(R.id.tvBack)
        spIncidentType = findViewById(R.id.spIncidentType)
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        etDateTime = findViewById(R.id.etDateTime)

        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        btnAddVideo = findViewById(R.id.btnAddVideo)
        btnAddFile = findViewById(R.id.btnAddFile)
        tvEvidence = findViewById(R.id.tvEvidence)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)

        auth = FirebaseAuth.getInstance()

        val database =
            SafeZoneDatabase.getDatabase(applicationContext)

        reportRepository = IncidentReportRepository(
            dao = database.incidentReportDao(),
            firestore = FirebaseFirestore.getInstance()
        )

        tvBack.setOnClickListener {
            finish()
        }

        // Localized incident types.
        // Keep the first item at position zero for validation.
        val incidentTypes = arrayOf(
            getString(R.string.personal_incident_type_select),
            getString(R.string.personal_incident_type_theft),
            getString(R.string.personal_incident_type_assault),
            getString(R.string.personal_incident_type_harassment),
            getString(R.string.personal_incident_type_accident),
            getString(R.string.personal_incident_type_suspicious),
            getString(R.string.personal_incident_type_property_damage),
            getString(R.string.personal_incident_type_missing_person),
            getString(R.string.personal_incident_type_other)
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

        etDateTime.setText(
            SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
            ).format(Date())
        )

        tvEvidence.text =
            getString(R.string.personal_incident_no_evidence)

        getCurrentLocation()

        btnAddPhoto.setOnClickListener {
            openEvidencePicker("image/*", PHOTO_REQUEST)
        }

        btnAddVideo.setOnClickListener {
            openEvidencePicker("video/*", VIDEO_REQUEST)
        }

        btnAddFile.setOnClickListener {
            openEvidencePicker("*/*", FILE_REQUEST)
        }

        btnSubmitReport.setOnClickListener {
            submitReport()
        }
    }

    // ==================================================
    // OPEN EVIDENCE PICKER
    // ==================================================

    private fun openEvidencePicker(
        mimeType: String,
        requestCode: Int
    ) {
        val intent = Intent(
            Intent.ACTION_OPEN_DOCUMENT
        ).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

        startActivityForResult(intent, requestCode)
    }

    // ==================================================
    // DISPLAY SELECTED EVIDENCE
    // ==================================================

    private fun updateEvidenceDisplay() {
        val evidence = mutableListOf<String>()

        selectedPhotoUri?.let {
            evidence.add(
                getString(
                    R.string.personal_incident_evidence_photo,
                    it.lastPathSegment
                        ?: getString(R.string.personal_incident_selected)
                )
            )
        }

        selectedVideoUri?.let {
            evidence.add(
                getString(
                    R.string.personal_incident_evidence_video,
                    it.lastPathSegment
                        ?: getString(R.string.personal_incident_selected)
                )
            )
        }

        selectedFileUri?.let {
            evidence.add(
                getString(
                    R.string.personal_incident_evidence_file,
                    it.lastPathSegment
                        ?: getString(R.string.personal_incident_selected)
                )
            )
        }

        tvEvidence.text = if (evidence.isEmpty()) {
            getString(R.string.personal_incident_no_evidence)
        } else {
            evidence.joinToString("\n")
        }
    }

    // ==================================================
    // GET CURRENT LOCATION
    // ==================================================

    private fun getCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

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

        val locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        val gpsEnabled = try {
            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        } catch (_: Exception) {
            false
        }

        val networkEnabled = try {
            locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        } catch (_: Exception) {
            false
        }

        if (!gpsEnabled && !networkEnabled) {
            currentLatitude = null
            currentLongitude = null

            etLocation.setText(
                getString(R.string.personal_incident_location_services_off)
            )

            Toast.makeText(
                this,
                getString(R.string.personal_incident_enable_location),
                Toast.LENGTH_LONG
            ).show()

            try {
                startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            } catch (_: Exception) {
                // Settings could not be opened.
            }

            return
        }

        var bestLocation: Location? = null

        if (gpsEnabled) {
            try {
                bestLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )
            } catch (_: SecurityException) {
                // Permission may have changed.
            }
        }

        if (networkEnabled) {
            try {
                val networkLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )

                if (
                    networkLocation != null &&
                    (
                            bestLocation == null ||
                                    networkLocation.accuracy <
                                    bestLocation.accuracy
                            )
                ) {
                    bestLocation = networkLocation
                }
            } catch (_: SecurityException) {
                // Permission may have changed.
            }
        }

        if (bestLocation != null) {
            currentLatitude = bestLocation.latitude
            currentLongitude = bestLocation.longitude

            etLocation.setText(
                getString(
                    R.string.personal_incident_coordinates_display,
                    bestLocation.latitude.toString(),
                    bestLocation.longitude.toString()
                )
            )
        } else {
            currentLatitude = null
            currentLongitude = null

            etLocation.setText(
                getString(R.string.personal_incident_location_unavailable)
            )
        }
    }

    // ==================================================
    // LOCATION PERMISSIONS
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

        if (requestCode == LOCATION_REQUEST) {
            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                getCurrentLocation()
            } else {
                currentLatitude = null
                currentLongitude = null

                etLocation.setText(
                    getString(
                        R.string.personal_incident_location_permission_denied
                    )
                )

                Toast.makeText(
                    this,
                    getString(
                        R.string.personal_incident_location_permission_required
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ==================================================
    // HANDLE SELECTED PHOTO, VIDEO OR FILE
    // ==================================================

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        val resultIntent = data ?: return
        val uri = resultIntent.data ?: return

        try {
            val hasReadPermission =
                (resultIntent.flags and
                        Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0

            if (hasReadPermission) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        } catch (_: Exception) {
            // Continue if the provider does not support
            // persistable URI permissions.
        }

        when (requestCode) {
            PHOTO_REQUEST -> selectedPhotoUri = uri
            VIDEO_REQUEST -> selectedVideoUri = uri
            FILE_REQUEST -> selectedFileUri = uri
        }

        updateEvidenceDisplay()
    }

    // ==================================================
    // SUBMIT REPORT
    // ==================================================

    private fun submitReport() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                getString(R.string.personal_incident_login_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (spIncidentType.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                getString(R.string.personal_incident_select_type_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val incidentType =
            spIncidentType.selectedItem.toString()

        val description =
            etDescription.text.toString().trim()

        val location =
            etLocation.text.toString().trim()

        val dateTime =
            etDateTime.text.toString().trim()

        if (description.isEmpty()) {
            etDescription.error =
                getString(R.string.personal_incident_description_error)

            etDescription.requestFocus()
            return
        }

        if (
            location.isBlank() ||
            location.contains("unavailable", ignoreCase = true) ||
            location.contains("turned off", ignoreCase = true) ||
            location.contains(
                "permission not granted",
                ignoreCase = true
            ) ||
            location.contains(
                "ayitholakali",
                ignoreCase = true
            ) ||
            location.contains(
                "ga e teng",
                ignoreCase = true
            )
        ) {
            Toast.makeText(
                this,
                getString(
                    R.string.personal_incident_valid_location_error
                ),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val latitude = currentLatitude
        val longitude = currentLongitude

        if (latitude == null || longitude == null) {
            Toast.makeText(
                this,
                getString(
                    R.string.personal_incident_coordinates_unavailable
                ),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Preserve all three independent evidence selections.
        val photoUri = selectedPhotoUri?.toString() ?: ""
        val videoUri = selectedVideoUri?.toString() ?: ""
        val fileUri = selectedFileUri?.toString() ?: ""

        val photoName = selectedPhotoUri?.lastPathSegment ?: ""
        val videoName = selectedVideoUri?.lastPathSegment ?: ""
        val fileName = selectedFileUri?.lastPathSegment ?: ""

        val userId = currentUser.uid

        btnSubmitReport.isEnabled = false
        btnSubmitReport.text =
            getString(R.string.personal_incident_saving)

        lifecycleScope.launch {
            try {
                // Save locally first. The repository's existing
                // implementation is retained.
                reportRepository.saveReportLocally(
                    userId = userId,
                    incidentType = incidentType,
                    description = description,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    dateTime = dateTime,
                    photoEvidence = photoUri,
                    videoEvidence = videoUri,
                    fileEvidence = fileUri,
                    photoName = photoName,
                    videoName = videoName,
                    fileName = fileName,
                    createdAt = System.currentTimeMillis()
                )

                // Schedule synchronization separately.
                IncidentReportSyncScheduler.schedule(
                    applicationContext,
                    userId
                )

                Toast.makeText(
                    this@PersonalIncidentReportActivity,
                    getString(
                        R.string.personal_incident_saved_offline
                    ),
                    Toast.LENGTH_LONG
                ).show()

                finish()
            } catch (exception: Exception) {
                Toast.makeText(
                    this@PersonalIncidentReportActivity,
                    getString(
                        R.string.personal_incident_save_failed,
                        exception.message
                            ?: getString(
                                R.string.personal_incident_unknown_error
                            )
                    ),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnSubmitReport.isEnabled = true
                btnSubmitReport.text =
                    getString(R.string.personal_incident_submit)
            }
        }
    }
}
