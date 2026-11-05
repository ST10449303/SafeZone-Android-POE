package com.example.safezone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.safezone.api.RetrofitClient
import com.example.safezone.api.SafetyAlert
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.SafetyAlertEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CampusAlertsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SafeZoneCampusAlerts"

        private const val PREFS_NAME =
            "safezone_campus_preferences"

        private const val KEY_LAST_CAMPUS =
            "last_selected_campus"

        private const val ALERT_CHANNEL_ID =
            "safezone_campus_alerts"

        private val AVAILABLE_CAMPUSES = listOf(
            "Pretoria",
            "Johannesburg",
            "Polokwane",
            "Durban",
            "Bloemfontein"
        )
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var alertsContainer: LinearLayout
    private lateinit var tvSelectedCampus: TextView

    private lateinit var btnFilterAll: AppCompatButton
    private lateinit var btnFilterEmergency: AppCompatButton
    private lateinit var btnFilterSafety: AppCompatButton
    private lateinit var btnFilterGeneral: AppCompatButton
    private lateinit var btnFilterEvents: AppCompatButton

    private lateinit var btnEnableNotifications: AppCompatButton

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var campusAlertsListener: ListenerRegistration? = null

    // Track which campus this listener belongs to so we do not
    // unnecessarily restart it after the profile lookup completes.
    private var listeningCampus: String? = null

    private var initialCampusSnapshotReceived = false

    // Prevent repeat notifications while this Activity instance
    // remains alive.
    private val notifiedAlertIds = mutableSetOf<String>()

    // =========================================================
    // LOCAL DATABASE
    // =========================================================

    private val alertDao by lazy {
        SafeZoneDatabase
            .getDatabase(applicationContext)
            .safetyAlertDao()
    }

    private val campusPreferences by lazy {
        getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    // =========================================================
    // ALERT DATA
    // =========================================================

    private var apiAlerts: List<SafetyAlert> = emptyList()

    private var selectedCampus = "Pretoria"

    private var selectedFilter = "All"

    private var offlineNotice: String? = null

    private var requestNumber = 0

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Campus Alerts screen opened")

        setContentView(R.layout.activity_campus_alerts)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initialiseViews()
        setupClickListeners()
        restoreSelectedCampus()
        createCampusNotificationChannel()
        loadStudentCampus()
    }

    // =========================================================
    // LIFECYCLE: START AND STOP REAL-TIME LISTENER
    // =========================================================

    override fun onStart() {
        super.onStart()

        if (::auth.isInitialized) {
            startCampusAlertsListener()
        }
    }

    override fun onStop() {
        stopCampusAlertsListener()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::alertsContainer.isInitialized &&
            ::auth.isInitialized
        ) {
            Log.d(
                TAG,
                "Campus Alerts resumed; refreshing local and live data"
            )

            loadAlertsFromApi()
        }
    }

    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {
        alertsContainer = findViewById(R.id.alertsContainer)
        tvSelectedCampus = findViewById(R.id.tvSelectedCampus)

        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterEmergency = findViewById(R.id.btnFilterEmergency)
        btnFilterSafety = findViewById(R.id.btnFilterSafety)
        btnFilterGeneral = findViewById(R.id.btnFilterGeneral)
        btnFilterEvents = findViewById(R.id.btnFilterEvents)

        btnEnableNotifications =
            findViewById(R.id.btnEnableNotifications)

        tvSelectedCampus.text = "$selectedCampus Campus"

        Log.d(TAG, "Campus Alerts views initialized")
    }

    // =========================================================
    // CLICK LISTENERS
    // =========================================================

    private fun setupClickListeners() {

        findViewById<View>(R.id.tvBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.ivNotification).setOnClickListener {
            openNotificationSettings()
        }

        findViewById<View>(R.id.campusSelector).setOnClickListener {
            showCampusSelector()
        }

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

        Log.d(TAG, "Campus Alerts click listeners configured")
    }

    // =========================================================
    // CAMPUS PREFERENCES
    // =========================================================

    private fun campusPreferenceKey(): String {
        val uid = auth.currentUser?.uid

        return if (uid.isNullOrBlank()) {
            KEY_LAST_CAMPUS
        } else {
            "campus_$uid"
        }
    }

    private fun restoreSelectedCampus() {
        val savedCampus = campusPreferences.getString(
            campusPreferenceKey(),
            null
        )

        selectedCampus = if (savedCampus.isNullOrBlank()) {
            "Pretoria"
        } else {
            cleanCampusName(savedCampus)
        }

        tvSelectedCampus.text = "$selectedCampus Campus"

        Log.d(
            TAG,
            "Restored locally saved campus: $selectedCampus"
        )
    }

    private fun saveSelectedCampus(campus: String) {
        val cleanedCampus = cleanCampusName(campus)

        selectedCampus = cleanedCampus

        campusPreferences.edit()
            .putString(campusPreferenceKey(), cleanedCampus)
            .putString(KEY_LAST_CAMPUS, cleanedCampus)
            .apply()

        tvSelectedCampus.text = "$cleanedCampus Campus"

        Log.d(
            TAG,
            "Saved selected campus locally: $cleanedCampus"
        )
    }

    // =========================================================
    // LOAD STUDENT CAMPUS FROM FIRESTORE
    // =========================================================

    private fun loadStudentCampus() {
        Log.d(TAG, "Loading student campus")

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.w(
                TAG,
                "No authenticated user. Using locally saved campus."
            )

            loadAlertsFromApi()
            return
        }

        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val campus = document.getString("campus")

                if (!campus.isNullOrBlank()) {
                    val registeredCampus =
                        cleanCampusName(campus)

                    val previouslySaved = campusPreferences.getString(
                        campusPreferenceKey(),
                        null
                    )

                    if (previouslySaved.isNullOrBlank()) {
                        saveSelectedCampus(registeredCampus)
                    } else {
                        Log.d(
                            TAG,
                            "Using previously selected campus: $selectedCampus"
                        )
                    }
                } else {
                    Log.d(
                        TAG,
                        "No campus found in Firestore; using local selection."
                    )
                }

                // Register the final campus after the Firestore profile and
                // local preference have been reconciled.
                FCMTokenManager.registerDeviceWithAzure(
                    accountType = "Campus",
                    campus = selectedCampus
                )

                // The listener may have started before the profile
                // lookup completed. Restart it for the final campus.
                if (!isFinishing && !isDestroyed) {
                    startCampusAlertsListener()
                }

                loadAlertsFromApi()
            }
            .addOnFailureListener { exception ->

                Log.w(
                    TAG,
                    "Could not retrieve campus from Firestore. " +
                            "Using the locally saved campus.",
                    exception
                )

                FCMTokenManager.registerDeviceWithAzure(
                    accountType = "Campus",
                    campus = selectedCampus
                )

                if (!isFinishing && !isDestroyed) {
                    startCampusAlertsListener()
                }

                loadAlertsFromApi()
            }
    }

    // =========================================================
    // CLEAN CAMPUS NAME
    // =========================================================

    private fun cleanCampusName(campus: String): String {
        return campus
            .replace(" Campus", "", ignoreCase = true)
            .trim()
    }

    // =========================================================
    // CAMPUS SELECTOR
    // =========================================================

    private fun showCampusSelector() {
        val popup = PopupMenu(
            this,
            findViewById(R.id.campusSelector)
        )

        AVAILABLE_CAMPUSES.forEach { campus ->
            popup.menu.add("$campus Campus")
        }

        popup.setOnMenuItemClickListener { item ->

            val campus = cleanCampusName(
                item.title.toString()
            )

            saveSelectedCampus(campus)
            selectedFilter = "All"

            // Update Azure registration whenever the student changes campus.
            FCMTokenManager.registerDeviceWithAzure(
                accountType = "Campus",
                campus = selectedCampus
            )

            Toast.makeText(
                this,
                "$campus Campus selected.",
                Toast.LENGTH_SHORT
            ).show()

            // Change the real-time subscription to the new campus.
            startCampusAlertsListener()
            loadAlertsFromApi()

            true
        }

        popup.show()
    }

    // =========================================================
    // REAL-TIME FIRESTORE LISTENER
    // =========================================================

    private fun startCampusAlertsListener() {
        val user = auth.currentUser

        if (user == null) {
            Log.w(
                TAG,
                "Real-time listener skipped: no authenticated user."
            )
            stopCampusAlertsListener()
            return
        }

        val campusToWatch = selectedCampus

        // Keep the current listener if it already watches this campus.
        // This avoids resetting the initial-snapshot flag unnecessarily.
        if (
            campusAlertsListener != null &&
            listeningCampus == campusToWatch
        ) {
            Log.d(
                TAG,
                "Listener already active for $campusToWatch; keeping it."
            )
            return
        }

        stopCampusAlertsListener()
        listeningCampus = campusToWatch
        initialCampusSnapshotReceived = false

        Log.d(
            TAG,
            "Starting real-time listener for campus='$campusToWatch', " +
                    "userUid=${user.uid}"
        )

        campusAlertsListener = db.collection("CampusAlerts")
            .whereEqualTo("campus", campusToWatch)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(
                        TAG,
                        "Campus alert listener failed for '$campusToWatch'. " +
                                "Check Firestore rules and connection.",
                        error
                    )
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "Firestore returned a null campus-alert snapshot.")
                    return@addSnapshotListener
                }

                // Ignore a stale callback from a previous campus.
                if (
                    selectedCampus != campusToWatch ||
                    listeningCampus != campusToWatch
                ) {
                    Log.d(
                        TAG,
                        "Ignoring stale snapshot for $campusToWatch; " +
                                "selected campus is $selectedCampus."
                    )
                    return@addSnapshotListener
                }

                Log.d(
                    TAG,
                    "Firestore snapshot received: campus='$campusToWatch', " +
                            "documents=${snapshot.size()}, " +
                            "changes=${snapshot.documentChanges.size}, " +
                            "fromCache=${snapshot.metadata.isFromCache}"
                )

                // The first snapshot contains documents that already existed.
                // Do not notify for those; only handle changes after this point.
                if (!initialCampusSnapshotReceived) {
                    initialCampusSnapshotReceived = true

                    Log.d(
                        TAG,
                        "Initial Firestore snapshot processed for " +
                                "'$campusToWatch': ${snapshot.size()} existing document(s). " +
                                "Waiting for newly added alerts."
                    )
                    return@addSnapshotListener
                }

                for (change in snapshot.documentChanges) {
                    Log.d(
                        TAG,
                        "Firestore document change: type=${change.type}, " +
                                "id=${change.document.id}"
                    )

                    if (change.type != DocumentChange.Type.ADDED) {
                        continue
                    }

                    val document = change.document
                    val alertId = document.id

                    if (!notifiedAlertIds.add(alertId)) {
                        Log.d(
                            TAG,
                            "Skipping duplicate notification for alert $alertId"
                        )
                        continue
                    }

                    val title = document.getString("title")
                        ?: "New Campus Safety Alert"

                    val message = document.getString("message")
                        ?: "A new alert has been posted for $campusToWatch Campus."

                    val alertType = document.getString("alertType")
                        ?: "General"

                    val alertCampus = document.getString("campus")
                        ?: campusToWatch

                    val location = document.getString("location")
                        ?: alertCampus

                    // Accept either a String or Firestore Timestamp for createdAt.
                    val createdAtValue = document.get("createdAt")
                    val createdAt = when (createdAtValue) {
                        is String -> createdAtValue
                        is com.google.firebase.Timestamp -> {
                            SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss",
                                Locale.getDefault()
                            ).format(createdAtValue.toDate())
                        }
                        else -> ""
                    }

                    Log.d(
                        TAG,
                        "New Firestore campus alert detected: id=$alertId, " +
                                "title='$title', type='$alertType', campus='$alertCampus'"
                    )

                    runOnUiThread {
                        if (
                            !isFinishing &&
                            !isDestroyed &&
                            selectedCampus == campusToWatch
                        ) {
                            showLiveCampusNotification(
                                alertId = alertId,
                                title = title,
                                message = message
                            )

                            val liveAlert = SafetyAlert(
                                id = alertId.hashCode(),
                                title = title,
                                message = message,
                                campus = alertCampus,
                                location = location,
                                alertType = alertType,
                                createdAt = createdAt,
                                isActive = true
                            )

                            // Put the new Firestore alert first and avoid adding
                            // the same Firestore document more than once.
                            apiAlerts = listOf(liveAlert) +
                                    apiAlerts.filterNot {
                                        it.title == liveAlert.title &&
                                                it.message == liveAlert.message &&
                                                it.campus.equals(
                                                    liveAlert.campus,
                                                    ignoreCase = true
                                                )
                                    }

                            offlineNotice = null
                            showAlerts()

                            Log.d(
                                TAG,
                                "Firestore alert added to the Campus Alerts screen: $title"
                            )
                        }
                    }
                }
            }
    }

    private fun stopCampusAlertsListener() {
        campusAlertsListener?.remove()
        campusAlertsListener = null
        listeningCampus = null
        initialCampusSnapshotReceived = false

        Log.d(TAG, "Campus alert listener stopped")
    }

    // NOTIFICATION CHANNEL
    // =========================================================

    private fun createCampusNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Campus Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Real-time safety alerts for the selected campus"
            }

            val manager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
            Log.d(TAG, "Campus notification channel created/verified.")
        }
    }

    // =========================================================
    // SHOW LOCAL ANDROID NOTIFICATION
    // =========================================================

    private fun showLiveCampusNotification(
        alertId: String,
        title: String,
        message: String
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted.")
            return
        }

        Log.d(
            TAG,
            "Preparing Android notification: id=$alertId, title='$title'"
        )

        val notification = NotificationCompat.Builder(
            this,
            ALERT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                alertId.hashCode(),
                notification
            )

            Log.d(
                TAG,
                "Displayed local notification for alert $alertId"
            )
        } catch (exception: SecurityException) {
            Log.e(
                TAG,
                "Could not display campus notification",
                exception
            )
        }
    }

    // =========================================================
    // PRELOAD DEFAULT CAMPUS GUIDANCE
    // =========================================================

    private suspend fun seedDefaultCampusGuidance(
        campus: String
    ) {
        val existingAlerts = alertDao.getAlertsForCampusOnce(campus)

        if (existingAlerts.isNotEmpty()) {
            Log.d(
                TAG,
                "Existing local alerts found for $campus"
            )
            return
        }

        val campusKey = campus
            .lowercase(Locale.ROOT)
            .replace(" ", "-")

        val guidance = listOf(
            Triple(
                "Safety",
                "Stay aware of your surroundings",
                "When moving around campus, remain aware of your " +
                        "surroundings and use well-lit, populated routes " +
                        "where possible."
            ),
            Triple(
                "Safety",
                "Protect your personal belongings",
                "Keep your phone, laptop, student card and other " +
                        "belongings secure. Avoid leaving valuables " +
                        "unattended in public areas."
            ),
            Triple(
                "General",
                "Know where to get assistance",
                "Identify the campus security office and available " +
                        "emergency contacts. If you face immediate danger, " +
                        "contact campus security or the appropriate " +
                        "emergency services."
            ),
            Triple(
                "General",
                "Share your journey plans",
                "When travelling alone, consider telling someone " +
                        "you trust where you are going and when you " +
                        "expect to arrive."
            ),
            Triple(
                "Events",
                "Plan for campus events",
                "Before attending a campus event, check the venue, " +
                        "plan your journey home and familiarise yourself " +
                        "with the available exits."
            )
        )

        val now = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val entities = guidance.mapIndexed { index, item ->
            SafetyAlertEntity(
                alertId = "offline-campus-$campusKey-${index + 1}",
                location = campus,
                campus = campus,
                alertType = item.first,
                title = "Offline Guidance: ${item.second}",
                message = item.third,
                createdAt = now
            )
        }

        alertDao.insertAll(entities)

        Log.d(
            TAG,
            "Preloaded ${entities.size} guidance items for $campus"
        )
    }

    // =========================================================
    // LOAD ALERTS: OFFLINE-FIRST APPROACH
    // =========================================================

    private fun loadAlertsFromApi() {
        val requestedCampus = selectedCampus
        val currentRequest = ++requestNumber

        Log.d(
            TAG,
            "Loading alerts for campus: $requestedCampus"
        )

        alertsContainer.removeAllViews()
        offlineNotice = null
        showLoadingMessage()

        lifecycleScope.launch {

            // STEP 1: LOAD LOCAL INFORMATION

            try {
                withContext(Dispatchers.IO) {
                    seedDefaultCampusGuidance(requestedCampus)
                }

                val cachedAlerts = withContext(Dispatchers.IO) {
                    alertDao.getAlertsForCampusOnce(requestedCampus)
                }

                if (
                    currentRequest != requestNumber ||
                    requestedCampus != selectedCampus
                ) {
                    return@launch
                }

                apiAlerts = cachedAlerts.map { cached ->
                    SafetyAlert(
                        id = cached.alertId.toIntOrNull() ?: 0,
                        title = cached.title,
                        message = cached.message,
                        campus = cached.campus,
                        location = cached.location,
                        alertType = cached.alertType,
                        createdAt = cached.createdAt,
                        isActive = true
                    )
                }

                showAlerts()

                Log.d(
                    TAG,
                    "Displayed ${apiAlerts.size} locally stored items " +
                            "for $requestedCampus"
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Unable to load local campus guidance",
                    e
                )

                if (
                    currentRequest != requestNumber ||
                    requestedCampus != selectedCampus
                ) {
                    return@launch
                }

                apiAlerts = emptyList()
                offlineNotice = "Local guidance could not be loaded."
                showAlerts()
            }

            // STEP 2: FETCH LIVE INFORMATION FROM REST API

            try {
                Log.d(
                    TAG,
                    "Requesting live alerts for $requestedCampus"
                )

                val response = RetrofitClient.apiService
                    .getAlertsByCampus(requestedCampus)

                if (
                    currentRequest != requestNumber ||
                    requestedCampus != selectedCampus
                ) {
                    return@launch
                }

                if (response.isSuccessful) {
                    val receivedAlerts =
                        response.body() ?: emptyList()

                    val activeAlerts = receivedAlerts.filter {
                        it.isActive
                    }

                    val entities = activeAlerts.map { alert ->
                        SafetyAlertEntity(
                            alertId = alert.id.toString(),
                            location = alert.location,
                            campus = cleanCampusName(alert.campus),
                            alertType = alert.alertType,
                            title = alert.title,
                            message = alert.message,
                            createdAt = alert.createdAt,
                            cachedAt = System.currentTimeMillis()
                        )
                    }

                    withContext(Dispatchers.IO) {
                        if (entities.isNotEmpty()) {
                            alertDao.insertAll(entities)
                        }
                    }

                    if (
                        currentRequest != requestNumber ||
                        requestedCampus != selectedCampus
                    ) {
                        return@launch
                    }

                    if (activeAlerts.isNotEmpty()) {
                        apiAlerts = activeAlerts
                        offlineNotice = null

                        Log.d(
                            TAG,
                            "Received ${activeAlerts.size} active live alerts"
                        )
                    } else {
                        val savedAlerts = withContext(Dispatchers.IO) {
                            alertDao.getAlertsForCampusOnce(
                                requestedCampus
                            )
                        }

                        if (
                            currentRequest != requestNumber ||
                            requestedCampus != selectedCampus
                        ) {
                            return@launch
                        }

                        apiAlerts = savedAlerts.map { cached ->
                            SafetyAlert(
                                id = cached.alertId.toIntOrNull() ?: 0,
                                title = cached.title,
                                message = cached.message,
                                campus = cached.campus,
                                location = cached.location,
                                alertType = cached.alertType,
                                createdAt = cached.createdAt,
                                isActive = true
                            )
                        }

                        offlineNotice =
                            "No live alerts were returned. " +
                                    "Saved guidance may still be available."
                    }

                    showAlerts()

                    Log.d(
                        TAG,
                        "Campus alerts refreshed successfully"
                    )
                } else {
                    Log.e(
                        TAG,
                        "API returned HTTP ${response.code()}"
                    )

                    offlineNotice =
                        "Unable to refresh live alerts. " +
                                "Showing saved campus information where available."

                    showAlerts()
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "API unavailable. Retaining local campus information.",
                    e
                )

                if (
                    currentRequest != requestNumber ||
                    requestedCampus != selectedCampus
                ) {
                    return@launch
                }

                offlineNotice =
                    "Offline mode: showing saved campus information. " +
                            "Live emergency alerts may not be current."

                showAlerts()
            }
        }
    }

    // =========================================================
    // DISPLAY ALERTS
    // =========================================================

    private fun showAlerts() {
        Log.d(
            TAG,
            "Displaying campus alerts using filter: $selectedFilter"
        )

        alertsContainer.removeAllViews()

        val filteredAlerts = if (selectedFilter == "All") {
            apiAlerts
        } else {
            apiAlerts.filter { alert ->
                alert.alertType.equals(
                    selectedFilter,
                    ignoreCase = true
                )
            }
        }

        updateFilterButtons(selectedFilter)

        offlineNotice?.let { notice ->
            addOfflineNoticeView(notice)
        }

        if (filteredAlerts.isEmpty()) {
            val emptyMessage = TextView(this)

            emptyMessage.text = if (selectedFilter == "All") {
                "No campus safety information is currently " +
                        "available for $selectedCampus Campus."
            } else {
                "No $selectedFilter information is currently " +
                        "available for $selectedCampus Campus."
            }

            emptyMessage.textSize = 15f
            emptyMessage.setTextColor(
                Color.parseColor("#637A98")
            )
            emptyMessage.gravity = Gravity.CENTER
            emptyMessage.setPadding(16, 40, 16, 40)

            alertsContainer.addView(emptyMessage)
            return
        }

        filteredAlerts.forEach { alert ->
            addAlertCard(alert)
        }

        Log.d(
            TAG,
            "Displayed ${filteredAlerts.size} campus items"
        )
    }

    // =========================================================
    // OFFLINE NOTICE
    // =========================================================

    private fun addOfflineNoticeView(message: String) {
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

        alertsContainer.addView(notice, 0)
    }

    // =========================================================
    // ALERT CARD
    // =========================================================

    private fun addAlertCard(alert: SafetyAlert) {
        Log.d(TAG, "Displaying alert: ${alert.title}")

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(18, 16, 18, 16)
        card.background = getDrawable(R.drawable.bg_alert_card)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cardParams.setMargins(0, 0, 0, 14)
        card.layoutParams = cardParams

        val typeText = TextView(this)
        typeText.text = alert.alertType.uppercase()
        typeText.textSize = 12f
        typeText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )
        typeText.setTextColor(getAlertTypeColor(alert.alertType))
        card.addView(typeText)

        val titleText = TextView(this)
        titleText.text = alert.title
        titleText.textSize = 18f
        titleText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )
        titleText.setTextColor(Color.parseColor("#12345A"))

        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        titleParams.setMargins(0, 6, 0, 0)
        titleText.layoutParams = titleParams
        card.addView(titleText)

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

        val locationText = TextView(this)
        locationText.text = "Location: ${alert.location}"
        locationText.textSize = 13f
        locationText.setTextColor(Color.parseColor("#526B8D"))

        val locationParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        locationParams.setMargins(0, 8, 0, 0)
        locationText.layoutParams = locationParams
        card.addView(locationText)

        val campusText = TextView(this)
        campusText.text = "Campus: ${alert.campus}"
        campusText.textSize = 13f
        campusText.setTextColor(Color.parseColor("#1976D2"))
        campusText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val campusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        campusParams.setMargins(0, 5, 0, 0)
        campusText.layoutParams = campusParams
        card.addView(campusText)

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

    // =========================================================
    // FORMAT DATE AND TIME
    // =========================================================

    private fun formatDateTime(createdAt: String): String {
        if (createdAt.isBlank()) {
            return "Date and time unavailable"
        }

        return try {
            val parts = createdAt.split("T")

            if (parts.size >= 2) {
                val date = parts[0]
                val time = parts[1].take(5)
                "Date: $date  •  Time: $time"
            } else {
                createdAt
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date/time", e)
            createdAt
        }
    }

    // =========================================================
    // ALERT TYPE COLOUR
    // =========================================================

    private fun getAlertTypeColor(type: String): Int {
        return when {
            type.equals("Emergency", ignoreCase = true) ->
                Color.parseColor("#C62828")

            type.equals("Safety", ignoreCase = true) ->
                Color.parseColor("#1976D2")

            type.equals("Events", ignoreCase = true) ->
                Color.parseColor("#7B1FA2")

            else ->
                Color.parseColor("#526B8D")
        }
    }

    // =========================================================
    // FILTER BUTTONS
    // =========================================================

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
        button.setBackgroundResource(
            R.drawable.bg_alert_filter_selected
        )
        button.setTextColor(Color.WHITE)
    }

    // =========================================================
    // LOADING MESSAGE
    // =========================================================

    private fun showLoadingMessage() {
        val text = TextView(this)

        text.text = "Loading saved campus information..."
        text.textSize = 15f
        text.gravity = Gravity.CENTER
        text.setTextColor(Color.parseColor("#637A98"))
        text.setPadding(16, 40, 16, 40)

        alertsContainer.addView(text)
    }

    // =========================================================
    // NOTIFICATION SETTINGS
    // =========================================================

    private fun openNotificationSettings() {
        try {
            val intent = Intent()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.action =
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS

                intent.putExtra(
                    Settings.EXTRA_APP_PACKAGE,
                    packageName
                )
            } else {
                intent.action =
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS

                intent.data = Uri.parse("package:$packageName")
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

        findViewById<View>(R.id.navHome).setOnClickListener {
            val intent = Intent(
                this,
                CampusHomeActivity::class.java
            )

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navMap).setOnClickListener {
            try {
                startActivity(
                    Intent(
                        this,
                        CampusSafetyMapActivity::class.java
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open campus map", e)

                Toast.makeText(
                    this,
                    "Campus map is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<View>(R.id.navReports).setOnClickListener {
            try {
                startActivity(
                    Intent(
                        this,
                        CampusReportsActivity::class.java
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open campus reports", e)

                Toast.makeText(
                    this,
                    "Campus reports are not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<View>(R.id.navNotifications).setOnClickListener {
            selectedFilter = "All"
            showAlerts()
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            try {
                startActivity(
                    Intent(
                        this,
                        CampusProfileActivity::class.java
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open campus profile", e)

                Toast.makeText(
                    this,
                    "Campus profile is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
