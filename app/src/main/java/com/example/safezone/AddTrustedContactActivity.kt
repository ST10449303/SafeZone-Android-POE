
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
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.TrustedContactEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.UUID

class AddTrustedContactActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var etFullName: EditText
    private lateinit var etRelationship: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var cbEmergencyAlerts: CheckBox
    private lateinit var btnSaveContact: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trusted_contact)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvBack = findViewById(R.id.tvBack)
        etFullName = findViewById(R.id.etFullName)
        etRelationship = findViewById(R.id.etRelationship)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        cbEmergencyAlerts = findViewById(R.id.cbEmergencyAlerts)
        btnSaveContact = findViewById(R.id.btnSaveContact)

        tvBack.setOnClickListener {
            finish()
        }

        btnSaveContact.setOnClickListener {
            saveContact()
        }
    }

    private fun saveContact() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showToast(getString(R.string.add_trusted_contact_session_expired))
            return
        }

        val fullName = etFullName.text.toString().trim()
        val relationship = etRelationship.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val emergencyAlerts = cbEmergencyAlerts.isChecked

        if (fullName.length < 2) {
            etFullName.error =
                getString(R.string.add_trusted_contact_invalid_name)
            etFullName.requestFocus()
            return
        }

        if (relationship.isEmpty()) {
            etRelationship.error =
                getString(R.string.add_trusted_contact_relationship_required)
            etRelationship.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            etPhone.error =
                getString(R.string.add_trusted_contact_phone_required)
            etPhone.requestFocus()
            return
        }

        val cleanPhone = phone.replace(Regex("[\\s()-]"), "")

        if (cleanPhone.length < 7) {
            etPhone.error =
                getString(R.string.add_trusted_contact_invalid_phone)
            etPhone.requestFocus()
            return
        }

        if (email.isNotEmpty() &&
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            etEmail.error =
                getString(R.string.add_trusted_contact_invalid_email)
            etEmail.requestFocus()
            return
        }

        btnSaveContact.isEnabled = false
        btnSaveContact.text =
            getString(R.string.add_trusted_contact_saving)

        val userId = currentUser.uid
        val contactId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        val contact = TrustedContactEntity(
            contactId = contactId,
            userId = userId,
            fullName = fullName,
            relationship = relationship,
            phone = phone,
            email = email,
            emergencyAlerts = emergencyAlerts,
            createdAt = createdAt,
            syncStatus = "Pending Sync"
        )

        lifecycleScope.launch {
            try {
                val dao = SafeZoneDatabase
                    .getDatabase(applicationContext)
                    .trustedContactDao()

                // Save locally first so the contact is available offline.
                dao.insert(contact)

                val contactData = hashMapOf<String, Any>(
                    "userId" to userId,
                    "fullName" to fullName,
                    "relationship" to relationship,
                    "phone" to phone,
                    "email" to email,
                    "emergencyAlerts" to emergencyAlerts,
                    "createdAt" to createdAt
                )

                // Use the same ID locally and in Firestore.
                db.collection("TrustedContacts")
                    .document(contactId)
                    .set(contactData)
                    .addOnSuccessListener {
                        lifecycleScope.launch {
                            try {
                                dao.updateSyncStatus(
                                    contactId = contactId,
                                    userId = userId,
                                    syncStatus = "Synced"
                                )
                            } catch (_: Exception) {
                                // Keep the pending status if the update fails.
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Keep the local contact pending cloud synchronisation.
                    }

                showToast(
                    getString(R.string.add_trusted_contact_saved_pending)
                )

                val intent = Intent(
                    this@AddTrustedContactActivity,
                    TrustedContactsActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                startActivity(intent)
                finish()

            } catch (_: Exception) {
                btnSaveContact.isEnabled = true
                btnSaveContact.text =
                    getString(R.string.add_trusted_contact_save)

                showToast(
                    getString(R.string.add_trusted_contact_save_failed)
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this@AddTrustedContactActivity,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
