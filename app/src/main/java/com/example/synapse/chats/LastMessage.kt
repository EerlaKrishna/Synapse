package com.example.synapse.chats // Or your actual package for models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties // Good practice for Firebase data classes
data class LastMessage(
    var text: String? = null,
    var senderName: String? = null,
    var timestamp: Long? = 0L,
    var messageType: String? = null // <-- THIS IS THE IMPORTANT ADDITION
) {
    // A no-argument constructor is needed by Firebase for deserialization,
    // especially if you have other constructors or if default values aren't sufficient.
    // For data classes where all properties have default values (like String? = null, Long? = 0L),
    // Firebase can often manage, but explicitly adding it is safer.
    constructor() : this(null, null, 0L, null)
}