package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalRegistrationActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnCreateAccount: Button
    private lateinit var tvLogin: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val TAG = "SafeZonePersonalRegistration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_registration)

        Log.d(TAG, "Personal Registration screen opened")

        // CONNECT XML CONTROLS
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvLogin = findViewById(R.id.tvLogin)

        // INITIALISE FIREBASE
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // PASSWORD VISIBILITY
        setupPasswordToggle(etPassword, "Password")
        setupPasswordToggle(etConfirmPassword, "Confirm Password")

        // CREATE ACCOUNT
        btnCreateAccount.setOnClickListener {

            Log.d(TAG, "Create Account button clicked")

            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // CHECK FULL NAME
            if (fullName.isEmpty()) {
                etFullName.error =
                    getString(R.string.enter_full_name_error)
                etFullName.requestFocus()
                return@setOnClickListener
            }

            // CHECK EMAIL
            if (email.isEmpty()) {
                etEmail.error =
                    getString(R.string.enter_registration_email_error)
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error =
                    getString(R.string.valid_registration_email_error)
                etEmail.requestFocus()
                return@setOnClickListener
            }

            // CHECK PASSWORD
            if (password.isEmpty()) {
                etPassword.error =
                    getString(R.string.create_password_error)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 8) {
                etPassword.error =
                    getString(R.string.password_min_eight_error)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (!password.any { it.isUpperCase() }) {
                etPassword.error =
                    getString(R.string.password_uppercase_error)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (!password.any { it.isLowerCase() }) {
                etPassword.error =
                    getString(R.string.password_lowercase_error)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (!password.any { it.isDigit() }) {
                etPassword.error =
                    getString(R.string.password_number_error)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (!password.any { !it.isLetterOrDigit() }) {
                etPassword.error =
                    getString(R.string.password_special_character_error)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // CHECK CONFIRM PASSWORD
            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error =
                    getString(R.string.confirm_password_error)
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error =
                    getString(R.string.passwords_do_not_match)
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            // DISABLE BUTTON WHILE REGISTERING
            btnCreateAccount.isEnabled = false
            btnCreateAccount.text =
                getString(R.string.creating_account)

            Log.d(TAG, "Starting Firebase account creation")

            // CREATE FIREBASE AUTHENTICATION ACCOUNT
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Log.d(
                            TAG,
                            "Firebase Authentication account created successfully"
                        )

                        val firebaseUser = auth.currentUser

                        if (firebaseUser == null) {
                            Log.e(TAG, "Firebase user is null")

                            resetCreateAccountButton()

                            showMessage(
                                getString(
                                    R.string.account_creation_failed
                                )
                            )
                            return@addOnCompleteListener
                        }

                        val userId = firebaseUser.uid

                        // CREATE USER PROFILE FOR FIRESTORE
                        val user = hashMapOf(
                            "userId" to userId,
                            "fullName" to fullName,
                            "email" to email,
                            "accountType" to "Personal",
                            "createdAt" to System.currentTimeMillis()
                        )

                        // SAVE USER PROFILE TO FIRESTORE
                        db.collection("Users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener {

                                Log.d(
                                    TAG,
                                    "Personal user profile saved successfully"
                                )

                                showMessage(
                                    getString(R.string.registration_success)
                                )

                                // OPEN PERSONAL LOGIN
                                val intent = Intent(
                                    this,
                                    PersonalLoginActivity::class.java
                                )

                                intent.putExtra(
                                    "registeredEmail",
                                    email
                                )

                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { exception ->

                                Log.e(
                                    TAG,
                                    "Failed to save personal user profile",
                                    exception
                                )

                                resetCreateAccountButton()

                                showMessage(
                                    getString(
                                        R.string.account_created_profile_failed,
                                        exception.localizedMessage
                                            ?: "Unknown error"
                                    )
                                )
                            }

                    } else {

                        Log.e(
                            TAG,
                            "Firebase account creation failed",
                            task.exception
                        )

                        resetCreateAccountButton()

                        val firebaseError =
                            task.exception?.message.orEmpty()

                        val errorMessage = when {
                            firebaseError.contains(
                                "email address is already in use",
                                ignoreCase = true
                            ) ||
                                    firebaseError.contains(
                                        "email-already-in-use",
                                        ignoreCase = true
                                    ) -> {
                                getString(
                                    R.string.email_already_registered
                                )
                            }

                            firebaseError.contains(
                                "badly formatted",
                                ignoreCase = true
                            ) ||
                                    firebaseError.contains(
                                        "invalid-email",
                                        ignoreCase = true
                                    ) -> {
                                getString(
                                    R.string.valid_registration_email_error
                                )
                            }

                            firebaseError.contains(
                                "network",
                                ignoreCase = true
                            ) -> {
                                getString(
                                    R.string.registration_network_error
                                )
                            }

                            else -> {
                                getString(R.string.registration_failed)
                            }
                        }

                        showMessage(errorMessage)
                    }
                }
        }

        // LOGIN LINK
        tvLogin.setOnClickListener {

            Log.d(TAG, "Login link selected")

            startActivity(
                Intent(this, PersonalLoginActivity::class.java)
            )

            finish()
        }
    }

    // RESET CREATE ACCOUNT BUTTON
    private fun resetCreateAccountButton() {
        btnCreateAccount.isEnabled = true
        btnCreateAccount.text =
            getString(R.string.create_account)
    }

    // DISPLAY MESSAGE
    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    // PASSWORD VISIBILITY TOGGLE
    private fun setupPasswordToggle(
        editText: EditText,
        fieldName: String
    ) {
        var passwordVisible = false

        editText.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd =
                    editText.compoundDrawablesRelative[2]

                if (drawableEnd != null) {

                    val drawableWidth =
                        drawableEnd.bounds.width()

                    val touchAreaStart =
                        editText.width -
                                editText.paddingEnd -
                                drawableWidth

                    if (event.x >= touchAreaStart) {

                        passwordVisible = !passwordVisible

                        if (passwordVisible) {

                            editText.transformationMethod =
                                HideReturnsTransformationMethod.getInstance()

                            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_lock,
                                0,
                                R.drawable.ic_visibility,
                                0
                            )

                            Log.d(
                                TAG,
                                "$fieldName visibility enabled"
                            )

                        } else {

                            editText.transformationMethod =
                                PasswordTransformationMethod.getInstance()

                            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_lock,
                                0,
                                R.drawable.ic_visibility_off,
                                0
                            )

                            Log.d(
                                TAG,
                                "$fieldName visibility disabled"
                            )
                        }

                        // Keep cursor at the end of the password
                        editText.setSelection(
                            editText.text.length
                        )

                        return@setOnTouchListener true
                    }
                }
            }

            false
        }
    }
}
