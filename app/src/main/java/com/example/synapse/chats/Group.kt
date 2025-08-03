package com.example.synapse.chats

data class Group(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
//    var lastMessageText: String? = null, // Ensure these are vars
//    var lastMessageTimestamp: Long? = null,
//    var lastMessageSenderName: String? = null,

    var lastMessage: LastMessage? = null,
    var unreadCount: Int = 0,
    // any other fields like memberIds, groupImageURL etc.
    var memberIds: List<String> = emptyList() // Example
) {
    constructor() : this("", "", null, null, 0, emptyList()) // No-arg constructor
}