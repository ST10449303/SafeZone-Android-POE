
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_personal_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bind existing views
        tvBack = findViewById(R.id.tvBack)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvAccountType = findViewById(R.id.tvAccountType)

        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnTrustedContacts = findViewById(R.id.btnTrustedContacts)
        btnMyReports = findViewById(R.id.btnMyReports)
        btnSettings = findViewById(R.id.btnSettings)
        btnLogout = findViewById(R.id.btnLogout)

        // Check whether a user is signed in
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        // Back navigation
        tvBack.setOnClickListener {
            finish()
        }

        // Edit Profile
        btnEditProfile.setOnClickListener {
            openActivity(EditProfileActivity::class.java)
        }

        // Trusted Contacts
        btnTrustedContacts.setOnClickListener {
            openActivity(TrustedContactsActivity::class.java)
        }

        // My Reports
        btnMyReports.setOnClickListener {
            openActivity(PersonalReportsActivity::class.java)
        }

        // Settings
        btnSettings.setOnClickListener {
            openActivity(PersonalSettingsActivity::class.java)
        }

        // Logout
        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    // Refresh the profile whenever this screen resumes
    override fun onResume() {
        super.onResume()

        auth.currentUser?.let { user ->
            loadUserProfile(user.uid)
        }
    }

    // Load profile information from the existing Firestore Users collection
    private fun loadUserProfile(userId: String) {

        tvProfileName.text =
            getString(R.string.personal_profile_default_name)

        tvEmail.text = auth.currentUser?.email
            ?: getString(R.string.personal_profile_not_available)

        tvPhone.text =
            getString(R.string.personal_profile_not_available)

        tvAccountType.text =
            getString(R.string.personal_profile_default_account_type)

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    return@addOnSuccessListener
                }

                document.getString("fullName")?.let { fullName ->
                    if (fullName.isNotEmpty()) {
                        tvProfileName.text = fullName
                    }
                }

                document.getString("email")?.let { email ->
                    if (email.isNotEmpty()) {
                        tvEmail.text = email
                    }
                }

                document.getString("phone")?.let { phone ->
                    if (phone.isNotEmpty()) {
                        tvPhone.text = phone
                    }
                }

                document.getString("accountType")?.let { accountType ->
                    if (accountType.isNotEmpty()) {
                        tvAccountType.text = accountType
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    getString(R.string.personal_profile_load_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Open another activity
    private fun openActivity(activityClass: Class<*>) {
        try {
            startActivity(
                Intent(this, activityClass)
            )
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.personal_profile_open_failed),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Sign out the current user
    private fun logoutUser() {

        auth.signOut()

        Toast.makeText(
            this,
            getString(R.string.personal_profile_logged_out),
            Toast.LENGTH_SHORT
        ).show()

        goToLogin()
    }

    // Redirect to login and clear the previous activity stack
    private fun goToLogin() {

        val intent = Intent(
            this,
            PersonalLoginActivity::class.java
        )

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
