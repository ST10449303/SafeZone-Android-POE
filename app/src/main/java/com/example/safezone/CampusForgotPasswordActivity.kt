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

        // Connect XML components
        etStudentEmail = findViewById(R.id.etStudentEmail)
        etStudentNumber = findViewById(R.id.etStudentNumber)
        btnVerifyAccount = findViewById(R.id.btnVerifyAccount)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        // VERIFY ACCOUNT / CONTINUE button
        btnVerifyAccount.setOnClickListener {

            val email = etStudentEmail.text.toString().trim()
            val studentNumber = etStudentNumber.text.toString().trim()

            // Check student email
            if (email.isEmpty()) {
                etStudentEmail.error = "Please enter your student email"
                etStudentEmail.requestFocus()
                return@setOnClickListener
            }

            // Check email format
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etStudentEmail.error = "Please enter a valid student email"
                etStudentEmail.requestFocus()
                return@setOnClickListener
            }

            // Check student number
            if (studentNumber.isEmpty()) {
                etStudentNumber.error = "Please enter your student number"
                etStudentNumber.requestFocus()
                return@setOnClickListener
            }

            // Prototype account verification
            Toast.makeText(
                this,
                "Account details accepted.",
                Toast.LENGTH_SHORT
            ).show()

            // Open Create New Password screen
            val intent = Intent(
                this,
                CampusCreateNewPasswordActivity::class.java
            )

            // Pass student email and student number
            intent.putExtra("email", email)
            intent.putExtra("studentNumber", studentNumber)

            startActivity(intent)
        }

        // BACK TO LOG IN
        tvBackToLogin.setOnClickListener {

            val intent = Intent(
                this,
                CampusLoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        }
    }
}