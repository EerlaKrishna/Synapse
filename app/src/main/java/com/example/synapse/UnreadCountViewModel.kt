package com.example.synapse // Or your viewmodel package e.g., com.example.synapse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UnreadCountViewModel : ViewModel() {

    // For Direct Messages
    private val _unreadDirectMessagesCount = MutableLiveData<Int>(0)
    val unreadDirectMessagesCount: LiveData<Int> = _unreadDirectMessagesCount

    // You could add other unread counts here, e.g., group messages, general notifications
    // private val _unreadGroupMessagesCount = MutableLiveData<Int>(0)
    // val unreadGroupMessagesCount: LiveData<Int> = _unreadGroupMessagesCount

    // --- Methods to update Direct Message count ---
    fun setUnreadDirectMessagesCount(count: Int) {
        _unreadDirectMessagesCount.value = count
    }

    fun incrementUnreadDirectMessages() {
        _unreadDirectMessagesCount.value = (_unreadDirectMessagesCount.value ?: 0) + 1
    }

    fun decrementUnreadDirectMessages() {
        val currentCount = _unreadDirectMessagesCount.value ?: 0
        if (currentCount > 0) {
            _unreadDirectMessagesCount.value = currentCount - 1
        }
    }

    fun clearUnreadDirectMessages() {
        _unreadDirectMessagesCount.value = 0
    }

    // --- Example methods for another type of notification (if you add more LiveData) ---
    // fun setUnreadGroupMessagesCount(count: Int) {
    //     _unreadGroupMessagesCount.value = count
    // }

    // fun clearAllUnreadCounts() {
    //     _unreadDirectMessagesCount.value = 0
    //     _unreadGroupMessagesCount.value = 0
    //     // etc.
    // }

    // TODO: Add methods to fetch initial counts from your data source (e.g., Firebase)
    // This might involve observing changes from a repository.
    // For example:
    // fun loadInitialCounts() {
    //     viewModelScope.launch {
    //         val initialDMCount = repository.getInitialUnreadDMCount()
    //         _unreadDirectMessagesCount.postValue(initialDMCount)
    //
    //         val initialGroupCount = repository.getInitialUnreadGroupCount()
    //         _unreadGroupMessagesCount.postValue(initialGroupCount)
    //     }
    // }
}