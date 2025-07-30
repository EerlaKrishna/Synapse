package com.example.synapse

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ChatTabAdapter(val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val titles = arrayOf("Improvements", "Drawbacks")

    override fun getCount() = titles.size
    override fun getItem(position: Int): Fragment =
        if (position == 0) ImprovementsFragment() else DrawbacksFragment()
    override fun getPageTitle(position: Int): CharSequence = titles[position]
}
