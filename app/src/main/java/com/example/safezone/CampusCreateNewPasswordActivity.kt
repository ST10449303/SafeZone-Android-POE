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

class CampusCreateNewPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var tvSuccess: TextView
    private lateinit var tvConfirmation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_create_new_password)

        // Connect XML components
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        tvSuccess = findViewById(R.id.tvSuccess)
        tvConfirmation = findViewById(R.id.tvConfirmation)

        // Get account information from Campus Forgot Password
        val email = intent.getStringExtra("email")
        val studentNumber = intent.getStringExtra("studentNumber")

        // Show / Hide New Password
        etNewPassword.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = etNewPassword.compoundDrawables[2]

                if (
                    drawableEnd != null &&
                    event.rawX >=
                    etNewPassword.right -
                    drawableEnd.bounds.width() -
                    etNewPassword.paddingEnd
                ) {
                    togglePasswordVisibility(etNewPassword)

                    return@setOnTouchListener true
                }
            }

            false
        }

        // Show / Hide Confirm Password
        etConfirmPassword.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = etConfirmPassword.compoundDrawables[2]

                if (
                    drawableEnd != null &&
                    event.rawX >=
                    etConfirmPassword.right -
                    drawableEnd.bounds.width() -
                    etConfirmPassword.paddingEnd
                ) {
                    togglePasswordVisibility(etConfirmPassword)

                    return@setOnTouchListener true
                }
            }

            false
        }

        // RESET PASSWORD button
        btnResetPassword.setOnClickListener {

            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Check new password
            if (newPassword.isEmpty()) {

                etNewPassword.error =
                    "Please enter a new password"

                etNewPassword.requestFocus()

                return@setOnClickListener
            }

            // Check password length
            if (newPassword.length < 6) {

                etNewPassword.error =
                    "Password must contain at least 6 characters"

                etNewPassword.requestFocus()

                return@setOnClickListener
            }

            // Check confirmation password
            if (confirmPassword.isEmpty()) {

                etConfirmPassword.error =
                    "Please confirm your new password"

                etConfirmPassword.requestFocus()

                return@setOnClickListener
            }

            // Check passwords match
            if (newPassword != confirmPassword) {

                etConfirmPassword.error =
                    "Passwords do not match"

                etConfirmPassword.requestFocus()

                return@setOnClickListener
            }

            // Prototype password reset successful
            tvSuccess.visibility = TextView.VISIBLE
            tvConfirmation.visibility = TextView.VISIBLE

            Toast.makeText(
                this,
                "Password reset successfully!",
                Toast.LENGTH_LONG
            ).show()

            // Hide reset button after successful reset
            btnResetPassword.visibility = Button.GONE

            // Clear password fields
            etNewPassword.text.clear()
            etConfirmPassword.text.clear()
        }

        // BACK TO CAMPUS LOGIN
        btnBackToLogin.setOnClickListener {

            val intent = Intent(
                this,
                CampusLoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            // Pass email back to Campus Login
            intent.putExtra("email", email)
            intent.putExtra("studentNumber", studentNumber)

            startActivity(intent)

            finish()
        }
    }

    // Toggle password visibility
    private fun togglePasswordVisibility(editText: EditText) {

        val cursorPosition = editText.selectionStart

        if (editText.transformationMethod == null) {

            // Hide password
            editText.transformationMethod =
                PasswordTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility_off,
                0
            )

        } else {

            // Show password
            editText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility,
                0
            )
        }

        editText.setSelection(
            cursorPosition.coerceAtLeast(0)
        )
    }
}