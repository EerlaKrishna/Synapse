package com.example.synapse.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _searchQuery = MutableLiveData<String?>()
    val searchQuery: LiveData<String?> = _searchQuery

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
    }
}