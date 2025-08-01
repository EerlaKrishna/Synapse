package com.example.synapse

data class Message(
    val sessionId: String = "", // Default value
    val content: String = "",   // Default value
    val timestamp: Long = 0L    // Default value
) {
    // Optional: If you need a secondary constructor for convenience when creating new messages,
    // but Firebase will use the primary constructor with defaults for deserialization.
    // constructor() : this("", "", 0L) // This is implicitly handled by defaults now
}
