package com.example.safezone

import android.content.Intent
import android.os.Build
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
import kotlinx.coroutines.launch

class PersonalAlertsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SafeZonePersonalAlerts"
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var alertsContainer: LinearLayout
    private lateinit var tvSelectedLocation: TextView

    private lateinit var btnFilterAll: AppCompatButton
    private lateinit var btnFilterEmergency: AppCompatButton
    private lateinit var btnFilterSafety: AppCompatButton
    private lateinit var btnFilterGeneral: AppCompatButton
    private lateinit var btnFilterEvents: AppCompatButton

    private lateinit var btnEnableNotifications: AppCompatButton

    // =========================================================
    // DATA
    // =========================================================

    private var apiAlerts: List<SafetyAlert> = emptyList()

    // Default Personal Alerts location
    private var selectedLocation = "Pretoria"

    private var selectedFilter = "All"

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        Log.d(TAG, "Personal Alerts screen opened")

        setContentView(R.layout.activity_personal_alerts)

        initialiseViews()

        setupClickListeners()

        Log.d(
            TAG,
            "Personal Alerts controls initialized successfully"
        )

        // Default location
        tvSelectedLocation.text = selectedLocation

        loadAlertsFromApi()
    }

    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        alertsContainer =
            findViewById(R.id.alertsContainer)

        tvSelectedLocation =
            findViewById(R.id.tvSelectedLocation)

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

        Log.d(
            TAG,
            "All Personal Alerts views connected successfully"
        )
    }

    // =========================================================
    // CLICK LISTENERS
    // =========================================================

    private fun setupClickListeners() {

        // =====================================================
        // BACK
        // =====================================================

        findViewById<View>(R.id.tvBack).setOnClickListener {

            Log.d(
                TAG,
                "Back button clicked"
            )

            finish()
        }

        // =====================================================
        // NOTIFICATION SETTINGS
        // =====================================================

        findViewById<View>(R.id.ivNotification).setOnClickListener {

            Log.d(
                TAG,
                "Notification settings icon clicked"
            )

            openNotificationSettings()
        }

        // =====================================================
        // LOCATION SELECTOR
        // =====================================================

        findViewById<View>(R.id.locationSelector).setOnClickListener {

            Log.d(
                TAG,
                "Personal location selector clicked"
            )

            showLocationSelector()
        }

        // =====================================================
        // FILTER - ALL
        // =====================================================

        btnFilterAll.setOnClickListener {

            Log.d(
                TAG,
                "All alerts filter selected"
            )

            selectedFilter = "All"

            showAlerts()
        }

        // =====================================================
        // FILTER - EMERGENCY
        // =====================================================

        btnFilterEmergency.setOnClickListener {

            Log.d(
                TAG,
                "Emergency alerts filter selected"
            )

            selectedFilter = "Emergency"

            showAlerts()
        }

        // =====================================================
        // FILTER - SAFETY
        // =====================================================

        btnFilterSafety.setOnClickListener {

            Log.d(
                TAG,
                "Safety alerts filter selected"
            )

            selectedFilter = "Safety"

            showAlerts()
        }

        // =====================================================
        // FILTER - GENERAL
        // =====================================================

        btnFilterGeneral.setOnClickListener {

            Log.d(
                TAG,
                "General alerts filter selected"
            )

            selectedFilter = "General"

            showAlerts()
        }

        // =====================================================
        // FILTER - EVENTS
        // =====================================================

        btnFilterEvents.setOnClickListener {

            Log.d(
                TAG,
                "Events alerts filter selected"
            )

            selectedFilter = "Events"

            showAlerts()
        }

        // =====================================================
        // ENABLE NOTIFICATIONS
        // =====================================================

        btnEnableNotifications.setOnClickListener {

            Log.d(
                TAG,
                "Enable notifications button clicked"
            )

            openNotificationSettings()
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        setupBottomNavigation()

        Log.d(
            TAG,
            "Personal Alerts click listeners configured"
        )
    }

    // =========================================================
    // LOCATION SELECTOR
    // =========================================================

    private fun showLocationSelector() {

        Log.d(
            TAG,
            "Opening personal location selection menu"
        )

        val popup = PopupMenu(
            this,
            findViewById(R.id.locationSelector)
        )

        // Personal Alerts locations
        popup.menu.add("Pretoria")
        popup.menu.add("Johannesburg")
        popup.menu.add("Durban")
        popup.menu.add("Bloemfontein")
        popup.menu.add("Cape Town")
        popup.menu.add("Polokwane")

        popup.setOnMenuItemClickListener { item ->

            selectedLocation =
                item.title.toString()

            tvSelectedLocation.text =
                selectedLocation

            // Reset filter whenever location changes
            selectedFilter = "All"

            Log.d(
                TAG,
                "Personal location changed to: $selectedLocation"
            )

            Toast.makeText(
                this,
                "$selectedLocation selected.",
                Toast.LENGTH_SHORT
            ).show()

            loadAlertsFromApi()

            true
        }

        popup.show()
    }

    // =========================================================
    // LOAD PERSONAL ALERTS FROM REST API
    // =========================================================

    private fun loadAlertsFromApi() {

        Log.d(
            TAG,
            "Starting REST API request for Personal Alerts"
        )

        Log.d(
            TAG,
            "Selected personal location: $selectedLocation"
        )

        alertsContainer.removeAllViews()

        showLoadingMessage()

        lifecycleScope.launch {

            try {

                Log.d(
                    TAG,
                    "Sending Personal Alerts request to SafeZone REST API"
                )

                // =================================================
                // IMPORTANT:
                // Personal Alerts use LOCATION, not CAMPUS.
                //
                // Example:
                // /api/SafetyAlerts/location/Pretoria
                // =================================================

                val response =
                    RetrofitClient.apiService
                        .getAlertsByLocation(
                            selectedLocation
                        )

                if (response.isSuccessful) {

                    Log.d(
                        TAG,
                        "Personal Alerts API request completed successfully"
                    )

                    apiAlerts =
                        response.body()
                            ?: emptyList()

                    Log.d(
                        TAG,
                        "Number of Personal Alerts received: ${apiAlerts.size}"
                    )

                    showAlerts()

                } else {

                    apiAlerts =
                        emptyList()

                    Log.e(
                        TAG,
                        "Personal Alerts API returned unsuccessful response: ${response.code()}"
                    )

                    showErrorMessage(
                        "Unable to load safety alerts.\n" +
                                "Server response: ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                apiAlerts =
                    emptyList()

                Log.e(
                    TAG,
                    "Unable to connect to SafeZone Personal Alerts API",
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
            "Displaying Personal Alerts using filter: $selectedFilter"
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
            "Personal Alerts after filtering: ${filteredAlerts.size}"
        )

        updateFilterButtons(selectedFilter)

        // =====================================================
        // NO ALERTS
        // =====================================================

        if (filteredAlerts.isEmpty()) {

            Log.d(
                TAG,
                "No Personal Alerts available for selected filter"
            )

            val emptyMessage =
                TextView(this)

            emptyMessage.text =
                if (selectedFilter == "All") {

                    "No safety alerts are currently available for $selectedLocation."

                } else {

                    "No $selectedFilter alerts are currently available for $selectedLocation."
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
                50,
                16,
                50
            )

            alertsContainer.addView(
                emptyMessage
            )

            return
        }

        // =====================================================
        // ADD ALERT CARDS
        // =====================================================

        for (alert in filteredAlerts) {

            addAlertCard(alert)
        }

        Log.d(
            TAG,
            "Personal alert cards displayed successfully"
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
            "Displaying Personal Alert: ${alert.title}"
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
        // DATE / TIME
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
    // FORMAT DATE/TIME
    // =========================================================

    private fun formatDateTime(
        createdAt: String
    ): String {

        if (createdAt.isBlank()) {

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

        resetFilterButton(btnFilterAll)

        resetFilterButton(btnFilterEmergency)

        resetFilterButton(btnFilterSafety)

        resetFilterButton(btnFilterGeneral)

        resetFilterButton(btnFilterEvents)

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

        val text =
            TextView(this)

        text.text =
            "Loading safety alerts..."

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
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
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

        // =====================================================
        // HOME
        // =====================================================

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
                    PersonalHomeActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(intent)

            finish()
        }

        // =====================================================
        // MAP
        // =====================================================

        findViewById<View>(
            R.id.navMap
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Map selected"
            )

            startActivity(
                Intent(
                    this,
                    FindHelpActivity::class.java
                )
            )
        }

        // =====================================================
        // REPORTS
        // =====================================================

        findViewById<View>(
            R.id.navReports
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Reports selected"
            )

            startActivity(
                Intent(
                    this,
                    PersonalReportsActivity::class.java
                )
            )
        }

        // =====================================================
        // ALERTS
        // =====================================================

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

        // =====================================================
        // PROFILE
        // =====================================================

        findViewById<View>(
            R.id.navProfile
        ).setOnClickListener {

            Log.d(
                TAG,
                "Bottom navigation: Profile selected"
            )

            startActivity(
                Intent(
                    this,
                    PersonalProfileActivity::class.java
                )
            )
        }
    }

    // =========================================================
    // REFRESH WHEN RETURNING
    // =========================================================

    override fun onResume() {

        super.onResume()

        Log.d(
            TAG,
            "Personal Alerts screen resumed"
        )

        if (::alertsContainer.isInitialized) {

            loadAlertsFromApi()
        }
    }
}