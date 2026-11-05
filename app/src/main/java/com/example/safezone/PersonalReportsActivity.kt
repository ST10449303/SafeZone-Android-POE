
package com.example.safezone

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.IncidentReportEntity
import com.example.safezone.data.SafeZoneDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonalReportsActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvReportCount: TextView
    private lateinit var reportsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var btnReportIncident: AppCompatButton

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val database by lazy {
        SafeZoneDatabase.getDatabase(applicationContext)
    }

    private var localReports: List<IncidentReportEntity> = emptyList()
    private var cloudReports: List<ReportItem> = emptyList()
    private var loadingCloud = false
    private var loadError: String? = null
    private var currentUserId: String? = null
    private var lastErrorToastShown = false

    private data class ReportItem(
        val id: String,
        val userId: String,
        val incidentType: String,
        val campus: String,
        val description: String,
        val location: String,
        val latitude: Double,
        val longitude: Double,
        val dateTime: String,
        val status: String,
        val photoEvidence: String,
        val videoEvidence: String,
        val fileEvidence: String,
        val photoName: String,
        val videoName: String,
        val fileName: String,
        val createdAt: Long,
        val syncStatus: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_reports)

        tvBack = findViewById(R.id.tvBack)
        tvReportCount = findViewById(R.id.tvReportCount)
        reportsContainer = findViewById(R.id.reportsContainer)
        emptyState = findViewById(R.id.emptyState)
        btnReportIncident = findViewById(R.id.btnReportIncident)

        tvBack.setOnClickListener {
            finish()
        }

        btnReportIncident.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PersonalIncidentReportActivity::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                getString(R.string.personal_reports_login_required),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        if (currentUserId != user.uid) {
            currentUserId = user.uid
            localReports = emptyList()
            cloudReports = emptyList()
            loadError = null
            lastErrorToastShown = false
            observeLocalReports(user.uid)
        }

        loadCloudReports(user.uid)
    }

    // ==================================================
    // OBSERVE REPORTS SAVED ON THE PHONE
    // ==================================================

    private fun observeLocalReports(userId: String) {
        lifecycleScope.launch {
            database.incidentReportDao()
                .getReportsForUser(userId)
                .collect { reports ->
                    localReports = reports
                    displayReports()
                }
        }
    }

    // ==================================================
    // LOAD REPORTS FROM FIRESTORE
    // ==================================================

    private fun loadCloudReports(userId: String) {
        if (loadingCloud) return

        loadingCloud = true
        loadError = null
        lastErrorToastShown = false
        displayReports()

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("IncidentReports")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                cloudReports = snapshot.documents.mapNotNull { doc ->
                    val ownerId =
                        doc.getString("userId") ?: return@mapNotNull null

                    if (ownerId != userId) {
                        return@mapNotNull null
                    }

                    ReportItem(
                        id = doc.id,
                        userId = ownerId,
                        incidentType = doc.getString("incidentType")
                            ?: doc.getString("type")
                            ?: getString(R.string.personal_reports_incident),
                        campus = doc.getString("campus")
                            ?: getString(R.string.personal_reports_not_specified),
                        description = doc.getString("description")
                            ?: getString(R.string.personal_reports_no_description),
                        location = doc.getString("location")
                            ?: getString(R.string.personal_reports_location_placeholder),
                        latitude = getDouble(doc.get("latitude")) ?: 0.0,
                        longitude = getDouble(doc.get("longitude")) ?: 0.0,
                        dateTime = doc.getString("dateTime") ?: "",
                        status = doc.getString("status")
                            ?: getString(R.string.personal_reports_submitted),
                        photoEvidence = doc.getString("photoEvidence") ?: "",
                        videoEvidence = doc.getString("videoEvidence") ?: "",
                        fileEvidence = doc.getString("fileEvidence") ?: "",
                        photoName = doc.getString("photoName")
                            ?: getString(R.string.personal_reports_default_photo),
                        videoName = doc.getString("videoName")
                            ?: getString(R.string.personal_reports_default_video),
                        fileName = doc.getString("fileName")
                            ?: getString(R.string.personal_reports_default_file),
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        syncStatus = "Synced"
                    )
                }
            } catch (exception: Exception) {
                loadError = exception.message
                    ?: getString(R.string.personal_reports_server_error)
            } finally {
                loadingCloud = false
                displayReports()
            }
        }
    }

    // ==================================================
    // COMBINE LOCAL AND CLOUD REPORTS
    // ==================================================

    private fun displayReports() {
        if (!::reportsContainer.isInitialized) return

        reportsContainer.removeAllViews()

        val combined = linkedMapOf<String, ReportItem>()

        // Add cloud records first.
        cloudReports.forEach { report ->
            combined[report.id] = report
        }

        // Local records override matching cloud records.
        // This preserves their pending-sync status.
        localReports.forEach { local ->
            combined[local.localId] = local.toReportItem()
        }

        val reports = combined.values.sortedByDescending {
            it.createdAt
        }

        tvReportCount.text = when {
            loadingCloud && reports.isEmpty() ->
                getString(R.string.personal_reports_loading)

            else ->
                resources.getQuantityString(
                    R.plurals.personal_reports_report_count,
                    reports.size,
                    reports.size
                )
        }

        if (reports.isEmpty()) {
            emptyState.visibility = View.VISIBLE

            if (loadError != null && !lastErrorToastShown) {
                tvReportCount.text =
                    getString(R.string.personal_reports_heading)

                Toast.makeText(
                    this,
                    getString(R.string.personal_reports_refresh_failed),
                    Toast.LENGTH_LONG
                ).show()

                lastErrorToastShown = true
            }

            return
        }

        emptyState.visibility = View.GONE

        reports.forEach { report ->
            createReportCard(report)
        }

        if (loadError != null && !lastErrorToastShown) {
            Toast.makeText(
                this,
                getString(R.string.personal_reports_offline_fallback),
                Toast.LENGTH_SHORT
            ).show()

            lastErrorToastShown = true
        }
    }

    // ==================================================
    // CONVERT ROOM ENTITY TO DISPLAY ITEM
    // ==================================================

    private fun IncidentReportEntity.toReportItem(): ReportItem {
        return ReportItem(
            id = localId,
            userId = userId,
            incidentType = incidentType,
            campus = getString(R.string.personal_reports_not_specified),
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            dateTime = dateTime,
            status = status,
            photoEvidence = photoEvidence,
            videoEvidence = videoEvidence,
            fileEvidence = fileEvidence,
            photoName = photoName,
            videoName = videoName,
            fileName = fileName,
            createdAt = createdAt,
            syncStatus = syncStatus
        )
    }

    // ==================================================
    // CREATE REPORT CARD
    // ==================================================

    private fun createReportCard(report: ReportItem) {
        val reportView = LayoutInflater.from(this).inflate(
            R.layout.item_personal_report,
            reportsContainer,
            false
        )

        val tvReportType =
            reportView.findViewById<TextView>(R.id.tvReportType)

        val tvReportId =
            reportView.findViewById<TextView>(R.id.tvReportId)

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

        val tvEvidenceTitle =
            reportView.findViewById<TextView>(R.id.tvEvidenceTitle)

        val btnViewLocation =
            reportView.findViewById<AppCompatButton>(R.id.btnViewLocation)

        val btnViewPhoto =
            reportView.findViewById<AppCompatButton>(R.id.btnViewPhoto)

        val btnViewVideo =
            reportView.findViewById<AppCompatButton>(R.id.btnViewVideo)

        val btnViewFile =
            reportView.findViewById<AppCompatButton>(R.id.btnViewFile)

        tvReportType.text = report.incidentType

        tvReportId.text = getString(
            R.string.personal_reports_incident_id,
            report.id
        )

        tvReportCampus.text = getString(
            R.string.personal_reports_campus,
            report.campus
        )

        tvReportDescription.text = report.description
        tvReportLocation.text = report.location

        tvReportStatus.text = if (report.syncStatus != "Synced") {
            getString(
                R.string.personal_reports_status_with_sync,
                report.status,
                getString(R.string.personal_reports_pending_sync)
            )
        } else {
            report.status
        }

        setStatusColour(tvReportStatus, report.status)

        displayDateTime(
            report.dateTime,
            report.createdAt,
            tvReportDate,
            tvReportTime
        )

        // Location
        if (report.latitude != 0.0 && report.longitude != 0.0) {
            tvReportCoordinates.visibility = View.VISIBLE

            tvReportCoordinates.text = getString(
                R.string.personal_reports_coordinates,
                report.latitude.toString(),
                report.longitude.toString()
            )

            btnViewLocation.visibility = View.VISIBLE
            btnViewLocation.setOnClickListener {
                openLocationInMaps(report.latitude, report.longitude)
            }
        } else {
            tvReportCoordinates.visibility = View.GONE
            btnViewLocation.visibility = View.GONE
        }

        // Evidence heading
        val hasEvidence = report.photoEvidence.isNotEmpty() ||
                report.videoEvidence.isNotEmpty() ||
                report.fileEvidence.isNotEmpty()

        tvEvidenceTitle.visibility =
            if (hasEvidence) View.VISIBLE else View.GONE

        // Photo
        if (report.photoEvidence.isNotEmpty()) {
            btnViewPhoto.visibility = View.VISIBLE
            btnViewPhoto.text = getString(
                R.string.personal_reports_view_photo,
                report.photoName
            )

            btnViewPhoto.setOnClickListener {
                openEvidence(report.photoEvidence, "image/*")
            }
        } else {
            btnViewPhoto.visibility = View.GONE
        }

        // Video
        if (report.videoEvidence.isNotEmpty()) {
            btnViewVideo.visibility = View.VISIBLE
            btnViewVideo.text = getString(
                R.string.personal_reports_view_video,
                report.videoName
            )

            btnViewVideo.setOnClickListener {
                openEvidence(report.videoEvidence, "video/*")
            }
        } else {
            btnViewVideo.visibility = View.GONE
        }

        // File
        if (report.fileEvidence.isNotEmpty()) {
            btnViewFile.visibility = View.VISIBLE
            btnViewFile.text = getString(
                R.string.personal_reports_view_file,
                report.fileName
            )

            btnViewFile.setOnClickListener {
                openEvidence(report.fileEvidence, "*/*")
            }
        } else {
            btnViewFile.visibility = View.GONE
        }

        reportsContainer.addView(reportView)
    }

    // ==================================================
    // DATE AND TIME
    // ==================================================

    private fun displayDateTime(
        dateTime: String?,
        createdAt: Long,
        tvDate: TextView,
        tvTime: TextView
    ) {
        if (!dateTime.isNullOrEmpty()) {
            val parts = dateTime.split(",")

            if (parts.size >= 2) {
                tvDate.text = getString(
                    R.string.personal_reports_date,
                    parts[0].trim()
                )

                tvTime.text = getString(
                    R.string.personal_reports_time,
                    parts[1].trim()
                )

                return
            }
        }

        if (createdAt > 0L) {
            val date = Date(createdAt)

            val formattedDate = SimpleDateFormat(
                "dd MMMM yyyy",
                Locale.getDefault()
            ).format(date)

            val formattedTime = SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(date)

            tvDate.text = getString(
                R.string.personal_reports_date,
                formattedDate
            )

            tvTime.text = getString(
                R.string.personal_reports_time,
                formattedTime
            )
        } else {
            tvDate.text =
                getString(R.string.personal_reports_date_placeholder)

            tvTime.text =
                getString(R.string.personal_reports_time_placeholder)
        }
    }

    private fun getDouble(value: Any?): Double? {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    // ==================================================
    // OPEN LOCATION IN MAPS
    // ==================================================

    private fun openLocationInMaps(
        latitude: Double,
        longitude: Double
    ) {
        val mapsUri = Uri.parse(
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        )

        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, mapsUri)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.personal_reports_maps_unavailable),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==================================================
    // OPEN EVIDENCE
    // ==================================================

    private fun openEvidence(
        uriString: String,
        mimeType: String
    ) {
        try {
            val uri = Uri.parse(uriString)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.personal_reports_evidence_unavailable),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==================================================
    // STATUS COLOURS
    // ==================================================

    private fun setStatusColour(
        textView: TextView,
        status: String
    ) {
        when (status.lowercase(Locale.getDefault())) {
            "resolved", "closed" -> {
                textView.setTextColor(Color.rgb(46, 125, 50))
            }

            "under review", "reviewing" -> {
                textView.setTextColor(Color.rgb(245, 124, 0))
            }

            "rejected" -> {
                textView.setTextColor(Color.rgb(211, 47, 47))
            }

            else -> {
                textView.setTextColor(Color.rgb(25, 118, 210))
            }
        }
    }
}
