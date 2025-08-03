package com.example.synapse.chats

data class LastMessage(
    var text: String? = null,
    var timestamp: Long? = null,
    var senderName: String? = null
    // Add a no-arg constructor if you ever write this directly to Firebase
    // constructor() : this(null, null, null)
) {
    constructor() : this(null, null, null) // No-arg constructor for Firebase deserialization
}
