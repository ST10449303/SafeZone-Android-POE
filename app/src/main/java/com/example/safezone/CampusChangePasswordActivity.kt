package com.example.safezone

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class CampusChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var btnChangePassword: AppCompatButton
    private lateinit var btnBackToSettings: AppCompatButton

    private lateinit var tvSuccess: TextView
    private lateinit var tvBack: TextView

    // Track visibility separately instead of comparing transformation methods.
    private val passwordVisibility = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_change_password)

        auth = FirebaseAuth.getInstance()

        tvBack = findViewById(R.id.tvBack)
        tvSuccess = findViewById(R.id.tvSuccess)

        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnBackToSettings = findViewById(R.id.btnBackToSettings)

        tvSuccess.visibility = TextView.GONE

        listOf(
            etCurrentPassword,
            etNewPassword,
            etConfirmPassword
        ).forEach { editText ->
            passwordVisibility[editText.id] = false
            setupPasswordVisibility(editText)
        }

        tvBack.setOnClickListener { finish() }
        btnBackToSettings.setOnClickListener { finish() }

        btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (currentPassword.isBlank()) {
            etCurrentPassword.error = "Please enter your current password"
            etCurrentPassword.requestFocus()
            return
        }

        if (newPassword.length < 6) {
            etNewPassword.error = "Password must contain at least 6 characters"
            etNewPassword.requestFocus()
            return
        }

        if (confirmPassword.isBlank()) {
            etConfirmPassword.error = "Please confirm your new password"
            etConfirmPassword.requestFocus()
            return
        }

        if (newPassword != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }

        if (currentPassword == newPassword) {
            etNewPassword.error = "New password must differ from your current password"
            etNewPassword.requestFocus()
            return
        }

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Your campus session has expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val email = user.email

        if (email.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Unable to find your campus account email.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnChangePassword.isEnabled = false
        btnChangePassword.text = "CHANGING PASSWORD..."

        val credential = EmailAuthProvider.getCredential(
            email,
            currentPassword
        )

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->

                if (!reauthTask.isSuccessful) {
                    btnChangePassword.isEnabled = true
                    btnChangePassword.text = "CHANGE PASSWORD"

                    etCurrentPassword.error =
                        "Current password is incorrect"
                    etCurrentPassword.requestFocus()

                    Toast.makeText(
                        this,
                        "Unable to verify your current password.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                user.updatePassword(newPassword)
                    .addOnCompleteListener { updateTask ->

                        btnChangePassword.isEnabled = true
                        btnChangePassword.text = "CHANGE PASSWORD"

                        if (updateTask.isSuccessful) {
                            tvSuccess.visibility = TextView.VISIBLE

                            etCurrentPassword.text.clear()
                            etNewPassword.text.clear()
                            etConfirmPassword.text.clear()

                            Toast.makeText(
                                this,
                                "Campus password changed successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                updateTask.exception?.localizedMessage
                                    ?: "Unable to change your password.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
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
        val isVisible = passwordVisibility[editText.id] ?: false
        val cursorPosition = editText.selectionStart

        if (isVisible) {
            editText.transformationMethod =
                PasswordTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility_off,
                0
            )

            passwordVisibility[editText.id] = false
        } else {
            editText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility,
                0
            )

            passwordVisibility[editText.id] = true
        }

        editText.setSelection(
            cursorPosition.coerceIn(0, editText.text.length)
        )
    }
}