
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

class PersonalLoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvCreateAccount: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager
    private lateinit var offlineLoginStore: OfflineLoginStore

    private var isPasswordVisible = false
    private var isAuthenticating = false

    companion object {
        private const val TAG = "SafeZonePersonalLogin"
        private const val EXTRA_OFFLINE_MODE = "offlineMode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)

        // Keep Personal credentials separate from Campus credentials.
        offlineLoginStore = OfflineLoginStore(
            context = applicationContext,
            accountType = "Personal"
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
                showMessage(getString(R.string.internet_required_reset))
            } else {
                startActivity(
                    Intent(this, ForgotPasswordActivity::class.java)
                )
            }
        }

        tvCreateAccount.setOnClickListener {
            startActivity(
                Intent(this, PersonalRegistrationActivity::class.java)
            )
        }
    }

    // ==========================================
    // INTERNET CONNECTION
    // ==========================================

    private fun isInternetAvailable(): Boolean {
        val manager = getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network)
            ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    // ==========================================
    // PASSWORD VISIBILITY
    // ==========================================

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
                    val iconStart =
                        etPassword.width -
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

    // ==========================================
    // EMAIL AND PASSWORD LOGIN
    // ==========================================

    private fun loginUser() {
        if (isAuthenticating) return

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = getString(R.string.enter_email_error)
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.valid_email_error)
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.enter_password_error)
            etPassword.requestFocus()
            return
        }

        // OFFLINE: verify locally saved credentials.
        if (!isInternetAvailable()) {
            openOfflineSession(email, password)
            return
        }

        // ONLINE: authenticate through Firebase.
        setAuthenticating(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser

                if (user == null) {
                    showLoginError(
                        getString(R.string.unable_verify_account)
                    )
                    return@addOnSuccessListener
                }

                verifyPersonalProfile(
                    uid = user.uid,
                    email = user.email ?: email,
                    password = password,
                    signInMethod = "email"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Email login failed", exception)

                showLoginError(
                    getLoginErrorMessage(exception.message)
                )
            }
    }

    // ==========================================
    // GOOGLE SIGN-IN
    // ==========================================

    private fun startGoogleSignIn() {
        if (isAuthenticating) return

        if (!isInternetAvailable()) {
            showMessage(getString(R.string.internet_required_google))
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
                    context = this@PersonalLoginActivity,
                    request = request
                )

                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(
                            credential.data
                        )

                    firebaseGoogleSignIn(googleCredential.idToken)
                } else {
                    showLoginError(
                        getString(R.string.valid_google_account)
                    )
                }

            } catch (exception: GetCredentialException) {
                Log.e(
                    TAG,
                    "Google account selection failed",
                    exception
                )

                showLoginError(
                    getString(R.string.google_signin_cancelled)
                )

            } catch (exception: Exception) {
                Log.e(TAG, "Google sign-in failed", exception)

                showLoginError(
                    getString(R.string.google_signin_failed)
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
                        getString(R.string.unable_verify_google)
                    )
                    return@addOnSuccessListener
                }

                verifyPersonalProfile(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    password = null,
                    signInMethod = "google"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(
                    TAG,
                    "Firebase Google authentication failed",
                    exception
                )

                showLoginError(
                    getString(R.string.google_auth_failed)
                )
            }
    }

    // ==========================================
    // VERIFY PERSONAL PROFILE ONLINE
    // ==========================================

    private fun verifyPersonalProfile(
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
                        getString(R.string.personal_profile_not_found)
                    )
                    return@addOnSuccessListener
                }

                if (document.getString("accountType") != "Personal") {
                    auth.signOut()

                    showLoginError(
                        getString(R.string.wrong_account_type)
                    )
                    return@addOnSuccessListener
                }

                val fullName =
                    document.getString("fullName") ?: "SafeZone User"

                // Retrieve the saved residential location.
                val savedLocation =
                    document.getString("location")?.trim().orEmpty()

                // Register the FCM device with Azure.
                // Registration is best-effort and does not block login.
                if (savedLocation.isNotBlank()) {

                    FCMTokenManager.registerDeviceWithAzure(
                        accountType = "Personal",
                        location = savedLocation
                    )

                    Log.d(
                        TAG,
                        "Personal FCM registration requested using saved residential location."
                    )

                } else {
                    Log.w(
                        TAG,
                        "No residential location found. Azure FCM registration skipped."
                    )
                }

                // Save verified offline profile.
                val offlineLoginSaved = saveVerifiedPersonalProfile(
                    userId = uid,
                    email = email,
                    fullName = fullName,
                    password = password
                )

                if (signInMethod == "email") {
                    if (offlineLoginSaved) {
                        Log.d(
                            TAG,
                            "PERSONAL_OFFLINE_SAVE_SUCCESS: Credentials saved."
                        )
                    } else {
                        Log.e(
                            TAG,
                            "PERSONAL_OFFLINE_SAVE_FAILED: Credentials were not saved."
                        )
                    }
                }

                setAuthenticating(false)

                val message = when {
                    signInMethod == "email" && !offlineLoginSaved ->
                        getString(R.string.offline_setup_failed)

                    signInMethod == "email" ->
                        getString(R.string.personal_login_offline_ready)

                    else ->
                        getString(R.string.personal_login_success)
                }

                showMessage(message)

                openPersonalHome(
                    fullName = fullName,
                    email = email,
                    offline = false
                )
            }
            .addOnFailureListener { exception ->

                Log.e(
                    TAG,
                    "Could not verify Personal profile",
                    exception
                )

                auth.signOut()

                showLoginError(
                    if (!isInternetAvailable()) {
                        getString(R.string.connection_lost_verify)
                    } else {
                        getString(R.string.profile_verification_failed)
                    }
                )
            }
    }

    // ==========================================
    // SAVE VERIFIED OFFLINE CREDENTIALS
    // ==========================================

    private fun saveVerifiedPersonalProfile(
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
            Log.e(
                TAG,
                "PERSONAL_OFFLINE_SAVE_EXCEPTION",
                exception
            )
            false
        }
    }

    // ==========================================
    // OFFLINE LOGIN
    // ==========================================

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
            Log.e(
                TAG,
                "PERSONAL_OFFLINE_VERIFY_EXCEPTION",
                exception
            )
            null
        }

        if (profile == null) {
            Log.w(TAG, "PERSONAL_OFFLINE_VERIFY_FAILED")

            showMessage(
                getString(R.string.offline_login_failed)
            )
            return
        }

        if (profile.accountType != "Personal") {
            showMessage(
                getString(R.string.offline_wrong_account_type)
            )
            return
        }

        Log.d(TAG, "PERSONAL_OFFLINE_VERIFY_SUCCESS")

        showMessage(
            getString(R.string.offline_personal_enabled)
        )

        openPersonalHome(
            fullName = profile.fullName,
            email = profile.email,
            offline = true
        )
    }

    // ==========================================
    // OPEN PERSONAL HOME
    // ==========================================

    private fun openPersonalHome(
        fullName: String = "SafeZone User",
        email: String = "",
        offline: Boolean = false
    ) {
        try {
            Log.d(
                TAG,
                "Opening PersonalHomeActivity. Offline mode: $offline"
            )

            val homeIntent = Intent(
                this@PersonalLoginActivity,
                PersonalHomeActivity::class.java
            ).apply {
                putExtra("fullName", fullName)
                putExtra("email", email)
                putExtra(EXTRA_OFFLINE_MODE, offline)

                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(homeIntent)
            finish()

            Log.d(TAG, "PersonalHomeActivity navigation requested")

        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Failed to open PersonalHomeActivity",
                exception
            )

            showMessage(
                getString(
                    R.string.unable_open_personal_home,
                    exception.message ?: "Unknown error"
                )
            )
        }
    }

    // ==========================================
    // AUTHENTICATION STATE
    // ==========================================

    private fun setAuthenticating(loading: Boolean) {
        isAuthenticating = loading

        btnLogin.isEnabled = !loading
        btnGoogleSignIn.isEnabled = !loading

        btnLogin.text = if (loading) {
            getString(R.string.signing_in)
        } else {
            getString(R.string.login)
        }

        btnGoogleSignIn.text = if (loading) {
            getString(R.string.please_wait)
        } else {
            getString(R.string.continue_with_google)
        }
    }

    // ==========================================
    // ERROR HANDLING
    // ==========================================

    private fun showLoginError(message: String) {
        setAuthenticating(false)
        showMessage(message)
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun getLoginErrorMessage(
        errorMessage: String?
    ): String {
        val message = errorMessage?.lowercase() ?: ""

        return when {
            message.contains("invalid-credential") ||
                    message.contains("wrong-password") ->
                getString(R.string.incorrect_credentials)

            message.contains("invalid-email") ->
                getString(R.string.valid_email_error)

            message.contains("user-not-found") ->
                getString(R.string.account_not_found)

            message.contains("too-many-requests") ->
                getString(R.string.too_many_login_attempts)

            message.contains("network") ->
                getString(R.string.network_error)

            else ->
                getString(R.string.login_failed)
        }
    }

    // ==========================================
    // ON RESUME
    // ==========================================

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
