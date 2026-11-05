package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalHomeActivity : AppCompatActivity() {

    // HEADER
    private lateinit var tvNotification: TextView
    private lateinit var tvProfile: TextView
    private lateinit var tvLogout: TextView
    private lateinit var tvUserName: TextView

    // QUICK ACTIONS
    private lateinit var btnSOS: Button
    private lateinit var btnEmergencyAssistance: Button
    private lateinit var btnSafetyCheckIn: Button
    private lateinit var btnTrustedContacts: Button
    private lateinit var btnFindHelp: Button
    private lateinit var btnReportIncident: Button
    private lateinit var btnSettings: Button

    // BOTTOM NAVIGATION
    private lateinit var navHome: TextView
    private lateinit var navMap: TextView
    private lateinit var navReports: TextView
    private lateinit var navNotifications: TextView
    private lateinit var navProfile: TextView

    // FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // OFFLINE SESSION
    private var offlineMode = false
    private var savedFullName = "SafeZone User"
    private var savedEmail = ""

    companion object {
        private const val TAG = "PersonalHomeActivity"
        const val EXTRA_OFFLINE_MODE = "offlineMode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        offlineMode = intent.getBooleanExtra(EXTRA_OFFLINE_MODE, false)

        savedFullName = intent.getStringExtra("fullName")
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.default_user_name)

        savedEmail = intent.getStringExtra("email").orEmpty()

        initializeViews()
        setupButtons()

        if (!checkLogin()) {
            return
        }

        displaySavedUserName()

        if (!offlineMode) {
            auth.currentUser?.let { user ->
                loadUserName(user.uid)
            }
        } else {
            Log.d(TAG, "Personal home opened in offline mode")
        }
    }

    // INITIALIZE VIEWS
    private fun initializeViews() {
        tvNotification = findViewById(R.id.tvNotification)
        tvProfile = findViewById(R.id.tvProfile)
        tvLogout = findViewById(R.id.tvLogout)
        tvUserName = findViewById(R.id.tvUserName)

        btnSOS = findViewById(R.id.btnSOS)
        btnEmergencyAssistance =
            findViewById(R.id.btnEmergencyAssistance)
        btnSafetyCheckIn = findViewById(R.id.btnSafetyCheckIn)
        btnTrustedContacts = findViewById(R.id.btnTrustedContacts)
        btnFindHelp = findViewById(R.id.btnFindHelp)
        btnReportIncident = findViewById(R.id.btnReportIncident)
        btnSettings = findViewById(R.id.btnSettings)

        navHome = findViewById(R.id.navHome)
        navMap = findViewById(R.id.navMap)
        navReports = findViewById(R.id.navReports)
        navNotifications = findViewById(R.id.navNotifications)
        navProfile = findViewById(R.id.navProfile)
    }

    // LOGIN CHECK
    private fun checkLogin(): Boolean {
        if (offlineMode) {
            Log.d(TAG, "Offline Personal session accepted")
            return true
        }

        if (auth.currentUser != null) {
            return true
        }

        showMessage(R.string.login_required)
        goToChoosePanel()
        return false
    }

    // DISPLAY SAVED USER
    private fun displaySavedUserName() {
        tvUserName.text = savedFullName
    }

    // LOAD USER NAME FROM FIRESTORE
    private fun loadUserName(userId: String) {
        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                if (document.exists()) {
                    val fullName = document.getString("fullName")

                    if (!fullName.isNullOrBlank()) {
                        savedFullName = fullName
                        tvUserName.text = fullName
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(
                    TAG,
                    "Unable to refresh profile; using saved name",
                    exception
                )

                if (!isFinishing && !isDestroyed) {
                    displaySavedUserName()
                }
            }
    }

    // SETUP BUTTONS
    private fun setupButtons() {

        // SOS
        btnSOS.setOnClickListener {
            openScreen(SOSConfirmationActivity::class.java)
        }

        // EMERGENCY ASSISTANCE
        btnEmergencyAssistance.setOnClickListener {
            openScreen(EmergencyAssistanceActivity::class.java)
        }

        // SAFETY CHECK-IN
        btnSafetyCheckIn.setOnClickListener {
            openScreen(SafetyCheckInActivity::class.java)
        }

        // TRUSTED CONTACTS
        btnTrustedContacts.setOnClickListener {
            openScreen(TrustedContactsActivity::class.java)
        }

        // FIND HELP
        btnFindHelp.setOnClickListener {
            openScreen(FindHelpActivity::class.java)
        }

        // REPORT INCIDENT
        btnReportIncident.setOnClickListener {
            openScreen(PersonalIncidentReportActivity::class.java)
        }

        // SETTINGS
        btnSettings.setOnClickListener {
            openScreen(PersonalSettingsActivity::class.java)
        }

        // NOTIFICATIONS
        tvNotification.setOnClickListener {
            openScreen(PersonalAlertsActivity::class.java)
        }

        // PROFILE
        tvProfile.setOnClickListener {
            openScreen(PersonalProfileActivity::class.java)
        }

        // LOGOUT
        tvLogout.setOnClickListener {
            logoutUser()
        }

        // BOTTOM NAVIGATION
        navHome.setOnClickListener {
            showMessage(R.string.already_on_home)
        }

        navMap.setOnClickListener {
            openScreen(FindHelpActivity::class.java)
        }

        navReports.setOnClickListener {
            openScreen(PersonalReportsActivity::class.java)
        }

        navNotifications.setOnClickListener {
            openScreen(PersonalAlertsActivity::class.java)
        }

        navProfile.setOnClickListener {
            openScreen(PersonalProfileActivity::class.java)
        }
    }

    // OPEN A SCREEN
    private fun openScreen(activityClass: Class<*>) {
        try {
            val intent = Intent(this, activityClass).apply {
                putExtra("fullName", savedFullName)
                putExtra("email", savedEmail)
                putExtra(EXTRA_OFFLINE_MODE, offlineMode)
            }

            startActivity(intent)
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Unable to open ${activityClass.simpleName}",
                exception
            )

            showMessage(R.string.feature_unavailable)
        }
    }

    // LOGOUT
    private fun logoutUser() {
        auth.signOut()

        showMessage(R.string.logged_out_successfully)

        // Keep saved offline credentials available for the next login.
        goToChoosePanel()
    }

    // CHOOSE PANEL
    private fun goToChoosePanel() {
        val intent = Intent(
            this,
            ChoosePanelActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun showMessage(messageResId: Int) {
        Toast.makeText(
            this,
            getString(messageResId),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::auth.isInitialized &&
            !offlineMode &&
            auth.currentUser == null
        ) {
            goToChoosePanel()
        }
    }
}
