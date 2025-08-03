package com.example.synapse

data class Channel(
    val id: String,          // Unique identifier for the channel/department
    val name: String,        // Name of the department/channel
    // Add any other properties relevant to a Channel/Department, for example:
    // val description: String? = null,
    // val memberCount: Int = 0,
    // val lastMessageTimestamp: Long = 0L // If you want to show last activity
) {
    // Add a no-argument constructor if you plan to use this with Firebase Realtime Database
    // and want to deserialize directly to this class.
    constructor() : this("", "")
}
