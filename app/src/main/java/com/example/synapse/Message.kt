package com.example.synapse

import com.google.firebase.database.Exclude

// Or your actual package

// Ensure this file is named Message.kt or similar
data class Message(
    val senderId: String? = null,
    val senderName: String? = null,
    val text: String? = null,       // This is likely the field you intend to use for content
    val timestamp: Long = 0L,
    val messageType: String? = null, // e.g., "improvement", "drawback", "text", "image"
    val sessionId: String? = null    // You mentioned not displaying this, but it might be in your class
    // Add any other fields like imageUrl, etc.
) {
    // Add a no-argument constructor if you're using Firebase Realtime Database
    // and want to deserialize directly to this class without issues if some fields are missing
    constructor() : this(null, null, null, 0L, null, null)

    // We need a way to associate the Firebase key/ID with this message object
    // if it's not part of the primary constructor. A common way is an additional property.
    @get:Exclude // For Firebase - so it doesn't try to serialize this 'id' back to the database
    @set:Exclude // For Firebase - so it doesn't try to deserialize 'id' from the database document
    var id: String? = null // This will hold the document ID from Firestore or key from RTDB
}