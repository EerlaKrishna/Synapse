package com.example.synapse.chats

import com.example.synapse.Message
import com.google.firebase.database.Exclude // Import Exclude

data class Group(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var members: Map<String, Boolean> = emptyMap(), // NEW: Changed to Map
    var createdBy: String? = null,
    var timestamp: Long? = null,
    var lastMessage: Message? = null,
    var lastMessageTimestamp: Long? = null,
    @get:Exclude
    var unreadCount: Int = 0,
    @get:Exclude
    var hasUnreadMessagesFromOthers: Boolean = false,
    @get:Exclude
    var showUnreadDot: Boolean = false // NEW: For the unread dot indicator
) {
    // No-argument constructor for Firebase deserialization
    constructor() : this(
        id = "",
        name = "",
        description = null,
        members = emptyMap(), // Updated
        createdBy = null,
        timestamp = null,
        lastMessage = null,
        lastMessageTimestamp = null
        // unreadCount and hasUnreadMessagesFromOthers are excluded, so not in primary constructor for Firebase
    )

    // Helper to get member UIDs if needed, derived from the map
    @Exclude
    fun getMemberIdList(): List<String> {
        return members.keys.toList()
    }
}