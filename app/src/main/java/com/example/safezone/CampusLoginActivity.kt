package com.example.safezone

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.safezone.data.OfflineLoginStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class CampusLoginActivity : AppCompatActivity() {

    private lateinit var etStudentEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager
    private lateinit var offlineLoginStore: OfflineLoginStore

    private var isPasswordVisible = false
    private var isAuthenticating = false

    companion object {
        private const val TAG = "SafeZoneCampusLogin"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            SafeZoneLanguageManager.wrapContext(newBase)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_login)

        etStudentEmail = findViewById(R.id.etStudentEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister = findViewById(R.id.tvRegister)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)

        offlineLoginStore = OfflineLoginStore(
            context = applicationContext,
            accountType = "Campus"
        )

        setupPasswordVisibility()

        btnLogin.setOnClickListener {
            loginUser()
        }

        btnGoogleSignIn.setOnClickListener {
            startGoogleSignIn()
        }

        tvForgotPassword.setOnClickListener {
            if (!isInternetAvailable()) {
                showMessage(getString(R.string.campus_login_internet_reset))
            } else {
                startActivity(
                    Intent(this, CampusForgotPasswordActivity::class.java)
                )
            }
        }

        tvRegister.setOnClickListener {
            startActivity(
                Intent(this, CampusRegistrationActivity::class.java)
            )
        }
    }

    // INTERNET CONNECTION

    private fun isInternetAvailable(): Boolean {
        val manager = getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    // PASSWORD VISIBILITY

    private fun setupPasswordVisibility() {
        isPasswordVisible = false

        etPassword.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        etPassword.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_lock,
            0,
            R.drawable.ic_visibility_off,
            0
        )

        etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = etPassword.compoundDrawables[2]

                if (drawableEnd != null) {
                    val iconStart = etPassword.width -
                            etPassword.paddingEnd -
                            drawableEnd.bounds.width() - 20

                    if (event.x >= iconStart) {
                        togglePasswordVisibility()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        etPassword.inputType =
            if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

        etPassword.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_lock,
            0,
            if (isPasswordVisible) {
                R.drawable.ic_visibility
            } else {
                R.drawable.ic_visibility_off
            },
            0
        )

        etPassword.setSelection(etPassword.text.length)
    }

    // EMAIL AND PASSWORD LOGIN

    private fun loginUser() {
        if (isAuthenticating) return

        val email = etStudentEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etStudentEmail.error =
                getString(R.string.campus_login_error_email_required)
            etStudentEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etStudentEmail.error =
                getString(R.string.campus_login_error_email_invalid)
            etStudentEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error =
                getString(R.string.campus_login_error_password_required)
            etPassword.requestFocus()
            return
        }

        if (!isInternetAvailable()) {
            openOfflineSession(email, password)
            return
        }

        setAuthenticating(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser

                if (user == null) {
                    showLoginError(
                        getString(R.string.campus_login_error_account_verify)
                    )
                    return@addOnSuccessListener
                }

                verifyCampusProfile(
                    uid = user.uid,
                    email = user.email ?: email,
                    password = password,
                    signInMethod = "email"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Email login failed", exception)
                showLoginError(getLoginErrorMessage(exception.message))
            }
    }

    // GOOGLE SIGN-IN

    private fun startGoogleSignIn() {
        if (isAuthenticating) return

        if (!isInternetAvailable()) {
            showMessage(
                getString(R.string.campus_login_google_internet)
            )
            return
        }

        setAuthenticating(true)

        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(
                        getString(R.string.default_web_client_id)
                    )
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = this@CampusLoginActivity,
                    request = request
                )

                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    firebaseGoogleSignIn(googleCredential.idToken)
                } else {
                    showLoginError(
                        getString(R.string.campus_login_google_invalid)
                    )
                }
            } catch (exception: GetCredentialException) {
                Log.e(TAG, "Google account selection failed", exception)
                showLoginError(
                    getString(R.string.campus_login_google_cancelled)
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Google sign-in failed", exception)
                showLoginError(
                    getString(R.string.campus_login_google_failed)
                )
            }
        }
    }

    private fun firebaseGoogleSignIn(idToken: String) {
        val firebaseCredential =
            GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnSuccessListener {
                val user = auth.currentUser

                if (user == null) {
                    showLoginError(
                        getString(R.string.campus_login_google_verify)
                    )
                    return@addOnSuccessListener
                }

                verifyCampusProfile(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    password = null,
                    signInMethod = "google"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Firebase Google authentication failed", exception)
                showLoginError(
                    getString(R.string.campus_login_google_auth_failed)
                )
            }
    }

    // VERIFY CAMPUS PROFILE ONLINE

    private fun verifyCampusProfile(
        uid: String,
        email: String,
        password: String?,
        signInMethod: String
    ) {
        db.collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    auth.signOut()
                    showLoginError(
                        getString(R.string.campus_login_profile_missing)
                    )
                    return@addOnSuccessListener
                }

                if (document.getString("accountType") != "Campus") {
                    auth.signOut()
                    showLoginError(
                        getString(R.string.campus_login_wrong_account_type)
                    )
                    return@addOnSuccessListener
                }

                val fullName =
                    document.getString("fullName")
                        ?: getString(R.string.campus_login_default_name)

                // Register the FCM token after verifying the campus profile.
                FCMTokenManager.saveToken("Campus")

                val offlineLoginSaved = saveVerifiedCampusProfile(
                    userId = uid,
                    email = email,
                    fullName = fullName,
                    password = password
                )

                if (signInMethod == "email") {
                    if (offlineLoginSaved) {
                        Log.d(TAG, "Campus offline credentials saved.")
                    } else {
                        Log.e(TAG, "Campus offline credentials could not be saved.")
                    }
                }

                setAuthenticating(false)

                val message = when {
                    signInMethod == "email" && !offlineLoginSaved ->
                        getString(R.string.campus_login_online_offline_setup_failed)

                    signInMethod == "email" ->
                        getString(R.string.campus_login_success_offline_ready)

                    else ->
                        getString(R.string.campus_login_success)
                }

                showMessage(message)

                openCampusHome(
                    fullName = fullName,
                    email = email,
                    offline = false
                )
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Could not verify campus profile", exception)
                auth.signOut()

                showLoginError(
                    if (!isInternetAvailable()) {
                        getString(R.string.campus_login_profile_connection_lost)
                    } else {
                        getString(R.string.campus_login_profile_verify_failed)
                    }
                )
            }
    }

    // SAVE OFFLINE CREDENTIALS

    private fun saveVerifiedCampusProfile(
        userId: String,
        email: String,
        fullName: String,
        password: String?
    ): Boolean {
        return try {
            offlineLoginStore.saveVerifiedLogin(
                userId = userId,
                email = email,
                fullName = fullName,
                password = password
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Campus offline save exception", exception)
            false
        }
    }

    // OFFLINE LOGIN

    private fun openOfflineSession(
        emailInput: String,
        password: String
    ) {
        val profile = try {
            offlineLoginStore.verifyOfflineLogin(
                email = emailInput,
                password = password
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Campus offline verification exception", exception)
            null
        }

        if (profile == null) {
            Log.w(TAG, "Campus offline verification failed")
            showMessage(
                getString(R.string.campus_login_offline_not_prepared)
            )
            return
        }

        if (profile.accountType != "Campus") {
            showMessage(
                getString(R.string.campus_login_offline_wrong_type)
            )
            return
        }

        Log.d(TAG, "Campus offline verification succeeded")

        showMessage(
            getString(R.string.campus_login_offline_success)
        )

        openCampusHome(
            fullName = profile.fullName,
            email = profile.email,
            offline = true
        )
    }

    // OPEN CAMPUS HOME

    private fun openCampusHome(
        fullName: String = "Campus User",
        email: String = "",
        offline: Boolean = false
    ) {
        try {
            Log.d(TAG, "Opening Campus Home. Offline mode: $offline")

            val homeIntent = Intent(
                this@CampusLoginActivity,
                CampusHomeActivity::class.java
            ).apply {
                putExtra("fullName", fullName)
                putExtra("email", email)
                putExtra(CampusHomeActivity.EXTRA_OFFLINE_MODE, offline)

                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(homeIntent)
            finish()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to open Campus Home", exception)
            showMessage(
                getString(
                    R.string.campus_login_open_home_failed,
                    exception.message ?: ""
                )
            )
        }
    }

    // AUTHENTICATION STATE

    private fun setAuthenticating(loading: Boolean) {
        isAuthenticating = loading

        btnLogin.isEnabled = !loading
        btnGoogleSignIn.isEnabled = !loading

        btnLogin.text = getString(
            if (loading) {
                R.string.campus_login_signing_in
            } else {
                R.string.campus_login_button
            }
        )

        btnGoogleSignIn.text = getString(
            if (loading) {
                R.string.campus_login_google_wait
            } else {
                R.string.campus_login_google_button
            }
        )
    }

    // ERROR HANDLING

    private fun showLoginError(message: String) {
        setAuthenticating(false)
        showMessage(message)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getLoginErrorMessage(errorMessage: String?): String {
        val message = errorMessage?.lowercase().orEmpty()

        return when {
            message.contains("invalid-credential") ||
                    message.contains("wrong-password") ||
                    message.contains("invalid-login-credentials") ->
                getString(R.string.campus_login_error_incorrect_credentials)

            message.contains("invalid-email") ->
                getString(R.string.campus_login_error_email_invalid)

            message.contains("user-not-found") ->
                getString(R.string.campus_login_error_user_not_found)

            message.contains("too-many-requests") ->
                getString(R.string.campus_login_error_too_many_attempts)

            message.contains("network") ->
                getString(R.string.campus_login_error_network)

            else ->
                getString(R.string.campus_login_error_generic)
        }
    }

    override fun onResume() {
        super.onResume()

        if (
            ::btnLogin.isInitialized &&
            ::btnGoogleSignIn.isInitialized &&
            !isAuthenticating
        ) {
            setAuthenticating(false)
        }
    }
}