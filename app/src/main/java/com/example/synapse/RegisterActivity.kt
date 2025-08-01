package com.example.synapse

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synapse.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }

        binding.textViewLoginLink.setOnClickListener {
            // Navigate back to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Optional: finish RegisterActivity to remove it from back stack
        }
    }

    private fun registerUser() {
        val email = binding.editTextRegisterEmail.text.toString().trim()
        val password = binding.editTextRegisterPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        // --- Input Validations ---
        if (email.isEmpty()) {
            binding.editTextRegisterEmail.error = "Email is required"
            binding.editTextRegisterEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextRegisterEmail.error = "Please enter a valid email"
            binding.editTextRegisterEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.editTextRegisterPassword.error = "Password is required"
            binding.editTextRegisterPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.editTextRegisterPassword.error = "Password must be at least 6 characters"
            binding.editTextRegisterPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.editTextConfirmPassword.error = "Confirm password is required"
            binding.editTextConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.editTextConfirmPassword.error = "Passwords do not match"
            binding.editTextConfirmPassword.requestFocus()
            return
        }

        binding.progressBarRegister.visibility = View.VISIBLE

        // --- Firebase User Creation ---
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarRegister.visibility = View.GONE
                if (task.isSuccessful) {
                    // Registration success
                    val firebaseUser = auth.currentUser
                    val userId = firebaseUser?.uid // This is the unique User ID from Firebase Auth

                    if (userId != null) {
                        // Optional: Store additional user information in Realtime Database
                        // For example, store the email or a creation timestamp under the user's UID
                        // val userProfile = mapOf("email" to email, "createdAt" to System.currentTimeMillis())
                        // FirebaseDatabase.getInstance().getReference("users")
                        //    .child(userId)
                        //    .setValue(userProfile)
                        //    .addOnSuccessListener {
                        //        Log.d("RegisterActivity", "User profile saved to RTDB.")
                        //    }
                        //    .addOnFailureListener { e ->
                        //        Log.e("RegisterActivity", "Failed to save user profile to RTDB.", e)
                        //    }

                        Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show()

                        // Redirect to LoginActivity
                        val intent = Intent(this, LoginActivity::class.java)
                        // Clear back stack so user can't go back to RegisterActivity after registration
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Finish RegisterActivity
                    } else {
                        // This case should ideally not happen if task.isSuccessful is true
                        Toast.makeText(this, "Registration succeeded but failed to get user ID.", Toast.LENGTH_LONG).show()
                    }

                } else {
                    // Registration failed
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}