package com.example.safezone

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.SafetyCheckInRepository
import com.example.safezone.sync.SafetyCheckInSyncScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale

class ActiveSafetyCheckActivity : AppCompatActivity() {

    // UI
    private lateinit var tvBack: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTrustedContact: TextView
    private lateinit var tvLocationSharing: TextView

    private lateinit var btnImSafe: Button
    private lateinit var btnCancelCheckIn: Button

    // Firebase
    private lateinit var db: FirebaseFirestore

    // Check-in data
    private var checkInId = ""
    private var expiresAt = 0L
    private var isLocalCheckIn = false
    private var isUpdating = false
    private var hasEnded = false

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_safety_check)

        // Connect XML
        tvBack = findViewById(R.id.tvBack)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvStatus = findViewById(R.id.tvStatus)
        tvTrustedContact = findViewById(R.id.tvTrustedContact)
        tvLocationSharing = findViewById(R.id.tvLocationSharing)
        btnImSafe = findViewById(R.id.btnImSafe)
        btnCancelCheckIn = findViewById(R.id.btnCancelCheckIn)

        db = FirebaseFirestore.getInstance()

        // Get check-in data
        checkInId = intent.getStringExtra("checkInId") ?: ""
        expiresAt = intent.getLongExtra("expiresAt", 0L)
        isLocalCheckIn = intent.getBooleanExtra("isLocalCheckIn", false)

        val trustedContact =
            intent.getStringExtra("trustedContact")
                ?: getString(R.string.active_check_not_configured)

        val locationSharing =
            intent.getBooleanExtra("locationSharing", false)

        // Display trusted contact
        tvTrustedContact.text = getString(
            R.string.active_check_trusted_contact,
            trustedContact
        )

        // Display location-sharing setting
        tvLocationSharing.text = getString(
            if (locationSharing) {
                R.string.active_check_location_on
            } else {
                R.string.active_check_location_off
            }
        )

        tvStatus.text = getString(
            if (isLocalCheckIn) {
                R.string.active_check_status_pending_sync
            } else {
                R.string.active_check_status_active
            }
        )

        // Back
        tvBack.setOnClickListener {
            showMessage(R.string.active_check_still_active)
            finish()
        }

        // Start countdown
        startCountdown()

        // I'm safe
        btnImSafe.setOnClickListener {
            completeSafetyCheck()
        }

        // Cancel check-in
        btnCancelCheckIn.setOnClickListener {
            cancelSafetyCheck()
        }
    }

    // COUNTDOWN

    private fun startCountdown() {
        if (expiresAt <= 0L) {
            tvCountdown.text = "--:--:--"
            tvStatus.text =
                getString(R.string.active_check_time_unavailable)
            return
        }

        val remaining = expiresAt - System.currentTimeMillis()

        if (remaining <= 0L) {
            expireSafetyCheck()
            return
        }

        countDownTimer = object : CountDownTimer(remaining, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = millisUntilFinished / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                tvCountdown.text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
                )
            }

            override fun onFinish() {
                expireSafetyCheck()
            }
        }.start()
    }

    // I'M SAFE

    private fun completeSafetyCheck() {
        if (checkInId.isEmpty()) {
            showMessage(
                R.string.active_check_information_missing,
                Toast.LENGTH_LONG
            )
            finish()
            return
        }

        if (isUpdating || hasEnded) return

        updateCheckInStatus("Completed")
    }

    // CANCEL CHECK-IN

    private fun cancelSafetyCheck() {
        if (checkInId.isEmpty()) {
            finish()
            return
        }

        if (isUpdating || hasEnded) return

        updateCheckInStatus("Cancelled")
    }

    // TIMER EXPIRED

    private fun expireSafetyCheck() {
        if (hasEnded || isUpdating) return

        tvCountdown.text = "00:00:00"
        updateCheckInStatus("Expired")
    }

    // UPDATE STATUS LOCALLY OR IN FIRESTORE

    private fun updateCheckInStatus(status: String) {
        if (isUpdating || hasEnded) return

        isUpdating = true
        countDownTimer?.cancel()

        btnImSafe.isEnabled = false
        btnCancelCheckIn.isEnabled = false
        tvStatus.text = getString(R.string.active_check_updating)

        if (isLocalCheckIn) {
            // Save the change to RoomDB first.
            // This works even when the phone is offline.
            lifecycleScope.launch {
                try {
                    val database =
                        SafeZoneDatabase.getDatabase(applicationContext)

                    val repository = SafetyCheckInRepository(
                        dao = database.safetyCheckInDao(),
                        firestore = db
                    )

                    repository.updateStatusLocally(
                        localId = checkInId,
                        status = status
                    )

                    // WorkManager will wait for a network connection
                    // if the device is offline.
                    SafetyCheckInSyncScheduler.schedule(applicationContext)

                    handleStatusUpdated(status, true)

                } catch (_: Exception) {
                    isUpdating = false

                    btnImSafe.isEnabled = true
                    btnCancelCheckIn.isEnabled = true

                    tvStatus.text =
                        getString(R.string.active_check_local_update_failed)

                    showMessage(
                        R.string.active_check_local_save_error,
                        Toast.LENGTH_LONG
                    )
                }
            }
        } else {
            // Backward compatibility for older check-ins
            // that were saved directly to Firestore.
            val updateData = mutableMapOf<String, Any>(
                "status" to status
            )

            val timestamp = System.currentTimeMillis()

            when (status) {
                "Completed" -> updateData["completedAt"] = timestamp
                "Cancelled" -> updateData["cancelledAt"] = timestamp
                "Expired" -> updateData["expiredAt"] = timestamp
            }

            db.collection("SafetyCheckIns")
                .document(checkInId)
                .update(updateData)
                .addOnSuccessListener {
                    handleStatusUpdated(status, false)
                }
                .addOnFailureListener {
                    isUpdating = false

                    // Allow another attempt if the update fails.
                    btnImSafe.isEnabled = true
                    btnCancelCheckIn.isEnabled = true

                    tvStatus.text =
                        getString(R.string.active_check_cloud_update_failed)

                    showMessage(
                        R.string.active_check_connection_error,
                        Toast.LENGTH_LONG
                    )
                }
        }
    }

    // HANDLE SUCCESSFUL STATUS CHANGE

    private fun handleStatusUpdated(
        status: String,
        savedLocally: Boolean
    ) {
        isUpdating = false
        hasEnded = true

        btnImSafe.isEnabled = false
        btnCancelCheckIn.isEnabled = false

        when (status) {
            "Completed" -> {
                tvCountdown.text = "✓"
                tvStatus.text =
                    getString(R.string.active_check_completed_status)
            }

            "Cancelled" -> {
                tvStatus.text =
                    getString(R.string.active_check_cancelled_status)
            }

            "Expired" -> {
                tvCountdown.text = "00:00:00"
                tvStatus.text =
                    getString(R.string.active_check_expired_status)
            }
        }

        val messageResId = when {
            savedLocally && status == "Completed" ->
                R.string.active_check_completed_pending

            savedLocally && status == "Cancelled" ->
                R.string.active_check_cancelled_pending

            savedLocally && status == "Expired" ->
                R.string.active_check_expired_pending

            status == "Completed" ->
                R.string.active_check_completed_message

            status == "Cancelled" ->
                R.string.active_check_cancelled_message

            else ->
                R.string.active_check_expired_message
        }

        showMessage(messageResId, Toast.LENGTH_LONG)

        if (status == "Cancelled") {
            finish()
        }
    }

    // SHOW TRANSLATED MESSAGE

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

    // CLEAN UP TIMER

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
