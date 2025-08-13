package com.example.synapse.chats.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.chats.model.Message
import com.example.synapse.MessageAdapter
import com.example.synapse.databinding.FragmentDrawbacksBinding // Ensure this binding class is correct
import com.google.firebase.auth.FirebaseAuth // For currentUserId
import com.google.firebase.database.*

class DrawbacksFragment : Fragment() {

    private var _binding: FragmentDrawbacksBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter // Using the new ListAdapter version
    // private val drawbackMessagesList = mutableListOf<Message>() // No longer needed here
    private var groupId: String? = null
    private var currentUserId: String? = null // To store the current user's ID

    private lateinit var dbRefDrawbacks: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): DrawbacksFragment {
            val fragment = DrawbacksFragment()
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
        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("DrawbacksFragment", "Current User ID is null. Chat functionality will be limited.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawbacksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (currentUserId == null) {
            binding.emptyViewTextDrawbacks.text = "Error: You must be logged in to view messages."
            binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
            binding.progressBarDrawbacks.visibility = View.GONE
            binding.recyclerViewDrawbacks.visibility = View.GONE
            return
        }

        setupRecyclerView() // Call setupRecyclerView after currentUserId is confirmed

        if (groupId == null) {
            Log.e("DrawbacksFragment", "Group ID is null. Cannot fetch messages.")
            binding.emptyViewTextDrawbacks.text = "Error: Group not specified."
            binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
            binding.progressBarDrawbacks.visibility = View.GONE
            return
        }

        dbRefDrawbacks = FirebaseDatabase.getInstance().getReference("messages")
            .child(groupId!!)
            .child("drawback_messages") // Key difference for drawbacks

        fetchDrawbackMessages()
    }

    private fun setupRecyclerView() {
        currentUserId?.let { uid ->
            messageAdapter = MessageAdapter(uid) // Pass current user ID
            binding.recyclerViewDrawbacks.apply {
                layoutManager = LinearLayoutManager(requireContext()).apply {
                    // For chat-like behavior where new messages appear at the bottom
                    // and the list scrolls to show them:
                    stackFromEnd = true
                    // reverseLayout = false; // (default) if data is oldest to newest
                    // Set reverseLayout = true if your data source is newest to oldest
                    // and you want to display it correctly with stackFromEnd.
                }
                adapter = messageAdapter
            }
        } ?: run {
            Log.e("DrawbacksFragment", "Cannot setup RecyclerView: Current User ID is null.")
            binding.emptyViewTextDrawbacks.text = "Error: User session not found."
            binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
        }
    }

    private fun fetchDrawbackMessages() {
        binding.progressBarDrawbacks.visibility = View.VISIBLE
        binding.emptyViewTextDrawbacks.visibility = View.GONE

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarDrawbacks.visibility = View.GONE
                val messages = mutableListOf<Message>() // Create a new list for each update
                if (snapshot.exists()) {
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        // IMPORTANT: Set the message ID from the Firebase key
                        message?.id = messageSnapshot.key
                        message?.let { messages.add(it) }
                    }

                    if (messages.isEmpty()) {
                        binding.emptyViewTextDrawbacks.text = "No drawbacks submitted yet."
                        binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
                    } else {
                        binding.emptyViewTextDrawbacks.visibility = View.GONE
                    }
                } else {
                    binding.emptyViewTextDrawbacks.text = "No drawbacks submitted yet."
                    binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
                }

                // Submit the new list to the ListAdapter
                if (::messageAdapter.isInitialized) { // Check if adapter is initialized
                    // Sort by timestamp before submitting to ensure order
                    messageAdapter.submitList(messages.sortedBy { it.timestamp })
                }

                // Scroll to the relevant position
                val layoutManager = binding.recyclerViewDrawbacks.layoutManager as LinearLayoutManager
                if (messages.isNotEmpty()) {
                    if (layoutManager.stackFromEnd) {
                        binding.recyclerViewDrawbacks.scrollToPosition(messages.size - 1)
                    } else if (layoutManager.reverseLayout) {
                        binding.recyclerViewDrawbacks.scrollToPosition(0)
                    } else {
                        binding.recyclerViewDrawbacks.scrollToPosition(messages.size - 1)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarDrawbacks.visibility = View.GONE
                Log.e("DrawbacksFragment", "Firebase error: ${error.message}")
                binding.emptyViewTextDrawbacks.text = "Failed to load drawbacks."
                binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
            }
        }
        // Fetch messages ordered by timestamp.
        dbRefDrawbacks.orderByChild("timestamp").addValueEventListener(valueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbRefDrawbacks.isInitialized && ::valueEventListener.isInitialized) {
            dbRefDrawbacks.removeEventListener(valueEventListener)
        }
        _binding = null
    }
}