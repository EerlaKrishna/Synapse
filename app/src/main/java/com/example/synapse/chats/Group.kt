package com.example.synapse.chats

// Assuming LastMessage is defined elsewhere and is correct
// data class LastMessage(
//     var text: String? = null,
//     var timestamp: Long? = null,
//     var senderName: String? = null,
//     var senderId: String? = null, // Good to have
//     var messageType: String? = null // e.g., "text", "image"
// )

data class Group(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var lastMessage: LastMessage? = null,
    var unreadCount: Int = 0,
    var memberIds: List<String> = emptyList(),

    // --- ADD THESE FIELDS ---
    var createdBy: String? = null,         // To store the UID of the user who created the group
    var timestamp: Long? = null    // To store the server timestamp when the group was created
) {
    // Update the no-arg constructor to include defaults for the new fields
    constructor() : this(
        id = "",
        name = "",
        description = null,
        lastMessage = null,
        unreadCount = 0,
        memberIds = emptyList(),
        createdBy = null,          // Default for new field
        timestamp = null   // Default for new field
    )
}
