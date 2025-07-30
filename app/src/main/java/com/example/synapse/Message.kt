package com.example.synapse

data class Message(
    val sessionId: String,
    val content: String,
    val timestamp: Long
)
