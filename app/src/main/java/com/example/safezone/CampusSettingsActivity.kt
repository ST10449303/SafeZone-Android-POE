package com.example.safezone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth

class CampusSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvSelectedLanguage: TextView

    companion object {
        private const val PREFERENCES_NAME = "SafeZonePreferences"
        private const val LANGUAGE_KEY = "app_language"
        private const val DEFAULT_LANGUAGE = "en"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_settings)

        auth = FirebaseAuth.getInstance()

        tvSelectedLanguage = findViewById(R.id.tvSelectedLanguage)

        updateSelectedLanguageLabel()
        setupButtons()
    }

    private fun setupButtons() {

        // Back button
        findViewById<View>(R.id.tvBack).setOnClickListener {
            finish()
        }

        // Account settings
        findViewById<View>(R.id.rowEditProfile).setOnClickListener {
            openActivity(
                EditProfileActivity::class.java,
                R.string.campus_settings_edit_profile_unavailable
            )
        }

        findViewById<View>(R.id.rowChangePassword).setOnClickListener {
            openActivity(
                CampusChangePasswordActivity::class.java,
                R.string.campus_settings_change_password_unavailable
            )
        }

        findViewById<View>(R.id.rowLanguage).setOnClickListener {
            showLanguageDialog()
        }

        findViewById<View>(R.id.rowLogout).setOnClickListener {
            logoutUser()
        }

        // Campus safety
        findViewById<View>(R.id.rowCampusAlerts).setOnClickListener {
            openActivity(
                CampusAlertsActivity::class.java,
                R.string.campus_settings_alerts_unavailable
            )
        }

        findViewById<View>(R.id.rowEmergencyPreferences).setOnClickListener {
            openActivity(
                CampusEmergencyPreferencesActivity::class.java,
                R.string.campus_settings_emergency_unavailable
            )
        }

        findViewById<View>(R.id.rowLocationSharing).setOnClickListener {
            openActivity(
                CampusLocationSharingActivity::class.java,
                R.string.campus_settings_location_unavailable
            )
        }

        // Support
        findViewById<View>(R.id.rowHelpSupport).setOnClickListener {
            openActivity(
                CampusHelpSupportActivity::class.java,
                R.string.campus_settings_help_unavailable
            )
        }

        findViewById<View>(R.id.rowAboutSafeZone).setOnClickListener {
            openActivity(
                CampusAboutSafeZoneActivity::class.java,
                R.string.campus_settings_about_unavailable
            )
        }

        // Bottom navigation
        findViewById<View>(R.id.navHome).setOnClickListener {
            navigateTo(CampusHomeActivity::class.java)
        }

        findViewById<View>(R.id.navMap).setOnClickListener {
            openCampusMap()
        }

        findViewById<View>(R.id.navReports).setOnClickListener {
            openActivity(
                CampusReportsActivity::class.java,
                R.string.campus_settings_reports_unavailable
            )
        }

        findViewById<View>(R.id.navNotifications).setOnClickListener {
            openActivity(
                CampusAlertsActivity::class.java,
                R.string.campus_settings_alerts_unavailable
            )
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            openActivity(
                CampusProfileActivity::class.java,
                R.string.campus_settings_profile_unavailable
            )
        }
    }

    /**
     * Displays the language selection dialog.
     *
     * The same preference file and key are used by the Personal panel:
     * SafeZonePreferences -> app_language
     *
     * en = English
     * zu = isiZulu
     * tn = Setswana
     */
    private fun showLanguageDialog() {

        val languages = arrayOf(
            "English",
            "isiZulu",
            "Setswana"
        )

        val languageCodes = arrayOf(
            "en",
            "zu",
            "tn"
        )

        val preferences = getSharedPreferences(
            PREFERENCES_NAME,
            MODE_PRIVATE
        )

        val currentLanguage = preferences.getString(
            LANGUAGE_KEY,
            DEFAULT_LANGUAGE
        ) ?: DEFAULT_LANGUAGE

        val checkedIndex = languageCodes
            .indexOf(currentLanguage)
            .coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.campus_settings_choose_language)
            .setSingleChoiceItems(
                languages,
                checkedIndex
            ) { dialog, selectedIndex ->

                val selectedLanguage = languageCodes[selectedIndex]

                if (selectedLanguage != currentLanguage) {

                    // Save the shared language preference.
                    preferences.edit()
                        .putString(LANGUAGE_KEY, selectedLanguage)
                        .apply()

                    // Apply the language globally to AppCompat activities.
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(selectedLanguage)
                    )
                }

                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.campus_settings_cancel,
                null
            )
            .show()
    }

    /**
     * Updates the language label in the Campus Settings screen.
     */
    private fun updateSelectedLanguageLabel() {

        val preferences = getSharedPreferences(
            PREFERENCES_NAME,
            MODE_PRIVATE
        )

        val language = preferences.getString(
            LANGUAGE_KEY,
            DEFAULT_LANGUAGE
        ) ?: DEFAULT_LANGUAGE

        tvSelectedLanguage.text = when (language) {
            "zu" -> "isiZulu"
            "tn" -> "Setswana"
            else -> "English"
        }
    }

    /**
     * Opens another activity and displays a translated error message
     * if the activity cannot be opened.
     */
    private fun openActivity(
        activityClass: Class<*>,
        errorMessageResId: Int
    ) {
        try {
            startActivity(
                Intent(this, activityClass)
            )
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(errorMessageResId),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Navigates to another Campus panel screen.
     */
    private fun navigateTo(activityClass: Class<*>) {

        val intent = Intent(
            this,
            activityClass
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        startActivity(intent)
        finish()
    }

    /**
     * Signs out the current Firebase user and returns to panel selection.
     */
    private fun logoutUser() {

        auth.signOut()

        Toast.makeText(
            this,
            R.string.campus_settings_logout_success,
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(
            this,
            ChoosePanelActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    /**
     * Opens the device map application.
     */
    private fun openCampusMap() {

        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=Pretoria Campus")
            )

            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(
                this,
                R.string.campus_settings_map_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}