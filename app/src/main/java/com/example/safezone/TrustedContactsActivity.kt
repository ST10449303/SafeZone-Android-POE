package com.example.safezone

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.TrustedContactEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class TrustedContactsActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var btnAddContact: Button
    private lateinit var contactsList: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var tvNoContacts: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trusted_contacts)

        tvBack = findViewById(R.id.tvBack)
        btnAddContact = findViewById(R.id.btnAddContact)
        contactsList = findViewById(R.id.contactsList)
        emptyState = findViewById(R.id.emptyState)
        tvNoContacts = findViewById(R.id.tvNoContacts)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvBack.setOnClickListener { finish() }

        btnAddContact.setOnClickListener {
            startActivity(
                Intent(this, AddTrustedContactActivity::class.java)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadTrustedContacts()
    }

    private fun loadTrustedContacts() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                getString(R.string.trusted_contacts_login_required),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        // Display the local cache first for offline access.
        lifecycleScope.launch {
            try {
                val dao = SafeZoneDatabase
                    .getDatabase(applicationContext)
                    .trustedContactDao()

                val cachedContacts =
                    dao.getContactsForUserOnce(user.uid)

                displayContacts(cachedContacts)

                // Synchronise pending deletions and refresh cloud data.
                syncPendingDeletions(user.uid)
                refreshContactsFromFirestore(user.uid)

            } catch (_: Exception) {
                showEmptyState(
                    getString(
                        R.string.trusted_contacts_load_device_error
                    )
                )
            }
        }
    }

    private fun refreshContactsFromFirestore(userId: String) {
        db.collection("TrustedContacts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                lifecycleScope.launch {
                    try {
                        val dao = SafeZoneDatabase
                            .getDatabase(applicationContext)
                            .trustedContactDao()

                        val cloudContacts =
                            result.documents.mapNotNull { document ->
                                val owner =
                                    document.getString("userId")
                                        ?: return@mapNotNull null

                                val name =
                                    document.getString("fullName")
                                        ?: return@mapNotNull null

                                TrustedContactEntity(
                                    contactId = document.id,
                                    userId = owner,
                                    fullName = name,
                                    relationship =
                                        document.getString("relationship")
                                            ?: getString(
                                                R.string.trusted_contacts_default_relationship
                                            ),
                                    phone =
                                        document.getString("phone") ?: "",
                                    email =
                                        document.getString("email") ?: "",
                                    emergencyAlerts =
                                        document.getBoolean("emergencyAlerts")
                                            ?: false,
                                    createdAt =
                                        document.getLong("createdAt") ?: 0L,
                                    syncStatus = "Synced"
                                )
                            }

                        // Preserve locally pending changes.
                        val existing =
                            dao.getContactsForUserOnce(userId)

                        val existingById =
                            existing.associateBy { it.contactId }

                        val safeToCache =
                            cloudContacts.filter { cloud ->
                                val local =
                                    existingById[cloud.contactId]

                                local == null ||
                                        local.syncStatus == "Synced"
                            }

                        dao.insertAll(safeToCache)

                        // Remove stale synced contacts only after a
                        // successful Firestore response.
                        val cloudIds =
                            cloudContacts.map { it.contactId }.toSet()

                        val refreshedExisting =
                            dao.getContactsForUserOnce(userId)

                        refreshedExisting.forEach { local ->
                            if (
                                local.syncStatus == "Synced" &&
                                local.contactId !in cloudIds
                            ) {
                                dao.deleteContact(
                                    local.contactId,
                                    userId
                                )
                            }
                        }

                        val updated =
                            dao.getContactsForUserOnce(userId)

                        displayContacts(updated)

                    } catch (_: Exception) {
                        // Keep cached contacts available.
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    getString(
                        R.string.trusted_contacts_cloud_unavailable
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun syncPendingDeletions(userId: String) {
        lifecycleScope.launch {
            try {
                val dao = SafeZoneDatabase
                    .getDatabase(applicationContext)
                    .trustedContactDao()

                val pending = dao.getPendingDeletions(userId)

                for (contact in pending) {
                    db.collection("TrustedContacts")
                        .document(contact.contactId)
                        .get()
                        .addOnSuccessListener { document ->
                            val owner =
                                document.getString("userId")

                            if (!document.exists()) {
                                lifecycleScope.launch {
                                    dao.deleteContact(
                                        contact.contactId,
                                        userId
                                    )
                                }
                            } else if (owner == userId) {
                                db.collection("TrustedContacts")
                                    .document(contact.contactId)
                                    .delete()
                                    .addOnSuccessListener {
                                        lifecycleScope.launch {
                                            dao.deleteContact(
                                                contact.contactId,
                                                userId
                                            )
                                        }
                                    }
                            }
                        }
                }
            } catch (_: Exception) {
                // Retry pending deletions on a later load.
            }
        }
    }

    private fun displayContacts(
        contacts: List<TrustedContactEntity>
    ) {
        contactsList.removeAllViews()

        if (contacts.isEmpty()) {
            showEmptyState(
                getString(
                    R.string.trusted_contacts_empty_description
                )
            )
            return
        }

        contactsList.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        contacts.forEach { contact ->
            addContactCard(
                contact.contactId,
                contact.fullName,
                contact.relationship,
                contact.phone,
                contact.email,
                contact.emergencyAlerts,
                contact.syncStatus
            )
        }
    }

    private fun showEmptyState(message: String) {
        contactsList.removeAllViews()
        contactsList.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        tvNoContacts.text = message
    }

    private fun addContactCard(
        contactId: String,
        fullName: String,
        relationship: String,
        phone: String,
        email: String,
        emergencyAlerts: Boolean,
        syncStatus: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = createRoundedBackground(
                Color.WHITE,
                Color.parseColor("#DCE5F0"),
                16
            )
        }

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, dp(10), 0, 0)
        card.layoutParams = cardParams

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundResource(
                R.drawable.bg_trusted_contacts_empty_icon
            )
            setImageResource(R.drawable.ic_person)
            contentDescription =
                getString(R.string.trusted_contacts_icon_description)
        }
        topRow.addView(icon)

        val nameContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(dp(12), 0, 0, 0)
            }
        }

        val nameText = TextView(this).apply {
            text = fullName
            setTextColor(Color.parseColor("#0B1F3A"))
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
        }
        nameContainer.addView(nameText)

        val relationshipText = TextView(this).apply {
            text = relationship
            setTextColor(Color.parseColor("#667085"))
            textSize = 13f
            setPadding(0, dp(3), 0, 0)
        }
        nameContainer.addView(relationshipText)

        topRow.addView(nameContainer)
        card.addView(topRow)

        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#E6EBF2"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                setMargins(0, dp(14), 0, dp(12))
            }
        }
        card.addView(divider)

        if (phone.isNotEmpty()) {
            val phoneRow = createInfoRow(
                R.drawable.ic_phone,
                phone
            )
            phoneRow.setOnClickListener {
                callContact(phone)
            }
            card.addView(phoneRow)
        }

        if (email.isNotEmpty()) {
            val emailRow = createInfoRow(
                R.drawable.ic_email,
                email
            )
            emailRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(8), 0, 0)
            }
            card.addView(emailRow)
        }

        val alertStatus = TextView(this).apply {
            text = if (emergencyAlerts) {
                getString(
                    R.string.trusted_contacts_alerts_enabled
                )
            } else {
                getString(
                    R.string.trusted_contacts_alerts_disabled
                )
            }

            setTextColor(
                if (emergencyAlerts) {
                    Color.parseColor("#18864B")
                } else {
                    Color.parseColor("#667085")
                }
            )

            textSize = 12f
            setTypeface(null, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(12), 0, 0)
            }
        }
        card.addView(alertStatus)

        if (syncStatus != "Synced") {
            val syncText = TextView(this).apply {
                text = getString(
                    R.string.trusted_contacts_sync_pending
                )
                setTextColor(Color.parseColor("#9A6700"))
                textSize = 12f
                setPadding(0, dp(6), 0, 0)
            }
            card.addView(syncText)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                setMargins(0, dp(14), 0, 0)
            }
        }

        val callButton = Button(this).apply {
            text = getString(R.string.trusted_contacts_call)
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)

            background = createRoundedBackground(
                Color.parseColor("#234572"),
                Color.TRANSPARENT,
                24
            )

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(0, 0, dp(5), 0)
            }

            setOnClickListener {
                if (phone.isNotEmpty()) {
                    callContact(phone)
                } else {
                    Toast.makeText(
                        this@TrustedContactsActivity,
                        getString(
                            R.string.trusted_contacts_no_phone
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        buttonRow.addView(callButton)

        val deleteButton = Button(this).apply {
            text = getString(R.string.trusted_contacts_delete)
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)

            background = createRoundedBackground(
                Color.parseColor("#D32F2F"),
                Color.TRANSPARENT,
                24
            )

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(dp(5), 0, 0, 0)
            }

            setOnClickListener {
                deleteContact(contactId)
            }
        }
        buttonRow.addView(deleteButton)

        card.addView(buttonRow)
        contactsList.addView(card)
    }

    private fun createInfoRow(
        iconResource: Int,
        text: String
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            setImageResource(iconResource)
            contentDescription =
                getString(R.string.trusted_contacts_info_description)
        }
        row.addView(icon)

        val textView = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#344054"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(dp(10), 0, 0, 0)
            }
        }
        row.addView(textView)

        return row
    }

    private fun callContact(phone: String) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:$phone")
                )
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.trusted_contacts_dialer_error
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteContact(contactId: String) {
        val user = auth.currentUser ?: run {
            Toast.makeText(
                this,
                getString(
                    R.string.trusted_contacts_login_again
                ),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                val dao = SafeZoneDatabase
                    .getDatabase(applicationContext)
                    .trustedContactDao()

                val localContact =
                    dao.getContactsForUserOnce(user.uid)
                        .firstOrNull {
                            it.contactId == contactId
                        }

                if (localContact == null) {
                    Toast.makeText(
                        this@TrustedContactsActivity,
                        getString(
                            R.string.trusted_contacts_not_found
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (localContact.syncStatus == "Pending Sync") {
                    // No cloud document exists for this pending upload.
                    dao.deleteContact(contactId, user.uid)
                } else {
                    // Mark the contact for deletion until Firestore confirms.
                    dao.markContactDeleted(contactId, user.uid)
                    syncPendingDeletions(user.uid)
                }

                displayContacts(
                    dao.getContactsForUserOnce(user.uid)
                )

                Toast.makeText(
                    this@TrustedContactsActivity,
                    getString(
                        R.string.trusted_contacts_removed_message
                    ),
                    Toast.LENGTH_LONG
                ).show()

            } catch (_: Exception) {
                Toast.makeText(
                    this@TrustedContactsActivity,
                    getString(
                        R.string.trusted_contacts_remove_error
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createRoundedBackground(
        fillColor: Int,
        strokeColor: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)

            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(1), strokeColor)
            }

            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
