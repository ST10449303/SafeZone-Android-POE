package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CampusHelpSupportActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView

    private lateinit var rowAccountHelp: LinearLayout
    private lateinit var rowSafetyHelp: LinearLayout
    private lateinit var rowEmergencyHelp: LinearLayout
    private lateinit var rowContactSupport: LinearLayout
    private lateinit var rowFaq: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Open the Help & Support XML
        setContentView(R.layout.activity_campus_help_support)

        // =====================================================
        // FIND VIEWS
        // =====================================================

        tvBack = findViewById(R.id.tvBack)

        rowAccountHelp = findViewById(R.id.rowAccountHelp)
        rowSafetyHelp = findViewById(R.id.rowSafetyHelp)
        rowEmergencyHelp = findViewById(R.id.rowEmergencyHelp)
        rowContactSupport = findViewById(R.id.rowContactSupport)
        rowFaq = findViewById(R.id.rowFaq)


        // =====================================================
        // BACK
        // =====================================================

        tvBack.setOnClickListener {
            finish()
        }


        // =====================================================
        // ACCOUNT HELP
        // =====================================================

        rowAccountHelp.setOnClickListener {

            Toast.makeText(
                this,
                "For account help, check your login details or use the password reset option.",
                Toast.LENGTH_LONG
            ).show()
        }


        // =====================================================
        // SAFETY HELP
        // =====================================================

        rowSafetyHelp.setOnClickListener {

            Toast.makeText(
                this,
                "Use Report Incident, Emergency Assistance, or the Campus Safety Map when you need help.",
                Toast.LENGTH_LONG
            ).show()
        }


        // =====================================================
        // EMERGENCY HELP
        // =====================================================

        rowEmergencyHelp.setOnClickListener {

            Toast.makeText(
                this,
                "For an emergency, use Campus Emergency or call 112.",
                Toast.LENGTH_LONG
            ).show()
        }


        // =====================================================
        // CONTACT SUPPORT
        // =====================================================

        rowContactSupport.setOnClickListener {

            try {

                val emailIntent = Intent(Intent.ACTION_SENDTO)

                emailIntent.data =
                    Uri.parse("mailto:support@safezone.app")

                emailIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "SafeZone Campus Support"
                )

                startActivity(emailIntent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "No email application is available on this device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


        // =====================================================
        // FAQ
        // =====================================================

        rowFaq.setOnClickListener {

            Toast.makeText(
                this,
                "Frequently Asked Questions will be available here.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}