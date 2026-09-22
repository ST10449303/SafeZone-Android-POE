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

class EditProfileActivity : AppCompatActivity() {

    // ==========================================
    // EDIT PROFILE FIELDS
    // ==========================================

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText

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

        setContentView(
            R.layout.activity_edit_profile
        )

        // ==========================================
        // INITIALISE FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ==========================================
        // CONNECT EDIT TEXT FIELDS
        // ==========================================

        etFullName =
            findViewById(R.id.etFullName)

        etEmail =
            findViewById(R.id.etEmail)

        etPhone =
            findViewById(R.id.etPhone)

        // ==========================================
        // CONNECT BUTTONS
        // ==========================================

        btnSaveChanges =
            findViewById(R.id.btnSaveChanges)

        tvBack =
            findViewById(R.id.tvBack)

        tvCancel =
            findViewById(R.id.tvCancel)

        // ==========================================
        // CHECK CURRENT USER
        // ==========================================

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Your session has expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }

        // ==========================================
        // LOAD EXISTING PROFILE
        // ==========================================

        loadProfile(
            currentUser.uid
        )

        // ==========================================
        // BACK BUTTON
        // ==========================================

        tvBack.setOnClickListener {

            finish()
        }

        // ==========================================
        // CANCEL
        // ==========================================

        tvCancel.setOnClickListener {

            finish()
        }

        // ==========================================
        // SAVE CHANGES
        // ==========================================

        btnSaveChanges.setOnClickListener {

            saveProfile(
                currentUser.uid
            )
        }
    }

    // ==================================================
    // LOAD PROFILE
    // ==================================================

    private fun loadProfile(
        userId: String
    ) {

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    // ----------------------------------
                    // FULL NAME
                    // ----------------------------------

                    val fullName =
                        document.getString(
                            "fullName"
                        ) ?: ""

                    // ----------------------------------
                    // EMAIL
                    // ----------------------------------

                    val email =
                        document.getString(
                            "email"
                        )
                            ?: auth.currentUser?.email
                            ?: ""

                    // ----------------------------------
                    // PHONE
                    // ----------------------------------

                    val phone =
                        document.getString(
                            "phone"
                        ) ?: ""

                    // ----------------------------------
                    // DISPLAY VALUES
                    // ----------------------------------

                    etFullName.setText(
                        fullName
                    )

                    etEmail.setText(
                        email
                    )

                    etPhone.setText(
                        phone
                    )
                }
                else {

                    // ----------------------------------
                    // FIRESTORE DOCUMENT DOES NOT EXIST
                    // ----------------------------------

                    etFullName.setText("")

                    etEmail.setText(
                        auth.currentUser?.email ?: ""
                    )

                    etPhone.setText("")
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load your profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==================================================
    // SAVE PROFILE
    // ==================================================

    private fun saveProfile(
        userId: String
    ) {

        // ==========================================
        // GET VALUES
        // ==========================================

        val fullName =
            etFullName.text
                .toString()
                .trim()

        val email =
            etEmail.text
                .toString()
                .trim()

        val phone =
            etPhone.text
                .toString()
                .trim()

        // ==========================================
        // VALIDATE FULL NAME
        // ==========================================

        if (fullName.isEmpty()) {

            etFullName.error =
                "Please enter your full name"

            etFullName.requestFocus()

            return
        }

        // ==========================================
        // VALIDATE EMAIL
        // ==========================================

        if (email.isEmpty()) {

            etEmail.error =
                "Please enter your email address"

            etEmail.requestFocus()

            return
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            etEmail.error =
                "Please enter a valid email address"

            etEmail.requestFocus()

            return
        }

        // ==========================================
        // VALIDATE PHONE
        // ==========================================

        if (phone.isEmpty()) {

            etPhone.error =
                "Please enter your phone number"

            etPhone.requestFocus()

            return
        }

        if (phone.length < 10) {

            etPhone.error =
                "Please enter a valid phone number"

            etPhone.requestFocus()

            return
        }

        // ==========================================
        // PREPARE FIRESTORE DATA
        // ==========================================

        val updates =
            hashMapOf<String, Any>(
                "fullName" to fullName,
                "email" to email,
                "phone" to phone
            )

        // ==========================================
        // DISABLE BUTTON WHILE SAVING
        // ==========================================

        btnSaveChanges.isEnabled = false

        btnSaveChanges.text =
            "Saving..."

        // ==========================================
        // UPDATE FIRESTORE
        // ==========================================

        db.collection("Users")
            .document(userId)
            .set(
                updates,
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {

                // ==================================
                // SUCCESS
                // ==================================

                Toast.makeText(
                    this,
                    "Profile updated successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                // ==================================
                // RETURN TO PROFILE
                // ==================================

                finish()
            }
            .addOnFailureListener { exception ->

                // ==================================
                // RESTORE BUTTON
                // ==================================

                btnSaveChanges.isEnabled = true

                btnSaveChanges.text =
                    "Save Changes"

                // ==================================
                // ERROR MESSAGE
                // ==================================

                Toast.makeText(
                    this,
                    "Failed to update profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}