package com.example.safezone

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ActiveSafetyCheckActivity : AppCompatActivity() {

    // ==========================================
    // UI
    // ==========================================

    private lateinit var tvBack: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTrustedContact: TextView
    private lateinit var tvLocationSharing: TextView

    private lateinit var btnImSafe: Button
    private lateinit var btnCancelCheckIn: Button

    // ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var db: FirebaseFirestore

    // ==========================================
    // DATA
    // ==========================================

    private var checkInId = ""
    private var expiresAt = 0L

    private var countDownTimer: CountDownTimer? = null

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_active_safety_check
        )

        // ==========================================
        // CONNECT XML
        // ==========================================

        tvBack =
            findViewById(R.id.tvBack)

        tvCountdown =
            findViewById(R.id.tvCountdown)

        tvStatus =
            findViewById(R.id.tvStatus)

        tvTrustedContact =
            findViewById(R.id.tvTrustedContact)

        tvLocationSharing =
            findViewById(R.id.tvLocationSharing)

        btnImSafe =
            findViewById(R.id.btnImSafe)

        btnCancelCheckIn =
            findViewById(R.id.btnCancelCheckIn)

        // ==========================================
        // FIREBASE
        // ==========================================

        db =
            FirebaseFirestore.getInstance()

        // ==========================================
        // GET DATA FROM SAFETY CHECK-IN
        // ==========================================

        checkInId =
            intent.getStringExtra(
                "checkInId"
            ) ?: ""

        expiresAt =
            intent.getLongExtra(
                "expiresAt",
                0L
            )

        val trustedContact =
            intent.getStringExtra(
                "trustedContact"
            ) ?: "Not configured"

        val locationSharing =
            intent.getBooleanExtra(
                "locationSharing",
                false
            )

        // ==========================================
        // DISPLAY TRUSTED CONTACT
        // ==========================================

        tvTrustedContact.text =
            "Trusted Contact: $trustedContact"

        // ==========================================
        // DISPLAY LOCATION STATUS
        // ==========================================

        tvLocationSharing.text =
            if (locationSharing) {

                "Location Sharing: ON"

            } else {

                "Location Sharing: OFF"
            }

        // ==========================================
        // BACK
        // ==========================================

        tvBack.setOnClickListener {

            Toast.makeText(
                this,
                "Your safety check is still active.",
                Toast.LENGTH_SHORT
            ).show()

            // Keep the safety check active.
            // Return to the previous screen.
            finish()
        }

        // ==========================================
        // START COUNTDOWN
        // ==========================================

        startCountdown()

        // ==========================================
        // I'M SAFE
        // ==========================================

        btnImSafe.setOnClickListener {

            completeSafetyCheck()
        }

        // ==========================================
        // CANCEL CHECK-IN
        // ==========================================

        btnCancelCheckIn.setOnClickListener {

            cancelSafetyCheck()
        }
    }

    // ==================================================
    // COUNTDOWN
    // ==================================================

    private fun startCountdown() {

        if (expiresAt <= 0L) {

            tvCountdown.text =
                "--:--:--"

            tvStatus.text =
                "Unable to determine the check-in time."

            return
        }

        val remaining =
            expiresAt -
                    System.currentTimeMillis()

        if (remaining <= 0L) {

            expireSafetyCheck()

            return
        }

        countDownTimer =
            object : CountDownTimer(
                remaining,
                1000
            ) {

                override fun onTick(
                    millisUntilFinished: Long
                ) {

                    val totalSeconds =
                        millisUntilFinished / 1000

                    val hours =
                        totalSeconds / 3600

                    val minutes =
                        (totalSeconds % 3600) / 60

                    val seconds =
                        totalSeconds % 60

                    tvCountdown.text =
                        String.format(
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

    // ==================================================
    // I'M SAFE
    // ==================================================

    private fun completeSafetyCheck() {

        if (checkInId.isEmpty()) {

            Toast.makeText(
                this,
                "Safety check information is missing.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }

        countDownTimer?.cancel()

        btnImSafe.isEnabled = false
        btnCancelCheckIn.isEnabled = false

        db.collection("SafetyCheckIns")
            .document(checkInId)
            .update(
                mapOf(
                    "status" to "Completed",
                    "completedAt" to
                            System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {

                tvCountdown.text =
                    "✓"

                tvStatus.text =
                    "You confirmed that you are safe."

                Toast.makeText(
                    this,
                    "Safety check completed.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {

                btnImSafe.isEnabled = true
                btnCancelCheckIn.isEnabled = true

                Toast.makeText(
                    this,
                    "Unable to update the safety check.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==================================================
    // CANCEL
    // ==================================================

    private fun cancelSafetyCheck() {

        if (checkInId.isEmpty()) {

            finish()

            return
        }

        countDownTimer?.cancel()

        btnImSafe.isEnabled = false
        btnCancelCheckIn.isEnabled = false

        db.collection("SafetyCheckIns")
            .document(checkInId)
            .update(
                mapOf(
                    "status" to "Cancelled",
                    "cancelledAt" to
                            System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Safety check cancelled.",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
            .addOnFailureListener {

                btnImSafe.isEnabled = true
                btnCancelCheckIn.isEnabled = true

                Toast.makeText(
                    this,
                    "Unable to cancel the safety check.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==================================================
    // TIMER EXPIRED
    // ==================================================

    private fun expireSafetyCheck() {

        tvCountdown.text =
            "00:00:00"

        tvStatus.text =
            "Your safety check has expired."

        btnImSafe.isEnabled = false
        btnCancelCheckIn.isEnabled = false

        if (checkInId.isNotEmpty()) {

            db.collection("SafetyCheckIns")
                .document(checkInId)
                .update(
                    mapOf(
                        "status" to "Expired",
                        "expiredAt" to
                                System.currentTimeMillis()
                    )
                )
        }

        Toast.makeText(
            this,
            "Your safety check has expired.",
            Toast.LENGTH_LONG
        ).show()
    }

    // ==================================================
    // CLEAN UP TIMER
    // ==================================================

    override fun onDestroy() {

        countDownTimer?.cancel()

        super.onDestroy()
    }
}