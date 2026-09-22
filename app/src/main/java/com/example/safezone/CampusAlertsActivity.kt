package com.example.safezone

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.safezone.api.RetrofitClient
import com.example.safezone.api.SafetyAlert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class CampusAlertsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SafeZoneCampusAlerts"
    }

    private lateinit var alertsContainer: LinearLayout
    private lateinit var tvSelectedCampus: TextView

    private lateinit var btnFilterAll: AppCompatButton
    private lateinit var btnFilterEmergency: AppCompatButton
    private lateinit var btnFilterSafety: AppCompatButton
    private lateinit var btnFilterGeneral: AppCompatButton
    private lateinit var btnFilterEvents: AppCompatButton

    private lateinit var btnEnableNotifications: AppCompatButton

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Alerts received from the REST API
    private var apiAlerts: List<SafetyAlert> = emptyList()

    // Current campus used by the API
    private var selectedCampus = "Pretoria"

    // Current filter
    private var selectedFilter = "All"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Campus Alerts screen opened")

        setContentView(R.layout.activity_campus_alerts)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Log.d(TAG, "Firebase Authentication and Firestore initialized")

        initialiseViews()
        setupClickListeners()

        Log.d(TAG, "Campus Alerts screen controls initialized successfully")

        loadStudentCampus()
    }


    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        alertsContainer =
            findViewById(R.id.alertsContainer)

        tvSelectedCampus =
            findViewById(R.id.tvSelectedCampus)

        btnFilterAll =
            findViewById(R.id.btnFilterAll)

        btnFilterEmergency =
            findViewById(R.id.btnFilterEmergency)

        btnFilterSafety =
            findViewById(R.id.btnFilterSafety)

        btnFilterGeneral =
            findViewById(R.id.btnFilterGeneral)

        btnFilterEvents =
            findViewById(R.id.btnFilterEvents)

        btnEnableNotifications =
            findViewById(R.id.btnEnableNotifications)

        Log.d(TAG, "All Campus Alerts views connected successfully")
    }


    // =========================================================
    // CLICK LISTENERS
    // =========================================================

    private fun setupClickListeners() {

        // Back button
        findViewById<View>(R.id.tvBack).setOnClickListener {

            Log.d(TAG, "Back button clicked")

            finish()
        }


        // Notification icon
        findViewById<View>(R.id.ivNotification).setOnClickListener {

            Log.d(TAG, "Notification icon clicked")

            openNotificationSettings()
        }


        // Campus selector
        findViewById<View>(R.id.campusSelector).setOnClickListener {

            Log.d(TAG, "Campus selector clicked")

            showCampusSelector()
        }


        // Filters
        btnFilterAll.setOnClickListener {

            Log.d(TAG, "All alerts filter selected")

            selectedFilter = "All"

            showAlerts()
        }


        btnFilterEmergency.setOnClickListener {

            Log.d(TAG, "Emergency alerts filter selected")

            selectedFilter = "Emergency"

            showAlerts()
        }


        btnFilterSafety.setOnClickListener {

            Log.d(TAG, "Safety alerts filter selected")

            selectedFilter = "Safety"

            showAlerts()
        }


        btnFilterGeneral.setOnClickListener {

            Log.d(TAG, "General alerts filter selected")

            selectedFilter = "General"

            showAlerts()
        }


        btnFilterEvents.setOnClickListener {

            Log.d(TAG, "Events alerts filter selected")

            selectedFilter = "Events"

            showAlerts()
        }


        // Enable notifications
        btnEnableNotifications.setOnClickListener {

            Log.d(TAG, "Enable notifications button clicked")

            openNotificationSettings()
        }


        // Bottom navigation
        setupBottomNavigation()

        Log.d(TAG, "Campus Alerts click listeners configured")
    }


    // =========================================================
    // LOAD STUDENT CAMPUS FROM FIRESTORE
    // =========================================================

    private fun loadStudentCampus() {

        Log.d(TAG, "Loading student's campus from Firestore")

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Log.e(TAG, "No authenticated campus user found")

            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(
                Intent(
                    this,
                    CampusLoginActivity::class.java
                )
            )

            finish()

            return
        }

        Log.d(TAG, "Authenticated campus user found")


        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                Log.d(TAG, "Student profile loaded from Firestore")

                val campus =
                    document.getString("campus")

                if (!campus.isNullOrBlank()) {

                    selectedCampus =
                        cleanCampusName(campus)

                    tvSelectedCampus.text =
                        "$selectedCampus Campus"

                    Log.d(
                        TAG,
                        "Student campus loaded successfully: $selectedCampus"
                    )

                } else {

                    selectedCampus = "Pretoria"

                    tvSelectedCampus.text =
                        "Pretoria Campus"

                    Log.d(
                        TAG,
                        "Student campus unavailable. Using default campus: Pretoria"
                    )
                }

                // Load alerts from REST API
                loadAlertsFromApi()
            }
            .addOnFailureListener { exception ->

                Log.e(
                    TAG,
                    "Failed to load student campus from Firestore",
                    exception
                )

                selectedCampus = "Pretoria"

                tvSelectedCampus.text =
                    "Pretoria Campus"

                Log.d(
                    TAG,
                    "Using default campus: Pretoria"
                )

                loadAlertsFromApi()
            }
    }


    // =========================================================
    // CLEAN CAMPUS NAME
    // =========================================================

    private fun cleanCampusName(campus: String): String {

        return campus
            .replace(
                " Campus",
                "",
                ignoreCase = true
            )
            .trim()
    }


    // =========================================================
    // CAMPUS SELECTOR
    // =========================================================

    private fun showCampusSelector() {

        Log.d(TAG, "Opening campus selection menu")

        val popup = PopupMenu(
            this,
            findViewById(R.id.campusSelector)
        )


        popup.menu.add("Pretoria Campus")
        popup.menu.add("Johannesburg Campus")
        popup.menu.add("Polokwane Campus")
        popup.menu.add("Durban Campus")
        popup.menu.add("Bloemfontein Campus")


        popup.setOnMenuItemClickListener { item ->

            val selectedName =
                item.title.toString()

            selectedCampus =
                cleanCampusName(selectedName)

            tvSelectedCampus.text =
                selectedName

            selectedFilter = "All"

            Log.d(
                TAG,
                "Campus changed to: $selectedCampus"
            )

            Toast.makeText(
                this,
                "$selectedName selected.",
                Toast.LENGTH_SHORT
            ).show()

            // Request alerts for selected campus
            loadAlertsFromApi()

            true
        }

        popup.show()
    }


    // =========================================================
    // LOAD ALERTS FROM REST API
    // =========================================================

    private fun loadAlertsFromApi() {

        Log.d(
            TAG,
            "Starting REST API request for campus: $selectedCampus"
        )

        alertsContainer.removeAllViews()

        showLoadingMessage()


        lifecycleScope.launch {

            try {

                Log.d(
                    TAG,
                    "Sending request to SafeZone REST API"
                )

                val response =
                    RetrofitClient.apiService
                        .getAlertsByCampus(selectedCampus)


                if (response.isSuccessful) {

                    Log.d(
                        TAG,
                        "REST API request completed successfully"
                    )

                    apiAlerts =
                        response.body() ?: emptyList()

                    Log.d(
                        TAG,
                        "Number of alerts received from API: ${apiAlerts.size}"
                    )

                    showAlerts()

                } else {

                    apiAlerts = emptyList()

                    Log.e(
                        TAG,
                        "REST API returned unsuccessful response: ${response.code()}"
                    )

                    showErrorMessage(
                        "Unable to load campus alerts.\n" +
                                "Server response: ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                apiAlerts = emptyList()

                Log.e(
                    TAG,
                    "Unable to connect to SafeZone REST API",
                    e
                )

                showErrorMessage(
                    "Unable to connect to the SafeZone API.\n\n" +
                            "Make sure SafeZoneAPI is running and " +
                            "your phone is connected to the same Wi-Fi."
                )
            }
        }
    }


    // =========================================================
    // DISPLAY ALERTS
    // =========================================================

    private fun showAlerts() {

        Log.d(
            TAG,
            "Displaying alerts using filter: $selectedFilter"
        )

        alertsContainer.removeAllViews()


        val filteredAlerts =
            if (selectedFilter == "All") {

                apiAlerts

            } else {

                apiAlerts.filter {

                    it.alertType.equals(
                        selectedFilter,
                        ignoreCase = true
                    )
                }
            }


        Log.d(
            TAG,
            "Alerts after filtering: ${filteredAlerts.size}"
        )


        updateFilterButtons(selectedFilter)


        if (filteredAlerts.isEmpty()) {

            Log.d(
                TAG,
                "No alerts available for selected filter"
            )

            val emptyMessage =
                TextView(this)

            emptyMessage.text =
                if (selectedFilter == "All") {

                    "No safety alerts are currently available for $selectedCampus Campus."

                } else {

                    "No $selectedFilter alerts are currently available for $selectedCampus Campus."
                }

            emptyMessage.textSize = 15f

            emptyMessage.setTextColor(
                android.graphics.Color.parseColor(
                    "#637A98"
                )
            )

            emptyMessage.gravity =
                Gravity.CENTER

            emptyMessage.setPadding(
                16,
                40,
                16,
                40
            )

            alertsContainer.addView(
                emptyMessage
            )

            return
        }


        for (alert in filteredAlerts) {

            addAlertCard(alert)
        }

        Log.d(
            TAG,
            "Alert cards displayed successfully"
        )
    }


    // =========================================================
    // ALERT CARD
    // =========================================================

    private fun addAlertCard(
        alert: SafetyAlert
    ) {

        Log.d(
            TAG,
            "Displaying alert: ${alert.title}"
        )

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            18,
            16,
            18,
            16
        )

        card.background =
            getDrawable(
                R.drawable.bg_alert_card
            )


        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.setMargins(
            0,
            0,
            0,
            14
        )

        card.layoutParams =
            cardParams


        // =====================================================
        // ALERT TYPE
        // =====================================================

        val typeText =
            TextView(this)

        typeText.text =
            alert.alertType.uppercase()

        typeText.textSize =
            12f

        typeText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        typeText.setTextColor(
            getAlertTypeColor(
                alert.alertType
            )
        )

        card.addView(
            typeText
        )


        // =====================================================
        // TITLE
        // =====================================================

        val titleText =
            TextView(this)

        titleText.text =
            alert.title

        titleText.textSize =
            18f

        titleText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        titleText.setTextColor(
            android.graphics.Color.parseColor(
                "#12345A"
            )
        )


        val titleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        titleParams.setMargins(
            0,
            6,
            0,
            0
        )

        titleText.layoutParams =
            titleParams

        card.addView(
            titleText
        )


        // =====================================================
        // MESSAGE
        // =====================================================

        val messageText =
            TextView(this)

        messageText.text =
            alert.message

        messageText.textSize =
            14f

        messageText.setTextColor(
            android.graphics.Color.parseColor(
                "#526B8D"
            )
        )


        val messageParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        messageParams.setMargins(
            0,
            8,
            0,
            0
        )

        messageText.layoutParams =
            messageParams

        card.addView(
            messageText
        )


        // =====================================================
        // LOCATION
        // =====================================================

        val locationText =
            TextView(this)

        locationText.text =
            "Location: ${alert.location}"

        locationText.textSize =
            13f

        locationText.setTextColor(
            android.graphics.Color.parseColor(
                "#526B8D"
            )
        )


        val locationParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        locationParams.setMargins(
            0,
            8,
            0,
            0
        )

        locationText.layoutParams =
            locationParams

        card.addView(
            locationText
        )


        // =====================================================
        // CAMPUS
        // =====================================================

        val campusText =
            TextView(this)

        campusText.text =
            "Campus: ${alert.campus}"

        campusText.textSize =
            13f

        campusText.setTextColor(
            android.graphics.Color.parseColor(
                "#1976D2"
            )
        )

        campusText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )


        val campusParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        campusParams.setMargins(
            0,
            5,
            0,
            0
        )

        campusText.layoutParams =
            campusParams

        card.addView(
            campusText
        )


        // =====================================================
        // TIME
        // =====================================================

        val timeText =
            TextView(this)

        timeText.text =
            formatDateTime(
                alert.createdAt
            )

        timeText.textSize =
            12f

        timeText.setTextColor(
            android.graphics.Color.parseColor(
                "#7B8EA8"
            )
        )


        val timeParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        timeParams.setMargins(
            0,
            12,
            0,
            0
        )

        timeText.layoutParams =
            timeParams

        card.addView(
            timeText
        )


        alertsContainer.addView(
            card
        )
    }


    // =========================================================
    // FORMAT API DATE/TIME
    // =========================================================

    private fun formatDateTime(
        createdAt: String
    ): String {

        if (createdAt.isBlank()) {

            Log.d(
                TAG,
                "Alert date/time is unavailable"
            )

            return "Date and time unavailable"
        }


        return try {

            val parts =
                createdAt.split("T")

            if (parts.size >= 2) {

                val date =
                    parts[0]

                val time =
                    parts[1]
                        .take(5)

                "Date: $date  •  Time: $time"

            } else {

                createdAt
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error formatting alert date/time",
                e
            )

            createdAt
        }
    }


    // =========================================================
    // ALERT TYPE COLOUR
    // =========================================================

    private fun getAlertTypeColor(
        type: String
    ): Int {

        return when {

            type.equals(
                "Emergency",
                ignoreCase = true
            ) ->
                android.graphics.Color.parseColor(
                    "#C62828"
                )

            type.equals(
                "Safety",
                ignoreCase = true
            ) ->
                android.graphics.Color.parseColor(
                    "#1976D2"
                )

            type.equals(
                "Events",
                ignoreCase = true
            ) ->
                android.graphics.Color.parseColor(
                    "#7B1FA2"
                )

            else ->
                android.graphics.Color.parseColor(
                    "#526B8D"
                )
        }
    }


    // =========================================================
    // FILTER BUTTONS
    // =========================================================

    private fun updateFilterButtons(
        selectedFilter: String
    ) {

        resetFilterButton(
            btnFilterAll
        )

        resetFilterButton(
            btnFilterEmergency
        )

        resetFilterButton(
            btnFilterSafety
        )

        resetFilterButton(
            btnFilterGeneral
        )

        resetFilterButton(
            btnFilterEvents
        )


        when (selectedFilter) {

            "All" ->
                selectFilterButton(
                    btnFilterAll
                )

            "Emergency" ->
                selectFilterButton(
                    btnFilterEmergency
                )

            "Safety" ->
                selectFilterButton(
                    btnFilterSafety
                )

            "General" ->
                selectFilterButton(
                    btnFilterGeneral
                )

            "Events" ->
                selectFilterButton(
                    btnFilterEvents
                )
        }

        Log.d(
            TAG,
            "Filter buttons updated. Selected filter: $selectedFilter"
        )
    }


    private fun resetFilterButton(
        button: AppCompatButton
    ) {

        button.setBackgroundResource(
            R.drawable.bg_alert_filter
        )

        button.setTextColor(
            android.graphics.Color.parseColor(
                "#243B5A"
            )
        )
    }


    private fun selectFilterButton(
        button: AppCompatButton
    ) {

        button.setBackgroundResource(
            R.drawable.bg_alert_filter_selected
        )

        button.setTextColor(
            android.graphics.Color.WHITE
        )
    }


    // =========================================================
    // LOADING MESSAGE
    // =========================================================

    private fun showLoadingMessage() {

        Log.d(
            TAG,
            "Showing API loading message"
        )

        val text =
            TextView(this)

        text.text =
            "Loading campus safety alerts..."

        text.textSize =
            15f

        text.gravity =
            Gravity.CENTER

        text.setTextColor(
            android.graphics.Color.parseColor(
                "#637A98"
            )
        )

        text.setPadding(
            16,
            40,
            16,
            40
        )

        alertsContainer.addView(
            text
        )
    }


    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private fun showErrorMessage(
        message: String
    ) {

        Log.e(
            TAG,
            "Displaying API error message"
        )

        val errorText =
            TextView(this)

        errorText.text =
            message

        errorText.textSize =
            15f

        errorText.gravity =
            Gravity.CENTER

        errorText.setTextColor(
            android.graphics.Color.parseColor(
                "#C62828"
            )
        )

        errorText.setPadding(
            20,
            40,
            20,
            40
        )

        alertsContainer.addView(
            errorText
        )
    }


    // =========================================================
    // NOTIFICATION SETTINGS
    // =========================================================

    private fun openNotificationSettings() {

        Log.d(
            TAG,
            "Opening Android notification settings"
        )

        try {

            val intent =
                Intent()

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
            ) {

                intent.action =
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS

                intent.putExtra(
                    Settings.EXTRA_APP_PACKAGE,
                    packageName
                )

            } else {

                intent.action =
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS

                intent.data =
                    android.net.Uri.parse(
                        "package:$packageName"
                    )
            }


            startActivity(intent)

            Log.d(
                TAG,
                "Notification settings opened successfully"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to open notification settings",
                e
            )

            Toast.makeText(
                this,
                "Notification settings are unavailable.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun setupBottomNavigation() {

        // HOME
        findViewById<View>(
            R.id.navHome
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Home selected"
            )

            val intent =
                Intent(
                    this,
                    CampusHomeActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(intent)

            finish()
        }


        // MAP
        findViewById<View>(
            R.id.navMap
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Map selected"
            )

            try {

                startActivity(
                    Intent(
                        this,
                        CampusSafetyMapActivity::class.java
                    )
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Campus map could not be opened",
                    e
                )

                Toast.makeText(
                    this,
                    "Campus map is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // REPORTS
        findViewById<View>(
            R.id.navReports
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Reports selected"
            )

            try {

                startActivity(
                    Intent(
                        this,
                        CampusReportsActivity::class.java
                    )
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Campus reports could not be opened",
                    e
                )

                Toast.makeText(
                    this,
                    "Reports are not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // NOTIFICATIONS / ALERTS
        findViewById<View>(
            R.id.navNotifications
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Alerts selected"
            )

            selectedFilter = "All"

            showAlerts()
        }


        // PROFILE
        findViewById<View>(
            R.id.navProfile
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Profile selected"
            )

            try {

                startActivity(
                    Intent(
                        this,
                        CampusProfileActivity::class.java
                    )
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Campus profile could not be opened",
                    e
                )

                Toast.makeText(
                    this,
                    "Profile is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        Log.d(
            TAG,
            "Bottom navigation configured successfully"
        )
    }


    // =========================================================
    // REFRESH WHEN RETURNING TO SCREEN
    // =========================================================

    override fun onResume() {

        super.onResume()

        Log.d(
            TAG,
            "Campus Alerts screen resumed"
        )

        // Only reload if the activity has already
        // loaded the student's campus.
        if (
            ::auth.isInitialized &&
            auth.currentUser != null &&
            ::tvSelectedCampus.isInitialized
        ) {

            Log.d(
                TAG,
                "Refreshing campus alerts from REST API"
            )

            loadAlertsFromApi()
        }
    }
}