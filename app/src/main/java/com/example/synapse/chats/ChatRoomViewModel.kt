package com.example.synapse.chats

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.synapse.Message // Ensure your Message model is correctly imported
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

    private val database: FirebaseDatabase = Firebase.database
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private var currentGroupMessagesTypeRef: DatabaseReference? = null
    private var currentPathStringForListener: String? = null
    private var messagesValueEventListener: ValueEventListener? = null

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // --- THIS IS THE MISSING PART ---
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    // --- END OF MISSING PART ---

    companion object {
        private const val TAG = "ChatRoomViewModel"
        private const val MESSAGES_NODE = "messages"
        private const val CHANNELS_NODE = "channels"
    }

    fun loadMessages(groupId: String, messageType: String) {
        if (currentUserId == null) {
            _error.value = "User not logged in. Cannot load messages."
            Log.e(TAG, "loadMessages: Current user is null for groupId: $groupId, messageType: $messageType")
            _isLoading.value = false // Also set loading to false here
            return
        }
        Log.d(TAG, "Loading messages for groupId: $groupId, messageType: $messageType")
        _isLoading.value = true // Set loading state TO TRUE before starting

        clearMessagesListener()

        val pathForListener = "$MESSAGES_NODE/$groupId/$messageType"
        currentPathStringForListener = pathForListener
        currentGroupMessagesTypeRef = database.getReference(pathForListener)

        messagesValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<Message>()
                if (snapshot.exists()) {
                    for (messageSnapshot in snapshot.children) {
                        try {
                            val message = messageSnapshot.getValue(Message::class.java)
                            message?.let {
                                it.id = messageSnapshot.key
                                messageList.add(it)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting message snapshot for $currentPathStringForListener, key: ${messageSnapshot.key}", e)
                        }
                    }
                }
                _messages.value = messageList.sortedBy { it.timestamp }
                Log.d(TAG, "Messages loaded for $currentPathStringForListener: ${messageList.size}")
                _isLoading.value = false // Set loading state TO FALSE after loading
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadMessages:onCancelled for $currentPathStringForListener", databaseError.toException())
                _error.value = "Failed to load messages: ${databaseError.message}"
                _isLoading.value = false // Set loading state TO FALSE on error
            }
        }
        currentGroupMessagesTypeRef?.orderByChild("timestamp")?.addValueEventListener(messagesValueEventListener!!)
    }

    fun sendMessage(groupId: String, messageType: String, text: String, senderName: String) {
        if (currentUserId == null) {
            _error.value = "Cannot send message: User not logged in."
            Log.e(TAG, "sendMessage: Current user is null for groupId $groupId.")
            return
        }

        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            Log.w(TAG, "Attempted to send blank message to groupId $groupId, type $messageType.")
            _error.value = "Message cannot be empty."
            return
        }

        val currentTimestamp = System.currentTimeMillis()
        val messagesPath = "$MESSAGES_NODE/$groupId/$messageType"
        val groupMessagesNodeRef = database.getReference(messagesPath)
        val newMessagePushKey = groupMessagesNodeRef.push().key

        if (newMessagePushKey == null) {
            _error.value = "Couldn't generate a unique key for the message."
            Log.e(TAG, "sendMessage: Failed to get push key for messages in $groupId/$messageType")
            return
        }

        val messageToSend = Message(
            senderId = currentUserId,
            senderName = senderName,
            text = trimmedText,
            timestamp = currentTimestamp,
            messageType = messageType
        )

        val lastMessageDataForChannel = hashMapOf(
            "text" to trimmedText,
            "senderName" to senderName,
            "timestamp" to currentTimestamp,
            "messageType" to messageType
        )

        val channelLastMessagePath = "$CHANNELS_NODE/$groupId/lastMessage"
        val fullMessagePath = "$messagesPath/$newMessagePushKey"

        val updates = hashMapOf<String, Any?>(
            fullMessagePath to messageToSend,
            channelLastMessagePath to lastMessageDataForChannel
        )

        Log.d(TAG, "Attempting multi-location update for GroupID: $groupId, MessageType: $messageType")
        viewModelScope.launch {
            try {
                database.reference.updateChildren(updates).await()
                Log.d(TAG, "Message sent and channel lastMessage updated successfully for $groupId/$messageType.")
                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message or updating channel for $groupId/$messageType", e)
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    private fun clearMessagesListener() {
        messagesValueEventListener?.let { listener ->
            currentGroupMessagesTypeRef?.removeEventListener(listener)
            Log.d(TAG, "Removed previous messages listener from path: $currentPathStringForListener")
        }
        messagesValueEventListener = null
        currentGroupMessagesTypeRef = null
        currentPathStringForListener = null
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