package com.example.synapse

data class GroupDisplayInfo(
    val id: String,
    val name: String,
    var lastMessageText: String? = "No messages yet",
    var lastMessageTimestamp: Long = 0L,
    var lastMessageSenderName: String? = null,
    var unreadCount: Int = 0
    // Add isMuted or other flags if needed later
)