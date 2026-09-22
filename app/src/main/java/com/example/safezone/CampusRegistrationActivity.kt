package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampusRegistrationActivity : AppCompatActivity() {

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // =========================================================
    // INPUT FIELDS
    // =========================================================

    private lateinit var etFullName: EditText
    private lateinit var etStudentEmail: EditText
    private lateinit var spInstitution: Spinner
    private lateinit var spCampus: Spinner
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    // =========================================================
    // BUTTONS / TEXT
    // =========================================================

    private lateinit var tvBack: TextView
    private lateinit var tvAlreadyRegistered: TextView
    private lateinit var tvLogin: TextView
    private lateinit var btnRegister: AppCompatButton

    // =========================================================
    // PASSWORD VISIBILITY
    // =========================================================

    private var passwordVisible = false
    private var confirmPasswordVisible = false

    // =========================================================
    // LOGGING
    // =========================================================

    companion object {
        private const val TAG = "SafeZoneCampusRegistration"
    }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_campus_registration
        )

        Log.d(
            TAG,
            "Campus Registration screen opened"
        )

        // =====================================================
        // FIREBASE
        // =====================================================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Log.d(
            TAG,
            "Firebase Authentication and Firestore initialized"
        )

        // =====================================================
        // INITIALIZE VIEWS
        // =====================================================

        initializeViews()

        Log.d(
            TAG,
            "Campus Registration screen controls connected successfully"
        )

        // =====================================================
        // SETUP DROPDOWNS
        // =====================================================

        setupInstitutionSpinner()
        setupCampusSpinner()

        Log.d(
            TAG,
            "Institution and campus dropdowns initialized"
        )

        // =====================================================
        // PASSWORD VISIBILITY
        // =====================================================

        setupPasswordVisibility()

        Log.d(
            TAG,
            "Password visibility controls initialized"
        )

        // =====================================================
        // BUTTONS
        // =====================================================

        setupButtons()

        Log.d(
            TAG,
            "Campus Registration screen buttons initialized"
        )
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        tvBack =
            findViewById(R.id.tvBack)

        tvAlreadyRegistered =
            findViewById(R.id.tvAlreadyRegistered)

        tvLogin =
            findViewById(R.id.tvLogin)

        etFullName =
            findViewById(R.id.etFullName)

        etStudentEmail =
            findViewById(R.id.etStudentEmail)

        spInstitution =
            findViewById(R.id.spInstitution)

        spCampus =
            findViewById(R.id.spCampus)

        etPassword =
            findViewById(R.id.etPassword)

        etConfirmPassword =
            findViewById(R.id.etConfirmPassword)

        btnRegister =
            findViewById(R.id.btnRegister)

        Log.d(
            TAG,
            "All registration XML controls found successfully"
        )
    }

    // =========================================================
    // INSTITUTION DROPDOWN
    // =========================================================

    private fun setupInstitutionSpinner() {

        Log.d(
            TAG,
            "Setting up institution dropdown"
        )

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.institutions,
            android.R.layout.simple_spinner_item
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spInstitution.adapter = adapter

        Log.d(
            TAG,
            "Institution dropdown populated successfully"
        )
    }

    // =========================================================
    // CAMPUS DROPDOWN
    // =========================================================

    private fun setupCampusSpinner() {

        Log.d(
            TAG,
            "Setting up campus dropdown"
        )

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.campuses,
            android.R.layout.simple_spinner_item
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCampus.adapter = adapter

        Log.d(
            TAG,
            "Campus dropdown populated successfully"
        )
    }

    // =========================================================
    // PASSWORD VISIBILITY
    // =========================================================

    private fun setupPasswordVisibility() {

        // -----------------------------------------------------
        // PASSWORD
        // -----------------------------------------------------

        etPassword.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd =
                    etPassword.compoundDrawables[2]

                if (
                    drawableEnd != null &&
                    event.x >=
                    etPassword.width -
                    etPassword.paddingEnd -
                    drawableEnd.bounds.width()
                ) {

                    Log.d(
                        TAG,
                        "Password visibility icon clicked"
                    )

                    togglePasswordVisibility()

                    return@setOnTouchListener true
                }
            }

            false
        }

        // -----------------------------------------------------
        // CONFIRM PASSWORD
        // -----------------------------------------------------

        etConfirmPassword.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd =
                    etConfirmPassword.compoundDrawables[2]

                if (
                    drawableEnd != null &&
                    event.x >=
                    etConfirmPassword.width -
                    etConfirmPassword.paddingEnd -
                    drawableEnd.bounds.width()
                ) {

                    Log.d(
                        TAG,
                        "Confirm password visibility icon clicked"
                    )

                    toggleConfirmPasswordVisibility()

                    return@setOnTouchListener true
                }
            }

            false
        }
    }

    // =========================================================
    // TOGGLE PASSWORD
    // =========================================================

    private fun togglePasswordVisibility() {

        passwordVisible =
            !passwordVisible

        if (passwordVisible) {

            // -------------------------------------------------
            // SHOW PASSWORD
            // -------------------------------------------------

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
                "Campus registration password visibility enabled"
            )

        } else {

            // -------------------------------------------------
            // HIDE PASSWORD
            // -------------------------------------------------

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
                "Campus registration password visibility disabled"
            )
        }

        etPassword.setSelection(
            etPassword.text.length
        )
    }

    // =========================================================
    // TOGGLE CONFIRM PASSWORD
    // =========================================================

    private fun toggleConfirmPasswordVisibility() {

        confirmPasswordVisible =
            !confirmPasswordVisible

        if (confirmPasswordVisible) {

            // -------------------------------------------------
            // SHOW CONFIRM PASSWORD
            // -------------------------------------------------

            etConfirmPassword.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility,
                0
            )

            Log.d(
                TAG,
                "Confirm password visibility enabled"
            )

        } else {

            // -------------------------------------------------
            // HIDE CONFIRM PASSWORD
            // -------------------------------------------------

            etConfirmPassword.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD

            etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock,
                0,
                R.drawable.ic_visibility_off,
                0
            )

            Log.d(
                TAG,
                "Confirm password visibility disabled"
            )
        }

        etConfirmPassword.setSelection(
            etConfirmPassword.text.length
        )
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private fun setupButtons() {

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        tvBack.setOnClickListener {

            Log.d(
                TAG,
                "Back button selected"
            )

            finish()

            Log.d(
                TAG,
                "Campus Registration screen closed"
            )
        }

        // -----------------------------------------------------
        // LOGIN
        // -----------------------------------------------------

        tvLogin.setOnClickListener {

            Log.d(
                TAG,
                "Login link selected"
            )

            openCampusLogin()
        }

        // -----------------------------------------------------
        // ALREADY REGISTERED
        // -----------------------------------------------------

        tvAlreadyRegistered.setOnClickListener {

            Log.d(
                TAG,
                "Already Registered link selected"
            )

            openCampusLogin()
        }

        // -----------------------------------------------------
        // REGISTER
        // -----------------------------------------------------

        btnRegister.setOnClickListener {

            Log.d(
                TAG,
                "Create Campus Account button clicked"
            )

            registerCampusUser()
        }
    }

    // =========================================================
    // OPEN CAMPUS LOGIN
    // =========================================================

    private fun openCampusLogin() {

        Log.d(
            TAG,
            "Preparing to open Campus Login screen"
        )

        val intent = Intent(
            this,
            CampusLoginActivity::class.java
        )

        startActivity(intent)

        Log.d(
            TAG,
            "Campus Login screen opened successfully"
        )

        finish()
    }

    // =========================================================
    // REGISTER CAMPUS USER
    // =========================================================

    private fun registerCampusUser() {

        Log.d(
            TAG,
            "Starting campus registration validation"
        )

        // =====================================================
        // GET INPUT VALUES
        // =====================================================

        val fullName =
            etFullName.text
                .toString()
                .trim()

        val email =
            etStudentEmail.text
                .toString()
                .trim()

        val password =
            etPassword.text
                .toString()

        val confirmPassword =
            etConfirmPassword.text
                .toString()

        Log.d(
            TAG,
            "Registration form values collected"
        )

        // =====================================================
        // GET SELECTED INSTITUTION
        // =====================================================

        val institution =
            if (spInstitution.selectedItem != null) {

                spInstitution.selectedItem
                    .toString()
                    .trim()

            } else {

                ""
            }

        // =====================================================
        // GET SELECTED CAMPUS
        // =====================================================

        val campus =
            if (spCampus.selectedItem != null) {

                spCampus.selectedItem
                    .toString()
                    .trim()

            } else {

                ""
            }

        // =====================================================
        // FULL NAME VALIDATION
        // =====================================================

        if (fullName.isEmpty()) {

            Log.w(
                TAG,
                "Registration validation failed: full name is empty"
            )

            etFullName.error =
                "Enter your full name"

            etFullName.requestFocus()

            return
        }

        Log.d(
            TAG,
            "Full name validation successful"
        )

        // =====================================================
        // EMAIL VALIDATION
        // =====================================================

        if (email.isEmpty()) {

            Log.w(
                TAG,
                "Registration validation failed: student email is empty"
            )

            etStudentEmail.error =
                "Enter your student email"

            etStudentEmail.requestFocus()

            return
        }

        /*
         * PROTOTYPE EMAIL VALIDATION
         *
         * Any valid email address can be used.
         *
         * No domain restriction is applied.
         */

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            Log.w(
                TAG,
                "Registration validation failed: invalid email format"
            )

            etStudentEmail.error =
                "Enter a valid email address"

            etStudentEmail.requestFocus()

            return
        }

        Log.d(
            TAG,
            "Student email validation successful"
        )

        // =====================================================
        // INSTITUTION VALIDATION
        // =====================================================

        if (
            institution.isEmpty() ||
            institution.equals(
                "Select Institution",
                ignoreCase = true
            )
        ) {

            Log.w(
                TAG,
                "Registration validation failed: institution was not selected"
            )

            Toast.makeText(
                this,
                "Please select an institution.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Log.d(
            TAG,
            "Institution selection validation successful"
        )

        // =====================================================
        // CAMPUS VALIDATION
        // =====================================================

        if (
            campus.isEmpty() ||
            campus.equals(
                "Select Campus",
                ignoreCase = true
            )
        ) {

            Log.w(
                TAG,
                "Registration validation failed: campus was not selected"
            )

            Toast.makeText(
                this,
                "Please select a campus.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Log.d(
            TAG,
            "Campus selection validation successful"
        )

        // =====================================================
        // PASSWORD VALIDATION
        // =====================================================

        if (password.isEmpty()) {

            Log.w(
                TAG,
                "Registration validation failed: password is empty"
            )

            etPassword.error =
                "Create a password"

            etPassword.requestFocus()

            return
        }

        if (password.length < 6) {

            Log.w(
                TAG,
                "Registration validation failed: password is shorter than 6 characters"
            )

            etPassword.error =
                "Password must contain at least 6 characters"

            etPassword.requestFocus()

            return
        }

        Log.d(
            TAG,
            "Password validation successful"
        )

        // =====================================================
        // CONFIRM PASSWORD
        // =====================================================

        if (confirmPassword.isEmpty()) {

            Log.w(
                TAG,
                "Registration validation failed: confirmation password is empty"
            )

            etConfirmPassword.error =
                "Confirm your password"

            etConfirmPassword.requestFocus()

            return
        }

        if (password != confirmPassword) {

            Log.w(
                TAG,
                "Registration validation failed: passwords do not match"
            )

            etConfirmPassword.error =
                "Passwords do not match"

            etConfirmPassword.requestFocus()

            return
        }

        Log.d(
            TAG,
            "Password confirmation validation successful"
        )

        // =====================================================
        // DISABLE REGISTER BUTTON
        // =====================================================

        btnRegister.isEnabled = false

        btnRegister.text =
            "Creating Account..."

        Log.d(
            TAG,
            "Register button disabled while registration is processing"
        )

        // =====================================================
        // CREATE FIREBASE ACCOUNT
        // =====================================================

        Log.d(
            TAG,
            "Starting Firebase campus account creation"
        )

        auth.createUserWithEmailAndPassword(
            email,
            password
        )
            .addOnCompleteListener { task ->

                // =================================================
                // REGISTRATION SUCCESSFUL
                // =================================================

                if (task.isSuccessful) {

                    Log.d(
                        TAG,
                        "Firebase campus account created successfully"
                    )

                    val currentUser =
                        auth.currentUser

                    if (currentUser == null) {

                        Log.e(
                            TAG,
                            "Account creation succeeded but current Firebase user is null"
                        )

                        registrationFailed(
                            "Registration failed. Please try again."
                        )

                        return@addOnCompleteListener
                    }

                    // =================================================
                    // FIREBASE USER ID
                    // =================================================

                    val userId =
                        currentUser.uid

                    Log.d(
                        TAG,
                        "Firebase campus user created successfully with unique user ID"
                    )

                    // =================================================
                    // CREATE USER PROFILE
                    // =================================================

                    Log.d(
                        TAG,
                        "Preparing campus user profile for Firestore"
                    )

                    val userData =
                        hashMapOf(

                            "userId" to userId,

                            "fullName" to fullName,

                            "email" to email,

                            "institution" to institution,

                            "campus" to campus,

                            "accountType" to "Campus",

                            "createdAt" to
                                    System.currentTimeMillis()
                        )

                    // =================================================
                    // SAVE USER PROFILE TO FIRESTORE
                    // =================================================

                    Log.d(
                        TAG,
                        "Saving campus user profile to Firestore"
                    )

                    db.collection("Users")
                        .document(userId)
                        .set(userData)
                        .addOnSuccessListener {

                            Log.d(
                                TAG,
                                "Campus user profile saved successfully to Firestore"
                            )

                            Toast.makeText(
                                this,
                                "Campus account created successfully!",
                                Toast.LENGTH_LONG
                            ).show()

                            // =========================================
                            // SIGN OUT AFTER REGISTRATION
                            // =========================================

                            Log.d(
                                TAG,
                                "Signing out Firebase user after registration"
                            )

                            auth.signOut()

                            Log.d(
                                TAG,
                                "Firebase user signed out successfully"
                            )

                            // =========================================
                            // GO TO CAMPUS LOGIN
                            // =========================================

                            Log.d(
                                TAG,
                                "Preparing to open Campus Login screen"
                            )

                            val intent =
                                Intent(
                                    this,
                                    CampusLoginActivity::class.java
                                )

                            intent.putExtra(
                                "registeredEmail",
                                email
                            )

                            startActivity(intent)

                            Log.d(
                                TAG,
                                "Campus Login screen opened successfully after registration"
                            )

                            finish()
                        }
                        .addOnFailureListener { exception ->

                            Log.e(
                                TAG,
                                "Failed to save campus user profile to Firestore",
                                exception
                            )

                            btnRegister.isEnabled =
                                true

                            btnRegister.text =
                                "Create Campus Account"

                            Toast.makeText(
                                this,
                                "Account created, but profile information could not be saved.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                } else {

                    // =================================================
                    // REGISTRATION FAILED
                    // =================================================

                    Log.e(
                        TAG,
                        "Firebase campus account creation failed",
                        task.exception
                    )

                    btnRegister.isEnabled =
                        true

                    btnRegister.text =
                        "Create Campus Account"

                    val message =
                        task.exception?.message
                            ?: "Registration failed."

                    val errorMessage =
                        getRegistrationErrorMessage(message)

                    Log.w(
                        TAG,
                        "Displaying Firebase registration error to user"
                    )

                    Toast.makeText(
                        this,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // =========================================================
    // REGISTRATION FAILED
    // =========================================================

    private fun registrationFailed(
        message: String
    ) {

        Log.e(
            TAG,
            "Campus registration failed: $message"
        )

        btnRegister.isEnabled =
            true

        btnRegister.text =
            "Create Campus Account"

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    // =========================================================
    // FIREBASE ERROR MESSAGE
    // =========================================================

    private fun getRegistrationErrorMessage(
        message: String
    ): String {

        Log.d(
            TAG,
            "Processing Firebase campus registration error"
        )

        return when {

            message.contains(
                "already in use",
                ignoreCase = true
            ) -> {

                Log.w(
                    TAG,
                    "Registration error: email address already registered"
                )

                "This email is already registered."
            }

            message.contains(
                "badly formatted",
                ignoreCase = true
            ) -> {

                Log.w(
                    TAG,
                    "Registration error: email address badly formatted"
                )

                "Please enter a valid email address."
            }

            message.contains(
                "password",
                ignoreCase = true
            ) -> {

                Log.w(
                    TAG,
                    "Registration error: password does not meet Firebase requirements"
                )

                "Password must contain at least 6 characters."
            }

            message.contains(
                "network",
                ignoreCase = true
            ) -> {

                Log.w(
                    TAG,
                    "Registration error: network problem"
                )

                "Network error. Please check your internet connection."
            }

            else -> {

                Log.w(
                    TAG,
                    "Registration error: unspecified Firebase error"
                )

                "Registration failed. Please try again."
            }
        }
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        Log.d(
            TAG,
            "Campus Registration screen resumed"
        )

        btnRegister.isEnabled =
            true

        btnRegister.text =
            "Create Campus Account"

        Log.d(
            TAG,
            "Register button reset and enabled"
        )
    }
}