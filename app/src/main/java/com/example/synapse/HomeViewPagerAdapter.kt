package com.example.synapse

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomeViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // Number of tabs
    private val NUM_TABS = 2 // Assuming two tabs: Departments and Direct Messages

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BroadGroupFragment() // Your fragment for "Departments"
            1 -> DirectMessageFragment() // Your fragment for "Direct Messages" (Create this if it doesn't exist)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}