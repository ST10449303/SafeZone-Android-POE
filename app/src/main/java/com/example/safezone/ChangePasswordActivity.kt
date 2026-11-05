
package com.example.safezone

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.View
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

        // Find views using the IDs in activity_change_password.xml
        tvBack = findViewById(R.id.tvBack)
        tvSuccess = findViewById(R.id.tvSuccess)

        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnBackToSettings = findViewById(R.id.btnBackToSettings)

        // Hide the success message until the password is changed
        tvSuccess.visibility = View.GONE

        // Back navigation
        tvBack.setOnClickListener {
            finish()
        }

        btnBackToSettings.setOnClickListener {
            finish()
        }

        // Password visibility controls
        setupPasswordVisibility(etCurrentPassword)
        setupPasswordVisibility(etNewPassword)
        setupPasswordVisibility(etConfirmPassword)

        // Change password
        btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    // =====================================================
    // CHANGE PASSWORD
    // =====================================================

    private fun changePassword() {

        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Check whether a user is signed in
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                getString(R.string.change_password_no_user),
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Validate current password
        if (currentPassword.isEmpty()) {
            etCurrentPassword.error =
                getString(R.string.change_password_current_required)
            etCurrentPassword.requestFocus()
            return
        }

        // Validate new password
        if (newPassword.isEmpty()) {
            etNewPassword.error =
                getString(R.string.change_password_new_required)
            etNewPassword.requestFocus()
            return
        }

        // Require at least six characters
        if (newPassword.length < 6) {
            etNewPassword.error =
                getString(R.string.change_password_min_length)
            etNewPassword.requestFocus()
            return
        }

        // Validate confirmation
        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error =
                getString(R.string.change_password_confirm_required)
            etConfirmPassword.requestFocus()
            return
        }

        // Ensure both new password entries match
        if (newPassword != confirmPassword) {
            etConfirmPassword.error =
                getString(R.string.change_password_mismatch)
            etConfirmPassword.requestFocus()
            return
        }

        // Ensure the new password differs from the current password
        if (currentPassword == newPassword) {
            etNewPassword.error =
                getString(R.string.change_password_same_as_current)
            etNewPassword.requestFocus()
            return
        }

        // Retrieve the account email
        val email = user.email

        if (email.isNullOrEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.change_password_email_missing),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Disable the button while the request is processing
        btnChangePassword.isEnabled = false
        btnChangePassword.text =
            getString(R.string.change_password_processing)

        // Hide any previous success message
        tvSuccess.visibility = View.GONE

        // Re-authenticate the user with their current password
        val credential = EmailAuthProvider.getCredential(
            email,
            currentPassword
        )

        user.reauthenticate(credential)
            .addOnCompleteListener { reAuthTask ->

                if (reAuthTask.isSuccessful) {

                    // Update the password after successful re-authentication
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->

                            // Restore the button
                            btnChangePassword.isEnabled = true
                            btnChangePassword.text =
                                getString(R.string.change_password_button)

                            if (updateTask.isSuccessful) {

                                // Display success message
                                tvSuccess.text =
                                    getString(R.string.change_password_success)
                                tvSuccess.visibility = View.VISIBLE

                                Toast.makeText(
                                    this,
                                    getString(R.string.change_password_success),
                                    Toast.LENGTH_LONG
                                ).show()

                                // Clear password fields
                                etCurrentPassword.text.clear()
                                etNewPassword.text.clear()
                                etConfirmPassword.text.clear()

                            } else {

                                // Report password update failure
                                val errorMessage =
                                    updateTask.exception?.localizedMessage
                                        ?: getString(
                                            R.string.change_password_unknown_error
                                        )

                                Toast.makeText(
                                    this,
                                    getString(
                                        R.string.change_password_update_failed,
                                        errorMessage
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                } else {

                    // Restore the button if re-authentication fails
                    btnChangePassword.isEnabled = true
                    btnChangePassword.text =
                        getString(R.string.change_password_button)

                    etCurrentPassword.error =
                        getString(R.string.change_password_current_incorrect)
                    etCurrentPassword.requestFocus()

                    Toast.makeText(
                        this,
                        getString(R.string.change_password_current_incorrect),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // =====================================================
    // PASSWORD VISIBILITY
    // =====================================================

    private fun setupPasswordVisibility(editText: EditText) {

        editText.setOnTouchListener { view, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = editText.compoundDrawables[2]

                if (drawableEnd != null) {

                    // Calculate the drawable's clickable area
                    val drawableWidth = drawableEnd.bounds.width()

                    val clickPosition =
                        editText.width -
                                editText.paddingEnd -
                                drawableWidth

                    if (event.x >= clickPosition) {
                        togglePasswordVisibility(editText)

                        // Handle the icon tap without triggering other actions
                        view.performClick()
                        return@setOnTouchListener true
                    }
                }
            }

            false
        }
    }

    // =====================================================
    // TOGGLE PASSWORD VISIBILITY
    // =====================================================

    private fun togglePasswordVisibility(editText: EditText) {

        val cursorPosition = editText.selectionStart

        val isPasswordHidden =
            editText.transformationMethod is PasswordTransformationMethod

        if (isPasswordHidden) {

            // Show password
            editText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility,
                0
            )

        } else {

            // Hide password
            editText.transformationMethod =
                PasswordTransformationMethod.getInstance()

            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility_off,
                0
            )
        }

        // Preserve the cursor position
        if (cursorPosition >= 0) {
            editText.setSelection(
                cursorPosition.coerceAtMost(editText.text.length)
            )
        }
    }
}
