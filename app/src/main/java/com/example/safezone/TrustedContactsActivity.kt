package com.example.safezone

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TrustedContactsActivity : AppCompatActivity() {

    // ==========================================
    // XML VIEWS
    // ==========================================

    private lateinit var tvBack: TextView
    private lateinit var btnAddContact: Button
    private lateinit var contactsList: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var tvNoContacts: TextView

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
            R.layout.activity_trusted_contacts
        )

        // ==========================================
        // CONNECT XML
        // ==========================================

        tvBack =
            findViewById(R.id.tvBack)

        btnAddContact =
            findViewById(R.id.btnAddContact)

        contactsList =
            findViewById(R.id.contactsList)

        emptyState =
            findViewById(R.id.emptyState)

        tvNoContacts =
            findViewById(R.id.tvNoContacts)

        // ==========================================
        // FIREBASE
        // ==========================================

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()

        // ==========================================
        // BACK
        // ==========================================

        tvBack.setOnClickListener {

            finish()
        }

        // ==========================================
        // ADD CONTACT
        // ==========================================

        btnAddContact.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddTrustedContactActivity::class.java
                )

            startActivity(intent)
        }
    }

    // ==========================================
    // REFRESH CONTACTS
    // ==========================================

    override fun onResume() {

        super.onResume()

        loadTrustedContacts()
    }

    // ==================================================
    // LOAD TRUSTED CONTACTS
    // ==================================================

    private fun loadTrustedContacts() {

        val currentUser =
            auth.currentUser

        // ==========================================
        // CHECK LOGIN
        // ==========================================

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        // ==========================================
        // CLEAR CURRENT CONTACTS
        // ==========================================

        contactsList.removeAllViews()

        contactsList.visibility =
            View.GONE

        emptyState.visibility =
            View.GONE

        // ==========================================
        // LOAD CURRENT USER'S CONTACTS
        // ==========================================

        db.collection("TrustedContacts")
            .whereEqualTo(
                "userId",
                currentUser.uid
            )
            .get()
            .addOnSuccessListener { result ->

                // ==================================
                // NO CONTACTS
                // ==================================

                if (result.isEmpty) {

                    contactsList.visibility =
                        View.GONE

                    emptyState.visibility =
                        View.VISIBLE

                    tvNoContacts.text =
                        "Add someone you trust so they can be contacted during an emergency."

                    return@addOnSuccessListener
                }

                // ==================================
                // CONTACTS FOUND
                // ==================================

                contactsList.visibility =
                    View.VISIBLE

                emptyState.visibility =
                    View.GONE

                // ==================================
                // CREATE CONTACT CARDS
                // ==================================

                for (document in result) {

                    val fullName =
                        document.getString(
                            "fullName"
                        ) ?: "Unknown Contact"

                    val relationship =
                        document.getString(
                            "relationship"
                        ) ?: "Trusted Contact"

                    val phone =
                        document.getString(
                            "phone"
                        ) ?: ""

                    val email =
                        document.getString(
                            "email"
                        ) ?: ""

                    val emergencyAlerts =
                        document.getBoolean(
                            "emergencyAlerts"
                        ) ?: false

                    addContactCard(
                        document.id,
                        fullName,
                        relationship,
                        phone,
                        email,
                        emergencyAlerts
                    )
                }
            }
            .addOnFailureListener {

                contactsList.removeAllViews()

                contactsList.visibility =
                    View.GONE

                emptyState.visibility =
                    View.VISIBLE

                tvNoContacts.text =
                    "Unable to load your trusted contacts. Please try again."

                Toast.makeText(
                    this,
                    "Unable to load trusted contacts.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==================================================
    // CREATE CONTACT CARD
    // ==================================================

    private fun addContactCard(
        contactId: String,
        fullName: String,
        relationship: String,
        phone: String,
        email: String,
        emergencyAlerts: Boolean
    ) {

        // ==========================================
        // MAIN CARD
        // ==========================================

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            dp(16),
            dp(16),
            dp(16),
            dp(16)
        )

        card.background =
            createRoundedBackground(
                Color.WHITE,
                Color.parseColor("#DCE5F0"),
                16
            )

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.setMargins(
            0,
            dp(10),
            0,
            0
        )

        card.layoutParams =
            cardParams

        // ==========================================
        // TOP ROW
        // ==========================================

        val topRow =
            LinearLayout(this)

        topRow.orientation =
            LinearLayout.HORIZONTAL

        topRow.gravity =
            Gravity.CENTER_VERTICAL

        // ==========================================
        // PERSON ICON
        // ==========================================

        val icon =
            ImageView(this)

        icon.layoutParams =
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )

        icon.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        icon.setBackgroundResource(
            R.drawable.bg_trusted_contacts_empty_icon
        )

        icon.setImageResource(
            R.drawable.ic_person
        )

        icon.contentDescription =
            "Trusted contact"

        topRow.addView(icon)

        // ==========================================
        // NAME CONTAINER
        // ==========================================

        val nameContainer =
            LinearLayout(this)

        nameContainer.orientation =
            LinearLayout.VERTICAL

        val nameParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        nameParams.setMargins(
            dp(12),
            0,
            0,
            0
        )

        nameContainer.layoutParams =
            nameParams

        // ==========================================
        // NAME
        // ==========================================

        val nameText =
            TextView(this)

        nameText.text =
            fullName

        nameText.setTextColor(
            Color.parseColor("#0B1F3A")
        )

        nameText.textSize =
            17f

        nameText.setTypeface(
            null,
            Typeface.BOLD
        )

        nameContainer.addView(
            nameText
        )

        // ==========================================
        // RELATIONSHIP
        // ==========================================

        val relationshipText =
            TextView(this)

        relationshipText.text =
            relationship

        relationshipText.setTextColor(
            Color.parseColor("#667085")
        )

        relationshipText.textSize =
            13f

        relationshipText.setPadding(
            0,
            dp(3),
            0,
            0
        )

        nameContainer.addView(
            relationshipText
        )

        topRow.addView(
            nameContainer
        )

        card.addView(
            topRow
        )

        // ==========================================
        // DIVIDER
        // ==========================================

        val divider =
            View(this)

        divider.setBackgroundColor(
            Color.parseColor("#E6EBF2")
        )

        val dividerParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )

        dividerParams.setMargins(
            0,
            dp(14),
            0,
            dp(12)
        )

        divider.layoutParams =
            dividerParams

        card.addView(
            divider
        )

        // ==========================================
        // PHONE
        // ==========================================

        if (phone.isNotEmpty()) {

            val phoneRow =
                createInfoRow(
                    R.drawable.ic_phone,
                    phone
                )

            phoneRow.setOnClickListener {

                callContact(phone)
            }

            card.addView(
                phoneRow
            )
        }

        // ==========================================
        // EMAIL
        // ==========================================

        if (email.isNotEmpty()) {

            val emailRow =
                createInfoRow(
                    R.drawable.ic_email,
                    email
                )

            // FIX:
            // Create new LayoutParams instead of
            // casting a null layoutParams object.

            val emailParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            emailParams.setMargins(
                0,
                dp(8),
                0,
                0
            )

            emailRow.layoutParams =
                emailParams

            card.addView(
                emailRow
            )
        }

        // ==========================================
        // EMERGENCY ALERT STATUS
        // ==========================================

        val alertStatus =
            TextView(this)

        alertStatus.text =
            if (emergencyAlerts) {

                "✓  Emergency alerts enabled"

            } else {

                "○  Emergency alerts disabled"
            }

        if (emergencyAlerts) {

            alertStatus.setTextColor(
                Color.parseColor("#18864B")
            )

        } else {

            alertStatus.setTextColor(
                Color.parseColor("#667085")
            )
        }

        alertStatus.textSize =
            12f

        alertStatus.setTypeface(
            null,
            Typeface.BOLD
        )

        val alertParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        alertParams.setMargins(
            0,
            dp(12),
            0,
            0
        )

        alertStatus.layoutParams =
            alertParams

        card.addView(
            alertStatus
        )

        // ==========================================
        // BUTTON ROW
        // ==========================================

        val buttonRow =
            LinearLayout(this)

        buttonRow.orientation =
            LinearLayout.HORIZONTAL

        buttonRow.gravity =
            Gravity.CENTER_VERTICAL

        val buttonRowParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )

        buttonRowParams.setMargins(
            0,
            dp(14),
            0,
            0
        )

        buttonRow.layoutParams =
            buttonRowParams

        // ==========================================
        // CALL BUTTON
        // ==========================================

        val callButton =
            Button(this)

        callButton.text =
            "Call"

        callButton.isAllCaps =
            false

        callButton.textSize =
            13f

        callButton.setTextColor(
            Color.WHITE
        )

        callButton.setTypeface(
            null,
            Typeface.BOLD
        )

        callButton.background =
            createRoundedBackground(
                Color.parseColor("#234572"),
                Color.TRANSPARENT,
                24
            )

        callButton.setOnClickListener {

            if (phone.isNotEmpty()) {

                callContact(phone)

            } else {

                Toast.makeText(
                    this,
                    "No phone number available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val callParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )

        callParams.setMargins(
            0,
            0,
            dp(5),
            0
        )

        callButton.layoutParams =
            callParams

        buttonRow.addView(
            callButton
        )

        // ==========================================
        // DELETE BUTTON
        // ==========================================

        val deleteButton =
            Button(this)

        deleteButton.text =
            "Delete"

        deleteButton.isAllCaps =
            false

        deleteButton.textSize =
            13f

        deleteButton.setTextColor(
            Color.WHITE
        )

        deleteButton.setTypeface(
            null,
            Typeface.BOLD
        )

        deleteButton.background =
            createRoundedBackground(
                Color.parseColor("#D32F2F"),
                Color.TRANSPARENT,
                24
            )

        deleteButton.setOnClickListener {

            deleteContact(contactId)
        }

        val deleteParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )

        deleteParams.setMargins(
            dp(5),
            0,
            0,
            0
        )

        deleteButton.layoutParams =
            deleteParams

        buttonRow.addView(
            deleteButton
        )

        card.addView(
            buttonRow
        )

        // ==========================================
        // ADD CARD TO LIST
        // ==========================================

        contactsList.addView(
            card
        )
    }

    // ==================================================
    // CREATE INFORMATION ROW
    // ==================================================

    private fun createInfoRow(
        iconResource: Int,
        text: String
    ): LinearLayout {

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        // ==========================================
        // ICON
        // ==========================================

        val icon =
            ImageView(this)

        icon.layoutParams =
            LinearLayout.LayoutParams(
                dp(26),
                dp(26)
            )

        icon.setImageResource(
            iconResource
        )

        icon.contentDescription =
            "Contact information"

        row.addView(
            icon
        )

        // ==========================================
        // TEXT
        // ==========================================

        val textView =
            TextView(this)

        textView.text =
            text

        textView.setTextColor(
            Color.parseColor("#344054")
        )

        textView.textSize =
            13f

        val textParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        textParams.setMargins(
            dp(10),
            0,
            0,
            0
        )

        textView.layoutParams =
            textParams

        row.addView(
            textView
        )

        return row
    }

    // ==================================================
    // CALL CONTACT
    // ==================================================

    private fun callContact(
        phone: String
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse(
                        "tel:$phone"
                    )
                )

            startActivity(
                intent
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open the phone dialler.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==================================================
    // DELETE CONTACT
    // ==================================================

    private fun deleteContact(
        contactId: String
    ) {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ==========================================
        // VERIFY CONTACT OWNERSHIP
        // ==========================================

        db.collection(
            "TrustedContacts"
        )
            .document(
                contactId
            )
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Contact no longer exists.",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadTrustedContacts()

                    return@addOnSuccessListener
                }

                val contactUserId =
                    document.getString(
                        "userId"
                    )

                // ==================================
                // SECURITY CHECK
                // ==================================

                if (
                    contactUserId !=
                    currentUser.uid
                ) {

                    Toast.makeText(
                        this,
                        "You cannot delete this contact.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // ==================================
                // DELETE
                // ==================================

                db.collection(
                    "TrustedContacts"
                )
                    .document(
                        contactId
                    )
                    .delete()
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Trusted contact deleted.",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadTrustedContacts()
                    }
                    .addOnFailureListener {

                        Toast.makeText(
                            this,
                            "Unable to delete contact.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to verify the contact.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==================================================
    // ROUNDED BACKGROUND
    // ==================================================

    private fun createRoundedBackground(
        fillColor: Int,
        strokeColor: Int,
        radiusDp: Int
    ): android.graphics.drawable.GradientDrawable {

        val drawable =
            android.graphics.drawable.GradientDrawable()

        drawable.shape =
            android.graphics.drawable.GradientDrawable.RECTANGLE

        drawable.setColor(
            fillColor
        )

        if (
            strokeColor !=
            Color.TRANSPARENT
        ) {

            drawable.setStroke(
                dp(1),
                strokeColor
            )
        }

        drawable.cornerRadius =
            dp(radiusDp).toFloat()

        return drawable
    }

    // ==================================================
    // DP CONVERSION
    // ==================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}