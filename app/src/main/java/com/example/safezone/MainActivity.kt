
package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SafeZoneFCM"
        private const val PREFS_NAME = "SafeZonePreferences"
        private const val LANGUAGE_KEY = "app_language"
    }

    private val splashDuration = 3000L

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d(TAG, "Notification permission granted: $isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the saved language before displaying the UI.
        applySavedLanguage()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission if required.
        requestNotificationPermission()

        // Retrieve the FCM registration token.
        getFCMToken()

        // Preserve existing SafeZone navigation.
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(
                this,
                ChoosePanelActivity::class.java
            )

            startActivity(intent)
            finish()
        }, splashDuration)
    }

    private fun applySavedLanguage() {
        val preferences = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        val language = preferences.getString(
            LANGUAGE_KEY,
            "en"
        ) ?: "en"

        val currentLocales =
            AppCompatDelegate.getApplicationLocales()

        if (currentLocales.toLanguageTags() != language) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language)
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result

                    // Never print the complete token in Logcat.
                    Log.d(
                        TAG,
                        "FCM token retrieved successfully. Length: ${token.length}"
                    )
                } else {
                    Log.e(
                        TAG,
                        "Failed to retrieve FCM token",
                        task.exception
                    )
                }
            }
    }
}
