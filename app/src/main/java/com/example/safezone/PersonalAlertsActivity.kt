package com.example.safezone

import android.content.Intent
import android.graphics.Color
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
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.SafetyAlertEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class PersonalAlertsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SafeZonePersonalAlerts"
    }

    // Views from activity_personal_alerts.xml
    private lateinit var alertsContainer: LinearLayout
    private lateinit var tvSelectedLocation: TextView

    private lateinit var btnFilterAll: AppCompatButton
    private lateinit var btnFilterEmergency: AppCompatButton
    private lateinit var btnFilterSafety: AppCompatButton
    private lateinit var btnFilterGeneral: AppCompatButton
    private lateinit var btnFilterEvents: AppCompatButton
    private lateinit var btnEnableNotifications: AppCompatButton

    // Room database access
    private val alertDao by lazy {
        SafeZoneDatabase
            .getDatabase(applicationContext)
            .safetyAlertDao()
    }

    // Model used to display saved and API alerts.
    private data class AlertDisplay(
        val alertId: String,
        val alertType: String,
        val title: String,
        val message: String,
        val location: String,
        val createdAt: String
    )

    private var apiAlerts: List<AlertDisplay> = emptyList()

    // These are actual location/category values, not translated labels.
    private var selectedLocation = "Pretoria"
    private var selectedFilter = "All"
    private var requestNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Personal Alerts screen opened")

        setContentView(R.layout.activity_personal_alerts)

        initialiseViews()
        setupClickListeners()

        tvSelectedLocation.text = selectedLocation

        updateFilterButtons(selectedFilter)

        Log.d(TAG, "Personal Alerts initialized successfully")
    }

    private fun initialiseViews() {
        alertsContainer = findViewById(R.id.alertsContainer)
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation)

        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterEmergency = findViewById(R.id.btnFilterEmergency)
        btnFilterSafety = findViewById(R.id.btnFilterSafety)
        btnFilterGeneral = findViewById(R.id.btnFilterGeneral)
        btnFilterEvents = findViewById(R.id.btnFilterEvents)

        btnEnableNotifications = findViewById(R.id.btnEnableNotifications)

        Log.d(TAG, "All Personal Alerts views connected")
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.tvBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.ivNotification).setOnClickListener {
            openNotificationSettings()
        }

        findViewById<View>(R.id.locationSelector).setOnClickListener {
            showLocationSelector()
        }

        // Internal filter values remain consistent with API/database values.
        btnFilterAll.setOnClickListener {
            selectedFilter = "All"
            showAlerts()
        }

        btnFilterEmergency.setOnClickListener {
            selectedFilter = "Emergency"
            showAlerts()
        }

        btnFilterSafety.setOnClickListener {
            selectedFilter = "Safety"
            showAlerts()
        }

        btnFilterGeneral.setOnClickListener {
            selectedFilter = "General"
            showAlerts()
        }

        btnFilterEvents.setOnClickListener {
            selectedFilter = "Events"
            showAlerts()
        }

        btnEnableNotifications.setOnClickListener {
            openNotificationSettings()
        }

        setupBottomNavigation()

        Log.d(TAG, "Personal Alerts click listeners configured")
    }

    // Location selector: translated UI, unchanged location values.
    private fun showLocationSelector() {
        val popup = PopupMenu(
            this,
            findViewById(R.id.locationSelector)
        )

        val locations = listOf(
            "Pretoria",
            "Johannesburg",
            "Durban",
            "Bloemfontein",
            "Cape Town",
            "Polokwane"
        )

        locations.forEach { location ->
            popup.menu.add(location)
        }

        popup.setOnMenuItemClickListener { item ->
            selectedLocation = item.title.toString()
            tvSelectedLocation.text = selectedLocation

            selectedFilter = "All"
            updateFilterButtons(selectedFilter)

            Toast.makeText(
                this,
                getString(
                    R.string.personal_alerts_location_selected,
                    selectedLocation
                ),
                Toast.LENGTH_SHORT
            ).show()

            loadAlertsFromApi()
            true
        }

        popup.show()
    }

    // Preload general safety guidance for offline use.
    // These are guidance items, not verified live alerts.
    private suspend fun seedDefaultOfflineAlerts() {
        val locations = listOf(
            "Pretoria",
            "Johannesburg",
            "Durban",
            "Bloemfontein",
            "Cape Town",
            "Polokwane"
        )

        val defaultGuidance = listOf(
            Triple(
                "Safety",
                "Protect your personal information",
                "Avoid sharing your home address, live location, passwords, or other sensitive information with strangers."
            ),
            Triple(
                "Safety",
                "Plan your journey",
                "Plan your route in advance, keep your phone charged, and let someone you trust know when you expect to arrive."
            ),
            Triple(
                "General",
                "Know how to get help",
                "Keep important emergency contact numbers accessible. If you are in immediate danger, contact the appropriate emergency services."
            )
        )

        for (location in locations) {
            val existingAlerts = alertDao.getAlertsForLocationOnce(location)

            // Do not overwrite existing cached alerts during seeding.
            if (existingAlerts.isNotEmpty()) continue

            val locationKey = location
                .lowercase()
                .replace(" ", "-")

            val defaultEntities = defaultGuidance.mapIndexed { index, guidance ->
                SafetyAlertEntity(
                    alertId = "offline-$locationKey-${index + 1}",
                    location = location,
                    alertType = guidance.first,
                    title = "Offline Guidance: ${guidance.second}",
                    message = guidance.third,
                    createdAt = "2026-10-11T00:00:00"
                )
            }

            alertDao.insertAll(defaultEntities)

            Log.d(
                TAG,
                "Preloaded ${defaultEntities.size} guidance items for $location"
            )
        }
    }

    // Load local data first, then try to refresh from the API.
    private fun loadAlertsFromApi() {
        val requestedLocation = selectedLocation
        val currentRequest = ++requestNumber

        Log.d(TAG, "Loading alerts for $requestedLocation")

        alertsContainer.removeAllViews()
        showLoadingMessage()

        lifecycleScope.launch {
            // STEP 1: Seed and read local database.
            try {
                withContext(Dispatchers.IO) {
                    seedDefaultOfflineAlerts()
                }

                val cachedAlerts = withContext(Dispatchers.IO) {
                    alertDao.getAlertsForLocationOnce(requestedLocation)
                }

                if (
                    currentRequest != requestNumber ||
                    requestedLocation != selectedLocation
                ) {
                    return@launch
                }

                apiAlerts = cachedAlerts.map { cached ->
                    AlertDisplay(
                        alertId = cached.alertId,
                        alertType = cached.alertType,
                        title = cached.title,
                        message = cached.message,
                        location = cached.location,
                        createdAt = cached.createdAt
                    )
                }

                showAlerts()

                Log.d(
                    TAG,
                    "Loaded ${apiAlerts.size} saved alerts for $requestedLocation"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to seed or load local alerts", e)

                if (
                    currentRequest != requestNumber ||
                    requestedLocation != selectedLocation
                ) {
                    return@launch
                }

                apiAlerts = emptyList()
                showAlerts()
            }

            // STEP 2: Fetch latest alerts from REST API.
            try {
                val response = RetrofitClient.apiService
                    .getAlertsByLocation(requestedLocation)

                if (
                    currentRequest != requestNumber ||
                    requestedLocation != selectedLocation
                ) {
                    return@launch
                }

                if (response.isSuccessful) {
                    val receivedAlerts = response.body() ?: emptyList()

                    val entities = receivedAlerts.map { alert ->
                        SafetyAlertEntity(
                            alertId = createAlertId(alert),
                            location = alert.location,
                            alertType = alert.alertType,
                            title = alert.title,
                            message = alert.message,
                            createdAt = alert.createdAt
                        )
                    }

                    // Update the cache only after a successful API response.
                    withContext(Dispatchers.IO) {
                        alertDao.deleteAlertsForLocation(requestedLocation)

                        if (entities.isNotEmpty()) {
                            alertDao.insertAll(entities)
                        }
                    }

                    if (
                        currentRequest != requestNumber ||
                        requestedLocation != selectedLocation
                    ) {
                        return@launch
                    }

                    apiAlerts = if (receivedAlerts.isNotEmpty()) {
                        receivedAlerts.map { alert ->
                            AlertDisplay(
                                alertId = createAlertId(alert),
                                alertType = alert.alertType,
                                title = alert.title,
                                message = alert.message,
                                location = alert.location,
                                createdAt = alert.createdAt
                            )
                        }
                    } else {
                        // Keep locally stored guidance if the server returns no alerts.
                        val savedAlerts = withContext(Dispatchers.IO) {
                            alertDao.getAlertsForLocationOnce(requestedLocation)
                        }

                        savedAlerts.map { cached ->
                            AlertDisplay(
                                alertId = cached.alertId,
                                alertType = cached.alertType,
                                title = cached.title,
                                message = cached.message,
                                location = cached.location,
                                createdAt = cached.createdAt
                            )
                        }
                    }

                    showAlerts()

                    Log.d(TAG, "Alerts refreshed successfully")
                } else {
                    Log.e(TAG, "API returned HTTP ${response.code()}")

                    if (
                        currentRequest == requestNumber &&
                        requestedLocation == selectedLocation
                    ) {
                        showOfflineNotice(
                            getString(
                                R.string.personal_alerts_server_error,
                                response.code()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "API unavailable; keeping saved guidance", e)

                if (
                    currentRequest != requestNumber ||
                    requestedLocation != selectedLocation
                ) {
                    return@launch
                }

                showOfflineNotice(
                    getString(R.string.personal_alerts_offline_notice)
                )
            }
        }
    }

    // Generate a stable ID from the alert's content.
    private fun createAlertId(alert: SafetyAlert): String {
        val source = listOf(
            alert.location.trim(),
            alert.alertType.trim(),
            alert.title.trim(),
            alert.message.trim(),
            alert.createdAt.trim()
        ).joinToString("|")

        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    // Filter using the original API/database category values.
    private fun showAlerts() {
        alertsContainer.removeAllViews()

        val filteredAlerts = if (selectedFilter == "All") {
            apiAlerts
        } else {
            apiAlerts.filter { alert ->
                alert.alertType.equals(selectedFilter, ignoreCase = true)
            }
        }

        updateFilterButtons(selectedFilter)

        if (filteredAlerts.isEmpty()) {
            val emptyMessage = TextView(this)

            emptyMessage.text = if (selectedFilter == "All") {
                getString(
                    R.string.personal_alerts_empty_all,
                    selectedLocation
                )
            } else {
                getString(
                    R.string.personal_alerts_empty_category,
                    getTranslatedCategory(selectedFilter),
                    selectedLocation
                )
            }

            emptyMessage.textSize = 15f
            emptyMessage.setTextColor(Color.parseColor("#637A98"))
            emptyMessage.gravity = Gravity.CENTER
            emptyMessage.setPadding(16, 50, 16, 50)

            alertsContainer.addView(emptyMessage)
            return
        }

        for (alert in filteredAlerts) {
            addAlertCard(alert)
        }

        Log.d(TAG, "Displayed ${filteredAlerts.size} alerts")
    }

    // Create a card without changing the existing card drawable.
    private fun addAlertCard(alert: AlertDisplay) {
        val card = LinearLayout(this)

        card.orientation = LinearLayout.VERTICAL
        card.setPadding(18, 16, 18, 16)
        card.setBackgroundResource(R.drawable.bg_alert_card)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cardParams.setMargins(0, 0, 0, 14)
        card.layoutParams = cardParams

        // Category label.
        val typeText = TextView(this)
        typeText.text = getTranslatedCategory(alert.alertType).uppercase()
        typeText.textSize = 12f
        typeText.setTypeface(null, android.graphics.Typeface.BOLD)
        typeText.setTextColor(getAlertTypeColor(alert.alertType))
        card.addView(typeText)

        // Alert title from the API or database.
        val titleText = TextView(this)
        titleText.text = alert.title
        titleText.textSize = 18f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        titleText.setTextColor(Color.parseColor("#12345A"))

        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        titleParams.setMargins(0, 6, 0, 0)
        titleText.layoutParams = titleParams
        card.addView(titleText)

        // Alert message from the API or database.
        val messageText = TextView(this)
        messageText.text = alert.message
        messageText.textSize = 14f
        messageText.setTextColor(Color.parseColor("#526B8D"))

        val messageParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        messageParams.setMargins(0, 8, 0, 0)
        messageText.layoutParams = messageParams
        card.addView(messageText)

        // Location label is localized; the actual place name is unchanged.
        val locationText = TextView(this)
        locationText.text = getString(
            R.string.personal_alerts_card_location,
            alert.location
        )
        locationText.textSize = 13f
        locationText.setTextColor(Color.parseColor("#526B8D"))

        val locationParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        locationParams.setMargins(0, 8, 0, 0)
        locationText.layoutParams = locationParams
        card.addView(locationText)

        // Date and time.
        val timeText = TextView(this)
        timeText.text = formatDateTime(alert.createdAt)
        timeText.textSize = 12f
        timeText.setTextColor(Color.parseColor("#7B8EA8"))

        val timeParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        timeParams.setMargins(0, 12, 0, 0)
        timeText.layoutParams = timeParams
        card.addView(timeText)

        alertsContainer.addView(card)
    }

    private fun formatDateTime(createdAt: String): String {
        if (createdAt.isBlank()) {
            return getString(R.string.personal_alerts_datetime_unavailable)
        }

        return try {
            val parts = createdAt.split("T")

            if (parts.size >= 2) {
                val date = parts[0]
                val time = parts[1].take(5)

                getString(
                    R.string.personal_alerts_date_time_format,
                    date,
                    time
                )
            } else {
                createdAt
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date/time", e)
            createdAt
        }
    }

    // Translate known categories for display without changing their stored values.
    private fun getTranslatedCategory(type: String): String {
        return when {
            type.equals("Emergency", ignoreCase = true) ->
                getString(R.string.personal_alerts_filter_emergency)

            type.equals("Safety", ignoreCase = true) ->
                getString(R.string.personal_alerts_filter_safety)

            type.equals("General", ignoreCase = true) ->
                getString(R.string.personal_alerts_filter_general)

            type.equals("Events", ignoreCase = true) ->
                getString(R.string.personal_alerts_filter_events)

            else -> type
        }
    }

    private fun getAlertTypeColor(type: String): Int {
        return when {
            type.equals("Emergency", ignoreCase = true) ->
                Color.parseColor("#C62828")

            type.equals("Safety", ignoreCase = true) ->
                Color.parseColor("#1976D2")

            type.equals("Events", ignoreCase = true) ->
                Color.parseColor("#7B1FA2")

            else -> Color.parseColor("#526B8D")
        }
    }

    private fun updateFilterButtons(selectedFilter: String) {
        resetFilterButton(btnFilterAll)
        resetFilterButton(btnFilterEmergency)
        resetFilterButton(btnFilterSafety)
        resetFilterButton(btnFilterGeneral)
        resetFilterButton(btnFilterEvents)

        when (selectedFilter) {
            "All" -> selectFilterButton(btnFilterAll)
            "Emergency" -> selectFilterButton(btnFilterEmergency)
            "Safety" -> selectFilterButton(btnFilterSafety)
            "General" -> selectFilterButton(btnFilterGeneral)
            "Events" -> selectFilterButton(btnFilterEvents)
        }
    }

    private fun resetFilterButton(button: AppCompatButton) {
        button.setBackgroundResource(R.drawable.bg_alert_filter)
        button.setTextColor(Color.parseColor("#243B5A"))
    }

    private fun selectFilterButton(button: AppCompatButton) {
        button.setBackgroundResource(R.drawable.bg_alert_filter_selected)
        button.setTextColor(Color.WHITE)
    }

    private fun showLoadingMessage() {
        val text = TextView(this)

        text.text = getString(R.string.personal_alerts_loading)
        text.textSize = 15f
        text.gravity = Gravity.CENTER
        text.setTextColor(Color.parseColor("#637A98"))
        text.setPadding(16, 40, 16, 40)

        alertsContainer.addView(text)
    }

    private fun showOfflineNotice(message: String) {
        val notice = TextView(this)

        notice.text = message
        notice.textSize = 13f
        notice.setTextColor(Color.parseColor("#7B5E00"))
        notice.setBackgroundColor(Color.parseColor("#FFF4CC"))
        notice.setPadding(14, 12, 14, 12)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 14)
        notice.layoutParams = params

        // Insert the notice above the alert cards.
        alertsContainer.addView(notice, 0)
    }

    private fun showErrorMessage(message: String) {
        val errorText = TextView(this)

        errorText.text = message
        errorText.textSize = 15f
        errorText.gravity = Gravity.CENTER
        errorText.setTextColor(Color.parseColor("#C62828"))
        errorText.setPadding(20, 40, 20, 40)

        alertsContainer.addView(errorText)
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = android.net.Uri.parse("package:$packageName")
            }

            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open notification settings", e)

            Toast.makeText(
                this,
                R.string.personal_alerts_notification_settings_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupBottomNavigation() {
        // HOME
        findViewById<View>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, PersonalHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        // MAP
        findViewById<View>(R.id.navMap).setOnClickListener {
            startActivity(Intent(this, FindHelpActivity::class.java))
        }

        // REPORTS
        findViewById<View>(R.id.navReports).setOnClickListener {
            startActivity(Intent(this, PersonalReportsActivity::class.java))
        }

        // ALERTS
        findViewById<View>(R.id.navNotifications).setOnClickListener {
            selectedFilter = "All"
            showAlerts()
        }

        // PROFILE
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, PersonalProfileActivity::class.java))
        }
    }

    // Refresh cached/API alerts when returning to the screen.
    override fun onResume() {
        super.onResume()

        if (::alertsContainer.isInitialized) {
            loadAlertsFromApi()
        }
    }
}