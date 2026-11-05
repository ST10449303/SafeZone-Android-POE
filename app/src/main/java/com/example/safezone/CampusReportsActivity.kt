package com.example.safezone

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.IncidentReportDao
import com.example.safezone.data.IncidentReportEntity
import com.example.safezone.data.SafeZoneDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CampusReportsActivity : AppCompatActivity() {

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var tvBack: TextView
    private lateinit var reportsContainer: LinearLayout
    private lateinit var tvNoReports: TextView
    private lateinit var btnRefreshReports: TextView
    private lateinit var tvReportCount: TextView

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // =========================================================
    // ROOM DATABASE
    // =========================================================

    private lateinit var incidentReportDao: IncidentReportDao

    // =========================================================
    // REPORT OBSERVATION
    // =========================================================

    private var isObservingReports = false

    // =========================================================
    // REPORT DISPLAY MODEL
    // =========================================================

    private data class ReportDisplayData(
        val reportId: String,
        val userId: String,
        val incidentType: String,
        val campus: String,
        val description: String,
        val location: String,
        val status: String,
        val date: String?,
        val time: String?,
        val dateTime: String?,
        val createdAt: Long,
        val latitude: Double?,
        val longitude: Double?,
        val photoEvidence: String,
        val videoEvidence: String,
        val audioEvidence: String,
        val fileEvidence: String,
        val syncStatus: String
    )

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_reports)

        // Initialise Firebase.
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialise Room database.
        incidentReportDao =
            SafeZoneDatabase.getDatabase(applicationContext)
                .incidentReportDao()

        // Initialise views.
        tvBack = findViewById(R.id.tvBack)
        reportsContainer = findViewById(R.id.reportsContainer)
        tvNoReports = findViewById(R.id.tvNoReports)
        btnRefreshReports = findViewById(R.id.btnRefreshReports)
        tvReportCount = findViewById(R.id.tvReportCount)

        // Refresh reports when the refresh button is pressed.
        btnRefreshReports.setOnClickListener {
            refreshCampusReports()
        }

        // Back button.
        tvBack.setOnClickListener {
            finish()
        }

        // Observe reports stored locally.
        observeLocalReports()
    }

    // =========================================================
    // REFRESH WHEN SCREEN RESUMES
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (
            ::reportsContainer.isInitialized &&
            ::incidentReportDao.isInitialized &&
            ::auth.isInitialized
        ) {
            refreshCampusReports()
        }
    }

    // =========================================================
    // CHECK INTERNET CONNECTION
    // =========================================================

    private fun isInternetAvailable(): Boolean {

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        val network =
            connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    // =========================================================
    // OBSERVE LOCAL ROOM REPORTS
    // =========================================================

    private fun observeLocalReports() {

        if (isObservingReports) return

        val currentUser = auth.currentUser

        if (currentUser == null) {
            openCampusLogin()
            return
        }

        isObservingReports = true

        lifecycleScope.launch {

            incidentReportDao
                .getReportsForUser(currentUser.uid)
                .collect { reports ->

                    val displayData = reports.map { report ->
                        report.toDisplayData()
                    }

                    displayReports(displayData)
                }
        }
    }

    // =========================================================
    // REFRESH CAMPUS REPORTS
    // =========================================================

    private fun refreshCampusReports() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            openCampusLogin()
            return
        }

        // Room reports remain available when offline.
        if (!isInternetAvailable()) return

        // Retry uploading pending reports.
        syncPendingReports(currentUser.uid)

        // Retrieve this user's reports from Firestore.
        db.collection("IncidentReports")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->

                lifecycleScope.launch {

                    try {
                        for (document in documents.documents) {

                            val report =
                                document.toIncidentReportEntity(
                                    currentUser.uid
                                )

                            incidentReportDao.insert(report)
                        }

                    } catch (exception: Exception) {

                        Log.e(
                            TAG,
                            "Some reports could not be saved locally.",
                            exception
                        )

                        showToast(
                            R.string.campus_reports_local_save_warning
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->

                Log.e(
                    TAG,
                    "Unable to refresh reports from Firestore.",
                    exception
                )

                // Locally stored reports remain available.
                showToast(R.string.campus_reports_refresh_failed)
            }
    }

    // =========================================================
    // RETRY PENDING REPORT UPLOADS
    // =========================================================

    private fun syncPendingReports(userId: String) {

        if (!isInternetAvailable()) return

        lifecycleScope.launch {

            try {

                val pendingReports =
                    incidentReportDao.getPendingSyncReports(userId)

                for (report in pendingReports) {
                    uploadPendingReport(report)
                }

            } catch (exception: Exception) {

                Log.e(
                    TAG,
                    "Unable to retrieve pending reports.",
                    exception
                )
            }
        }
    }

    // =========================================================
    // UPLOAD A PENDING REPORT
    // =========================================================

    private fun uploadPendingReport(
        report: IncidentReportEntity
    ) {

        if (!isInternetAvailable()) return

        // Keep the same Firestore document ID when retrying.
        val reportReference =
            db.collection("IncidentReports")
                .document(report.localId)

        val dateParts = splitDateTime(report.dateTime)

        val reportData = hashMapOf<String, Any>(
            "reportId" to report.localId,
            "userId" to report.userId,
            "incidentType" to report.incidentType,
            "campus" to report.campus,
            "description" to report.description,
            "location" to report.location,
            "date" to dateParts.first,
            "time" to dateParts.second,
            "dateTime" to report.dateTime,
            "status" to report.status,
            "campusSafetyNotified" to false,
            "createdAt" to report.createdAt,
            "latitude" to report.latitude,
            "longitude" to report.longitude,
            "photoEvidence" to report.photoEvidence,
            "videoEvidence" to report.videoEvidence,
            "audioEvidence" to report.audioEvidence,
            "fileEvidence" to report.fileEvidence
        )

        reportReference
            .set(reportData, SetOptions.merge())
            .addOnSuccessListener {

                lifecycleScope.launch {

                    try {

                        incidentReportDao.updateSyncStatus(
                            localId = report.localId,
                            userId = report.userId,
                            syncStatus = "Synced"
                        )

                    } catch (exception: Exception) {

                        Log.e(
                            TAG,
                            "Report uploaded, but local sync status could not be updated.",
                            exception
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->

                // Leave the local report available for another retry.
                Log.e(
                    TAG,
                    "Pending report upload failed: ${report.localId}",
                    exception
                )
            }
    }

    // =========================================================
    // SPLIT DATE AND TIME
    // =========================================================

    private fun splitDateTime(
        dateTime: String
    ): Pair<String, String> {

        if (dateTime.isBlank()) {
            return Pair("", "")
        }

        val parts = dateTime.split(",", limit = 2)

        return if (parts.size >= 2) {
            Pair(parts[0].trim(), parts[1].trim())
        } else {
            Pair(dateTime.trim(), "")
        }
    }

    // =========================================================
    // CONVERT ROOM ENTITY TO DISPLAY MODEL
    // =========================================================

    private fun IncidentReportEntity.toDisplayData():
            ReportDisplayData {

        val dateParts = splitDateTime(dateTime)

        return ReportDisplayData(
            reportId = localId,
            userId = userId,
            incidentType = incidentType,
            campus = campus.ifBlank {
                getString(R.string.campus_reports_campus_not_provided)
            },
            description = description,
            location = location,
            status = status,
            date = dateParts.first,
            time = dateParts.second,
            dateTime = dateTime,
            createdAt = createdAt,
            latitude = latitude,
            longitude = longitude,
            photoEvidence = photoEvidence,
            videoEvidence = videoEvidence,
            audioEvidence = audioEvidence,
            fileEvidence = fileEvidence,
            syncStatus = syncStatus
        )
    }

    // =========================================================
    // CONVERT FIRESTORE DOCUMENT TO ROOM ENTITY
    // =========================================================

    private fun DocumentSnapshot.toIncidentReportEntity(
        currentUserId: String
    ): IncidentReportEntity {

        val documentDate = getString("date").orEmpty()
        val documentTime = getString("time").orEmpty()
        val documentDateTime = getString("dateTime").orEmpty()

        val finalDateTime = documentDateTime.ifBlank {
            listOf(documentDate, documentTime)
                .filter { it.isNotBlank() }
                .joinToString(", ")
        }

        val finalCreatedAt =
            getLong("createdAt")
                ?: getTimestamp("createdAt")
                    ?.toDate()
                    ?.time
                ?: 0L

        return IncidentReportEntity(
            localId = getString("reportId") ?: id,
            userId = getString("userId") ?: currentUserId,
            campus = getString("campus")
                ?: getString(R.string.campus_reports_campus_not_provided),
            incidentType = getString("incidentType")
                ?: "Incident Report",
            description = getString("description")
                ?: getString(R.string.campus_reports_no_description),
            location = getString("location")
                ?: getString(R.string.campus_reports_location_not_provided),
            latitude = getDoubleValue(get("latitude")) ?: 0.0,
            longitude = getDoubleValue(get("longitude")) ?: 0.0,
            dateTime = finalDateTime,
            status = getString("status") ?: "Submitted",
            photoEvidence = getString("photoEvidence") ?: "",
            videoEvidence = getString("videoEvidence") ?: "",
            audioEvidence = getString("audioEvidence") ?: "",
            fileEvidence = getString("fileEvidence") ?: "",
            photoName = "",
            videoName = "",
            fileName = "",
            createdAt = finalCreatedAt,
            syncStatus = "Synced"
        )
    }

    // =========================================================
    // CONVERT FIRESTORE NUMBERS
    // =========================================================

    private fun getDoubleValue(value: Any?): Double? {

        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    // =========================================================
    // DISPLAY REPORTS
    // =========================================================

    private fun displayReports(
        reports: List<ReportDisplayData>
    ) {

        reportsContainer.removeAllViews()

        tvReportCount.text = if (reports.isEmpty()) {
            getString(R.string.campus_reports_count_zero)
        } else {
            getString(
                R.string.campus_reports_count,
                reports.size
            )
        }

        if (reports.isEmpty()) {
            showNoReports()
            return
        }

        tvNoReports.visibility = View.GONE

        reports.sortedByDescending { it.createdAt }
            .forEach { report ->
                createReportCard(report)
            }
    }

    // =========================================================
    // CREATE REPORT CARD
    // =========================================================

    private fun createReportCard(
        report: ReportDisplayData
    ) {

        val reportView =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_campus_report,
                    reportsContainer,
                    false
                )

        // Find views.
        val tvReportId =
            reportView.findViewById<TextView>(R.id.tvReportId)

        val tvReportType =
            reportView.findViewById<TextView>(R.id.tvReportType)

        val tvReportCampus =
            reportView.findViewById<TextView>(R.id.tvReportCampus)

        val tvReportDate =
            reportView.findViewById<TextView>(R.id.tvReportDate)

        val tvReportTime =
            reportView.findViewById<TextView>(R.id.tvReportTime)

        val tvReportDescription =
            reportView.findViewById<TextView>(R.id.tvReportDescription)

        val tvReportLocation =
            reportView.findViewById<TextView>(R.id.tvReportLocation)

        val tvReportCoordinates =
            reportView.findViewById<TextView>(R.id.tvReportCoordinates)

        val tvReportStatus =
            reportView.findViewById<TextView>(R.id.tvReportStatus)

        val tvSyncStatus =
            reportView.findViewById<TextView>(R.id.tvSyncStatus)

        val tvEvidenceTitle =
            reportView.findViewById<TextView>(R.id.tvEvidenceTitle)

        val btnViewLocation =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewLocation
            )

        val btnViewPhoto =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewPhoto
            )

        val btnViewVideo =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewVideo
            )

        val btnViewAudio =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewAudio
            )

        val btnViewFile =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewFile
            )

        // -----------------------------------------------------
        // BASIC REPORT INFORMATION
        // -----------------------------------------------------

        tvReportId.text = getString(
            R.string.campus_reports_incident_id,
            report.reportId
        )

        tvReportType.text = report.incidentType

        tvReportCampus.text = getString(
            R.string.campus_reports_campus,
            report.campus.ifBlank {
                getString(R.string.campus_reports_campus_not_provided)
            }
        )

        tvReportDescription.text = report.description.ifBlank {
            getString(R.string.campus_reports_no_description)
        }

        tvReportLocation.text = report.location.ifBlank {
            getString(R.string.campus_reports_location_not_provided)
        }

        // -----------------------------------------------------
        // REPORT STATUS
        // -----------------------------------------------------

        // Set the report status once; do not overwrite it later.
        tvReportStatus.text = getString(
            R.string.campus_reports_status,
            report.status
        )

        // -----------------------------------------------------
        // SYNC STATUS
        // -----------------------------------------------------

        tvSyncStatus.visibility = View.VISIBLE

        val isSynced = report.syncStatus.equals(
            "Synced",
            ignoreCase = true
        )

        tvSyncStatus.text = getString(
            R.string.campus_reports_sync_status,
            getString(
                if (isSynced) {
                    R.string.campus_reports_synced
                } else {
                    R.string.campus_reports_pending_sync
                }
            )
        )

        // -----------------------------------------------------
        // DATE AND TIME
        // -----------------------------------------------------

        displayDateTime(
            dateValue = report.date,
            timeValue = report.time,
            dateTime = report.dateTime,
            createdAt = report.createdAt,
            tvDate = tvReportDate,
            tvTime = tvReportTime
        )

        // -----------------------------------------------------
        // GPS LOCATION
        // -----------------------------------------------------

        val latitude = report.latitude
        val longitude = report.longitude

        if (
            latitude != null &&
            longitude != null &&
            latitude != 0.0 &&
            longitude != 0.0
        ) {

            tvReportCoordinates.visibility = View.VISIBLE

            tvReportCoordinates.text = getString(
                R.string.campus_reports_gps,
                latitude,
                longitude
            )

            btnViewLocation.visibility = View.VISIBLE

            btnViewLocation.setText(
                R.string.campus_reports_view_location
            )

            btnViewLocation.setOnClickListener {
                openLocationInMaps(latitude, longitude)
            }

        } else {

            tvReportCoordinates.visibility = View.GONE
            btnViewLocation.visibility = View.GONE
        }

        // -----------------------------------------------------
        // EVIDENCE
        // -----------------------------------------------------

        val photoEvidence = report.photoEvidence
        val videoEvidence = report.videoEvidence
        val audioEvidence = report.audioEvidence
        val fileEvidence = report.fileEvidence

        val hasEvidence =
            photoEvidence.isNotBlank() ||
                    videoEvidence.isNotBlank() ||
                    audioEvidence.isNotBlank() ||
                    fileEvidence.isNotBlank()

        tvEvidenceTitle.visibility = if (hasEvidence) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (hasEvidence) {
            tvEvidenceTitle.setText(R.string.campus_reports_evidence)
        }

        // Photo evidence.
        if (photoEvidence.isNotBlank()) {

            btnViewPhoto.visibility = View.VISIBLE
            btnViewPhoto.setText(R.string.campus_reports_view_photo)

            btnViewPhoto.setOnClickListener {
                openEvidence(photoEvidence, "image/*")
            }

        } else {
            btnViewPhoto.visibility = View.GONE
        }

        // Video evidence.
        if (videoEvidence.isNotBlank()) {

            btnViewVideo.visibility = View.VISIBLE
            btnViewVideo.setText(R.string.campus_reports_view_video)

            btnViewVideo.setOnClickListener {
                openEvidence(videoEvidence, "video/*")
            }

        } else {
            btnViewVideo.visibility = View.GONE
        }

        // Audio evidence.
        if (audioEvidence.isNotBlank()) {

            btnViewAudio.visibility = View.VISIBLE
            btnViewAudio.setText(R.string.campus_reports_view_audio)

            btnViewAudio.setOnClickListener {
                openEvidence(audioEvidence, "audio/*")
            }

        } else {
            btnViewAudio.visibility = View.GONE
        }

        // File evidence.
        if (fileEvidence.isNotBlank()) {

            btnViewFile.visibility = View.VISIBLE
            btnViewFile.setText(R.string.campus_reports_view_file)

            btnViewFile.setOnClickListener {
                openEvidence(fileEvidence, "*/*")
            }

        } else {
            btnViewFile.visibility = View.GONE
        }

        // Add card to the screen.
        reportsContainer.addView(reportView)
    }

    // =========================================================
    // DISPLAY DATE AND TIME
    // =========================================================

    private fun displayDateTime(
        dateValue: String?,
        timeValue: String?,
        dateTime: String?,
        createdAt: Long,
        tvDate: TextView,
        tvTime: TextView
    ) {

        val notAvailable =
            getString(R.string.campus_reports_not_available)

        // Use separate date and time values if available.
        if (
            !dateValue.isNullOrBlank() &&
            !timeValue.isNullOrBlank()
        ) {

            tvDate.text = getString(
                R.string.campus_reports_date,
                dateValue
            )

            tvTime.text = getString(
                R.string.campus_reports_time,
                timeValue
            )

            return
        }

        // Otherwise, try the combined date/time value.
        if (!dateTime.isNullOrBlank()) {

            val parts = dateTime.split(",", limit = 2)

            if (parts.size >= 2) {

                tvDate.text = getString(
                    R.string.campus_reports_date,
                    parts[0].trim()
                )

                tvTime.text = getString(
                    R.string.campus_reports_time,
                    parts[1].trim()
                )

                return
            }

            tvDate.text = getString(
                R.string.campus_reports_date,
                dateTime
            )

            tvTime.text = getString(
                R.string.campus_reports_time,
                notAvailable
            )

            return
        }

        // Fall back to the creation timestamp.
        if (createdAt > 0L) {

            val date = Date(createdAt)

            val dateFormat = SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )

            val timeFormat = SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            )

            tvDate.text = getString(
                R.string.campus_reports_date,
                dateFormat.format(date)
            )

            tvTime.text = getString(
                R.string.campus_reports_time,
                timeFormat.format(date)
            )

        } else {

            tvDate.text = getString(
                R.string.campus_reports_date,
                notAvailable
            )

            tvTime.text = getString(
                R.string.campus_reports_time,
                notAvailable
            )
        }
    }

    // =========================================================
    // NO REPORTS
    // =========================================================

    private fun showNoReports() {

        reportsContainer.removeAllViews()
        tvNoReports.visibility = View.VISIBLE
    }

    // =========================================================
    // OPEN CAMPUS LOGIN
    // =========================================================

    private fun openCampusLogin() {

        showToast(R.string.campus_reports_login_required)

        val intent = Intent(
            this,
            CampusLoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    // =========================================================
    // OPEN LOCATION IN MAPS
    // =========================================================

    private fun openLocationInMaps(
        latitude: Double,
        longitude: Double
    ) {

        val geoUri = Uri.parse(
            "geo:$latitude,$longitude?q=$latitude,$longitude"
        )

        val geoIntent = Intent(
            Intent.ACTION_VIEW,
            geoUri
        )

        try {

            startActivity(geoIntent)

        } catch (exception: Exception) {

            val mapsUri = Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
            )

            val mapsIntent = Intent(
                Intent.ACTION_VIEW,
                mapsUri
            )

            try {

                startActivity(mapsIntent)

            } catch (mapException: Exception) {

                Log.e(TAG, "Unable to open the map.", mapException)

                showToast(R.string.campus_reports_map_unavailable)
            }
        }
    }

    // =========================================================
    // OPEN PHOTO / VIDEO / AUDIO / FILE
    // =========================================================

    private fun openEvidence(
        storedValue: String,
        fallbackMimeType: String
    ) {

        try {

            // -------------------------------------------------
            // LOCAL FILE PATH
            // -------------------------------------------------

            if (storedValue.startsWith("/")) {

                val file = File(storedValue)

                if (!file.exists()) {
                    showToast(R.string.campus_reports_file_missing)
                    return
                }

                val fileUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val mimeType = guessMimeType(file.name)

                val intent = Intent(
                    Intent.ACTION_VIEW
                ).apply {
                    setDataAndType(fileUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {

                    startActivity(
                        Intent.createChooser(
                            intent,
                            getString(R.string.campus_reports_open_evidence)
                        )
                    )

                } catch (exception: Exception) {

                    Log.e(TAG, "No app can open the evidence.", exception)

                    showToast(
                        R.string.campus_reports_no_evidence_app
                    )
                }

                return
            }

            // -------------------------------------------------
            // CONTENT URI
            // -------------------------------------------------

            if (storedValue.startsWith("content://")) {

                val uri = Uri.parse(storedValue)

                val detectedMimeType =
                    contentResolver.getType(uri) ?: fallbackMimeType

                val intent = Intent(
                    Intent.ACTION_VIEW
                ).apply {
                    setDataAndType(uri, detectedMimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {

                    startActivity(
                        Intent.createChooser(
                            intent,
                            getString(R.string.campus_reports_open_evidence)
                        )
                    )

                } catch (exception: Exception) {

                    Log.e(TAG, "Evidence URI is inaccessible.", exception)

                    showToast(
                        R.string.campus_reports_file_inaccessible
                    )
                }

                return
            }

            // -------------------------------------------------
            // UNKNOWN FORMAT
            // -------------------------------------------------

            showToast(R.string.campus_reports_invalid_evidence)

        } catch (exception: Exception) {

            Log.e(TAG, "Unable to open evidence.", exception)

            showToast(
                R.string.campus_reports_open_evidence_failed
            )
        }
    }

    // =========================================================
    // DETERMINE MIME TYPE
    // =========================================================

    private fun guessMimeType(fileName: String): String {

        val extension = fileName
            .substringAfterLast(".", "")
            .lowercase(Locale.getDefault())

        return when (extension) {

            // Images
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"

            // Video
            "mp4" -> "video/mp4"
            "3gp" -> "video/3gpp"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"

            // Audio
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"

            // PDF
            "pdf" -> "application/pdf"

            // Microsoft Word
            "doc" -> "application/msword"
            "docx" ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

            // Microsoft Excel
            "xls" -> "application/vnd.ms-excel"
            "xlsx" ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            // Microsoft PowerPoint
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" ->
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"

            // Text
            "txt" -> "text/plain"
            "csv" -> "text/csv"

            else -> "*/*"
        }
    }

    // =========================================================
    // DISPLAY TOAST
    // =========================================================

    private fun showToast(messageResId: Int) {

        Toast.makeText(
            this,
            getString(messageResId),
            Toast.LENGTH_LONG
        ).show()
    }

    // =========================================================
    // CONSTANTS
    // =========================================================

    companion object {
        private const val TAG = "CampusReportsActivity"
    }
}
