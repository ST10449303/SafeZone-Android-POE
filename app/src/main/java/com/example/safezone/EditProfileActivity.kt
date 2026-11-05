
package com.example.safezone

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditProfileActivity : AppCompatActivity() {

    // ==========================================
    // EDIT PROFILE FIELDS
    // ==========================================

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLocation: EditText

    // ==========================================
    // BUTTONS
    // ==========================================

    private lateinit var btnSaveChanges: AppCompatButton
    private lateinit var tvBack: TextView
    private lateinit var tvCancel: TextView

    // ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_profile)

        // Initialise Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Connect edit text fields
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etLocation = findViewById(R.id.etLocation)

        // Connect buttons
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        tvBack = findViewById(R.id.tvBack)
        tvCancel = findViewById(R.id.tvCancel)

        // Check current user
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                getString(R.string.edit_profile_session_expired),
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Load existing profile
        loadProfile(currentUser.uid)

        // Back button
        tvBack.setOnClickListener {
            finish()
        }

        // Cancel editing
        tvCancel.setOnClickListener {
            finish()
        }

        // Save changes
        btnSaveChanges.setOnClickListener {
            saveProfile(currentUser.uid)
        }
    }

    // ==========================================
    // LOAD PROFILE
    // ==========================================

    private fun loadProfile(userId: String) {

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    // Full name
                    val fullName =
                        document.getString("fullName") ?: ""

                    // Email
                    val email =
                        document.getString("email")
                            ?: auth.currentUser?.email
                            ?: ""

                    // Phone
                    val phone =
                        document.getString("phone") ?: ""

                    // Residential area / suburb
                    val location =
                        document.getString("location") ?: ""

                    // Display existing values
                    etFullName.setText(fullName)
                    etEmail.setText(email)
                    etPhone.setText(phone)
                    etLocation.setText(location)

                } else {

                    // Firestore document does not exist
                    etFullName.setText("")
                    etEmail.setText(
                        auth.currentUser?.email ?: ""
                    )
                    etPhone.setText("")
                    etLocation.setText("")
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    getString(
                        R.string.edit_profile_load_failed,
                        exception.message
                            ?: getString(
                                R.string.edit_profile_unknown_error
                            )
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==========================================
    // SAVE PROFILE
    // ==========================================

    private fun saveProfile(userId: String) {

        // Get values
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val location = etLocation.text.toString().trim()

        // Validate full name
        if (fullName.isEmpty()) {
            etFullName.error =
                getString(R.string.edit_profile_name_required)

            etFullName.requestFocus()
            return
        }

        // Validate email
        if (email.isEmpty()) {
            etEmail.error =
                getString(R.string.edit_profile_email_required)

            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error =
                getString(R.string.edit_profile_email_invalid)

            etEmail.requestFocus()
            return
        }

        // Validate phone
        if (phone.isEmpty()) {
            etPhone.error =
                getString(R.string.edit_profile_phone_required)

            etPhone.requestFocus()
            return
        }

        if (phone.length < 10) {
            etPhone.error =
                getString(R.string.edit_profile_phone_invalid)

            etPhone.requestFocus()
            return
        }

        // Validate residential location
        if (location.isEmpty()) {
            etLocation.error =
                getString(R.string.edit_profile_location_required)

            etLocation.requestFocus()
            return
        }

        if (location.length < 3) {
            etLocation.error =
                getString(R.string.edit_profile_location_invalid)

            etLocation.requestFocus()
            return
        }

        // Prepare Firestore data
        val updates = hashMapOf<String, Any>(
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "location" to location
        )

        // Disable button while saving
        btnSaveChanges.isEnabled = false
        btnSaveChanges.text =
            getString(R.string.edit_profile_saving)

        // Update existing Firestore document
        db.collection("Users")
            .document(userId)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    getString(R.string.edit_profile_save_success),
                    Toast.LENGTH_SHORT
                ).show()

                // Return to profile
                finish()
            }
            .addOnFailureListener { exception ->

                // Restore button after failure
                btnSaveChanges.isEnabled = true
                btnSaveChanges.text =
                    getString(R.string.edit_profile_save_changes)

                // Display error
                Toast.makeText(
                    this,
                    getString(
                        R.string.edit_profile_save_failed,
                        exception.message
                            ?: getString(
                                R.string.edit_profile_unknown_error
                            )
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
