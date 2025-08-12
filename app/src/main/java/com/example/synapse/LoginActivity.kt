package com.example.synapse

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kotlin.text.isEmpty
import kotlin.text.trim

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, check for local session or create one
            // Using Firebase UID as the session ID is a good practice here.
            val sharedPrefs = getSharedPreferences("session", MODE_PRIVATE)
            val currentSessionId = sharedPrefs.getString("sessionId", null)
            if (currentSessionId == null || currentSessionId != currentUser.uid) {
                // Save new session only if it's null or doesn't match current user's UID
                saveNewSession(currentUser.uid)
            }
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            keepSplashOnScreen = false // Allow splash to dismiss
            return // Skip further setup
        }
        keepSplashOnScreen = false // Allow splash to dismiss if no user

        binding.loginButton.setOnClickListener {
            loginUser()
        }

        // If you have a registration link and want to remove/hide it:
        // binding.textViewRegisterLink.visibility = View.GONE
    }

    private fun loginUser() {
        val employeeCodeOrEmail = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        // Clear previous errors
        binding.emailEditText.error = null
        binding.passwordEditText.error = null

        if (employeeCodeOrEmail.isEmpty()) {
            binding.emailEditText.error = "Employee Code/Email is required"
            binding.emailEditText.requestFocus()
            return
        }

        // OPTIONAL: Email format validation.
        // If your employee codes are NOT standard email formats, you might want to remove this
        // or implement a different validation specific to your employee code format.
        /*
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(employeeCodeOrEmail).matches()) {
            binding.emailEditText.error = "Please enter a valid Employee Code or Email format"
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
                        Log.d("LoginActivity", "User UID: ${it.uid} logged in.")
                        // Save session using Firebase UID
                        saveNewSession(it.uid)
                    }

                    startActivity(Intent(this, HomeActivity::class.java))
                    finishAffinity() // Finish LoginActivity and any other activities in the task
                } else {
                    // Login failed
                    val exception = task.exception
                    Log.w("LoginActivity", "signInWithEmail:failure", exception) // Log the exception

                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            // Error code: ERROR_USER_NOT_FOUND
                            binding.emailEditText.error = "No account found with this Employee Code/Email."
                            binding.emailEditText.requestFocus()
                            Toast.makeText(baseContext, "Login failed: No account found.", Toast.LENGTH_LONG).show()
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            // Error codes: ERROR_WRONG_PASSWORD, sometimes ERROR_USER_NOT_FOUND (to prevent enumeration)
                            binding.emailEditText.error = "Invalid Employee Code/Email or Password."
                            binding.passwordEditText.error = "Invalid Employee Code/Email or Password."
                            binding.emailEditText.requestFocus() // Or passwordEditText.requestFocus()
                            Toast.makeText(baseContext, "Login failed: Invalid credentials.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            // Other errors (network, too many requests, etc.)
                            // FirebaseTooManyRequestsException, FirebaseNetworkException, etc.
                            Toast.makeText(baseContext, "Login failed: ${exception?.message ?: "An unexpected error occurred."}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun saveNewSession(userId: String?) {
        if (userId == null) {
            Log.w("LoginActivity", "Attempted to save session with null userId.")
            return
        }
        getSharedPreferences("session", MODE_PRIVATE).edit()
            .putString("sessionId", userId) // Store Firebase UID as session ID
            .apply()
        Log.d("LoginActivity", "Session ID saved: $userId")
    }
}