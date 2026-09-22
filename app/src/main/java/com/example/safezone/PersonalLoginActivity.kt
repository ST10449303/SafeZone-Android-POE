package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalLoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvCreateAccount: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var passwordVisible = false

    // --------------------------------------------------
    // LOGGING
    // --------------------------------------------------

    companion object {
        private const val TAG = "SafeZonePersonalLogin"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_personal_login)

        Log.d(TAG, "Personal Login screen opened")

        // --------------------------------------------------
        // CONNECT XML CONTROLS
        // --------------------------------------------------

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)

        Log.d(TAG, "Login screen controls connected successfully")

        // --------------------------------------------------
        // INITIALISE FIREBASE
        // --------------------------------------------------

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Log.d(TAG, "Firebase Authentication and Firestore initialized")

        // --------------------------------------------------
        // GET REGISTERED EMAIL
        // --------------------------------------------------

        val registeredEmail = intent.getStringExtra("registeredEmail")

        if (!registeredEmail.isNullOrEmpty()) {
            etEmail.setText(registeredEmail)

            Log.d(TAG, "Registered email received from registration screen")
        }

        // --------------------------------------------------
        // PASSWORD VISIBILITY
        // --------------------------------------------------

        etPassword.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd =
                    etPassword.compoundDrawablesRelative[2]

                if (drawableEnd != null) {

                    val drawableWidth =
                        drawableEnd.bounds.width()

                    val touchArea =
                        etPassword.width -
                                etPassword.paddingEnd -
                                drawableWidth

                    if (event.x >= touchArea) {

                        passwordVisible = !passwordVisible

                        if (passwordVisible) {

                            // SHOW PASSWORD
                            etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or
                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_lock,
                                0,
                                R.drawable.ic_visibility,
                                0
                            )

                            Log.d(TAG, "Password visibility enabled")

                        } else {

                            // HIDE PASSWORD
                            etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or
                                        InputType.TYPE_TEXT_VARIATION_PASSWORD

                            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_lock,
                                0,
                                R.drawable.ic_visibility_off,
                                0
                            )

                            Log.d(TAG, "Password visibility disabled")
                        }

                        // Keep cursor at the end
                        etPassword.setSelection(
                            etPassword.text.length
                        )

                        return@setOnTouchListener true
                    }
                }
            }

            false
        }

        // --------------------------------------------------
        // LOGIN BUTTON
        // --------------------------------------------------

        btnLogin.setOnClickListener {

            Log.d(TAG, "Login button clicked")

            val email =
                etEmail.text.toString().trim()

            val password =
                etPassword.text.toString()

            // --------------------------------------------------
            // EMAIL VALIDATION
            // --------------------------------------------------

            if (email.isEmpty()) {

                Log.w(TAG, "Login validation failed: email is empty")

                etEmail.error =
                    "Please enter your email address"

                etEmail.requestFocus()

                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                Log.w(TAG, "Login validation failed: invalid email format")

                etEmail.error =
                    "Please enter a valid email address"

                etEmail.requestFocus()

                return@setOnClickListener
            }

            // --------------------------------------------------
            // PASSWORD VALIDATION
            // --------------------------------------------------

            if (password.isEmpty()) {

                Log.w(TAG, "Login validation failed: password is empty")

                etPassword.error =
                    "Please enter your password"

                etPassword.requestFocus()

                return@setOnClickListener
            }

            Log.d(TAG, "Login input validation successful")

            // --------------------------------------------------
            // DISABLE LOGIN BUTTON
            // --------------------------------------------------

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            Log.d(TAG, "Login button disabled while authentication is processing")

            // --------------------------------------------------
            // FIREBASE LOGIN
            // --------------------------------------------------

            Log.d(TAG, "Starting Firebase authentication")

            auth.signInWithEmailAndPassword(
                email,
                password
            ).addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    Log.d(TAG, "Firebase authentication successful")

                    val firebaseUser =
                        auth.currentUser

                    if (firebaseUser == null) {

                        Log.e(
                            TAG,
                            "Authentication reported success but current Firebase user is null"
                        )

                        btnLogin.isEnabled = true
                        btnLogin.text = "Login"

                        Toast.makeText(
                            this,
                            "Login failed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    val userId =
                        firebaseUser.uid

                    Log.d(TAG, "Firebase user authenticated successfully")

                    // --------------------------------------------------
                    // GET USER PROFILE FROM FIRESTORE
                    // --------------------------------------------------

                    Log.d(TAG, "Loading personal user profile from Firestore")

                    db.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->

                            Log.d(TAG, "Firestore user profile loaded successfully")

                            val fullName =
                                document.getString("fullName")
                                    ?: "SafeZone User"

                            Log.d(TAG, "Preparing to open Personal Home screen")

                            // --------------------------------------------------
                            // OPEN PERSONAL DASHBOARD
                            // --------------------------------------------------

                            val intent =
                                Intent(
                                    this,
                                    PersonalHomeActivity::class.java
                                )

                            intent.putExtra(
                                "fullName",
                                fullName
                            )

                            startActivity(intent)

                            Log.d(TAG, "Personal Home screen opened successfully")

                            finish()
                        }
                        .addOnFailureListener { exception ->

                            Log.e(
                                TAG,
                                "Unable to load Firestore user profile. Continuing with default profile.",
                                exception
                            )

                            // Login succeeded even if the
                            // Firestore profile cannot be loaded.

                            val intent =
                                Intent(
                                    this,
                                    PersonalHomeActivity::class.java
                                )

                            intent.putExtra(
                                "fullName",
                                "SafeZone User"
                            )

                            startActivity(intent)

                            Log.d(
                                TAG,
                                "Personal Home screen opened with default user name"
                            )

                            finish()
                        }

                } else {

                    Log.e(
                        TAG,
                        "Firebase authentication failed",
                        task.exception
                    )

                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"

                    val error =
                        task.exception?.message ?: ""

                    val errorMessage = when {

                        error.contains(
                            "invalid credential",
                            ignoreCase = true
                        ) -> {
                            Log.w(
                                TAG,
                                "Authentication rejected because the credentials are invalid"
                            )

                            "Incorrect email or password."
                        }

                        error.contains(
                            "password",
                            ignoreCase = true
                        ) -> {
                            Log.w(
                                TAG,
                                "Authentication rejected because of a password-related error"
                            )

                            "Incorrect password."
                        }

                        error.contains(
                            "user",
                            ignoreCase = true
                        ) -> {
                            Log.w(
                                TAG,
                                "Authentication rejected because the user account could not be found"
                            )

                            "No account found with this email."
                        }

                        error.contains(
                            "network",
                            ignoreCase = true
                        ) -> {
                            Log.w(
                                TAG,
                                "Authentication failed because of a network error"
                            )

                            "Network error. Please check your internet connection."
                        }

                        else -> {
                            Log.w(
                                TAG,
                                "Authentication failed with an unspecified error"
                            )

                            "Login failed. Please check your email and password."
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
        // FORGOT PASSWORD
        // --------------------------------------------------

        tvForgotPassword.setOnClickListener {

            Log.d(TAG, "Forgot Password selected")

            val intent =
                Intent(
                    this,
                    ForgotPasswordActivity::class.java
                )

            startActivity(intent)
        }

        // --------------------------------------------------
        // CREATE ACCOUNT
        // --------------------------------------------------

        tvCreateAccount.setOnClickListener {

            Log.d(TAG, "Create Account selected")

            val intent =
                Intent(
                    this,
                    PersonalRegistrationActivity::class.java
                )

            startActivity(intent)
        }
    }
}