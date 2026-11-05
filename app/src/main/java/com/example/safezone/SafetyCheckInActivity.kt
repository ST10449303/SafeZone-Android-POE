package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.SafetyCheckInRepository
import com.example.safezone.data.TrustedContactEntity
import com.example.safezone.sync.SafetyCheckInSyncScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class SafetyCheckInActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvSelectedDuration: TextView

    private lateinit var btn30Minutes: Button
    private lateinit var btn1Hour: Button
    private lateinit var btn2Hours: Button
    private lateinit var btnCustom: Button

    private lateinit var spTrustedContact: Spinner
    private lateinit var switchLocation: SwitchCompat
    private lateinit var btnStartSafetyCheck: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var durationMinutes = 30

    // Store complete contact records, not just names.
    private val trustedContacts = mutableListOf<TrustedContactEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_check_in)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvBack = findViewById(R.id.tvBack)
        tvSelectedDuration = findViewById(R.id.tvSelectedDuration)
        btn30Minutes = findViewById(R.id.btn30Minutes)
        btn1Hour = findViewById(R.id.btn1Hour)
        btn2Hours = findViewById(R.id.btn2Hours)
        btnCustom = findViewById(R.id.btnCustom)
        spTrustedContact = findViewById(R.id.spTrustedContact)
        switchLocation = findViewById(R.id.switchLocation)
        btnStartSafetyCheck = findViewById(R.id.btnStartSafetyCheck)

        if (auth.currentUser == null) {
            showMessage(R.string.login_required)
            goToLogin()
            return
        }

        tvBack.setOnClickListener {
            finish()
        }

        btn30Minutes.setOnClickListener {
            durationMinutes = 30
            updateSelectedDuration()
        }

        btn1Hour.setOnClickListener {
            durationMinutes = 60
            updateSelectedDuration()
        }

        btn2Hours.setOnClickListener {
            durationMinutes = 120
            updateSelectedDuration()
        }

        btnCustom.setOnClickListener {
            showCustomDurationDialog()
        }

        btnStartSafetyCheck.setOnClickListener {
            startSafetyCheck()
        }

        // Load cached contacts first, then attempt a cloud refresh.
        loadTrustedContacts()

        // Schedule check-in synchronization.
        SafetyCheckInSyncScheduler.schedule(applicationContext)
    }

    // LOAD CONTACTS FROM ROOMDB FIRST

    private fun loadTrustedContacts() {
        val user = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                val dao = SafeZoneDatabase
                    .getDatabase(applicationContext)
                    .trustedContactDao()

                val cachedContacts = dao.getContactsForUserOnce(user.uid)
                displayTrustedContacts(cachedContacts)

                // Refresh from Firestore without blocking offline use.
                refreshContactsFromFirestore(user.uid)

            } catch (_: Exception) {
                showMessage(
                    R.string.contacts_load_failed,
                    Toast.LENGTH_LONG
                )
            }
        }
    }

    // REFRESH CONTACTS FROM FIRESTORE

    private fun refreshContactsFromFirestore(userId: String) {
        db.collection("TrustedContacts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->

                lifecycleScope.launch {
                    try {
                        val dao = SafeZoneDatabase
                            .getDatabase(applicationContext)
                            .trustedContactDao()

                        val cachedContacts =
                            dao.getContactsForUserOnce(userId)

                        val cachedById =
                            cachedContacts.associateBy { it.contactId }

                        val pendingDeletions =
                            dao.getPendingDeletions(userId)
                                .map { it.contactId }
                                .toSet()

                        val cloudContacts = documents.mapNotNull { document ->
                            val owner = document.getString("userId")
                                ?: return@mapNotNull null

                            val name = document.getString("fullName")
                                ?: return@mapNotNull null

                            TrustedContactEntity(
                                contactId = document.id,
                                userId = owner,
                                fullName = name,
                                relationship = document.getString("relationship")
                                    ?: "Trusted Contact",
                                phone = document.getString("phone") ?: "",
                                email = document.getString("email") ?: "",
                                emergencyAlerts = document.getBoolean(
                                    "emergencyAlerts"
                                ) ?: false,
                                createdAt = document.getLong("createdAt") ?: 0L,
                                syncStatus = "Synced"
                            )
                        }

                        // Do not overwrite locally pending uploads or
                        // resurrect contacts awaiting cloud deletion.
                        val contactsToCache = cloudContacts.filter { cloud ->
                            val existing = cachedById[cloud.contactId]

                            cloud.contactId !in pendingDeletions &&
                                    (existing == null ||
                                            existing.syncStatus == "Synced")
                        }

                        dao.insertAll(contactsToCache)

                        val updatedContacts =
                            dao.getContactsForUserOnce(userId)

                        displayTrustedContacts(updatedContacts)

                    } catch (_: Exception) {
                        // Keep locally cached contacts available.
                    }
                }
            }
            .addOnFailureListener {
                // If offline, continue using RoomDB.
                showMessage(
                    R.string.contacts_cloud_unavailable,
                    Toast.LENGTH_SHORT
                )
            }
    }

    // DISPLAY CONTACTS IN SPINNER

    private fun displayTrustedContacts(
        contacts: List<TrustedContactEntity>
    ) {
        trustedContacts.clear()
        trustedContacts.addAll(
            contacts.filter {
                it.syncStatus != "Deleted Pending Sync"
            }
        )

        val names = mutableListOf(
            getString(R.string.select_trusted_contact)
        )

        names.addAll(trustedContacts.map { it.fullName })

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            names
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spTrustedContact.adapter = adapter

        if (trustedContacts.isEmpty()) {
            showMessage(
                R.string.no_saved_contacts,
                Toast.LENGTH_LONG
            )
        }
    }

    // UPDATE THE SELECTED DURATION

    private fun updateSelectedDuration() {
        val durationText = resources.getQuantityString(
            R.plurals.duration_minutes,
            durationMinutes,
            durationMinutes
        )

        tvSelectedDuration.text = getString(
            R.string.duration_selected,
            durationText
        )
    }

    // CUSTOM DURATION

    private fun showCustomDurationDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.enter_minutes)
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.custom_duration_title)
            .setMessage(R.string.custom_duration_message)
            .setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.set) { _, _ ->
                val minutes = input.text.toString().toIntOrNull()

                if (minutes == null || minutes < 5 || minutes > 1440) {
                    showMessage(
                        R.string.duration_range_error,
                        Toast.LENGTH_LONG
                    )
                    return@setPositiveButton
                }

                durationMinutes = minutes
                updateSelectedDuration()
            }
            .show()
    }

    // START SAFETY CHECK-IN

    private fun startSafetyCheck() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showMessage(R.string.login_required)
            goToLogin()
            return
        }

        val contactPosition = spTrustedContact.selectedItemPosition

        if (trustedContacts.isEmpty() || contactPosition <= 0) {
            showMessage(
                R.string.select_trusted_contact_error,
                Toast.LENGTH_LONG
            )
            return
        }

        val selectedContact = trustedContacts[contactPosition - 1]

        // Avoid selecting a contact whose deletion is pending.
        if (selectedContact.syncStatus == "Deleted Pending Sync") {
            showMessage(R.string.select_another_contact)
            return
        }

        val locationSharing = switchLocation.isChecked
        val startedAt = System.currentTimeMillis()
        val expiresAt = startedAt + durationMinutes * 60 * 1000L

        btnStartSafetyCheck.isEnabled = false
        btnStartSafetyCheck.text = getString(R.string.saving)

        lifecycleScope.launch {
            try {
                val dao = SafeZoneDatabase
                    .getDatabase(applicationContext)
                    .safetyCheckInDao()

                val repository = SafetyCheckInRepository(
                    dao = dao,
                    firestore = db
                )

                // Save locally before attempting cloud synchronization.
                val checkIn = repository.saveCheckInLocally(
                    userId = currentUser.uid,
                    trustedContact = selectedContact.fullName,
                    durationMinutes = durationMinutes,
                    locationSharing = locationSharing,
                    startedAt = startedAt,
                    expiresAt = expiresAt
                )

                SafetyCheckInSyncScheduler.schedule(applicationContext)

                showMessage(
                    R.string.check_in_saved,
                    Toast.LENGTH_SHORT
                )

                val intent = Intent(
                    this@SafetyCheckInActivity,
                    ActiveSafetyCheckActivity::class.java
                ).apply {
                    putExtra("checkInId", checkIn.localId)
                    putExtra("expiresAt", checkIn.expiresAt)
                    putExtra("trustedContact", checkIn.trustedContact)
                    putExtra("locationSharing", checkIn.locationSharing)
                    putExtra("isLocalCheckIn", true)
                }

                startActivity(intent)
                finish()

            } catch (_: Exception) {
                btnStartSafetyCheck.isEnabled = true
                btnStartSafetyCheck.text =
                    getString(R.string.start_safety_check)

                showMessage(
                    R.string.check_in_save_failed,
                    Toast.LENGTH_LONG
                )
            }
        }
    }

    // SHOW A TRANSLATED MESSAGE

    private fun showMessage(
        messageResId: Int,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(
            this,
            getString(messageResId),
            duration
        ).show()
    }

    // NAVIGATE TO LOG IN

    private fun goToLogin() {
        val intent = Intent(
            this,
            PersonalLoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}
