package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnSendCode: Button
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_forgot_password)

        // Connect XML controls
        etEmail = findViewById(R.id.etEmail)
        btnSendCode = findViewById(R.id.btnSendCode)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        // RESET PASSWORD
        btnSendCode.setOnClickListener {

            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error =
                    getString(R.string.forgot_enter_email)
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error =
                    getString(R.string.forgot_invalid_email)
                etEmail.requestFocus()
                return@setOnClickListener
            }

            // Open Create New Password screen
            val intent = Intent(
                this,
                CreateNewPasswordActivity::class.java
            )

            // Pass email to the next screen
            intent.putExtra("email", email)

            startActivity(intent)
        }

        // BACK TO PERSONAL LOGIN
        tvBackToLogin.setOnClickListener {

            val intent = Intent(
                this,
                PersonalLoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        }
    }
}
