
package com.example.safezone

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var radioLanguage: RadioGroup
    private lateinit var radioEnglish: RadioButton
    private lateinit var radioZulu: RadioButton
    private lateinit var radioTswana: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        radioLanguage = findViewById(R.id.radioLanguage)
        radioEnglish = findViewById(R.id.radioEnglish)
        radioZulu = findViewById(R.id.radioZulu)
        radioTswana = findViewById(R.id.radioTswana)

        val saveButton: Button = findViewById(R.id.btnSaveLanguage)
        val backButton: TextView = findViewById(R.id.btnBack)

        backButton.setOnClickListener {
            finish()
        }

        // Select the currently saved language.
        when (getSavedLanguage()) {
            "zu" -> radioZulu.isChecked = true
            "tn" -> radioTswana.isChecked = true
            else -> radioEnglish.isChecked = true
        }

        saveButton.setOnClickListener {
            val selectedLanguage = when (radioLanguage.checkedRadioButtonId) {
                R.id.radioZulu -> "zu"
                R.id.radioTswana -> "tn"
                R.id.radioEnglish -> "en"
                else -> null
            }

            if (selectedLanguage == null) {
                Toast.makeText(
                    this,
                    getString(R.string.select_language),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Save the preference locally.
            getSharedPreferences(
                "SafeZonePreferences",
                MODE_PRIVATE
            ).edit()
                .putString("app_language", selectedLanguage)
                .apply()

            // Apply the chosen language to AppCompat screens.
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(selectedLanguage)
            )

            Toast.makeText(
                this,
                getString(R.string.language_saved),
                Toast.LENGTH_SHORT
            ).show()

            // The locale change may recreate this activity automatically.
        }
    }

    private fun getSavedLanguage(): String {
        return getSharedPreferences(
            "SafeZonePreferences",
            MODE_PRIVATE
        ).getString("app_language", "en") ?: "en"
    }
}
