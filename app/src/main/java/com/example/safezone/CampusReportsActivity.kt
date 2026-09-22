package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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


    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_reports
        )

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()

        tvBack =
            findViewById(R.id.tvBack)

        reportsContainer =
            findViewById(R.id.reportsContainer)

        tvNoReports =
            findViewById(R.id.tvNoReports)


        // =====================================================
        // BACK
        // =====================================================

        tvBack.setOnClickListener {

            finish()
        }


        loadReports()
    }


    // =========================================================
    // REFRESH WHEN SCREEN OPENS
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (::reportsContainer.isInitialized) {

            loadReports()
        }
    }


    // =========================================================
    // LOAD REPORTS
    // =========================================================

    private fun loadReports() {

        val currentUser =
            auth.currentUser


        // =====================================================
        // CHECK LOGIN
        // =====================================================

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
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

            finish()

            return
        }


        // =====================================================
        // CLEAR OLD REPORTS
        // =====================================================

        reportsContainer.removeAllViews()

        tvNoReports.visibility =
            View.GONE


        /*
         * My Campus Reports and Manage Reports
         * are the same feature.
         *
         * Therefore, we display the reports created
         * by the currently signed-in Campus user.
         */


        // =====================================================
        // GET USER'S REPORTS
        // =====================================================

        db.collection(
            "IncidentReports"
        )
            .whereEqualTo(
                "userId",
                currentUser.uid
            )
            .get()
            .addOnSuccessListener { documents ->

                displayReports(
                    documents.documents
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load your campus reports: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // DISPLAY REPORTS
    // =========================================================

    private fun displayReports(
        documents: List<DocumentSnapshot>
    ) {

        reportsContainer.removeAllViews()


        // =====================================================
        // NO REPORTS
        // =====================================================

        if (documents.isEmpty()) {

            showNoReports()

            return
        }


        tvNoReports.visibility =
            View.GONE


        // =====================================================
        // NEWEST REPORTS FIRST
        // =====================================================

        val reports =
            documents.sortedByDescending {

                it.getLong(
                    "createdAt"
                ) ?: 0L
            }


        // =====================================================
        // CREATE EACH CARD
        // =====================================================

        for (document in reports) {

            createReportCard(
                document
            )
        }
    }


    // =========================================================
    // CREATE REPORT CARD
    // =========================================================

    private fun createReportCard(
        document: DocumentSnapshot
    ) {

        val reportView =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_campus_report,
                    reportsContainer,
                    false
                )


        // =====================================================
        // FIND VIEWS
        // =====================================================

        val tvReportId =
            reportView.findViewById<TextView>(
                R.id.tvReportId
            )

        val tvReportType =
            reportView.findViewById<TextView>(
                R.id.tvReportType
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

        val btnViewAudio =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewAudio
            )

        val btnViewFile =
            reportView.findViewById<AppCompatButton>(
                R.id.btnViewFile
            )


        // =====================================================
        // GET REPORT DATA
        // =====================================================

        val reportId =
            document.getString(
                "reportId"
            ) ?: document.id


        val incidentType =
            document.getString(
                "incidentType"
            ) ?: "Incident Report"


        val campus =
            document.getString(
                "campus"
            ) ?: "Campus not provided"


        val description =
            document.getString(
                "description"
            ) ?: "No description provided."


        val location =
            document.getString(
                "location"
            ) ?: "Location not provided."


        val status =
            document.getString(
                "status"
            ) ?: "Submitted"


        val dateValue =
            document.getString(
                "date"
            )


        val timeValue =
            document.getString(
                "time"
            )


        val dateTime =
            document.getString(
                "dateTime"
            )


        val createdAt =
            document.getLong(
                "createdAt"
            ) ?: 0L


        // =====================================================
        // GPS COORDINATES
        // =====================================================

        val latitude =
            getDouble(
                document.get(
                    "latitude"
                )
            )


        val longitude =
            getDouble(
                document.get(
                    "longitude"
                )
            )


        // =====================================================
        // EVIDENCE
        // =====================================================

        /*
         * These values can now be:
         *
         * 1. A SafeZone local file path:
         *    /data/user/0/com.example.safezone/files/evidence/...
         *
         * OR
         *
         * 2. An old content:// URI from previous reports.
         */

        val photoEvidence =
            document.getString(
                "photoEvidence"
            ) ?: ""


        val videoEvidence =
            document.getString(
                "videoEvidence"
            ) ?: ""


        val audioEvidence =
            document.getString(
                "audioEvidence"
            ) ?: ""


        val fileEvidence =
            document.getString(
                "fileEvidence"
            ) ?: ""


        // =====================================================
        // BASIC INFORMATION
        // =====================================================

        tvReportId.text =
            "Incident ID: SZ-$reportId"


        tvReportType.text =
            incidentType


        tvReportCampus.text =
            "Campus: $campus"


        tvReportDescription.text =
            description


        tvReportLocation.text =
            location


        tvReportStatus.text =
            status


        // =====================================================
        // DATE AND TIME
        // =====================================================

        displayDateTime(
            dateValue = dateValue,
            timeValue = timeValue,
            dateTime = dateTime,
            createdAt = createdAt,
            tvDate = tvReportDate,
            tvTime = tvReportTime
        )


        // =====================================================
        // GPS LOCATION
        // =====================================================

        if (
            latitude != null &&
            longitude != null &&
            latitude != 0.0 &&
            longitude != 0.0
        ) {

            tvReportCoordinates.visibility =
                View.VISIBLE


            tvReportCoordinates.text =
                String.format(
                    Locale.US,
                    "GPS: %.6f, %.6f",
                    latitude,
                    longitude
                )


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


        // =====================================================
        // EVIDENCE SECTION
        // =====================================================

        val hasEvidence =
            photoEvidence.isNotBlank() ||
                    videoEvidence.isNotBlank() ||
                    audioEvidence.isNotBlank() ||
                    fileEvidence.isNotBlank()


        if (hasEvidence) {

            tvEvidenceTitle.visibility =
                View.VISIBLE

        } else {

            tvEvidenceTitle.visibility =
                View.GONE
        }


        // =====================================================
        // PHOTO
        // =====================================================

        if (photoEvidence.isNotBlank()) {

            btnViewPhoto.visibility =
                View.VISIBLE


            btnViewPhoto.text =
                "View Photo"


            btnViewPhoto.setOnClickListener {

                openEvidence(
                    photoEvidence,
                    "image/*"
                )
            }

        } else {

            btnViewPhoto.visibility =
                View.GONE
        }


        // =====================================================
        // VIDEO
        // =====================================================

        if (videoEvidence.isNotBlank()) {

            btnViewVideo.visibility =
                View.VISIBLE


            btnViewVideo.text =
                "View Video"


            btnViewVideo.setOnClickListener {

                openEvidence(
                    videoEvidence,
                    "video/*"
                )
            }

        } else {

            btnViewVideo.visibility =
                View.GONE
        }


        // =====================================================
        // AUDIO
        // =====================================================

        if (audioEvidence.isNotBlank()) {

            btnViewAudio.visibility =
                View.VISIBLE


            btnViewAudio.text =
                "View Audio"


            btnViewAudio.setOnClickListener {

                openEvidence(
                    audioEvidence,
                    "audio/*"
                )
            }

        } else {

            btnViewAudio.visibility =
                View.GONE
        }


        // =====================================================
        // FILE
        // =====================================================

        if (fileEvidence.isNotBlank()) {

            btnViewFile.visibility =
                View.VISIBLE


            btnViewFile.text =
                "View File"


            btnViewFile.setOnClickListener {

                openEvidence(
                    fileEvidence,
                    "*/*"
                )
            }

        } else {

            btnViewFile.visibility =
                View.GONE
        }


        // =====================================================
        // ADD CARD
        // =====================================================

        reportsContainer.addView(
            reportView
        )
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

        // =====================================================
        // DATE + TIME FIELDS
        // =====================================================

        if (
            !dateValue.isNullOrBlank() &&
            !timeValue.isNullOrBlank()
        ) {

            tvDate.text =
                "Date: $dateValue"


            tvTime.text =
                "Time: $timeValue"


            return
        }


        // =====================================================
        // DATE TIME FALLBACK
        // =====================================================

        if (!dateTime.isNullOrBlank()) {

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


        // =====================================================
        // CREATED AT FALLBACK
        // =====================================================

        if (createdAt > 0L) {

            val date =
                Date(createdAt)


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


    // =========================================================
    // NO REPORTS
    // =========================================================

    private fun showNoReports() {

        reportsContainer.removeAllViews()

        tvNoReports.visibility =
            View.VISIBLE
    }


    // =========================================================
    // CONVERT FIRESTORE NUMBER
    // =========================================================

    private fun getDouble(
        value: Any?
    ): Double? {

        return when (value) {

            is Double ->
                value

            is Float ->
                value.toDouble()

            is Long ->
                value.toDouble()

            is Int ->
                value.toDouble()

            is Number ->
                value.toDouble()

            is String ->
                value.toDoubleOrNull()

            else ->
                null
        }
    }


    // =========================================================
    // OPEN LOCATION
    // =========================================================

    private fun openLocationInMaps(
        latitude: Double,
        longitude: Double
    ) {

        val geoUri =
            Uri.parse(
                "geo:$latitude,$longitude?q=$latitude,$longitude"
            )


        val geoIntent =
            Intent(
                Intent.ACTION_VIEW,
                geoUri
            )


        try {

            startActivity(
                geoIntent
            )

        } catch (e: Exception) {

            // =================================================
            // FALLBACK TO BROWSER GOOGLE MAPS
            // =================================================

            val mapsUri =
                Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                )


            val mapsIntent =
                Intent(
                    Intent.ACTION_VIEW,
                    mapsUri
                )


            try {

                startActivity(
                    mapsIntent
                )

            } catch (mapException: Exception) {

                Toast.makeText(
                    this,
                    "Unable to open the map.",
                    Toast.LENGTH_LONG
                ).show()
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

            // =================================================
            // NEW SAFEZONE LOCAL FILE
            // =================================================

            if (
                storedValue.startsWith("/")
            ) {

                val file =
                    File(storedValue)


                // ---------------------------------------------
                // CHECK FILE EXISTS
                // ---------------------------------------------

                if (!file.exists()) {

                    Toast.makeText(
                        this,
                        "This evidence file is no longer available.",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }


                // ---------------------------------------------
                // CREATE SECURE FILEPROVIDER URI
                // ---------------------------------------------

                val fileUri =
                    FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )


                // ---------------------------------------------
                // DETERMINE MIME TYPE
                // ---------------------------------------------

                val mimeType =
                    guessMimeType(
                        file.name
                    )


                // ---------------------------------------------
                // CREATE INTENT
                // ---------------------------------------------

                val intent =
                    Intent(
                        Intent.ACTION_VIEW
                    ).apply {

                        setDataAndType(
                            fileUri,
                            mimeType
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }


                // ---------------------------------------------
                // OPEN FILE
                // ---------------------------------------------

                try {

                    startActivity(
                        Intent.createChooser(
                            intent,
                            "Open Evidence"
                        )
                    )

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "No app is available to open this evidence type.",
                        Toast.LENGTH_LONG
                    ).show()
                }


                return
            }


            // =================================================
            // OLD CONTENT URI
            // =================================================

            if (
                storedValue.startsWith(
                    "content://"
                )
            ) {

                val uri =
                    Uri.parse(
                        storedValue
                    )


                val detectedMimeType =
                    contentResolver.getType(
                        uri
                    ) ?: fallbackMimeType


                val intent =
                    Intent(
                        Intent.ACTION_VIEW
                    ).apply {

                        setDataAndType(
                            uri,
                            detectedMimeType
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }


                try {

                    startActivity(
                        Intent.createChooser(
                            intent,
                            "Open Evidence"
                        )
                    )

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "This older evidence file is no longer accessible.",
                        Toast.LENGTH_LONG
                    ).show()
                }


                return
            }


            // =================================================
            // UNKNOWN FORMAT
            // =================================================

            Toast.makeText(
                this,
                "Invalid evidence file.",
                Toast.LENGTH_LONG
            ).show()


        } catch (e: Exception) {

            e.printStackTrace()


            Toast.makeText(
                this,
                "Unable to open this evidence.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // DETERMINE MIME TYPE
    // =========================================================

    private fun guessMimeType(
        fileName: String
    ): String {

        val extension =
            fileName
                .substringAfterLast(
                    ".",
                    ""
                )
                .lowercase(
                    Locale.getDefault()
                )


        return when (extension) {

            // =================================================
            // IMAGES
            // =================================================

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "png" ->
                "image/png"

            "gif" ->
                "image/gif"

            "webp" ->
                "image/webp"

            "bmp" ->
                "image/bmp"


            // =================================================
            // VIDEO
            // =================================================

            "mp4" ->
                "video/mp4"

            "3gp" ->
                "video/3gpp"

            "mkv" ->
                "video/x-matroska"

            "avi" ->
                "video/x-msvideo"

            "mov" ->
                "video/quicktime"


            // =================================================
            // AUDIO
            // =================================================

            "mp3" ->
                "audio/mpeg"

            "wav" ->
                "audio/wav"

            "m4a" ->
                "audio/mp4"

            "aac" ->
                "audio/aac"

            "ogg" ->
                "audio/ogg"

            "flac" ->
                "audio/flac"


            // =================================================
            // PDF
            // =================================================

            "pdf" ->
                "application/pdf"


            // =================================================
            // MICROSOFT WORD
            // =================================================

            "doc" ->
                "application/msword"

            "docx" ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"


            // =================================================
            // MICROSOFT EXCEL
            // =================================================

            "xls" ->
                "application/vnd.ms-excel"

            "xlsx" ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"


            // =================================================
            // MICROSOFT POWERPOINT
            // =================================================

            "ppt" ->
                "application/vnd.ms-powerpoint"

            "pptx" ->
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"


            // =================================================
            // TEXT
            // =================================================

            "txt" ->
                "text/plain"

            "csv" ->
                "text/csv"


            // =================================================
            // UNKNOWN
            // =================================================

            else ->
                "*/*"
        }
    }
}