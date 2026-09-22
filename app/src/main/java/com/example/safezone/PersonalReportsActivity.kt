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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonalReportsActivity : AppCompatActivity() {

    // ==========================================
    // VIEWS
    // ==========================================

    private lateinit var tvBack: TextView
    private lateinit var tvReportCount: TextView
    private lateinit var reportsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var btnReportIncident: AppCompatButton

    // ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_personal_reports
        )

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ==========================================
        // CONNECT VIEWS
        // ==========================================

        tvBack =
            findViewById(R.id.tvBack)

        tvReportCount =
            findViewById(R.id.tvReportCount)

        reportsContainer =
            findViewById(R.id.reportsContainer)

        emptyState =
            findViewById(R.id.emptyState)

        btnReportIncident =
            findViewById(R.id.btnReportIncident)

        // ==========================================
        // BACK
        // ==========================================

        tvBack.setOnClickListener {
            finish()
        }

        // ==========================================
        // REPORT NEW INCIDENT
        // ==========================================

        btnReportIncident.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PersonalIncidentReportActivity::class.java
                )
            )
        }

        loadReports()
    }

    // ==========================================
    // REFRESH
    // ==========================================

    override fun onResume() {

        super.onResume()

        if (::reportsContainer.isInitialized) {
            loadReports()
        }
    }

    // ==========================================
    // LOAD PERSONAL REPORTS
    // ==========================================

    private fun loadReports() {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        reportsContainer.removeAllViews()

        emptyState.visibility =
            View.GONE

        tvReportCount.text =
            "Loading your reports..."

        db.collection("IncidentReports")
            .whereEqualTo(
                "userId",
                currentUser.uid
            )
            .get()
            .addOnSuccessListener { documents ->

                reportsContainer.removeAllViews()

                if (documents.isEmpty) {

                    tvReportCount.text =
                        "Your Reports"

                    emptyState.visibility =
                        View.VISIBLE

                    return@addOnSuccessListener
                }

                emptyState.visibility =
                    View.GONE

                val reports =
                    documents.documents.sortedByDescending {

                        it.getLong(
                            "createdAt"
                        ) ?: 0L
                    }

                tvReportCount.text =
                    if (reports.size == 1) {
                        "1 Report"
                    } else {
                        "${reports.size} Reports"
                    }

                for (document in reports) {

                    createReportCard(
                        document
                    )
                }
            }
            .addOnFailureListener { exception ->

                reportsContainer.removeAllViews()

                emptyState.visibility =
                    View.VISIBLE

                tvReportCount.text =
                    "Your Reports"

                Toast.makeText(
                    this,
                    "Unable to load your reports: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==========================================
    // CREATE REPORT CARD
    // ==========================================

    private fun createReportCard(
        document: DocumentSnapshot
    ) {

        val reportView =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_personal_report,
                    reportsContainer,
                    false
                )

        // ==========================================
        // VIEWS
        // ==========================================

        val tvReportType =
            reportView.findViewById<TextView>(
                R.id.tvReportType
            )

        val tvReportId =
            reportView.findViewById<TextView>(
                R.id.tvReportId
            )

        val tvReportCampus =
            reportView.findViewById<TextView>(
                R.id.tvReportCampus
            )

        val tvReportDate =
            reportView.findViewById<TextView>(
                R.id.tvReportDate
            )

        val tvReportTime =
            reportView.findViewById<TextView>(
                R.id.tvReportTime
            )

        val tvReportDescription =
            reportView.findViewById<TextView>(
                R.id.tvReportDescription
            )

        val tvReportLocation =
            reportView.findViewById<TextView>(
                R.id.tvReportLocation
            )

        val tvReportCoordinates =
            reportView.findViewById<TextView>(
                R.id.tvReportCoordinates
            )

        val tvReportStatus =
            reportView.findViewById<TextView>(
                R.id.tvReportStatus
            )

        val tvEvidenceTitle =
            reportView.findViewById<TextView>(
                R.id.tvEvidenceTitle
            )

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

        val btnViewFile =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewFile
            )

        // ==========================================
        // FIRESTORE VALUES
        // ==========================================

        val reportId =
            document.id

        val incidentType =
            document.getString(
                "incidentType"
            )
                ?: document.getString(
                    "type"
                )
                ?: "Incident Report"

        val campus =
            document.getString(
                "campus"
            )
                ?: "Not specified"

        val description =
            document.getString(
                "description"
            )
                ?: "No description provided."

        val location =
            document.getString(
                "location"
            )
                ?: "Location not provided."

        val status =
            document.getString(
                "status"
            )
                ?: "Submitted"

        val dateTime =
            document.getString(
                "dateTime"
            )

        val createdAt =
            document.getLong(
                "createdAt"
            ) ?: 0L

        val latitude =
            getDouble(
                document.get("latitude")
            )

        val longitude =
            getDouble(
                document.get("longitude")
            )

        // ==========================================
        // EVIDENCE
        // ==========================================

        val photoUri =
            document.getString(
                "photoEvidence"
            ) ?: ""

        val videoUri =
            document.getString(
                "videoEvidence"
            ) ?: ""

        val fileUri =
            document.getString(
                "fileEvidence"
            ) ?: ""

        // ==========================================
        // EVIDENCE NAMES
        // ==========================================

        val photoName =
            document.getString(
                "photoName"
            ) ?: "Photo evidence"

        val videoName =
            document.getString(
                "videoName"
            ) ?: "Video evidence"

        val fileName =
            document.getString(
                "fileName"
            ) ?: "Attached file"

        // ==========================================
        // DISPLAY BASIC INFORMATION
        // ==========================================

        tvReportType.text =
            incidentType

        tvReportId.text =
            "Incident ID: $reportId"

        tvReportCampus.text =
            "Campus: $campus"

        tvReportDescription.text =
            description

        tvReportLocation.text =
            location

        tvReportStatus.text =
            status

        setStatusColour(
            tvReportStatus,
            status
        )

        // ==========================================
        // DATE + TIME
        // ==========================================

        displayDateTime(
            dateTime,
            createdAt,
            tvReportDate,
            tvReportTime
        )

        // ==========================================
        // GPS
        // ==========================================

        if (
            latitude != null &&
            longitude != null &&
            latitude != 0.0 &&
            longitude != 0.0
        ) {

            tvReportCoordinates.visibility =
                View.VISIBLE

            tvReportCoordinates.text =
                "Coordinates: $latitude, $longitude"

            btnViewLocation.visibility =
                View.VISIBLE

            btnViewLocation.setOnClickListener {

                openLocationInMaps(
                    latitude,
                    longitude
                )
            }

        } else {

            tvReportCoordinates.visibility =
                View.GONE

            btnViewLocation.visibility =
                View.GONE
        }

        // ==========================================
        // EVIDENCE
        // ==========================================

        val hasEvidence =
            photoUri.isNotEmpty() ||
                    videoUri.isNotEmpty() ||
                    fileUri.isNotEmpty()

        tvEvidenceTitle.visibility =
            if (hasEvidence) {
                View.VISIBLE
            } else {
                View.GONE
            }

        // ==========================================
        // PHOTO
        // ==========================================

        if (photoUri.isNotEmpty()) {

            btnViewPhoto.visibility =
                View.VISIBLE

            btnViewPhoto.text =
                "📷  View Photo\n$photoName"

            btnViewPhoto.setOnClickListener {

                openEvidence(
                    photoUri,
                    "image/*"
                )
            }

        } else {

            btnViewPhoto.visibility =
                View.GONE
        }

        // ==========================================
        // VIDEO
        // ==========================================

        if (videoUri.isNotEmpty()) {

            btnViewVideo.visibility =
                View.VISIBLE

            btnViewVideo.text =
                "🎥  View Video\n$videoName"

            btnViewVideo.setOnClickListener {

                openEvidence(
                    videoUri,
                    "video/*"
                )
            }

        } else {

            btnViewVideo.visibility =
                View.GONE
        }

        // ==========================================
        // FILE
        // ==========================================

        if (fileUri.isNotEmpty()) {

            btnViewFile.visibility =
                View.VISIBLE

            btnViewFile.text =
                "📄  Open File\n$fileName"

            btnViewFile.setOnClickListener {

                openEvidence(
                    fileUri,
                    "*/*"
                )
            }

        } else {

            btnViewFile.visibility =
                View.GONE
        }

        // ==========================================
        // ADD CARD
        // ==========================================

        reportsContainer.addView(
            reportView
        )
    }

    // ==========================================
    // DATE + TIME
    // ==========================================

    private fun displayDateTime(
        dateTime: String?,
        createdAt: Long,
        tvDate: TextView,
        tvTime: TextView
    ) {

        if (!dateTime.isNullOrEmpty()) {

            val parts =
                dateTime.split(",")

            if (parts.size >= 2) {

                tvDate.text =
                    "Date: ${parts[0].trim()}"

                tvTime.text =
                    "Time: ${parts[1].trim()}"

                return
            }
        }

        if (createdAt > 0) {

            val date =
                Date(createdAt)

            val dateFormat =
                SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale.getDefault()
                )

            val timeFormat =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            tvDate.text =
                "Date: ${dateFormat.format(date)}"

            tvTime.text =
                "Time: ${timeFormat.format(date)}"

        } else {

            tvDate.text =
                "Date: Not available"

            tvTime.text =
                "Time: Not available"
        }
    }

    // ==========================================
    // GET DOUBLE
    // ==========================================

    private fun getDouble(
        value: Any?
    ): Double? {

        return when (value) {

            is Number ->
                value.toDouble()

            is String ->
                value.toDoubleOrNull()

            else ->
                null
        }
    }

    // ==========================================
    // OPEN LOCATION
    // ==========================================

    private fun openLocationInMaps(
        latitude: Double,
        longitude: Double
    ) {

        val mapsUri =
            Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
            )

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                mapsUri
            )

        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No Maps application is available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // OPEN EVIDENCE
    // ==========================================

    private fun openEvidence(
        uriString: String,
        mimeType: String
    ) {

        try {

            val uri =
                Uri.parse(uriString)

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                )

            intent.setDataAndType(
                uri,
                mimeType
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open this evidence.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // STATUS COLOUR
    // ==========================================

    private fun setStatusColour(
        textView: TextView,
        status: String
    ) {

        when (
            status.lowercase(
                Locale.getDefault()
            )
        ) {

            "resolved",
            "closed" -> {

                textView.setTextColor(
                    Color.rgb(
                        46,
                        125,
                        50
                    )
                )
            }

            "under review",
            "reviewing" -> {

                textView.setTextColor(
                    Color.rgb(
                        245,
                        124,
                        0
                    )
                )
            }

            "rejected" -> {

                textView.setTextColor(
                    Color.rgb(
                        211,
                        47,
                        47
                    )
                )
            }

            else -> {

                textView.setTextColor(
                    Color.rgb(
                        25,
                        118,
                        210
                    )
                )
            }
        }
    }
}