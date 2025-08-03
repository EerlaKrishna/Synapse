package com.example.synapse.chats // Or your actual package for models

data class ChatListItem(
    val groupId: String,
    var groupName: String,
    var lastMessageText: String? = null, // Default to null
    var lastMessageTimestamp: Long = 0L, // Default to 0 or a sensible initial value
    var lastMessageSenderName: String? = null, // Default to null
    var unreadCount: Int = 0,
    // This new field helps prevent incrementing unread for the same message multiple times
    var lastMessageTimestampProcessedForUnread: Long = 0L
)

// You might also have your GroupData class here or elsewhere
// data class GroupData(val id: String, val name: String, /* other fields */)