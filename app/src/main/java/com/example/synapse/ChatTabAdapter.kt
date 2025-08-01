package com.example.synapse

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ChatTabAdapter(activity: FragmentActivity, private val groupId: String) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2 // Two tabs: Improvements and Drawbacks

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ImprovementsFragment.newInstance(groupId)
            1 -> DrawbacksFragment.newInstance(groupId)
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}