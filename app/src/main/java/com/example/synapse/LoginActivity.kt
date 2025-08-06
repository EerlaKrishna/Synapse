package com.example.synapse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns // Keep if your employee codes are email-formatted, otherwise remove
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.synapse.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import java.util.UUID
import kotlin.text.isEmpty
import kotlin.text.trim

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    // private var isInitialCheckDone = false // Flag for splash screen, review if still needed with new logic

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash screen on screen until we've checked auth status
        // You might adjust this condition based on how quickly auth check completes
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, check for local session or create one
            val currentSessionId = getSharedPreferences("session", MODE_PRIVATE).getString("sessionId", null)
            if (currentSessionId == null) {
                saveNewSession(currentUser.uid) // Save session using Firebase UID
            }
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            keepSplashOnScreen = false // Allow splash to dismiss
            return // Skip further setup
        }
        keepSplashOnScreen = false // Allow splash to dismiss if no user

        // isInitialCheckDone = true // If you had other initial checks

        binding.loginButton.setOnClickListener {
            loginUser()
        }

        // Remove the registration link and its listener
        // binding.textViewRegisterLink.visibility = View.GONE // Or remove from XML
    }

    private fun loginUser() {
        // Use "employeeCode" or similar if it's not an email.
        // For Firebase, this will be treated as the "email" field.
        val employeeCodeOrEmail = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (employeeCodeOrEmail.isEmpty()) {
            binding.emailEditText.error = "Employee Code/Email is required"
            binding.emailEditText.requestFocus()
            return
        }

        // OPTIONAL: If your employee codes are NOT in standard email format, REMOVE or COMMENT OUT this check.
        // If they ARE in email format (e.g., emp001@example.com), you can keep it.
        /*
        if (!Patterns.EMAIL_ADDRESS.matcher(employeeCodeOrEmail).matches()) {
            binding.emailEditText.error = "Valid email format required for login"
            binding.emailEditText.requestFocus()
            return
        }
        */

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password is required"
            binding.passwordEditText.requestFocus()
            return
        }

        binding.progressBarLogin.visibility = View.VISIBLE

        auth.signInWithEmailAndPassword(employeeCodeOrEmail, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarLogin.visibility = View.GONE
                if (task.isSuccessful) {
                    // Login success
                    val firebaseUser = auth.currentUser
                    Toast.makeText(this, "Login Successful.", Toast.LENGTH_SHORT).show()
                    firebaseUser?.let {
                        Log.d("AdminSetup", "Admin User UID: ${it.uid}") // <-- THIS IS THE UID YOU NEED
                        // Copy this UID from Logcat
                    }
                    // Save a session identifier. Using Firebase UID is common.
                    // If you have specific reasons for a separate random UUID, keep your original logic.
                    saveNewSession(firebaseUser?.uid) // Pass UID to link session to user

                    startActivity(Intent(this, HomeActivity::class.java))
                    finishAffinity() // Finish LoginActivity and any other activities in the task
                } else {
                    // Login failed
                    try {
                        throw task.exception!!
                    } catch (e: FirebaseAuthInvalidUserException) {
                        binding.emailEditText.error = "No account found with this code/email."
                        binding.emailEditText.requestFocus()
                        Toast.makeText(baseContext, "Login failed: Employee Code/Email not found.", Toast.LENGTH_LONG).show()
                    } catch (e: FirebaseAuthInvalidCredentialsException) {
                        binding.passwordEditText.error = "Incorrect password."
                        binding.passwordEditText.requestFocus()
                        Toast.makeText(baseContext, "Login failed: Incorrect password.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(baseContext, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun saveNewSession(identifier: String? = null) {
        // If an identifier (like Firebase UID) is passed, use it.
        // Otherwise, generate a new random one (though with logged-in users, UID is preferred).
        val sessionIdToSave = identifier ?: UUID.randomUUID().toString()
        getSharedPreferences("session", MODE_PRIVATE).edit()
            .putString("sessionId", sessionIdToSave)
            .apply()
        // Log.d("LoginActivity", "Session ID saved: $sessionIdToSave")
    }
}