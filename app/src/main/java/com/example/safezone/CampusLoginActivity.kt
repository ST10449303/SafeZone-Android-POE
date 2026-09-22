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

class CampusLoginActivity : AppCompatActivity() {

    // ==========================================
    // INPUT FIELDS
    // ==========================================

    private lateinit var etStudentEmail: EditText
    private lateinit var etPassword: EditText

    // ==========================================
    // CHECKBOX
    // ==========================================

    private lateinit var cbRememberMe: CheckBox

    // ==========================================
    // BUTTONS / TEXT
    // ==========================================

    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    // ==========================================
    // FIREBASE AUTHENTICATION
    // ==========================================

    private lateinit var auth: FirebaseAuth

    // ==========================================
    // PASSWORD VISIBILITY
    // ==========================================

    private var isPasswordVisible = false

    // ==========================================
    // LOGGING
    // ==========================================

    companion object {
        private const val TAG = "SafeZoneCampusLogin"
    }

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_login
        )

        Log.d(
            TAG,
            "Campus Login screen opened"
        )

        // ==========================================
        // INITIALISE FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()

        Log.d(
            TAG,
            "Firebase Authentication initialized"
        )

        // ==========================================
        // CONNECT XML CONTROLS
        // ==========================================

        etStudentEmail =
            findViewById(R.id.etStudentEmail)

        etPassword =
            findViewById(R.id.etPassword)

        cbRememberMe =
            findViewById(R.id.cbRememberMe)

        btnLogin =
            findViewById(R.id.btnLogin)

        tvForgotPassword =
            findViewById(R.id.tvForgotPassword)

        tvRegister =
            findViewById(R.id.tvRegister)

        Log.d(
            TAG,
            "Campus Login screen controls connected successfully"
        )

        // ==========================================
        // SETUP PASSWORD EYE
        // ==========================================

        setupPasswordVisibility()

        Log.d(
            TAG,
            "Password visibility control initialized"
        )

        // ==========================================
        // LOGIN
        // ==========================================

        btnLogin.setOnClickListener {

            Log.d(
                TAG,
                "Campus Login button clicked"
            )

            loginUser()
        }

        // ==========================================
        // FORGOT PASSWORD
        // ==========================================

        tvForgotPassword.setOnClickListener {

            Log.d(
                TAG,
                "Forgot Password link selected"
            )

            val intent =
                Intent(
                    this,
                    CampusForgotPasswordActivity::class.java
                )

            startActivity(intent)

            Log.d(
                TAG,
                "Campus Forgot Password screen opened"
            )
        }

        // ==========================================
        // REGISTER
        // ==========================================

        tvRegister.setOnClickListener {

            Log.d(
                TAG,
                "Register link selected"
            )

            val intent =
                Intent(
                    this,
                    CampusRegistrationActivity::class.java
                )

            startActivity(intent)

            Log.d(
                TAG,
                "Campus Registration screen opened"
            )
        }
    }

    // ==================================================
    // PASSWORD VISIBILITY
    // ==================================================

    private fun setupPasswordVisibility() {

        // Start with password hidden
        isPasswordVisible = false

        etPassword.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Make sure the eye-off icon is displayed
        etPassword.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_lock,
            0,
            R.drawable.ic_visibility_off,
            0
        )

        Log.d(
            TAG,
            "Password field initialized in hidden mode"
        )

        // Make the eye icon clickable
        etPassword.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd =
                    etPassword.compoundDrawables[2]

                if (drawableEnd != null) {

                    val drawableWidth =
                        drawableEnd.bounds.width()

                    val touchPosition =
                        event.x

                    val iconStartPosition =
                        etPassword.width -
                                etPassword.paddingEnd -
                                drawableWidth -
                                20

                    // Check whether the user tapped
                    // the eye icon
                    if (touchPosition >= iconStartPosition) {

                        Log.d(
                            TAG,
                            "Password visibility icon clicked"
                        )

                        togglePasswordVisibility()

                        return@setOnTouchListener true
                    }
                }
            }

            false
        }
    }

    // ==================================================
    // TOGGLE PASSWORD VISIBILITY
    // ==================================================

    private fun togglePasswordVisibility() {

        isPasswordVisible =
            !isPasswordVisible

        if (isPasswordVisible) {

            // ==========================================
            // SHOW PASSWORD
            // ==========================================

            etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility,
                0
            )

            Log.d(
                TAG,
                "Campus password visibility enabled"
            )

        } else {

            // ==========================================
            // HIDE PASSWORD
            // ==========================================

            etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD

            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility_off,
                0
            )

            Log.d(
                TAG,
                "Campus password visibility disabled"
            )
        }

        // Keep cursor at the end of the password
        etPassword.setSelection(
            etPassword.text.length
        )
    }

    // ==================================================
    // LOGIN USER
    // ==================================================

    private fun loginUser() {

        Log.d(
            TAG,
            "Starting campus login validation"
        )

        // ==========================================
        // GET INPUT VALUES
        // ==========================================

        val email =
            etStudentEmail.text
                .toString()
                .trim()

        val password =
            etPassword.text
                .toString()

        // ==========================================
        // VALIDATE EMAIL
        // ==========================================

        if (email.isEmpty()) {

            Log.w(
                TAG,
                "Login validation failed: student email is empty"
            )

            etStudentEmail.error =
                "Please enter your student email"

            etStudentEmail.requestFocus()

            return
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            Log.w(
                TAG,
                "Login validation failed: invalid student email format"
            )

            etStudentEmail.error =
                "Please enter a valid email address"

            etStudentEmail.requestFocus()

            return
        }

        Log.d(
            TAG,
            "Student email validation successful"
        )

        // ==========================================
        // VALIDATE PASSWORD
        // ==========================================

        if (password.isEmpty()) {

            Log.w(
                TAG,
                "Login validation failed: password is empty"
            )

            etPassword.error =
                "Please enter your password"

            etPassword.requestFocus()

            return
        }

        Log.d(
            TAG,
            "Password validation successful"
        )

        // ==========================================
        // REMEMBER ME
        // ==========================================

        if (cbRememberMe.isChecked) {

            Log.d(
                TAG,
                "Remember Me option selected"
            )

        } else {

            Log.d(
                TAG,
                "Remember Me option not selected"
            )
        }

        // ==========================================
        // DISABLE LOGIN BUTTON
        // ==========================================

        btnLogin.isEnabled = false

        btnLogin.text =
            "Signing In..."

        Log.d(
            TAG,
            "Login button disabled while authentication is processing"
        )

        // ==========================================
        // FIREBASE LOGIN
        // ==========================================

        Log.d(
            TAG,
            "Starting Firebase campus authentication"
        )

        auth.signInWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {

                // ==================================
                // LOGIN SUCCESSFUL
                // ==================================

                Log.d(
                    TAG,
                    "Firebase campus authentication successful"
                )

                val firebaseUser =
                    auth.currentUser

                if (firebaseUser != null) {

                    Log.d(
                        TAG,
                        "Firebase campus user authenticated successfully"
                    )

                } else {

                    Log.w(
                        TAG,
                        "Firebase login succeeded but current user is null"
                    )
                }

                Toast.makeText(
                    this,
                    "Campus login successful!",
                    Toast.LENGTH_SHORT
                ).show()

                // ==================================
                // OPEN CAMPUS HOME
                // ==================================

                Log.d(
                    TAG,
                    "Preparing to open Campus Home screen"
                )

                val intent =
                    Intent(
                        this@CampusLoginActivity,
                        CampusHomeActivity::class.java
                    )

                // Prevent the user from returning
                // to the login screen using Back

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                Log.d(
                    TAG,
                    "Campus Home screen opened successfully"
                )

                finish()
            }
            .addOnFailureListener { exception ->

                // ==================================
                // LOGIN FAILED
                // ==================================

                Log.e(
                    TAG,
                    "Firebase campus authentication failed",
                    exception
                )

                btnLogin.isEnabled = true

                btnLogin.text =
                    "Login"

                val errorMessage =
                    getLoginErrorMessage(
                        exception.message
                    )

                Log.w(
                    TAG,
                    "Displaying campus login error message to user"
                )

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==================================================
    // FIREBASE ERROR MESSAGE
    // ==================================================

    private fun getLoginErrorMessage(
        errorMessage: String?
    ): String {

        Log.d(
            TAG,
            "Processing Firebase login error"
        )

        val message =
            errorMessage?.lowercase()
                ?: ""

        return when {

            message.contains(
                "invalid-credential"
            ) -> {

                Log.w(
                    TAG,
                    "Login error: invalid credentials"
                )

                "Incorrect email or password."
            }

            message.contains(
                "invalid-email"
            ) -> {

                Log.w(
                    TAG,
                    "Login error: invalid email"
                )

                "Please enter a valid email address."
            }

            message.contains(
                "user-not-found"
            ) -> {

                Log.w(
                    TAG,
                    "Login error: user account not found"
                )

                "No account was found with this email."
            }

            message.contains(
                "wrong-password"
            ) -> {

                Log.w(
                    TAG,
                    "Login error: incorrect password"
                )

                "Incorrect password."
            }

            message.contains(
                "too-many-requests"
            ) -> {

                Log.w(
                    TAG,
                    "Login error: too many authentication attempts"
                )

                "Too many login attempts. Please try again later."
            }

            message.contains(
                "network"
            ) -> {

                Log.w(
                    TAG,
                    "Login error: network problem"
                )

                "Network error. Please check your internet connection."
            }

            else -> {

                Log.w(
                    TAG,
                    "Login error: unspecified Firebase authentication error"
                )

                "Login failed. Please check your email and password."
            }
        }
    }

    // ==================================================
    // ON RESUME
    // ==================================================

    override fun onResume() {

        super.onResume()

        Log.d(
            TAG,
            "Campus Login screen resumed"
        )

        // ==========================================
        // RESET LOGIN BUTTON
        // ==========================================

        btnLogin.isEnabled = true

        btnLogin.text =
            "Login"

        Log.d(
            TAG,
            "Login button reset and enabled"
        )
    }
}