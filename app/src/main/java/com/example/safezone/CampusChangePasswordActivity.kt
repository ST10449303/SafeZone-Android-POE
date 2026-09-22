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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_change_password
        )

        auth = FirebaseAuth.getInstance()


        // ==============================
        // FIND VIEWS
        // ==============================

        tvBack =
            findViewById(R.id.tvBack)

        etCurrentPassword =
            findViewById(R.id.etCurrentPassword)

        etNewPassword =
            findViewById(R.id.etNewPassword)

        etConfirmPassword =
            findViewById(R.id.etConfirmPassword)

        btnChangePassword =
            findViewById(R.id.btnChangePassword)

        btnBackToSettings =
            findViewById(R.id.btnBackToSettings)

        tvSuccess =
            findViewById(R.id.tvSuccess)


        // ==============================
        // PASSWORD VISIBILITY
        // ==============================

        setupPasswordVisibility(
            etCurrentPassword
        )

        setupPasswordVisibility(
            etNewPassword
        )

        setupPasswordVisibility(
            etConfirmPassword
        )


        // ==============================
        // BACK ARROW
        // ==============================

        tvBack.setOnClickListener {
            finish()
        }


        // ==============================
        // BACK TO SETTINGS
        // ==============================

        btnBackToSettings.setOnClickListener {
            finish()
        }


        // ==============================
        // CHANGE PASSWORD
        // ==============================

        btnChangePassword.setOnClickListener {

            changePassword()
        }
    }


    // =====================================================
    // CHANGE PASSWORD
    // =====================================================

    private fun changePassword() {

        val currentPassword =
            etCurrentPassword.text.toString()

        val newPassword =
            etNewPassword.text.toString()

        val confirmPassword =
            etConfirmPassword.text.toString()


        // ==============================
        // CURRENT PASSWORD
        // ==============================

        if (currentPassword.isEmpty()) {

            etCurrentPassword.error =
                "Please enter your current password"

            etCurrentPassword.requestFocus()

            return
        }


        // ==============================
        // NEW PASSWORD
        // ==============================

        if (newPassword.isEmpty()) {

            etNewPassword.error =
                "Please enter a new password"

            etNewPassword.requestFocus()

            return
        }


        // ==============================
        // PASSWORD LENGTH
        // ==============================

        if (newPassword.length < 6) {

            etNewPassword.error =
                "Password must contain at least 6 characters"

            etNewPassword.requestFocus()

            return
        }


        // ==============================
        // CONFIRM PASSWORD
        // ==============================

        if (confirmPassword.isEmpty()) {

            etConfirmPassword.error =
                "Please confirm your new password"

            etConfirmPassword.requestFocus()

            return
        }


        // ==============================
        // MATCH PASSWORDS
        // ==============================

        if (newPassword != confirmPassword) {

            etConfirmPassword.error =
                "Passwords do not match"

            etConfirmPassword.requestFocus()

            return
        }


        // ==============================
        // NEW PASSWORD DIFFERENT
        // ==============================

        if (currentPassword == newPassword) {

            etNewPassword.error =
                "New password must be different from current password"

            etNewPassword.requestFocus()

            return
        }


        // ==============================
        // CHECK USER
        // ==============================

        val user =
            auth.currentUser

        if (user == null) {

            Toast.makeText(
                this,
                "Your campus account session has expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        // ==============================
        // GET EMAIL
        // ==============================

        val email =
            user.email

        if (email.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "Unable to find your campus account email.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // ==============================
        // DISABLE BUTTON
        // ==============================

        btnChangePassword.isEnabled = false

        btnChangePassword.text =
            "CHANGING PASSWORD..."


        // ==============================
        // RE-AUTHENTICATE USER
        // ==============================

        val credential =
            EmailAuthProvider.getCredential(
                email,
                currentPassword
            )


        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->

                if (reauthTask.isSuccessful) {

                    // ==============================
                    // UPDATE PASSWORD
                    // ==============================

                    user.updatePassword(
                        newPassword
                    ).addOnCompleteListener { updateTask ->

                        btnChangePassword.isEnabled = true

                        btnChangePassword.text =
                            "CHANGE PASSWORD"


                        if (updateTask.isSuccessful) {

                            tvSuccess.visibility =
                                TextView.VISIBLE

                            Toast.makeText(
                                this,
                                "Your campus password has been changed successfully.",
                                Toast.LENGTH_LONG
                            ).show()


                            // Clear fields

                            etCurrentPassword.text.clear()
                            etNewPassword.text.clear()
                            etConfirmPassword.text.clear()

                        } else {

                            Toast.makeText(
                                this,
                                updateTask.exception?.message
                                    ?: "Unable to change password.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                } else {

                    btnChangePassword.isEnabled = true

                    btnChangePassword.text =
                        "CHANGE PASSWORD"


                    Toast.makeText(
                        this,
                        "Current password is incorrect.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    // =====================================================
    // PASSWORD VISIBILITY
    // =====================================================

    private fun setupPasswordVisibility(
        editText: EditText
    ) {

        editText.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd =
                    editText.compoundDrawables[2]

                if (
                    drawableEnd != null &&
                    event.rawX >=
                    editText.right -
                    drawableEnd.bounds.width() -
                    editText.paddingEnd
                ) {

                    togglePasswordVisibility(
                        editText
                    )

                    return@setOnTouchListener true
                }
            }

            false
        }
    }


    // =====================================================
    // TOGGLE PASSWORD
    // =====================================================

    private fun togglePasswordVisibility(
        editText: EditText
    ) {

        val cursorPosition =
            editText.selectionStart


        if (
            editText.transformationMethod == null
        ) {

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