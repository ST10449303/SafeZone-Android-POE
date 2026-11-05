
package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CampusCreateNewPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var tvSuccess: TextView
    private lateinit var tvConfirmation: TextView

    private var email: String? = null
    private var studentNumber: String? = null

    private val resetUrl =
        "https://safezone-campus-password-api.onrender.com/reset-campus-password"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_create_new_password)

        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        tvSuccess = findViewById(R.id.tvSuccess)
        tvConfirmation = findViewById(R.id.tvConfirmation)

        email = intent.getStringExtra("studentEmail")
            ?: intent.getStringExtra("email")

        studentNumber = intent.getStringExtra("studentNumber")

        tvSuccess.visibility = TextView.GONE
        tvConfirmation.visibility = TextView.GONE

        setupPasswordVisibility(etNewPassword)
        setupPasswordVisibility(etConfirmPassword)

        btnResetPassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (email.isNullOrBlank() || studentNumber.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Student details are missing. Please start again.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                etNewPassword.error = "Please enter a new password"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                etNewPassword.error =
                    "Password must contain at least 6 characters"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error =
                    "Please confirm your new password"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            resetCampusPassword(newPassword)
        }

        btnBackToLogin.setOnClickListener {
            returnToCampusLogin()
        }
    }

    private fun resetCampusPassword(newPassword: String) {
        btnResetPassword.isEnabled = false
        btnResetPassword.text = "Resetting..."

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val requestBody = JSONObject().apply {
                    put("email", email)
                    put("studentNumber", studentNumber)
                    put("newPassword", newPassword)
                }

                connection = (URL(resetUrl).openConnection()
                        as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 20000
                    doOutput = true
                    setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )
                    setRequestProperty("Accept", "application/json")
                }

                connection.outputStream.use { output ->
                    output.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                }

                val statusCode = connection.responseCode
                val responseStream =
                    if (statusCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText = responseStream?.bufferedReader()
                    ?.use { it.readText() } ?: ""

                val response = if (responseText.isNotBlank()) {
                    JSONObject(responseText)
                } else {
                    JSONObject()
                }

                val success = statusCode in 200..299 &&
                        response.optBoolean("success", false)

                val message = response.optString(
                    "message",
                    if (success) {
                        "Password updated successfully."
                    } else {
                        "Unable to reset the password."
                    }
                )

                runOnUiThread {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Reset Password"

                    if (success) {
                        tvSuccess.visibility = TextView.VISIBLE
                        tvConfirmation.visibility = TextView.VISIBLE

                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                        ).show()

                        btnResetPassword.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                returnToCampusLogin()
                            }
                        }, 1500)
                    } else {
                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Reset Password"

                    Toast.makeText(
                        this,
                        "Could not connect to the server. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun returnToCampusLogin() {
        val loginIntent = Intent(
            this,
            CampusLoginActivity::class.java
        )

        loginIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        email?.let {
            loginIntent.putExtra("email", it)
        }

        startActivity(loginIntent)
        finish()
    }

    private fun setupPasswordVisibility(editText: EditText) {
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2]

                if (drawableEnd != null) {
                    val clickPosition =
                        editText.right -
                                drawableEnd.bounds.width() -
                                editText.paddingEnd

                    if (event.rawX >= clickPosition) {
                        togglePasswordVisibility(editText)
                        return@setOnTouchListener true
                    }
                }
            }

            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText) {
        val cursorPosition = editText.selectionStart

        if (editText.transformationMethod is PasswordTransformationMethod) {
            editText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility,
                0
            )
        } else {
            editText.transformationMethod =
                PasswordTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility_off,
                0
            )
        }

        editText.setSelection(
            cursorPosition.coerceIn(0, editText.text.length)
        )
    }
}
