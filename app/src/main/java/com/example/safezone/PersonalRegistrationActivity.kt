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

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // --------------------------------------------------
    // LOGGING
    // --------------------------------------------------

    companion object {
        private const val TAG = "SafeZonePersonalRegistration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_personal_registration)

        Log.d(TAG, "Personal Registration screen opened")

        // --------------------------------------------------
        // CONNECT XML CONTROLS
        // --------------------------------------------------

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvLogin = findViewById(R.id.tvLogin)

        Log.d(TAG, "Registration screen controls connected successfully")

        // --------------------------------------------------
        // INITIALISE FIREBASE
        // --------------------------------------------------

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Log.d(TAG, "Firebase Authentication and Firestore initialized")

        // --------------------------------------------------
        // PASSWORD EYE TOGGLES
        // --------------------------------------------------

        setupPasswordToggle(
            editText = etPassword,
            fieldName = "Password"
        )

        setupPasswordToggle(
            editText = etConfirmPassword,
            fieldName = "Confirm Password"
        )

        Log.d(TAG, "Password visibility controls initialized")

        // --------------------------------------------------
        // CREATE ACCOUNT BUTTON
        // --------------------------------------------------

        btnCreateAccount.setOnClickListener {

            Log.d(TAG, "Create Account button clicked")

            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // --------------------------------------------------
            // CHECK FULL NAME
            // --------------------------------------------------

            if (fullName.isEmpty()) {

                Log.w(
                    TAG,
                    "Registration validation failed: full name is empty"
                )

                etFullName.error = "Please enter your full name"
                etFullName.requestFocus()

                return@setOnClickListener
            }

            Log.d(TAG, "Full name validation successful")

            // --------------------------------------------------
            // CHECK EMAIL
            // --------------------------------------------------

            if (email.isEmpty()) {

                Log.w(
                    TAG,
                    "Registration validation failed: email is empty"
                )

                etEmail.error = "Please enter your email address"
                etEmail.requestFocus()

                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                Log.w(
                    TAG,
                    "Registration validation failed: invalid email format"
                )

                etEmail.error = "Please enter a valid email address"
                etEmail.requestFocus()

                return@setOnClickListener
            }

            Log.d(TAG, "Email validation successful")

            // --------------------------------------------------
            // CHECK PASSWORD
            // --------------------------------------------------

            if (password.isEmpty()) {

                Log.w(
                    TAG,
                    "Registration validation failed: password is empty"
                )

                etPassword.error = "Please create a password"
                etPassword.requestFocus()

                return@setOnClickListener
            }

            // Password length
            if (password.length < 8) {

                Log.w(
                    TAG,
                    "Registration validation failed: password is shorter than 8 characters"
                )

                etPassword.error =
                    "Password must be at least 8 characters"

                etPassword.requestFocus()

                return@setOnClickListener
            }

            // Uppercase
            if (!password.any { it.isUpperCase() }) {

                Log.w(
                    TAG,
                    "Registration validation failed: password has no uppercase letter"
                )

                etPassword.error =
                    "Password needs an uppercase letter"

                etPassword.requestFocus()

                return@setOnClickListener
            }

            // Lowercase
            if (!password.any { it.isLowerCase() }) {

                Log.w(
                    TAG,
                    "Registration validation failed: password has no lowercase letter"
                )

                etPassword.error =
                    "Password needs a lowercase letter"

                etPassword.requestFocus()

                return@setOnClickListener
            }

            // Number
            if (!password.any { it.isDigit() }) {

                Log.w(
                    TAG,
                    "Registration validation failed: password has no number"
                )

                etPassword.error =
                    "Password needs a number"

                etPassword.requestFocus()

                return@setOnClickListener
            }

            // Special character
            if (!password.any { !it.isLetterOrDigit() }) {

                Log.w(
                    TAG,
                    "Registration validation failed: password has no special character"
                )

                etPassword.error =
                    "Password needs a special character"

                etPassword.requestFocus()

                return@setOnClickListener
            }

            Log.d(TAG, "Password security validation successful")

            // --------------------------------------------------
            // CHECK CONFIRM PASSWORD
            // --------------------------------------------------

            if (confirmPassword.isEmpty()) {

                Log.w(
                    TAG,
                    "Registration validation failed: confirmation password is empty"
                )

                etConfirmPassword.error =
                    "Please confirm your password"

                etConfirmPassword.requestFocus()

                return@setOnClickListener
            }

            // Compare passwords
            if (password != confirmPassword) {

                Log.w(
                    TAG,
                    "Registration validation failed: passwords do not match"
                )

                etConfirmPassword.error =
                    "Passwords do not match"

                etConfirmPassword.requestFocus()

                return@setOnClickListener
            }

            Log.d(TAG, "Password confirmation successful")

            // --------------------------------------------------
            // DISABLE BUTTON WHILE REGISTERING
            // --------------------------------------------------

            btnCreateAccount.isEnabled = false
            btnCreateAccount.text = "Creating Account..."

            Log.d(
                TAG,
                "Create Account button disabled while registration is processing"
            )

            // --------------------------------------------------
            // CREATE FIREBASE AUTHENTICATION ACCOUNT
            // --------------------------------------------------

            Log.d(TAG, "Starting Firebase account creation")

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Log.d(
                            TAG,
                            "Firebase Authentication account created successfully"
                        )

                        // Get the newly created Firebase user
                        val firebaseUser = auth.currentUser

                        if (firebaseUser == null) {

                            Log.e(
                                TAG,
                                "Account creation reported success but current Firebase user is null"
                            )

                            btnCreateAccount.isEnabled = true
                            btnCreateAccount.text = "Create Account"

                            Toast.makeText(
                                this,
                                "Account creation failed. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnCompleteListener
                        }

                        // Firebase automatically gives the user a unique ID
                        val userId = firebaseUser.uid

                        Log.d(
                            TAG,
                            "Firebase user created successfully with unique user ID"
                        )

                        // --------------------------------------------------
                        // CREATE USER PROFILE FOR FIRESTORE
                        // --------------------------------------------------

                        Log.d(
                            TAG,
                            "Preparing personal user profile for Firestore"
                        )

                        val user = hashMapOf(
                            "userId" to userId,
                            "fullName" to fullName,
                            "email" to email,
                            "accountType" to "Personal",
                            "createdAt" to System.currentTimeMillis()
                        )

                        // --------------------------------------------------
                        // SAVE USER PROFILE TO FIRESTORE
                        // --------------------------------------------------

                        Log.d(
                            TAG,
                            "Saving personal user profile to Firestore"
                        )

                        db.collection("Users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener {

                                Log.d(
                                    TAG,
                                    "Personal user profile saved successfully to Firestore"
                                )

                                Toast.makeText(
                                    this,
                                    "Account created successfully!",
                                    Toast.LENGTH_LONG
                                ).show()

                                // --------------------------------------------------
                                // OPEN PERSONAL LOGIN
                                // --------------------------------------------------

                                Log.d(
                                    TAG,
                                    "Opening Personal Login screen after successful registration"
                                )

                                val intent = Intent(
                                    this,
                                    PersonalLoginActivity::class.java
                                )

                                // Pass registered email to login screen
                                intent.putExtra(
                                    "registeredEmail",
                                    email
                                )

                                startActivity(intent)

                                Log.d(
                                    TAG,
                                    "Personal Login screen opened successfully"
                                )

                                finish()
                            }
                            .addOnFailureListener { exception ->

                                Log.e(
                                    TAG,
                                    "Failed to save personal user profile to Firestore",
                                    exception
                                )

                                btnCreateAccount.isEnabled = true
                                btnCreateAccount.text = "Create Account"

                                Toast.makeText(
                                    this,
                                    "Account created, but profile could not be saved: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                    } else {

                        Log.e(
                            TAG,
                            "Firebase account creation failed",
                            task.exception
                        )

                        btnCreateAccount.isEnabled = true
                        btnCreateAccount.text = "Create Account"

                        // --------------------------------------------------
                        // FIREBASE REGISTRATION ERROR
                        // --------------------------------------------------

                        val errorMessage = when {
                            task.exception?.message?.contains(
                                "email address is already in use",
                                ignoreCase = true
                            ) == true -> {

                                Log.w(
                                    TAG,
                                    "Registration failed because the email address is already registered"
                                )

                                "This email address is already registered."
                            }

                            task.exception?.message?.contains(
                                "badly formatted",
                                ignoreCase = true
                            ) == true -> {

                                Log.w(
                                    TAG,
                                    "Registration failed because the email address was badly formatted"
                                )

                                "Please enter a valid email address."
                            }

                            task.exception?.message?.contains(
                                "network",
                                ignoreCase = true
                            ) == true -> {

                                Log.w(
                                    TAG,
                                    "Registration failed because of a network error"
                                )

                                "Network error. Please check your internet connection."
                            }

                            else -> {

                                Log.w(
                                    TAG,
                                    "Registration failed with an unspecified Firebase error"
                                )

                                "Registration failed. Please try again."
                            }
                        }

                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // --------------------------------------------------
        // LOGIN LINK
        // --------------------------------------------------

        tvLogin.setOnClickListener {

            Log.d(TAG, "Login link selected")

            val intent = Intent(
                this,
                PersonalLoginActivity::class.java
            )

            startActivity(intent)

            Log.d(
                TAG,
                "Personal Login screen opened from registration"
            )

            finish()
        }
    }

    // --------------------------------------------------
    // PASSWORD VISIBILITY TOGGLE
    // --------------------------------------------------

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

                            // Show password
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

                            // Hide password
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