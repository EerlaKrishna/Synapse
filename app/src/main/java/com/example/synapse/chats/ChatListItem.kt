package com.example.synapse.chats // Or your actual package for models

data class ChatListItem(
    var groupId: String = "",
    var groupName: String? = null,
    var lastMessageText: String? = null,
    var lastMessageTimestamp: Long = 0L,
    var lastMessageSenderName: String? = null,
    var unreadCount: Int = 0,
    var lastMessageTimestampProcessedForUnread: Long = 0L // Internal logic for unread count
) {
    constructor() : this("", null, null, 0L, null, 0, 0L) // No-argument constructor
}
