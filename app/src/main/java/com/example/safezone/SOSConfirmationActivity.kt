package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SOSConfirmationActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTrustedContacts: TextView
    private lateinit var btnActivateSOS: Button
    private lateinit var btnCancelSOS: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sos_confirmation)

        // --------------------------------------------------
        // CONNECT XML CONTROLS
        // --------------------------------------------------

        tvBack = findViewById(R.id.tvBack)
        tvUserName = findViewById(R.id.tvUserName)
        tvLocation = findViewById(R.id.tvLocation)
        tvTrustedContacts = findViewById(R.id.tvTrustedContacts)

        btnActivateSOS = findViewById(R.id.btnActivateSOS)
        btnCancelSOS = findViewById(R.id.btnCancelSOS)

        // --------------------------------------------------
        // FIREBASE
        // --------------------------------------------------

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --------------------------------------------------
        // BACK ARROW
        // --------------------------------------------------

        tvBack.setOnClickListener {
            finish()
        }

        // --------------------------------------------------
        // CHECK LOGGED-IN USER
        // --------------------------------------------------

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        // --------------------------------------------------
        // LOAD REGISTERED USER INFORMATION
        // --------------------------------------------------

        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val fullName =
                        document.getString("fullName")

                    if (!fullName.isNullOrEmpty()) {

                        tvUserName.text =
                            "Your Name: $fullName"

                    } else {

                        tvUserName.text =
                            "Your Name: SafeZone User"
                    }

                } else {

                    tvUserName.text =
                        "Your Name: SafeZone User"
                }
            }
            .addOnFailureListener {

                tvUserName.text =
                    "Your Name: Unable to load"

                Toast.makeText(
                    this,
                    "Unable to load your profile.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // --------------------------------------------------
        // LOCATION
        // --------------------------------------------------

        tvLocation.text =
            "Current Location: Location services not connected"

        // --------------------------------------------------
        // TRUSTED CONTACTS
        // --------------------------------------------------

        tvTrustedContacts.text =
            "Trusted Contacts to be notified: 0"

        // --------------------------------------------------
        // ACTIVATE SOS
        // --------------------------------------------------

        btnActivateSOS.setOnClickListener {

            val intent = Intent(
                this,
                SOSActiveActivity::class.java
            )

            startActivity(intent)

            finish()
        }

        // --------------------------------------------------
        // CANCEL
        // --------------------------------------------------

        btnCancelSOS.setOnClickListener {
            finish()
        }
    }
}