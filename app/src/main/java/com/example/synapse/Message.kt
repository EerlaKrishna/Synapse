package com.example.synapse

import com.google.firebase.database.Exclude // Optional: if you have fields not in Firebase
import com.google.firebase.database.IgnoreExtraProperties // Good practice

@IgnoreExtraProperties // Ignores properties in snapshot not in this class
data class Message(
    @get:Exclude var id: String? = null, // Exclude from being written to Firebase as a field, it's the node key
    var senderId: String? = null,
    var senderName: String? = null,
    var text: String? = null,
    var timestamp: Long? = null,
    var messageType: String? = null // Optional: if you store it within the message object itself
    // Add any other fields like imageUrl, etc.
) {
    // No-argument constructor is required by Firebase for deserialization
    constructor() : this(null, null, null, null, null, null)
}