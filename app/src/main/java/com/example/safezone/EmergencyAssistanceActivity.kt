package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.EmergencyRequestRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmergencyAssistanceActivity : AppCompatActivity() {

    // HEADER
    private lateinit var tvBack: TextView

    // SELECTED ASSISTANCE
    private lateinit var tvSelectedType: TextView

    // EMERGENCY BUTTONS
    private lateinit var btnMedical: Button
    private lateinit var btnInjury: Button
    private lateinit var btnAccident: Button
    private lateinit var btnFire: Button
    private lateinit var btnSecurity: Button
    private lateinit var btnHarassment: Button
    private lateinit var btnOther: Button
    private lateinit var btnContinue: Button

    // SELECTED EMERGENCY
    private var selectedAssistanceType: String? = null
    private var selectedAssistanceLabelRes: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_assistance)

        initializeViews()
        setupEmergencyButtons()
    }

    // INITIALIZE VIEWS
    private fun initializeViews() {
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
    }

    // EMERGENCY BUTTON LISTENERS
    private fun setupEmergencyButtons() {

        tvBack.setOnClickListener {
            finish()
        }

        btnMedical.setOnClickListener {
            selectAssistanceType(
                "medical",
                R.string.emergency_type_medical
            )
        }

        btnInjury.setOnClickListener {
            selectAssistanceType(
                "injury",
                R.string.emergency_type_injury
            )
        }

        btnAccident.setOnClickListener {
            selectAssistanceType(
                "accident",
                R.string.emergency_type_accident
            )
        }

        btnFire.setOnClickListener {
            selectAssistanceType(
                "fire",
                R.string.emergency_type_fire
            )
        }

        btnSecurity.setOnClickListener {
            selectAssistanceType(
                "security",
                R.string.emergency_type_security
            )
        }

        btnHarassment.setOnClickListener {
            selectAssistanceType(
                "harassment",
                R.string.emergency_type_harassment
            )
        }

        btnOther.setOnClickListener {
            selectAssistanceType(
                "other",
                R.string.emergency_type_other
            )
        }

        btnContinue.setOnClickListener {
            saveEmergencyRequest()
        }
    }

    // SELECT EMERGENCY TYPE
    private fun selectAssistanceType(
        type: String,
        labelResId: Int
    ) {
        selectedAssistanceType = type
        selectedAssistanceLabelRes = labelResId

        tvSelectedType.text = getString(
            R.string.emergency_type_selected,
            getString(labelResId)
        )
    }

    // SAVE EMERGENCY REQUEST
    private fun saveEmergencyRequest() {

        val assistanceType = selectedAssistanceType

        if (assistanceType.isNullOrBlank()) {
            showMessage(R.string.select_emergency_type_error)
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId.isNullOrBlank()) {
            showMessage(
                R.string.sign_in_emergency_required,
                Toast.LENGTH_LONG
            )
            return
        }

        btnContinue.isEnabled = false
        btnContinue.text = getString(R.string.saving_emergency_request)

        lifecycleScope.launch {
            try {

                // SAVE LOCALLY FIRST
                val requestId = withContext(Dispatchers.IO) {
                    EmergencyRequestRepository(applicationContext)
                        .saveRequest(userId, assistanceType)
                }

                showMessage(
                    R.string.emergency_request_saved_pending,
                    Toast.LENGTH_LONG
                )

                // OPEN REQUEST DETAILS
                val intent = Intent(
                    this@EmergencyAssistanceActivity,
                    EmergencyRequestActivity::class.java
                ).apply {
                    putExtra("requestId", requestId)
                    putExtra("assistanceType", assistanceType)
                    putExtra("syncStatus", "Pending Sync")
                }

                startActivity(intent)
                finish()

            } catch (e: Exception) {

                btnContinue.isEnabled = true
                btnContinue.text = getString(R.string.continue_with_arrow)

                showMessage(
                    R.string.emergency_request_save_failed,
                    Toast.LENGTH_LONG
                )
            }
        }
    }

    // DISPLAY TRANSLATED MESSAGES
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
}