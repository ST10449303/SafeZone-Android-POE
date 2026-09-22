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

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupButtons()
        loadCampusProfile()
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        tvProfileName =
            findViewById(R.id.tvProfileName)

        tvStudentDetails =
            findViewById(R.id.tvStudentDetails)

        tvEmail =
            findViewById(R.id.tvEmail)

        tvPhone =
            findViewById(R.id.tvPhone)

        tvInstitution =
            findViewById(R.id.tvInstitution)

        tvCampus =
            findViewById(R.id.tvCampus)
    }


    // =========================================================
    // LOAD CAMPUS PROFILE
    // =========================================================

    private fun loadCampusProfile() {

        val currentUser = auth.currentUser

        // -----------------------------------------------------
        // USER NOT LOGGED IN
        // -----------------------------------------------------

        if (currentUser == null) {

            val intent = Intent(
                this,
                ChoosePanelActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()

            return
        }


        val userId = currentUser.uid


        // -----------------------------------------------------
        // DEFAULT VALUES
        // -----------------------------------------------------

        tvProfileName.text =
            "Student"

        tvStudentDetails.text =
            "Student • Campus"

        tvEmail.text =
            currentUser.email ?: "No email"

        tvPhone.text =
            "Phone number not provided"

        tvInstitution.text =
            "Institution not provided"

        tvCampus.text =
            "Campus not provided"


        // -----------------------------------------------------
        // GET USER FROM FIRESTORE
        // -----------------------------------------------------

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Campus profile information was not found.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }


                // =================================================
                // GET SAVED REGISTRATION INFORMATION
                // =================================================

                val fullName =
                    document.getString("fullName")

                val email =
                    document.getString("email")

                val phone =
                    document.getString("phone")

                val institution =
                    document.getString("institution")

                val campus =
                    document.getString("campus")


                // =================================================
                // STUDENT NAME
                // =================================================

                if (!fullName.isNullOrBlank()) {

                    tvProfileName.text =
                        fullName
                }


                // =================================================
                // EMAIL
                // =================================================

                if (!email.isNullOrBlank()) {

                    tvEmail.text =
                        email

                } else {

                    tvEmail.text =
                        currentUser.email ?: "No email"
                }


                // =================================================
                // PHONE
                // =================================================

                if (!phone.isNullOrBlank()) {

                    tvPhone.text =
                        phone

                } else {

                    tvPhone.text =
                        "Phone number not provided"
                }


                // =================================================
                // INSTITUTION
                // =================================================

                if (!institution.isNullOrBlank()) {

                    tvInstitution.text =
                        institution

                } else {

                    tvInstitution.text =
                        "Institution not provided"
                }


                // =================================================
                // CAMPUS
                // =================================================

                if (!campus.isNullOrBlank()) {

                    tvCampus.text =
                        campus

                } else {

                    tvCampus.text =
                        "Campus not provided"
                }


                // =================================================
                // STUDENT DETAILS
                // =================================================

                val institutionText =
                    if (!institution.isNullOrBlank()) {
                        institution
                    } else {
                        "Institution"
                    }

                val campusText =
                    if (!campus.isNullOrBlank()) {
                        campus
                    } else {
                        "Campus"
                    }

                tvStudentDetails.text =
                    "Student • $institutionText — $campusText"
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to load campus profile.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // =========================================================
    // BUTTONS
    // =========================================================

    private fun setupButtons() {

        // =====================================================
        // BACK
        // =====================================================

        findViewById<View>(R.id.tvBack)
            .setOnClickListener {

                finish()
            }


        // =====================================================
        // CHANGE PROFILE PHOTO
        // =====================================================

        findViewById<View>(R.id.btnChangePhoto)
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Profile photo selection will be added here.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        // =====================================================
        // EDIT PROFILE
        // =====================================================

        findViewById<View>(R.id.btnEditProfile)
            .setOnClickListener {

                try {

                    startActivity(
                        Intent(
                            this,
                            EditProfileActivity::class.java
                        )
                    )

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Edit Profile is not available.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        // =====================================================
        // TRUSTED CONTACTS
        // =====================================================

        findViewById<View>(R.id.btnTrustedContacts)
            .setOnClickListener {

                try {

                    startActivity(
                        Intent(
                            this,
                            TrustedContactsActivity::class.java
                        )
                    )

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Trusted Contacts are not available.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        // =====================================================
        // MY REPORTS
        // =====================================================

        findViewById<View>(R.id.btnMyReports)
            .setOnClickListener {

                openReports()
            }


        // =====================================================
        // SETTINGS
        // =====================================================

        findViewById<View>(R.id.btnSettings)
            .setOnClickListener {

                try {

                    startActivity(
                        Intent(
                            this,
                            CampusSettingsActivity::class.java
                        )
                    )

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Settings are not available.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        // =====================================================
        // LOGOUT
        // =====================================================

        findViewById<View>(R.id.btnLogout)
            .setOnClickListener {

                logout()
            }


        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        findViewById<View>(R.id.navHome)
            .setOnClickListener {

                val intent = Intent(
                    this,
                    CampusHomeActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

                startActivity(intent)

                finish()
            }


        // -----------------------------------------------------
        // MAP
        // -----------------------------------------------------

        findViewById<View>(R.id.navMap)
            .setOnClickListener {

                openCampusMap()
            }


        // -----------------------------------------------------
        // REPORTS
        // -----------------------------------------------------

        findViewById<View>(R.id.navReports)
            .setOnClickListener {

                openReports()
            }


        // -----------------------------------------------------
        // ALERTS
        // -----------------------------------------------------

        findViewById<View>(R.id.navNotifications)
            .setOnClickListener {

                openCampusAlerts()
            }


        // -----------------------------------------------------
        // PROFILE
        // -----------------------------------------------------

        findViewById<View>(R.id.navProfile)
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "You are viewing your profile.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // =========================================================
    // OPEN CAMPUS MAP
    // =========================================================

    private fun openCampusMap() {

        try {

            // Open the actual SafeZone campus map screen
            val intent = Intent(
                this,
                CampusSafetyMapActivity::class.java
            )

            startActivity(intent)

        } catch (e: Exception) {

            // Fallback to Google Maps
            try {

                val mapIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "geo:0,0?q=Pretoria Campus"
                    )
                )

                startActivity(mapIntent)

            } catch (mapException: Exception) {

                Toast.makeText(
                    this,
                    "Campus Safety Map is not available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // OPEN REPORTS
    // =========================================================

    private fun openReports() {

        try {

            val intent = Intent(
                this,
                PersonalReportsActivity::class.java
            )

            // Tell the Reports screen that the user
            // is coming from the Campus section.
            intent.putExtra(
                "campusReports",
                true
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Reports are not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // OPEN CAMPUS ALERTS
    // =========================================================

    private fun openCampusAlerts() {

        try {

            val intent = Intent(
                this,
                CampusAlertsActivity::class.java
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Campus Alerts are not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    private fun logout() {

        // Sign out Firebase user
        auth.signOut()

        Toast.makeText(
            this,
            "You have been logged out.",
            Toast.LENGTH_SHORT
        ).show()


        // -----------------------------------------------------
        // GO TO CHOOSE PANEL
        // -----------------------------------------------------

        val intent = Intent(
            this,
            ChoosePanelActivity::class.java
        )

        // Clear the entire previous navigation stack.
        // This prevents the user from pressing Back
        // and returning to the campus profile.
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }


    // =========================================================
    // REFRESH PROFILE
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (::auth.isInitialized) {

            loadCampusProfile()
        }
    }
}