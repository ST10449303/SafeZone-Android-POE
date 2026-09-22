package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddTrustedContactActivity : AppCompatActivity() {

    // ==========================================
    // UI COMPONENTS
    // ==========================================

    private lateinit var tvBack: TextView

    private lateinit var etFullName: EditText
    private lateinit var etRelationship: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText

    private lateinit var cbEmergencyAlerts: CheckBox
    private lateinit var btnSaveContact: Button

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

        setContentView(R.layout.activity_add_trusted_contact)

        // ==========================================
        // INITIALISE FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ==========================================
        // CONNECT XML VIEWS
        // ==========================================

        tvBack = findViewById(R.id.tvBack)

        etFullName = findViewById(R.id.etFullName)

        etRelationship = findViewById(R.id.etRelationship)

        etPhone = findViewById(R.id.etPhone)

        etEmail = findViewById(R.id.etEmail)

        cbEmergencyAlerts = findViewById(R.id.cbEmergencyAlerts)

        btnSaveContact = findViewById(R.id.btnSaveContact)

        // ==========================================
        // BACK BUTTON
        // ==========================================

        tvBack.setOnClickListener {
            finish()
        }

        // ==========================================
        // SAVE CONTACT BUTTON
        // ==========================================

        btnSaveContact.setOnClickListener {
            saveContact()
        }
    }

    // ==================================================
    // SAVE CONTACT
    // ==================================================

    private fun saveContact() {

        // ==========================================
        // CHECK LOGGED-IN USER
        // ==========================================

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Your session has expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // ==========================================
        // GET VALUES FROM INPUT FIELDS
        // ==========================================

        val fullName =
            etFullName.text.toString().trim()

        val relationship =
            etRelationship.text.toString().trim()

        val phone =
            etPhone.text.toString().trim()

        val email =
            etEmail.text.toString().trim()

        val emergencyAlerts =
            cbEmergencyAlerts.isChecked

        // ==========================================
        // VALIDATE FULL NAME
        // ==========================================

        if (fullName.isEmpty()) {

            etFullName.error =
                "Please enter the contact's full name."

            etFullName.requestFocus()

            return
        }

        if (fullName.length < 2) {

            etFullName.error =
                "Please enter a valid full name."

            etFullName.requestFocus()

            return
        }

        // ==========================================
        // VALIDATE RELATIONSHIP
        // ==========================================

        if (relationship.isEmpty()) {

            etRelationship.error =
                "Please enter the relationship."

            etRelationship.requestFocus()

            return
        }

        // ==========================================
        // VALIDATE PHONE NUMBER
        // ==========================================

        if (phone.isEmpty()) {

            etPhone.error =
                "Please enter a phone number."

            etPhone.requestFocus()

            return
        }

        // Remove spaces, brackets and hyphens
        // for basic validation.

        val cleanPhone =
            phone.replace(
                Regex("[\\s()\\-]"),
                ""
            )

        if (cleanPhone.length < 7) {

            etPhone.error =
                "Please enter a valid phone number."

            etPhone.requestFocus()

            return
        }

        // ==========================================
        // VALIDATE OPTIONAL EMAIL
        // ==========================================

        if (email.isNotEmpty()) {

            if (!Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {

                etEmail.error =
                    "Please enter a valid email address."

                etEmail.requestFocus()

                return
            }
        }

        // ==========================================
        // PREVENT DOUBLE CLICK
        // ==========================================

        btnSaveContact.isEnabled = false

        btnSaveContact.text = "Saving..."

        // ==========================================
        // CREATE FIRESTORE DATA
        // ==========================================

        val contactData: Map<String, Any> =
            hashMapOf(

                // ID of the logged-in user.
                // This is important because the
                // user should only see their own
                // trusted contacts.

                "userId" to currentUser.uid,

                // Contact information

                "fullName" to fullName,

                "relationship" to relationship,

                "phone" to phone,

                "email" to email,

                // Whether this contact should
                // receive emergency alerts.

                "emergencyAlerts" to emergencyAlerts,

                // Creation time

                "createdAt" to
                        System.currentTimeMillis()
            )

        // ==========================================
        // SAVE TO FIRESTORE
        // ==========================================

        db.collection("TrustedContacts")
            .add(contactData)

            .addOnSuccessListener {

                // ==================================
                // SUCCESS
                // ==================================

                Toast.makeText(
                    this,
                    "Trusted contact saved successfully.",
                    Toast.LENGTH_LONG
                ).show()

                // Return to Trusted Contacts screen

                val intent = Intent(
                    this,
                    TrustedContactsActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                startActivity(intent)

                finish()
            }

            .addOnFailureListener { exception ->

                // ==================================
                // FIRESTORE ERROR
                // ==================================

                btnSaveContact.isEnabled = true

                btnSaveContact.text =
                    "Save Contact"

                Toast.makeText(
                    this,
                    "Unable to save contact: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}