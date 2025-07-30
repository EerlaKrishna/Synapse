package com.example.synapse

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.android.identity.util.UUID
import com.example.synapse.databinding.ActivityLoginBinding
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener {
            val code = binding.codeInput.text.toString()
            verifyEmployee(code)
        }
    }

    private fun verifyEmployee(code: String) {
        db.child("employees").child(code).get().addOnSuccessListener {
            if (it.exists()) {
                val sessionId = UUID.randomUUID().toString()
                saveSession(sessionId)
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSession(sessionId: String) {
        getSharedPreferences("session", MODE_PRIVATE).edit()
            .putString("sessionId", sessionId).apply()
    }
}
