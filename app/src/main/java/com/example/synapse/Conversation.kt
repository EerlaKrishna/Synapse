package com.example.synapse

data class Conversation(
    val id: String = System.nanoTime().toString(), // Simple unique ID, replace with Firebase ID
    val participantsDisplayNames: List<String> = emptyList(),
    val lastMessageSnippet: String = "",
    val timestamp: Long = System.currentTimeMillis(), // Default to now, replace with actual
    val avatarUrl: String? = null, // URL or local resource identifier
    var isUnread: Boolean = false
    // Consider adding:
    // val participantIds: List<String> = emptyList() // For easier querying
    // val lastMessageSenderId: String? = null
)