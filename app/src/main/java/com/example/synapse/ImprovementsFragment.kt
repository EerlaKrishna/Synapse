package com.example.synapse

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.databinding.FragmentImprovementsBinding // Create this layout
import com.google.firebase.database.*

class ImprovementsFragment : Fragment() {

    private var _binding: FragmentImprovementsBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter
    private val improvementMessagesList = mutableListOf<Message>()
    private var groupId: String? = null

    private lateinit var dbRefImprovements: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): ImprovementsFragment {
            val fragment = ImprovementsFragment()
            val args = Bundle()
            args.putString(ARG_GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImprovementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        if (groupId == null) {
            Log.e("ImprovementsFragment", "Group ID is null. Cannot fetch messages.")
            binding.emptyViewTextImprovements.text = "Error: Group not specified."
            binding.emptyViewTextImprovements.visibility = View.VISIBLE
            binding.progressBarImprovements.visibility = View.GONE
            return
        }

        dbRefImprovements = FirebaseDatabase.getInstance().getReference("messages")
            .child(groupId!!)
            .child("improvement_messages")

        fetchImprovementMessages()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(improvementMessagesList)
        binding.recyclerViewImprovements.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true // Optional: Show newest messages at the bottom
                stackFromEnd = true  // Optional: Show newest messages at the bottom
            }
            adapter = messageAdapter
        }
    }

    private fun fetchImprovementMessages() {
        binding.progressBarImprovements.visibility = View.VISIBLE
        binding.emptyViewTextImprovements.visibility = View.GONE

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarImprovements.visibility = View.GONE
                improvementMessagesList.clear()
                if (snapshot.exists()) {
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        message?.let { improvementMessagesList.add(it) }
                    }
                    // Sort by timestamp if needed, though Firebase push keys are chronological
                    // improvementMessagesList.sortBy { it.timestamp } // Ascending
                    // improvementMessagesList.sortByDescending { it.timestamp } // Descending (if not using reverseLayout)

                    if (improvementMessagesList.isEmpty()) {
                        binding.emptyViewTextImprovements.text = "No improvements submitted yet."
                        binding.emptyViewTextImprovements.visibility = View.VISIBLE
                    } else {
                        binding.emptyViewTextImprovements.visibility = View.GONE
                    }
                } else {
                    binding.emptyViewTextImprovements.text = "No improvements submitted yet."
                    binding.emptyViewTextImprovements.visibility = View.VISIBLE
                }
                messageAdapter.updateMessages(improvementMessagesList)
                // Scroll to bottom if new messages are added at the end
                if (improvementMessagesList.isNotEmpty() && (binding.recyclerViewImprovements.layoutManager as LinearLayoutManager).reverseLayout) {
                    binding.recyclerViewImprovements.scrollToPosition(0) // Scroll to the newest message
                } else if (improvementMessagesList.isNotEmpty()){
                    binding.recyclerViewImprovements.scrollToPosition(improvementMessagesList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarImprovements.visibility = View.GONE
                Log.e("ImprovementsFragment", "Firebase error: ${error.message}")
                binding.emptyViewTextImprovements.text = "Failed to load improvements."
                binding.emptyViewTextImprovements.visibility = View.VISIBLE
                // Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        dbRefImprovements.orderByChild("timestamp").addValueEventListener(valueEventListener) // Order by timestamp
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbRefImprovements.isInitialized && ::valueEventListener.isInitialized) {
            dbRefImprovements.removeEventListener(valueEventListener)
        }
        _binding = null
    }
}