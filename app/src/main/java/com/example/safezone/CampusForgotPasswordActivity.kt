
package com.example.safezone

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CampusForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etStudentEmail: EditText
    private lateinit var etStudentNumber: EditText
    private lateinit var btnVerifyAccount: Button
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_campus_forgot_password)

        etStudentEmail = findViewById(R.id.etStudentEmail)
        etStudentNumber = findViewById(R.id.etStudentNumber)
        btnVerifyAccount = findViewById(R.id.btnVerifyAccount)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnVerifyAccount.setOnClickListener {

            val email = etStudentEmail.text.toString().trim()
            val studentNumber = etStudentNumber.text.toString().trim()

            if (email.isEmpty()) {
                etStudentEmail.error =
                    "Please enter your registered campus email"
                etStudentEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etStudentEmail.error =
                    "Please enter a valid email address"
                etStudentEmail.requestFocus()
                return@setOnClickListener
            }

            if (studentNumber.isEmpty()) {
                etStudentNumber.error =
                    "Please enter your student number"
                etStudentNumber.requestFocus()
                return@setOnClickListener
            }

            // Prototype flow: continue directly to the
            // Create New Password screen without sending email.
            val passwordIntent = Intent(
                this,
                CampusCreateNewPasswordActivity::class.java
            )

            passwordIntent.putExtra("studentEmail", email)
            passwordIntent.putExtra("studentNumber", studentNumber)

            startActivity(passwordIntent)
        }

        tvBackToLogin.setOnClickListener {
            val loginIntent = Intent(
                this,
                CampusLoginActivity::class.java
            )

            loginIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(loginIntent)
            finish()
        }
    }
}
