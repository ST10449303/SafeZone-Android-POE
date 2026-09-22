package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalProfileActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView

    private lateinit var tvProfileName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAccountType: TextView

    private lateinit var btnEditProfile: Button
    private lateinit var btnTrustedContacts: Button
    private lateinit var btnMyReports: Button
    private lateinit var btnSettings: Button
    private lateinit var btnLogout: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_personal_profile
        )

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()

        // ==========================================
        // VIEWS
        // ==========================================

        tvBack =
            findViewById(R.id.tvBack)

        tvProfileName =
            findViewById(R.id.tvProfileName)

        tvEmail =
            findViewById(R.id.tvEmail)

        tvPhone =
            findViewById(R.id.tvPhone)

        tvAccountType =
            findViewById(R.id.tvAccountType)

        btnEditProfile =
            findViewById(R.id.btnEditProfile)

        btnTrustedContacts =
            findViewById(R.id.btnTrustedContacts)

        btnMyReports =
            findViewById(R.id.btnMyReports)

        btnSettings =
            findViewById(R.id.btnSettings)

        btnLogout =
            findViewById(R.id.btnLogout)

        // ==========================================
        // LOGIN CHECK
        // ==========================================

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            goToLogin()

            return
        }

        // ==========================================
        // BACK
        // ==========================================

        tvBack.setOnClickListener {

            finish()
        }

        // ==========================================
        // EDIT PROFILE
        // ==========================================

        btnEditProfile.setOnClickListener {

            openActivity(
                EditProfileActivity::class.java
            )
        }

        // ==========================================
        // TRUSTED CONTACTS
        // ==========================================

        btnTrustedContacts.setOnClickListener {

            openActivity(
                TrustedContactsActivity::class.java
            )
        }

        // ==========================================
        // MY REPORTS
        // ==========================================

        btnMyReports.setOnClickListener {

            openActivity(
                PersonalReportsActivity::class.java
            )
        }

        // ==========================================
        // SETTINGS
        // ==========================================

        btnSettings.setOnClickListener {

            openActivity(
                PersonalSettingsActivity::class.java
            )
        }

        // ==========================================
        // LOGOUT
        // ==========================================

        btnLogout.setOnClickListener {

            logoutUser()
        }
    }

    // ==========================================
    // REFRESH PROFILE
    // ==========================================

    override fun onResume() {

        super.onResume()

        val currentUser =
            auth.currentUser

        if (currentUser != null) {

            loadUserProfile(
                currentUser.uid
            )
        }
    }

    // ==========================================
    // LOAD PROFILE
    // ==========================================

    private fun loadUserProfile(
        userId: String
    ) {

        tvProfileName.text =
            "SafeZone User"

        tvEmail.text =
            auth.currentUser?.email
                ?: "Not available"

        tvPhone.text =
            "Not available"

        tvAccountType.text =
            "Personal"

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    return@addOnSuccessListener
                }

                document.getString(
                    "fullName"
                )?.let {

                    if (it.isNotEmpty()) {
                        tvProfileName.text = it
                    }
                }

                document.getString(
                    "email"
                )?.let {

                    if (it.isNotEmpty()) {
                        tvEmail.text = it
                    }
                }

                document.getString(
                    "phone"
                )?.let {

                    if (it.isNotEmpty()) {
                        tvPhone.text = it
                    }
                }

                document.getString(
                    "accountType"
                )?.let {

                    if (it.isNotEmpty()) {
                        tvAccountType.text = it
                    }
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to load some profile information.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ==========================================
    // OPEN ACTIVITY
    // ==========================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        try {

            startActivity(
                Intent(
                    this,
                    activityClass
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open this screen.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // LOGOUT
    // ==========================================

    private fun logoutUser() {

        auth.signOut()

        Toast.makeText(
            this,
            "You have been logged out.",
            Toast.LENGTH_SHORT
        ).show()

        goToLogin()
    }

    // ==========================================
    // LOGIN
    // ==========================================

    private fun goToLogin() {

        val intent =
            Intent(
                this,
                PersonalLoginActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}