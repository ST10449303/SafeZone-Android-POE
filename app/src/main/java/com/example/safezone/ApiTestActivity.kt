package com.example.safezone

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ApiTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "SafeZone REST API Test"
            textSize = 22f
        }

        val resultText = TextView(this).apply {
            text = "Connecting to SafeZone API..."
            textSize = 16f
            setPadding(0, 24, 0, 0)
        }

        val progressBar = ProgressBar(this)

        layout.addView(title)
        layout.addView(progressBar)
        layout.addView(resultText)

        setContentView(layout)

        lifecycleScope.launch {
            try {
                val response =
                    RetrofitClient.safetyAlertApi.getActiveAlerts()

                progressBar.visibility = android.view.View.GONE

                if (response.isSuccessful) {
                    val alerts = response.body().orEmpty()

                    resultText.text =
                        if (alerts.isEmpty()) {
                            "API connection successful!\n\nNo active alerts found."
                        } else {
                            buildString {
                                append("API connection successful!\n")
                                append("Alerts found: ${alerts.size}\n\n")

                                alerts.forEach { alert ->
                                    append("Title: ${alert.title}\n")
                                    append("Message: ${alert.message}\n")
                                    append("Audience: ${alert.audience}\n")
                                    append("Location: ${alert.location}\n")
                                    append("Active: ${alert.isActive}\n\n")
                                }
                            }
                        }
                } else {
                    resultText.text =
                        "API request failed.\nHTTP status: ${response.code()}\n${response.message()}"
                }

            } catch (exception: Exception) {
                progressBar.visibility = android.view.View.GONE

                resultText.text =
                    "Could not connect to the API.\n\n" +
                            (exception.localizedMessage ?: "Unknown error")

                Toast.makeText(
                    this@ApiTestActivity,
                    "API connection failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}