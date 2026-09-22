package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmergencyAssistanceActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvSelectedType: TextView

    private lateinit var btnMedical: Button
    private lateinit var btnInjury: Button
    private lateinit var btnAccident: Button
    private lateinit var btnFire: Button
    private lateinit var btnSecurity: Button
    private lateinit var btnHarassment: Button
    private lateinit var btnOther: Button
    private lateinit var btnContinue: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedAssistanceType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_emergency_assistance)

        // ================================
        // CONNECT CONTROLS
        // ================================

        tvBack = findViewById(R.id.tvBack)
        tvSelectedType = findViewById(R.id.tvSelectedType)

        btnMedical = findViewById(R.id.btnMedical)
        btnInjury = findViewById(R.id.btnInjury)
        btnAccident = findViewById(R.id.btnAccident)
        btnFire = findViewById(R.id.btnFire)
        btnSecurity = findViewById(R.id.btnSecurity)
        btnHarassment = findViewById(R.id.btnHarassment)
        btnOther = findViewById(R.id.btnOther)
        btnContinue = findViewById(R.id.btnContinue)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ================================
        // BACK ARROW
        // ================================

        tvBack.setOnClickListener {
            finish()
        }

        // ================================
        // ASSISTANCE OPTIONS
        // ================================

        btnMedical.setOnClickListener {
            selectAssistance("Medical Emergency")
        }

        btnInjury.setOnClickListener {
            selectAssistance("Injury / Fall")
        }

        btnAccident.setOnClickListener {
            selectAssistance("Accident")
        }

        btnFire.setOnClickListener {
            selectAssistance("Fire")
        }

        btnSecurity.setOnClickListener {
            selectAssistance("Security Emergency")
        }

        btnHarassment.setOnClickListener {
            selectAssistance("Harassment")
        }

        btnOther.setOnClickListener {
            selectAssistance("Other")
        }

        // ================================
        // CONTINUE
        // ================================

        btnContinue.setOnClickListener {

            if (selectedAssistanceType == null) {

                Toast.makeText(
                    this,
                    "Please select the type of assistance you need.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            createEmergencyRequest()
        }
    }

    // ============================================================
    // SELECT ASSISTANCE
    // ============================================================

    private fun selectAssistance(type: String) {

        selectedAssistanceType = type

        tvSelectedType.text =
            "Selected: $type"

        Toast.makeText(
            this,
            "$type selected.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ============================================================
    // CREATE EMERGENCY REQUEST
    // ============================================================

    private fun createEmergencyRequest() {

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

        val assistanceType =
            selectedAssistanceType ?: return

        val emergencyData: Map<String, Any> =
            mapOf(

                "userId" to currentUser.uid,

                "assistanceType" to assistanceType,

                "status" to "Requested",

                "createdAt" to System.currentTimeMillis()
            )

        btnContinue.isEnabled = false
        btnContinue.text = "Sending Request..."

        db.collection("EmergencyRequests")
            .add(emergencyData)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Emergency assistance request sent.",
                    Toast.LENGTH_LONG
                ).show()

                val intent =
                    Intent(
                        this,
                        EmergencyRequestActivity::class.java
                    )

                intent.putExtra(
                    "assistanceType",
                    assistanceType
                )

                startActivity(intent)

                finish()
            }
            .addOnFailureListener {

                btnContinue.isEnabled = true
                btnContinue.text = "Continue"

                Toast.makeText(
                    this,
                    "Unable to send emergency request. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}