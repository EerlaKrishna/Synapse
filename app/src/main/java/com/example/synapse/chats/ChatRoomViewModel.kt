package com.example.synapse.chats

import android.util.Log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.synapse.Message
// Make sure your Message model is correctly imported
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatRoomViewModel : ViewModel() {

    // Get a reference to the Firebase Realtime Database
    private val database: FirebaseDatabase = Firebase.database
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Reference to the 'messages' node for a specific group
    // Path will be something like: /group_messages/{groupId}/
    private var groupMessagesRef: DatabaseReference? = null
    private var messagesListener: ValueEventListener? = null

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    companion object {
        private const val TAG = "ChatRoomViewModel_RTDB" // Changed tag for clarity
        private const val MESSAGES_NODE_PREFIX = "messages"
    }

    fun loadMessages(groupId: String, messageType: String) {
        if (currentUserId == null) {
            _error.value = "User not logged in."
            Log.e(TAG, "loadMessages: Current user is null.")
            return
        }
        Log.d(TAG, "Loading messages for groupId: $groupId")

        // Remove any existing listener
        clearMessagesListener()

        groupMessagesRef = database.getReference("$MESSAGES_NODE_PREFIX/$groupId/$messageType")

        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<Message>()
                if (snapshot.exists()) {
                    for (messageSnapshot in snapshot.children) {
                        try {
                            // For RTDB, you might need to manually map or ensure your Message class
                            // has a no-arg constructor and public properties for automatic mapping.
                            val message = messageSnapshot.getValue(com.example.synapse.Message::class.java)
                            message?.let {
                                it.id = messageSnapshot.key // Manually set the id property
                                messageList.add(it)
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error converting message snapshot for group $groupId, key: ${messageSnapshot.key}", ex)
                        }
                    }
                }
                // RTDB doesn't have built-in server-side ordering like Firestore's orderBy with addSnapshotListener
                // in the same way for complex queries. Messages are typically ordered by push key (chronological).
                // If you need specific client-side sorting, you can do it here.
                // For simple chronological order based on push keys, this should be fine.
                _messages.value = messageList.sortedBy { it.timestamp } // Ensure timestamp is used for sorting
                Log.d(TAG, "Messages loaded for group $groupId: ${messageList.size}")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadMessages:onCancelled for group $groupId", databaseError.toException())
                _error.value = "Failed to load messages: ${databaseError.message}"
            }
        }
        // Use orderByChild("timestamp") if you want to explicitly order by the timestamp field
        // This requires the timestamp to be indexed in your RTDB rules for performance.
        groupMessagesRef?.orderByChild("timestamp")?.addValueEventListener(messagesListener!!)
        // If you rely on push keys for order (default chronological):
        // groupMessagesRef?.addValueEventListener(messagesListener!!)
    }

    // This is part of your ChatRoomViewModel for Firebase Realtime Database

    // In ChatRoomViewModel.kt (Realtime Database version)
    fun sendMessage(groupId: String, messageType: String,  text: String, senderName: String) { // Added senderName parameter
        if (currentUserId == null) {
            _error.value = "Cannot send message: User not logged in."
            Log.e(TAG, "sendMessage: Current user is null for groupId $groupId.")
            return
        }
        if (text.isBlank()) {
            Log.w(TAG, "Attempted to send blank message to groupId $groupId.")
            return
        }

        val groupMessagesNode = database.getReference("$MESSAGES_NODE_PREFIX/$groupId/$messageType")
        val newMessagePushKey = groupMessagesNode.push().key

        if (newMessagePushKey == null) {
            _error.value = "Couldn't generate a unique key for the message."
            Log.e(TAG, "sendMessage: Failed to get push key for messages in group $groupId")
            return
        }

        val messageToSend = com.example.synapse.Message( // Use your Message class
            senderId = currentUserId,
            senderName = senderName, // Use the passed-in senderName
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                groupMessagesNode.child(newMessagePushKey).setValue(messageToSend).await()
                Log.d(TAG, "Message sent successfully to groupId: $groupId, messageId (push key): $newMessagePushKey")
                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to groupId: $groupId, push key: $newMessagePushKey", e)
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    private fun clearMessagesListener() {
        messagesListener?.let { listener ->
            groupMessagesRef?.removeEventListener(listener)
            Log.d(TAG, "Removed previous messages listener.")
        }
        messagesListener = null
        groupMessagesRef = null
    }

    override fun onCleared() {
        super.onCleared()
        clearMessagesListener()
        Log.d(TAG, "ViewModel cleared, messages listener removed.")
    }

    fun clearErrorMessage() {
        _error.value = null
    }
}
