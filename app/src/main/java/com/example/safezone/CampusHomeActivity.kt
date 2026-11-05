
package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context

class CampusHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvWelcome: TextView
    private lateinit var tvCampusName: TextView
    private lateinit var tvCampusStatus: TextView

    private lateinit var tvNotification: ImageView
    private lateinit var tvProfile: ImageView

    private lateinit var btnReportCampusIncident: AppCompatButton
    private lateinit var btnCampusEmergency: AppCompatButton
    private lateinit var btnCampusSafetyMap: AppCompatButton
    private lateinit var btnCampusAlerts: AppCompatButton
    private lateinit var btnMyCampusReports: AppCompatButton
    private lateinit var btnLogout: AppCompatButton

    private var offlineMode = false
    private var savedFullName = ""
    private var savedEmail = ""

    companion object {
        private const val TAG = "CampusHomeActivity"
        const val EXTRA_OFFLINE_MODE = "offlineMode"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            SafeZoneLanguageManager.wrapContext(newBase)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        offlineMode = intent.getBooleanExtra(EXTRA_OFFLINE_MODE, false)

        savedFullName = intent.getStringExtra("fullName")
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.campus_home_default_name)

        savedEmail = intent.getStringExtra("email").orEmpty()

        initializeViews()
        setupButtons()
        setupBackNavigation()

        if (checkLogin()) {
            loadCampusUser()
        }
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvCampusName = findViewById(R.id.tvCampusName)
        tvCampusStatus = findViewById(R.id.tvCampusStatus)

        tvNotification = findViewById(R.id.tvNotification)
        tvProfile = findViewById(R.id.tvProfile)

        btnReportCampusIncident =
            findViewById(R.id.btnReportCampusIncident)

        btnCampusEmergency =
            findViewById(R.id.btnCampusEmergency)

        btnCampusSafetyMap =
            findViewById(R.id.btnCampusSafetyMap)

        btnCampusAlerts =
            findViewById(R.id.btnCampusAlerts)

        btnMyCampusReports =
            findViewById(R.id.btnMyCampusReports)

        btnLogout = findViewById(R.id.btnLogout)
    }

    // Allow locally verified offline sessions without Firebase.
    private fun checkLogin(): Boolean {
        if (offlineMode) {
            Log.d(TAG, "Offline Campus session accepted")
            return true
        }

        if (auth.currentUser != null) {
            return true
        }

        showMessage(getString(R.string.campus_home_login_required))

        val loginIntent = Intent(
            this,
            CampusLoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(loginIntent)
        finish()
        return false
    }

    private fun loadCampusUser() {
        tvWelcome.text = getString(
            R.string.campus_home_welcome,
            getFirstName(savedFullName)
        )

        tvCampusName.text = getString(R.string.campus_home_default_campus)
        updateCampusStatus()

        // Firestore cannot be relied upon while offline.
        if (offlineMode) {
            Log.d(TAG, "Using saved Campus profile while offline")
            return
        }

        val currentUser = auth.currentUser ?: return

        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (!document.exists()) {
                    Log.w(TAG, "Campus profile document not found")
                    return@addOnSuccessListener
                }

                val fullName = document.getString("fullName")
                    ?.takeIf { it.isNotBlank() }
                    ?: savedFullName

                savedFullName = fullName

                tvWelcome.text = getString(
                    R.string.campus_home_welcome,
                    getFirstName(fullName)
                )

                val campus = document.getString("campus")
                if (!campus.isNullOrBlank()) {
                    tvCampusName.text = formatCampusName(campus)
                }

                updateCampusStatus()
            }
            .addOnFailureListener { exception ->
                Log.w(
                    TAG,
                    "Unable to load profile; displaying saved information",
                    exception
                )

                if (!isFinishing && !isDestroyed) {
                    tvWelcome.text = getString(
                        R.string.campus_home_welcome,
                        getFirstName(savedFullName)
                    )

                    tvCampusStatus.text = getString(
                        R.string.campus_home_status_profile_outdated
                    )
                }
            }
    }

    private fun updateCampusStatus() {
        tvCampusStatus.text = getString(
            if (offlineMode) {
                R.string.campus_home_status_offline
            } else {
                R.string.campus_home_status_available
            }
        )
    }

    private fun formatCampusName(campus: String): String {
        val cleaned = campus.trim()

        if (cleaned.isEmpty()) {
            return getString(R.string.campus_home_default_campus)
        }

        return if (cleaned.endsWith("Campus", ignoreCase = true)) {
            cleaned
        } else {
            getString(R.string.campus_home_campus_name_format, cleaned)
        }
    }

    private fun getFirstName(fullName: String): String {
        return fullName.trim()
            .split("\\s+".toRegex())
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.campus_home_default_learner)
    }

    private fun setupButtons() {
        tvNotification.setOnClickListener { openCampusAlerts() }
        tvProfile.setOnClickListener { openProfile() }

        btnReportCampusIncident.setOnClickListener {
            openCampusIncidentReport()
        }

        btnCampusEmergency.setOnClickListener {
            openCampusEmergency()
        }

        btnCampusSafetyMap.setOnClickListener {
            openCampusSafetyMap()
        }

        btnCampusAlerts.setOnClickListener {
            openCampusAlerts()
        }

        btnMyCampusReports.setOnClickListener {
            openCampusReports(false)
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }

        findViewById<View>(R.id.navHome).setOnClickListener {}

        findViewById<View>(R.id.navMap).setOnClickListener {
            openCampusSafetyMap()
        }

        findViewById<View>(R.id.navReports).setOnClickListener {
            openCampusReports(false)
        }

        findViewById<View>(R.id.navNotifications).setOnClickListener {
            openCampusAlerts()
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            openProfile()
        }

        setupOptionalManageReportsButton()
    }

    private fun setupOptionalManageReportsButton() {
        val possibleIds = listOf(
            "btnManageReports",
            "btnManageCampusReports"
        )

        for (idName in possibleIds) {
            val id = resources.getIdentifier(
                idName,
                "id",
                packageName
            )

            if (id != 0) {
                findViewById<View>(id)?.setOnClickListener {
                    openCampusReports(true)
                }
            }
        }
    }

    private fun logoutUser() {
        auth.signOut()

        // Saved offline credentials are not deleted by logout.
        showMessage(getString(R.string.campus_home_logout_success))

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

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }

    private fun openCampusIncidentReport() {
        try {
            startActivity(
                Intent(this, CampusIncidentReportActivity::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open incident reporting", e)
            showUnavailable(getString(R.string.campus_home_feature_incident))
        }
    }

    private fun openCampusEmergency() {
        try {
            startActivity(
                Intent(this, CampusEmergencyActivity::class.java)
            )
        } catch (e: Exception) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:112")
                    )
                )
            } catch (dialException: Exception) {
                Log.e(TAG, "Cannot open emergency dialler", dialException)
                showUnavailable(getString(R.string.campus_home_feature_emergency))
            }
        }
    }

    private fun openCampusSafetyMap() {
        try {
            startActivity(
                Intent(this, CampusSafetyMapActivity::class.java)
            )
        } catch (e: Exception) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=Pretoria Campus")
                    )
                )
            } catch (mapException: Exception) {
                Log.e(TAG, "Cannot open safety map", mapException)
                showUnavailable(getString(R.string.campus_home_feature_map))
            }
        }
    }

    private fun openCampusAlerts() {
        try {
            startActivity(
                Intent(this, CampusAlertsActivity::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open Campus Alerts", e)
            showUnavailable(getString(R.string.campus_home_feature_alerts))
        }
    }

    private fun openCampusReports(manageReports: Boolean) {
        try {
            val intent = Intent(
                this,
                CampusReportsActivity::class.java
            ).apply {
                putExtra("manageReports", manageReports)
            }

            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open Campus Reports", e)
            showUnavailable(getString(R.string.campus_home_feature_reports))
        }
    }

    private fun openProfile() {
        try {
            startActivity(
                Intent(this, CampusProfileActivity::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open Campus Profile", e)
            showUnavailable(getString(R.string.campus_home_feature_profile))
        }
    }

    private fun showUnavailable(feature: String) {
        showMessage(
            getString(R.string.campus_home_feature_unavailable, feature)
        )
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()

        if (::auth.isInitialized && !offlineMode &&
            auth.currentUser == null
        ) {
            checkLogin()
        }
    }
}
