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

class CreateNewPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var tvSuccess: TextView
    private lateinit var tvConfirmation: TextView
    private lateinit var btnBackToLogin: Button

    private var email: String? = null

    private val resetUrl =
        "https://safezone-campus-password-api.onrender.com/reset-personal-password"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_password)

        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        tvSuccess = findViewById(R.id.tvSuccess)
        tvConfirmation = findViewById(R.id.tvConfirmation)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        email = intent.getStringExtra("email")

        tvSuccess.visibility = TextView.GONE
        tvConfirmation.visibility = TextView.GONE

        setupPasswordVisibility(etNewPassword)
        setupPasswordVisibility(etConfirmPassword)

        btnResetPassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (email.isNullOrBlank()) {
                showMessage(R.string.password_reset_missing_email)
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                etNewPassword.error =
                    getString(R.string.enter_new_password_error)
                etNewPassword.requestFocus()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                etNewPassword.error =
                    getString(R.string.password_min_six_error)
                etNewPassword.requestFocus()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error =
                    getString(R.string.confirm_new_password_error)
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                etConfirmPassword.error =
                    getString(R.string.passwords_do_not_match)
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            resetPersonalPassword(newPassword)
        }

        btnBackToLogin.setOnClickListener {
            returnToPersonalLogin()
        }
    }

    private fun resetPersonalPassword(newPassword: String) {
        btnResetPassword.isEnabled = false
        btnResetPassword.text = getString(R.string.resetting_password)

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val requestBody = JSONObject().apply {
                    put("email", email)
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
                    output.write(
                        requestBody.toString().toByteArray(Charsets.UTF_8)
                    )
                }

                val statusCode = connection.responseCode

                val responseStream =
                    if (statusCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                val response = if (responseText.isNotBlank()) {
                    JSONObject(responseText)
                } else {
                    JSONObject()
                }

                val success = statusCode in 200..299 &&
                        response.optBoolean("success", false)

                val message = response.optString("message", "")

                runOnUiThread {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text =
                        getString(R.string.reset_password)

                    if (success) {
                        tvSuccess.visibility = TextView.VISIBLE
                        tvConfirmation.visibility = TextView.VISIBLE

                        etNewPassword.text.clear()
                        etConfirmPassword.text.clear()

                        showMessage(R.string.password_reset_successful)

                        btnResetPassword.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                returnToPersonalLogin()
                            }
                        }, 1500)
                    } else {
                        showMessage(
                            if (message.isNotBlank()) message
                            else getString(R.string.password_reset_failed)
                        )
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text =
                        getString(R.string.reset_password)

                    showMessage(R.string.password_reset_network_error)
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun returnToPersonalLogin() {
        val loginIntent = Intent(
            this,
            PersonalLoginActivity::class.java
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

                if (
                    drawableEnd != null &&
                    event.rawX >= editText.right -
                    drawableEnd.bounds.width() -
                    editText.paddingEnd
                ) {
                    togglePasswordVisibility(editText)
                    return@setOnTouchListener true
                }
            }

            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText) {
        val cursorPosition = editText.selectionStart

        if (
            editText.transformationMethod
                    is PasswordTransformationMethod
        ) {
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

    private fun showMessage(messageResId: Int) {
        Toast.makeText(
            this,
            getString(messageResId),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}