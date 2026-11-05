package com.example.safezone

import android.util.Log
import com.example.safezone.api.DeviceTokenRegistrationRequest
import com.example.safezone.api.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Saves this device's FCM token to Firestore and registers it with the SafeZone
 * Azure API. The API verifies the Firebase ID token before accepting the token.
 */
object FCMTokenManager {

    private const val TAG = "FCMTokenManager"

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Keeps the existing Firestore token record and also registers the device
     * with Azure so the API can target notifications by account type/campus.
     *
     * For Campus accounts, pass the selected campus.
     * For Personal accounts, pass a non-empty location when available.
     */
    fun registerDeviceWithAzure(
        accountType: String,
        campus: String = "",
        location: String = ""
    ) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.w(TAG, "Cannot register FCM token: no Firebase user is signed in.")
            return
        }

        val normalizedType = accountType.trim()
        if (normalizedType != "Personal" && normalizedType != "Campus") {
            Log.e(TAG, "Invalid account type '$accountType'. Expected Personal or Campus.")
            return
        }

        val normalizedCampus = campus.trim().removeSuffix(" Campus").trim()
        val normalizedLocation = location.trim()

        if (normalizedType == "Campus" && normalizedCampus.isBlank()) {
            Log.w(TAG, "Cannot register Campus token without a campus.")
            return
        }

        if (normalizedType == "Personal" && normalizedLocation.isBlank()) {
            Log.w(TAG, "Personal token will not be registered: location is required by the API.")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                if (fcmToken.isNullOrBlank()) {
                    Log.e(TAG, "Firebase returned an empty FCM token.")
                    return@addOnSuccessListener
                }

                // Preserve the app's existing Firestore token storage.
                val tokenData = hashMapOf(
                    "token" to fcmToken,
                    "accountType" to normalizedType,
                    "campus" to normalizedCampus,
                    "location" to normalizedLocation,
                    "platform" to "Android",
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(user.uid)
                    .collection("fcmTokens")
                    .document(fcmToken.hashCode().toString())
                    .set(tokenData)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token saved to Firestore for $normalizedType.")
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Failed to save FCM token to Firestore.", error)
                    }

                // Obtain a Firebase ID token. Azure's DeviceTokens endpoint
                // verifies this bearer token to identify the signed-in user.
                user.getIdToken(false)
                    .addOnSuccessListener { authResult ->
                        val idToken = authResult.token
                        if (idToken.isNullOrBlank()) {
                            Log.e(TAG, "Firebase ID token was empty; Azure registration skipped.")
                            return@addOnSuccessListener
                        }

                        managerScope.launch {
                            try {
                                val request = DeviceTokenRegistrationRequest(
                                    fcmToken = fcmToken,
                                    audience = normalizedType,
                                    campus = if (normalizedType == "Campus") normalizedCampus else "",
                                    location = if (normalizedType == "Personal") normalizedLocation else ""
                                )

                                val response = RetrofitClient.apiService
                                    .registerDeviceToken(
                                        authorization = "Bearer $idToken",
                                        request = request
                                    )

                                if (response.isSuccessful) {
                                    Log.d(
                                        TAG,
                                        "Device token registered with Azure for $normalizedType " +
                                                if (normalizedType == "Campus") "campus=$normalizedCampus" else "location=$normalizedLocation"
                                    )
                                } else {
                                    val errorBody = try {
                                        response.errorBody()?.string().orEmpty()
                                    } catch (_: Exception) {
                                        ""
                                    }
                                    Log.e(
                                        TAG,
                                        "Azure device-token registration failed: " +
                                                "HTTP ${response.code()} ${errorBody.take(500)}"
                                    )
                                }
                            } catch (error: Exception) {
                                Log.e(TAG, "Error registering FCM token with Azure.", error)
                            }
                        }
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Could not obtain Firebase ID token for Azure registration.", error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to retrieve Firebase Cloud Messaging token.", error)
            }
    }

    /**
     * Backwards-compatible entry point used by existing login activities.
     * This preserves the previous Firestore-only behaviour. CampusAlertsActivity
     * calls registerDeviceWithAzure() once the selected campus is known.
     */
    fun saveToken(accountType: String) {
        saveTokenToFirestoreOnly(accountType)
    }

    private fun saveTokenToFirestoreOnly(accountType: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Log.d(TAG, "No Firebase user signed in; token not saved.")
            return
        }

        if (accountType != "Personal" && accountType != "Campus") {
            Log.e(TAG, "Invalid account type: $accountType")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                val tokenData = hashMapOf(
                    "token" to token,
                    "accountType" to accountType,
                    "platform" to "Android",
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(user.uid)
                    .collection("fcmTokens")
                    .document(token.hashCode().toString())
                    .set(tokenData)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token saved for $accountType user.")
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Failed to save FCM token.", error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to retrieve FCM token.", error)
            }
    }
}

