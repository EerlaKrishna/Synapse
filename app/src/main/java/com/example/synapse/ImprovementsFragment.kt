package com.example.synapse

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.databinding.FragmentImprovementsBinding // Your ViewBinding class
import com.google.firebase.auth.FirebaseAuth // For currentUserId
import com.google.firebase.database.*

class ImprovementsFragment : Fragment() {

    private var _binding: FragmentImprovementsBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter // Using the new ListAdapter version
    // private val improvementMessagesList = mutableListOf<Message>() // No longer needed here, ListAdapter manages its own list
    private var groupId: String? = null
    private var currentUserId: String? = null // To store the current user's ID

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
        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("ImprovementsFragment", "Current User ID is null. Chat functionality will be limited.")
            // Handle this appropriately, maybe show an error or disable sending messages
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

        if (currentUserId == null) {
            // Display an error message if the user is not logged in
            binding.emptyViewTextImprovements.text = "Error: You must be logged in to view messages."
            binding.emptyViewTextImprovements.visibility = View.VISIBLE
            binding.progressBarImprovements.visibility = View.GONE
            binding.recyclerViewImprovements.visibility = View.GONE
            return // Don't proceed if no user
        }

        setupRecyclerView() // Call setupRecyclerView after currentUserId is confirmed

        if (groupId == null) {
            Log.e("ImprovementsFragment", "Group ID is null. Cannot fetch messages.")
            binding.emptyViewTextImprovements.text = "Error: Group not specified."
            binding.emptyViewTextImprovements.visibility = View.VISIBLE
            binding.progressBarImprovements.visibility = View.GONE
            return
        }

        // Path to your messages in Firebase, adjust if necessary
        // Assuming messages are stored under "messages/{groupId}/improvement_messages"
        dbRefImprovements = FirebaseDatabase.getInstance().getReference("messages")
            .child(groupId!!) // Group specific messages
            .child("improvement_messages") // Specific type of messages for this fragment

        fetchImprovementMessages()
    }

    private fun setupRecyclerView() {
        // Ensure currentUserId is not null before initializing adapter
        currentUserId?.let { uid ->
            messageAdapter = MessageAdapter(uid) // Pass current user ID
            binding.recyclerViewImprovements.apply {
                layoutManager = LinearLayoutManager(requireContext()).apply {
                    // stackFromEnd = true // For chat-like behavior, new items at bottom
                    // reverseLayout = true // To show newest items at the bottom and start from there
                    // If you want newest at the bottom and to start scrolled there:
                    stackFromEnd = true
                    // reverseLayout = false; // if you want newest at bottom but list starts at top
                    // if you want newest at top (like a feed), both false
                }
                adapter = messageAdapter
            }
        } ?: run {
            Log.e("ImprovementsFragment", "Cannot setup RecyclerView: Current User ID is null.")
            // Optionally, show an error message on the UI
            binding.emptyViewTextImprovements.text = "Error: User session not found."
            binding.emptyViewTextImprovements.visibility = View.VISIBLE
        }
    }

    private fun fetchImprovementMessages() {
        binding.progressBarImprovements.visibility = View.VISIBLE
        binding.emptyViewTextImprovements.visibility = View.GONE

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarImprovements.visibility = View.GONE
                val messages = mutableListOf<Message>() // Create a new list for each update
                if (snapshot.exists()) {
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        // IMPORTANT: Set the message ID from the Firebase key
                        message?.id = messageSnapshot.key
                        message?.let { messages.add(it) }
                    }

                    if (messages.isEmpty()) {
                        binding.emptyViewTextImprovements.text = "No improvements submitted yet."
                        binding.emptyViewTextImprovements.visibility = View.VISIBLE
                    } else {
                        binding.emptyViewTextImprovements.visibility = View.GONE
                    }
                } else {
                    binding.emptyViewTextImprovements.text = "No improvements submitted yet."
                    binding.emptyViewTextImprovements.visibility = View.VISIBLE
                }

                // Submit the new list to the ListAdapter
                if(::messageAdapter.isInitialized) { // Check if adapter is initialized
                    messageAdapter.submitList(messages.sortedBy { it.timestamp }) // Sort by timestamp before submitting
                }


                // Scroll to bottom if new messages are added at the end
                // ListAdapter handles updates, scrolling might need adjustment based on how submitList works
                // and your layout manager settings.
                val layoutManager = binding.recyclerViewImprovements.layoutManager as LinearLayoutManager
                if (messages.isNotEmpty()) {
                    if (layoutManager.stackFromEnd) {
                        // If stackFromEnd is true, new items are added at the bottom,
                        // and the view should automatically scroll if it was already at the bottom.
                        // For explicit scroll to the very last item:
                        binding.recyclerViewImprovements.scrollToPosition(messages.size - 1)
                    } else if (layoutManager.reverseLayout) {
                        // If reverseLayout is true (and stackFromEnd is false), new items are at the top (index 0)
                        binding.recyclerViewImprovements.scrollToPosition(0)
                    } else {
                        // Default: new items at bottom, scroll to last
                        binding.recyclerViewImprovements.scrollToPosition(messages.size - 1)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarImprovements.visibility = View.GONE
                Log.e("ImprovementsFragment", "Firebase error: ${error.message}")
                binding.emptyViewTextImprovements.text = "Failed to load improvements."
                binding.emptyViewTextImprovements.visibility = View.VISIBLE
            }
        }
        // Fetch messages ordered by timestamp. Firebase push keys are naturally chronological,
        // but explicit ordering is good practice if timestamps are manually set or critical.
        dbRefImprovements.orderByChild("timestamp").addValueEventListener(valueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbRefImprovements.isInitialized && ::valueEventListener.isInitialized) {
            dbRefImprovements.removeEventListener(valueEventListener)
        }
        _binding = null
    }
}
