
package com.example.safezone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SafeZoneMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "safezone_alerts"
        private const val TAG = "SafeZoneFCM"
    }

    /**
     * Retrieves the current FCM token.
     * This function must be called from an activity or another
     * appropriate app startup component to execute.
     */
    fun retrieveFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(
                        TAG,
                        "Failed to retrieve FCM token",
                        task.exception
                    )
                    return@addOnCompleteListener
                }

                val token = task.result

                // Never log the complete token.
                Log.d(
                    TAG,
                    "FCM token retrieved. Length: ${token.length}"
                )
            }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Do not print the complete token in production logs.
        Log.d(
            TAG,
            "FCM token refreshed. Length: ${token.length}"
        )

        // TODO: Send the refreshed token securely to your Azure API.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "FCM MESSAGE RECEIVED")
        Log.d(TAG, "Message ID: ${message.messageId}")
        Log.d(TAG, "Message data keys: ${message.data.keys}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "SafeZone Alert"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new safety update."

        Log.d(TAG, "Preparing notification: $title")

        createNotificationChannel()
        showNotification(title, body)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SafeZone Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Safety updates and emergency alerts"
            }

            val manager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(
                    TAG,
                    "Notification permission has not been granted."
                )
                return
            }

            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )

            Log.d(TAG, "Notification displayed successfully.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission denied.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification.", e)
        }
    }
}
