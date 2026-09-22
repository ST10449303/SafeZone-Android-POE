package com.example.safezone

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var btnChangePassword: Button
    private lateinit var btnBackToSettings: Button

    private lateinit var tvBack: TextView
    private lateinit var tvSuccess: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()

        // ==============================
        // FIND VIEWS
        // ==============================

        tvBack =
            findViewById(R.id.tvBack)

        tvSuccess =
            findViewById(R.id.tvSuccess)

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


        // ==============================
        // BACK ARROW
        // ==============================

        tvBack.setOnClickListener {
            finish()
        }


        // ==============================
        // PASSWORD VISIBILITY
        // ==============================

        setupPasswordVisibility(etCurrentPassword)

        setupPasswordVisibility(etNewPassword)

        setupPasswordVisibility(etConfirmPassword)


        // ==============================
        // CHANGE PASSWORD
        // ==============================

        btnChangePassword.setOnClickListener {

            changePassword()
        }


        // ==============================
        // BACK TO SETTINGS
        // ==============================

        btnBackToSettings.setOnClickListener {

            finish()
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
        // CHECK CURRENT USER
        // ==============================

        val user = auth.currentUser

        if (user == null) {

            Toast.makeText(
                this,
                "No logged-in account was found.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


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
        // PASSWORD MATCH
        // ==============================

        if (newPassword != confirmPassword) {

            etConfirmPassword.error =
                "Passwords do not match"

            etConfirmPassword.requestFocus()

            return
        }


        // ==============================
        // CHECK SAME PASSWORD
        // ==============================

        if (currentPassword == newPassword) {

            etNewPassword.error =
                "New password must be different from your current password"

            etNewPassword.requestFocus()

            return
        }


        // ==============================
        // GET USER EMAIL
        // ==============================

        val email = user.email

        if (email.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "Your account email could not be found.",
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


        // =================================================
        // RE-AUTHENTICATE USER
        // =================================================

        val credential =
            EmailAuthProvider.getCredential(
                email,
                currentPassword
            )

        user.reauthenticate(credential)
            .addOnCompleteListener { reAuthTask ->

                if (reAuthTask.isSuccessful) {

                    // ==========================================
                    // CURRENT PASSWORD IS CORRECT
                    // ==========================================

                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->

                            btnChangePassword.isEnabled = true

                            btnChangePassword.text =
                                "CHANGE PASSWORD"


                            if (updateTask.isSuccessful) {

                                // ==============================
                                // SUCCESS
                                // ==============================

                                tvSuccess.visibility =
                                    TextView.VISIBLE

                                Toast.makeText(
                                    this,
                                    "Password changed successfully!",
                                    Toast.LENGTH_LONG
                                ).show()


                                // Clear fields

                                etCurrentPassword.text.clear()

                                etNewPassword.text.clear()

                                etConfirmPassword.text.clear()


                            } else {

                                // ==============================
                                // UPDATE FAILED
                                // ==============================

                                val errorMessage =
                                    updateTask.exception?.message
                                        ?: "Unable to change password."

                                Toast.makeText(
                                    this,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                } else {

                    // ==============================
                    // CURRENT PASSWORD INCORRECT
                    // ==============================

                    btnChangePassword.isEnabled = true

                    btnChangePassword.text =
                        "CHANGE PASSWORD"

                    etCurrentPassword.error =
                        "Current password is incorrect"

                    etCurrentPassword.requestFocus()

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

                if (drawableEnd != null) {

                    val drawableWidth =
                        drawableEnd.bounds.width()

                    val clickPosition =
                        editText.right -
                                drawableWidth -
                                editText.paddingEnd

                    if (event.rawX >= clickPosition) {

                        togglePasswordVisibility(
                            editText
                        )

                        return@setOnTouchListener true
                    }
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


        if (cursorPosition >= 0) {

            editText.setSelection(
                cursorPosition.coerceAtMost(
                    editText.text.length
                )
            )
        }
    }
}