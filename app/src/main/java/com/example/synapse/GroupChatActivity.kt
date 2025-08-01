package com.example.synapse

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.synapse.databinding.ActivityGroupChatBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.FirebaseDatabase

class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var groupId: String
    private lateinit var groupName: String

    private var currentSelectedTabType: String = "improvement_messages" // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("group_id") ?: "unknown_group"
        groupName = intent.getStringExtra("group_name") ?: "Unnamed Group"

        if (groupId == "unknown_group") {
            Toast.makeText(this, "Error: Group ID missing.", Toast.LENGTH_LONG).show()
            finish() // Close activity if group ID is missing
            return
        }

        binding.topBarTitle.text = groupName

        setupTabs()
        setupSendButton()
        setupKeyboardInsetsListener() // Call the new method
    }

    private fun setupTabs() {
        val adapter = ChatTabAdapter(this, groupId)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Improvements" else "Drawbacks"
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentSelectedTabType = if (tab?.position == 0) {
                    "improvement_messages"
                } else {
                    "drawback_messages"
                }
                Log.d("GroupChatActivity", "Selected tab type: $currentSelectedTabType")
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSendButton() {
        binding.sendBtn.setOnClickListener {
            val messageContent = binding.messageInput.text.toString().trim()
            if (messageContent.isNotBlank()) {
                sendMessageToFirebase(messageContent)
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessageToFirebase(messageContent: String) {
        val sharedPreferences = getSharedPreferences("session", MODE_PRIVATE)
        val sessionId = sharedPreferences.getString("sessionId", null)

        if (sessionId == null) {
            Log.e("GroupChatActivity", "Session ID not found. Cannot send message.")
            Toast.makeText(this, "Error: Not logged in. Please restart the app.", Toast.LENGTH_LONG).show()
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("messages")
            .child(groupId)
            .child(currentSelectedTabType)

        val messageData = Message(sessionId, messageContent, System.currentTimeMillis())

        ref.push().setValue(messageData)
            .addOnSuccessListener {
                binding.messageInput.text.clear()
                Log.d("GroupChatActivity", "$currentSelectedTabType message sent to $groupId.")
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatActivity", "Failed to send $currentSelectedTabType message to $groupId", e)
                Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupKeyboardInsetsListener() {
        // We apply the listener to the root view of the activity
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // Store original padding values if not already done
            // This is a simple way; for more complex scenarios, you might store them once
            val originalBottomPadding = view.getTag(R.id.tag_original_bottom_padding) as? Int ?: view.paddingBottom
            if (view.getTag(R.id.tag_original_bottom_padding) == null) {
                view.setTag(R.id.tag_original_bottom_padding, originalBottomPadding)
            }

            if (imeVisible) {
                // Keyboard is visible, so add its height to the original bottom padding
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, originalBottomPadding + imeHeight)
                Log.d("KeyboardInsets", "Keyboard visible. IME height: $imeHeight. New bottom padding: ${view.paddingBottom}")
            } else {
                // Keyboard is hidden, so restore original bottom padding
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, originalBottomPadding)
                Log.d("KeyboardInsets", "Keyboard hidden. Restored bottom padding: ${view.paddingBottom}")
            }

            // It's important to return the insets so that child views can also process them if needed.
            // However, if the root view is handling the IME by adjusting its padding,
            // it might have "consumed" the part of the inset it handled.
            // For this specific case where the root view's padding is adjusted to accommodate the IME,
            // we typically want to pass the original insets along, minus what we've "used up".
            // A simpler approach for now is to just return the insets and let the system handle propagation.
            // If you find child views are not respecting other insets (like status bar), this might need refinement.
            windowInsets
        }
    }
}