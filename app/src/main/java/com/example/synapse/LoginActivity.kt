package com.example.synapse

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synapse.databinding.ActivityLoginBinding // Make sure this is the correct binding class name
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.util.UUID // Ensure this is the correct UUID import if you use it.
import kotlin.text.isEmpty
import kotlin.text.trim
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

// com.android.identity.util.UUID was used before, choose one consistently.
// For simplicity, java.util.UUID is fine.

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    // private val db = FirebaseDatabase.getInstance().reference // Keep if you need to check employee status later
    private var isInitialCheckDone = false // Flag to manage splash screen during initial checks

    override fun onCreate(savedInstanceState: Bundle?) {
        // Step 1: Install the splash screen.
        // This must be called before super.onCreate() and setContentView().
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {  false  }

        binding = ActivityLoginBinding.inflate(layoutInflater) // Ensure this matches your XML file name (e.g., activity_login.xml)
        setContentView(binding.root)

        auth = Firebase.auth

        // Optional: Check if user is already logged in via Firebase Auth AND has a session
        // This is a more robust check than just SharedPreferences
        val currentSessionId = getSharedPreferences("session", MODE_PRIVATE).getString("sessionId", null)
        if (auth.currentUser != null && currentSessionId != null) {
            // Potentially add a check here to ensure the session ID still corresponds to the logged-in user
            // For now, if both exist, assume valid session.
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return // Skip further setup if already logged in and session exists
        } else if (auth.currentUser != null && currentSessionId == null) {
            // User is authenticated with Firebase, but no local session ID. Create one.
            // This can happen if the app was closed and SharedPreferences were cleared, but Firebase token is still valid.
            saveNewSession(auth.currentUser!!.uid) // Or generate a new random UUID if preferred
        }
        isInitialCheckDone = true // Add this if not already handled by all paths


        binding.loginButton.setOnClickListener { // Ensure your login button ID is 'loginButton'
            loginUser()
        }

        binding.textViewRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim() // Ensure ID is 'emailEditText'
        val password = binding.passwordEditText.text.toString().trim() // Ensure ID is 'passwordEditText'

        if (email.isEmpty()) {
            binding.emailEditText.error = "Email is required"
            binding.emailEditText.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Valid email is required"
            binding.emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password is required"
            binding.passwordEditText.requestFocus()
            return
        }

        binding.progressBarLogin.visibility = View.VISIBLE // Ensure ID is 'progressBarLogin'

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarLogin.visibility = View.GONE
                if (task.isSuccessful) {
                    // Login success
                    val firebaseUser = auth.currentUser
                    Toast.makeText(this, "Login Successful.", Toast.LENGTH_SHORT).show()

                    // Now, generate and save the session ID for anonymous messaging
                    // You can use the Firebase UID itself as the session ID if you want to link
                    // anonymous messages back to a Firebase User (for your backend/admin purposes only,
                    // NOT for display to other users).
                    // Or, generate a new random UUID to keep it fully decoupled.
                    // For maximum message anonymity from other users, a random UUID is better.
                    // For your ability to potentially trace/moderate, linking to UID is better.
                    // Let's stick to the random UUID for now to maintain the original anonymity concept.
                    saveNewSession()


                    // How to handle "Employee" status now?
                    // For now, let's assume any logged-in user can proceed.
                    // You might add a check here later if certain emails/UIDs are "employees"
                    // e.g., checkUserEmployeeStatus(firebaseUser?.uid)

                    startActivity(Intent(this, HomeActivity::class.java))
                    finishAffinity() // Finish LoginActivity and any other activities in the task associated with it
                } else {
                    // Login failed
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveNewSession(identifier: String? = null) {
        // If an identifier (like Firebase UID) is passed, you could use it.
        // Otherwise, generate a new random one.
        val sessionIdToSave = identifier ?: UUID.randomUUID().toString()
        getSharedPreferences("session", MODE_PRIVATE).edit()
            .putString("sessionId", sessionIdToSave)
            .apply()
        // Log.d("LoginActivity", "Session ID saved: $sessionIdToSave")
    }
}
