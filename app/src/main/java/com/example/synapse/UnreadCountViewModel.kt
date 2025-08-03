// UnreadCountViewModel.kt
package com.example.synapse // Or your actual package

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log // For logging

class UnreadCountViewModel : ViewModel() {

    companion object {
        private const val TAG = "UnreadCountViewModel"
    }

    // Example LiveData for unread direct messages count
    private val _unreadDirectMessagesCount = MutableLiveData<Int>(0)
    val unreadDirectMessagesCount: LiveData<Int> = _unreadDirectMessagesCount

    // Example LiveData for unread group messages (if you manage any here, though it seems BroadGroupViewModel does this)
    // private val _unreadGroupMessagesCount = MutableLiveData<Int>(0)
    // val unreadGroupMessagesCount: LiveData<Int> = _unreadGroupMessagesCount

    fun setUnreadDirectMessagesCount(count: Int) {
        _unreadDirectMessagesCount.value = count
        Log.d(TAG, "Unread DM count set to: $count")
    }

    fun incrementUnreadDirectMessages() {
        _unreadDirectMessagesCount.value = (_unreadDirectMessagesCount.value ?: 0) + 1
        Log.d(TAG, "Unread DM count incremented to: ${_unreadDirectMessagesCount.value}")
    }

    fun clearUnreadDirectMessages() {
        _unreadDirectMessagesCount.value = 0
        Log.d(TAG, "Unread DM count cleared.")
    }

    // Add other methods for managing counts as needed...

    // --- THIS IS THE METHOD YOU NEED TO ADD ---
    fun clearAllData() {
        _unreadDirectMessagesCount.postValue(0) // Use postValue if called from non-UI thread, or value if on main
        // Reset any other LiveData or state variables here
        // e.g., _unreadGroupMessagesCount.postValue(0)
        Log.d(TAG, "All data cleared from UnreadCountViewModel.")
    }
}