package com.example.synapse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.synapse.databinding.FragmentBroadGroupBinding

class BroadGroupFragment : Fragment() {

    private lateinit var binding: FragmentBroadGroupBinding
    private val departments = listOf("Tech", "HR", "Finance")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBroadGroupBinding.inflate(inflater, container, false)

        val adapter = DepartmentAdapter(departments) { group ->
            val intent = Intent(requireContext(), GroupChatActivity::class.java)
            intent.putExtra("group", group)
            startActivity(intent)
        }

        binding.recyclerView.adapter = adapter
        return binding.root
    }
}
