package com.example.synapse.home.ui

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.synapse.groups.BroadGroupFragment

class HomeViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // Number of tabs
    // If you are removing DirectMessageFragment, you likely only have 1 tab left.
    private val NUM_TABS = 1 // Only "Departments" (BroadGroupFragment)

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BroadGroupFragment() // Your fragment for "Departments"
            // Case 1 for DirectMessageFragment is now removed.
            else -> {
                // If NUM_TABS is 1, this 'else' should ideally not be reached
                // if getItemCount() is correctly respected by the ViewPager.
                // However, it's good practice to handle unexpected positions.
                Log.e("HomeViewPagerAdapter", "Invalid position requested: $position. Only 0 is valid.")
                // You could return a default fragment or throw an exception.
                // If you are sure only BroadGroupFragment should exist:
                BroadGroupFragment() // Or throw IllegalArgumentException("Invalid position: $position. Expected 0.")
            }
        }
    }
}
