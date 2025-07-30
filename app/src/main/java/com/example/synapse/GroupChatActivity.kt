package com.example.synapse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.synapse.databinding.ActivityGroupChatBinding
import com.google.firebase.database.FirebaseDatabase

class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var group: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        group = intent.getStringExtra("group") ?: ""
        binding.topBarTitle.text = "Total"

        setupTabs()
        setupSendButton()
    }

    private fun setupTabs() {
        val adapter = ChatTabAdapter(this, supportFragmentManager)
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    private fun setupSendButton() {
        binding.sendBtn.setOnClickListener {
            val message = binding.messageInput.text.toString()
            sendMessageToFirebase(message)
        }
    }

    private fun sendMessageToFirebase(message: String) {
        val sessionId = getSharedPreferences("session", MODE_PRIVATE).getString("sessionId", "unknown")
        val ref = FirebaseDatabase.getInstance().getReference("messages").child(group)
        ref.push().setValue(sessionId?.let { Message(it, message, System.currentTimeMillis()) })
        binding.messageInput.text.clear()
    }
}
