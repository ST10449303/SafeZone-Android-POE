package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampusProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvProfileName: TextView
    private lateinit var tvStudentDetails: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvInstitution: TextView
    private lateinit var tvCampus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupButtons()
        setDefaultProfileValues()
    }

    private fun initializeViews() {
        tvProfileName = findViewById(R.id.tvProfileName)
        tvStudentDetails = findViewById(R.id.tvStudentDetails)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvInstitution = findViewById(R.id.tvInstitution)
        tvCampus = findViewById(R.id.tvCampus)
    }

    private fun setDefaultProfileValues() {
        tvProfileName.text = getString(R.string.campus_profile_default_name)

        tvStudentDetails.text = getString(
            R.string.campus_profile_student_details_default
        )

        tvEmail.text = getString(R.string.campus_profile_no_email)
        tvPhone.text = getString(R.string.campus_profile_phone_missing)
        tvInstitution.text = getString(R.string.campus_profile_institution_missing)
        tvCampus.text = getString(R.string.campus_profile_campus_missing)
    }

    override fun onResume() {
        super.onResume()

        if (::auth.isInitialized) {
            loadCampusProfile()
        }
    }

    // =========================================================
    // LOAD CAMPUS PROFILE FROM FIRESTORE
    // =========================================================

    private fun loadCampusProfile() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            val intent = Intent(
                this,
                ChoosePanelActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
            return
        }

        val userId = currentUser.uid

        // Show Firebase Authentication email while profile loads.
        tvEmail.text = currentUser.email
            ?: getString(R.string.campus_profile_no_email)

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (!document.exists()) {
                    setDefaultProfileValues()

                    // Preserve the authenticated user's email if available.
                    tvEmail.text = currentUser.email
                        ?: getString(R.string.campus_profile_no_email)

                    Toast.makeText(
                        this,
                        R.string.campus_profile_not_found,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val fullName = document.getString("fullName")
                val email = document.getString("email")
                val phone = document.getString("phone")
                val institution = document.getString("institution")
                val campus = document.getString("campus")

                tvProfileName.text = fullName
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.campus_profile_default_name)

                tvEmail.text = email
                    ?.takeIf { it.isNotBlank() }
                    ?: currentUser.email
                            ?: getString(R.string.campus_profile_no_email)

                tvPhone.text = phone
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.campus_profile_phone_missing)

                tvInstitution.text = institution
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.campus_profile_institution_missing)

                tvCampus.text = campus
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.campus_profile_campus_missing)

                val institutionText = institution
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.campus_profile_institution_label)

                val campusText = campus
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.campus_profile_campus_label)

                tvStudentDetails.text = getString(
                    R.string.campus_profile_student_details,
                    institutionText,
                    campusText
                )
            }
            .addOnFailureListener { exception ->

                if (isFinishing || isDestroyed) return@addOnFailureListener

                Toast.makeText(
                    this,
                    R.string.campus_profile_load_failed,
                    Toast.LENGTH_SHORT
                ).show()

                android.util.Log.e(
                    "CampusProfile",
                    "Failed to load campus profile",
                    exception
                )
            }
    }

    // =========================================================
    // BUTTONS AND NAVIGATION
    // =========================================================

    private fun setupButtons() {

        findViewById<View>(R.id.tvBack).setOnClickListener {
            finish()
        }

        // Profile photo: selection/upload functionality remains a placeholder.
        findViewById<View>(R.id.btnChangePhoto).setOnClickListener {
            Toast.makeText(
                this,
                R.string.campus_profile_photo_coming_soon,
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            openActivity(
                EditProfileActivity::class.java,
                R.string.campus_profile_edit_unavailable
            )
        }

        findViewById<View>(R.id.btnTrustedContacts).setOnClickListener {
            openActivity(
                TrustedContactsActivity::class.java,
                R.string.campus_profile_trusted_contacts_unavailable
            )
        }

        findViewById<View>(R.id.btnMyReports).setOnClickListener {
            openReports()
        }

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            openActivity(
                CampusSettingsActivity::class.java,
                R.string.campus_profile_settings_unavailable
            )
        }

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            logout()
        }

        // Bottom navigation
        findViewById<View>(R.id.navHome).setOnClickListener {
            navigateTo(CampusHomeActivity::class.java)
        }

        findViewById<View>(R.id.navMap).setOnClickListener {
            openCampusMap()
        }

        findViewById<View>(R.id.navReports).setOnClickListener {
            openReports()
        }

        findViewById<View>(R.id.navNotifications).setOnClickListener {
            openCampusAlerts()
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            Toast.makeText(
                this,
                R.string.campus_profile_current_profile,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openActivity(
        activityClass: Class<*>,
        errorMessageResId: Int
    ) {
        try {
            startActivity(Intent(this, activityClass))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                errorMessageResId,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        startActivity(intent)
        finish()
    }

    // =========================================================
    // CAMPUS MAP
    // =========================================================

    private fun openCampusMap() {
        try {
            startActivity(
                Intent(this, CampusSafetyMapActivity::class.java)
            )
        } catch (e: Exception) {
            try {
                val mapIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=Pretoria Campus")
                )

                startActivity(mapIntent)
            } catch (mapException: Exception) {
                Toast.makeText(
                    this,
                    R.string.campus_profile_map_unavailable,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // REPORTS
    // =========================================================

    private fun openReports() {
        try {
            val intent = Intent(
                this,
                PersonalReportsActivity::class.java
            ).apply {
                putExtra("campusReports", true)
            }

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                R.string.campus_profile_reports_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // CAMPUS ALERTS
    // =========================================================

    private fun openCampusAlerts() {
        openActivity(
            CampusAlertsActivity::class.java,
            R.string.campus_profile_alerts_unavailable
        )
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private fun logout() {
        auth.signOut()

        Toast.makeText(
            this,
            R.string.campus_profile_logout_success,
            Toast.LENGTH_SHORT
        ).show()

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
}