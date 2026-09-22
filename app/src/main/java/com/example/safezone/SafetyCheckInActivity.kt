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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SafetyCheckInActivity : AppCompatActivity() {

    // ==========================================
    // UI
    // ==========================================

    private lateinit var tvBack: TextView
    private lateinit var tvSelectedDuration: TextView

    private lateinit var btn30Minutes: Button
    private lateinit var btn1Hour: Button
    private lateinit var btn2Hours: Button
    private lateinit var btnCustom: Button

    private lateinit var spTrustedContact: Spinner
    private lateinit var switchLocation: SwitchCompat
    private lateinit var btnStartSafetyCheck: Button

    // ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // ==========================================
    // DATA
    // ==========================================

    private var durationMinutes = 30

    private val trustedContactNames =
        ArrayList<String>()

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_safety_check_in
        )

        // ==========================================
        // INITIALISE FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ==========================================
        // CONNECT XML
        // ==========================================

        tvBack =
            findViewById(R.id.tvBack)

        tvSelectedDuration =
            findViewById(R.id.tvSelectedDuration)

        btn30Minutes =
            findViewById(R.id.btn30Minutes)

        btn1Hour =
            findViewById(R.id.btn1Hour)

        btn2Hours =
            findViewById(R.id.btn2Hours)

        btnCustom =
            findViewById(R.id.btnCustom)

        spTrustedContact =
            findViewById(R.id.spTrustedContact)

        switchLocation =
            findViewById(R.id.switchLocation)

        btnStartSafetyCheck =
            findViewById(R.id.btnStartSafetyCheck)

        // ==========================================
        // CHECK USER
        // ==========================================

        if (auth.currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
            ).show()

            goToLogin()

            return
        }

        // ==========================================
        // BACK BUTTON
        // ==========================================

        tvBack.setOnClickListener {

            finish()
        }

        // ==========================================
        // LOAD TRUSTED CONTACTS
        // ==========================================

        loadTrustedContacts()

        // ==========================================
        // DURATION - 30 MINUTES
        // ==========================================

        btn30Minutes.setOnClickListener {

            durationMinutes = 30

            tvSelectedDuration.text =
                "Duration: 30 minutes"
        }

        // ==========================================
        // DURATION - 1 HOUR
        // ==========================================

        btn1Hour.setOnClickListener {

            durationMinutes = 60

            tvSelectedDuration.text =
                "Duration: 1 hour"
        }

        // ==========================================
        // DURATION - 2 HOURS
        // ==========================================

        btn2Hours.setOnClickListener {

            durationMinutes = 120

            tvSelectedDuration.text =
                "Duration: 2 hours"
        }

        // ==========================================
        // CUSTOM DURATION
        // ==========================================

        btnCustom.setOnClickListener {

            showCustomDurationDialog()
        }

        // ==========================================
        // START SAFETY CHECK
        // ==========================================

        btnStartSafetyCheck.setOnClickListener {

            startSafetyCheck()
        }
    }

    // ==================================================
    // LOAD REAL TRUSTED CONTACTS
    // ==================================================

    private fun loadTrustedContacts() {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {
            return
        }

        trustedContactNames.clear()

        trustedContactNames.add(
            "Select Trusted Contact"
        )

        db.collection("TrustedContacts")
            .whereEqualTo(
                "userId",
                currentUser.uid
            )
            .get()
            .addOnSuccessListener { documents ->

                for (document in documents) {

                    val name =
                        document.getString("fullName")

                    if (!name.isNullOrBlank()) {

                        trustedContactNames.add(
                            name
                        )
                    }
                }

                val adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        trustedContactNames
                    )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spTrustedContact.adapter =
                    adapter

                // ==========================================
                // NO CONTACTS
                // ==========================================

                if (trustedContactNames.size == 1) {

                    Toast.makeText(
                        this,
                        "No trusted contacts found. Please add one first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to load trusted contacts.",
                    Toast.LENGTH_LONG
                ).show()

                trustedContactNames.clear()

                trustedContactNames.add(
                    "No trusted contacts available"
                )

                val adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        trustedContactNames
                    )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spTrustedContact.adapter =
                    adapter
            }
    }

    // ==================================================
    // CUSTOM DURATION
    // ==================================================

    private fun showCustomDurationDialog() {

        val input =
            EditText(this)

        input.hint =
            "Enter minutes"

        input.inputType =
            InputType.TYPE_CLASS_NUMBER

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Custom Check-In Duration"
                )
                .setMessage(
                    "Enter the duration in minutes."
                )
                .setView(input)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Set"
                ) { _, _ ->

                    val minutes =
                        input.text
                            .toString()
                            .toIntOrNull()

                    if (
                        minutes == null ||
                        minutes < 5 ||
                        minutes > 1440
                    ) {

                        Toast.makeText(
                            this,
                            "Enter between 5 and 1440 minutes.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@setPositiveButton
                    }

                    durationMinutes =
                        minutes

                    tvSelectedDuration.text =
                        "Duration: $minutes minutes"
                }
                .create()

        dialog.show()
    }

    // ==================================================
    // START SAFETY CHECK
    // ==================================================

    private fun startSafetyCheck() {

        val currentUser =
            auth.currentUser

        // ==========================================
        // USER CHECK
        // ==========================================

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
            ).show()

            goToLogin()

            return
        }

        // ==========================================
        // CONTACT CHECK
        // ==========================================

        if (trustedContactNames.size <= 1) {

            Toast.makeText(
                this,
                "Please add a trusted contact before starting a safety check.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val contactPosition =
            spTrustedContact.selectedItemPosition

        if (contactPosition <= 0) {

            Toast.makeText(
                this,
                "Please select a trusted contact.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val trustedContact =
            spTrustedContact.selectedItem
                .toString()

        // ==========================================
        // LOCATION
        // ==========================================

        val locationSharing =
            switchLocation.isChecked

        // ==========================================
        // TIME
        // ==========================================

        val startedAt =
            System.currentTimeMillis()

        val durationMillis =
            durationMinutes * 60 * 1000L

        val expiresAt =
            startedAt + durationMillis

        // ==========================================
        // FIRESTORE DATA
        // ==========================================

        val checkInData =
            hashMapOf<String, Any>(

                "userId" to currentUser.uid,

                "trustedContact" to trustedContact,

                "durationMinutes" to durationMinutes,

                "locationSharing" to locationSharing,

                "status" to "Active",

                "startedAt" to startedAt,

                "expiresAt" to expiresAt
            )

        // ==========================================
        // BUTTON STATE
        // ==========================================

        btnStartSafetyCheck.isEnabled =
            false

        btnStartSafetyCheck.text =
            "Starting..."

        // ==========================================
        // SAVE CHECK-IN
        // ==========================================

        db.collection("SafetyCheckIns")
            .add(checkInData)
            .addOnSuccessListener { documentReference ->

                Toast.makeText(
                    this,
                    "Safety Check-In started.",
                    Toast.LENGTH_SHORT
                ).show()

                // ==========================================
                // OPEN ACTIVE SAFETY CHECK
                // ==========================================

                val intent =
                    Intent(
                        this@SafetyCheckInActivity,
                        ActiveSafetyCheckActivity::class.java
                    )

                intent.putExtra(
                    "checkInId",
                    documentReference.id
                )

                intent.putExtra(
                    "expiresAt",
                    expiresAt
                )

                intent.putExtra(
                    "trustedContact",
                    trustedContact
                )

                intent.putExtra(
                    "locationSharing",
                    locationSharing
                )

                startActivity(intent)

                finish()
            }
            .addOnFailureListener {

                btnStartSafetyCheck.isEnabled =
                    true

                btnStartSafetyCheck.text =
                    "Start Safety Check"

                Toast.makeText(
                    this,
                    "Unable to start safety check. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==================================================
    // GO TO LOGIN
    // ==================================================

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