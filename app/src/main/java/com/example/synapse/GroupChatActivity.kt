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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.collections.toMap

class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding

    // Define constants for intent extras within a companion object
    companion object {
        const val EXTRA_GROUP_ID = "com.example.synapse.EXTRA_GROUP_ID" // Make sure this is unique
        const val EXTRA_GROUP_NAME = "com.example.synapse.EXTRA_GROUP_NAME" // Make sure this is unique
        // Add any other extras you might need, e.g., for specific message types
        // const val EXTRA_MESSAGE_TYPE = "com.example.synapse.EXTRA_MESSAGE_TYPE"
    }


    private var currentGroupId: String? = null
    private var currentGroupName: String? = null

    private var currentSelectedTabType: String = "improvement_messages" // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

// --- SINGLE, CONSISTENT RETRIEVAL ---
        val receivedGroupId = intent.getStringExtra(EXTRA_GROUP_ID)
        val receivedGroupName = intent.getStringExtra(EXTRA_GROUP_NAME)

        Log.i("GroupChatActivity", "Attempting to open chat. Received Group ID from intent: '$receivedGroupId', Name: '$receivedGroupName'")

        if (receivedGroupId.isNullOrBlank()) { // Use isNullOrBlank for safety
            Toast.makeText(this, "CRITICAL ERROR: Group ID is missing or blank in intent.", Toast.LENGTH_LONG).show()
            Log.e("GroupChatActivity", "Group ID is null or blank from key $EXTRA_GROUP_ID. Cannot initialize chat. Finishing activity.")
            finish()
            return
        }
        // Assign to your member variables
        this.currentGroupId = receivedGroupId
        this.currentGroupName = receivedGroupName

        Log.d("GroupChatActivity", "Successfully initialized. Current Group ID: '${this.currentGroupId}', Name: '${this.currentGroupName}'")

        // Use the consistent member variables throughout the activity
        supportActionBar?.title = this.currentGroupName ?: "Group Chat"
        binding.topBarTitle.text = this.currentGroupName ?: "Group Chat" // Use currentGroupName

        // Pass the non-nullable currentGroupId to functions that require it
        // Or ensure functions handle nullable if currentGroupId could be null (though we check above)
        setupTabs(this.currentGroupId!!) // Pass the validated, non-null ID
        setupSendButton() // sendMessageToFirebase will use this.currentGroupId
        setupKeyboardInsetsListener()
    }

    private fun setupTabs(groupIdForAdapter: String) {
        val adapter = ChatTabAdapter(this, groupIdForAdapter)
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

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.e("GroupChatActivity", "Firebase user is null. Cannot send message. User needs to be authenticated with Firebase.")
            Toast.makeText(this, "Error: Not authenticated with Firebase. Please restart.", Toast.LENGTH_LONG).show()
            return
        }


        val currentUserId = firebaseUser.uid
        val currentUserName = firebaseUser.displayName ?: "Anonymous" // Or however you get the sender's name
        val messageText = binding.messageInput.text.toString() // The actual message content
        val currentTime = System.currentTimeMillis()
        val typeOfMessage = currentSelectedTabType // e.g., "improvement_messages"

       // val sharedPreferences = getSharedPreferences("session", MODE_PRIVATE)
       // val sessionId = sharedPreferences.getString("sessionId", null)
//
//        if (sessionId == null) {
//            Log.e("GroupChatActivity", "Session ID not found. Cannot send message.")
//            Toast.makeText(this, "Error: Not logged in. Please restart the app.", Toast.LENGTH_LONG).show()
//            return
//        }
        // Ensure currentGroupId is not null before using, though it should be validated in onCreate
        val groupIdToSend = this.currentGroupId
        if (groupIdToSend.isNullOrBlank()) {
            Log.e("GroupChatActivity", "Cannot send message, Group ID is missing internally (was '${this.currentGroupId}').")
            Toast.makeText(this, "Error: Group context lost.", Toast.LENGTH_SHORT).show()
            return
        }


        val ref = FirebaseDatabase.getInstance().getReference("messages")
            .child(groupIdToSend)
            .child(currentSelectedTabType)

       // val messageData = Message(sessionId, messageContent, System.currentTimeMillis().toString())

        val messageData = Message(
            senderId = currentUserId,
            senderName = currentUserName,
            text = messageText,
            timestamp = currentTime,      // Passed as Long
            messageType = typeOfMessage,
            sessionId = currentUserId     // Or null if not used for this purpose
        )


        Log.d("GroupChatActivity", "Firebase User UID before setValue: ${FirebaseAuth.getInstance().currentUser?.uid}") // ADD THIS LINE
        Log.d("GroupChatActivity", "Attempting to send messageData: $messageData to path: ${ref.path.toString()}/<pushId>")


        ref.push().setValue(messageData)
            .addOnSuccessListener {
                binding.messageInput.text.clear()
                Log.d("GroupChatActivity", "$currentSelectedTabType message sent to $groupIdToSend.")
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatActivity", "Failed to send $currentSelectedTabType message to $groupIdToSend", e)
                Toast.makeText(this, "Failed to send message to $groupIdToSend: ${e.message}", Toast.LENGTH_LONG).show()
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