package com.example.synapse.chats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.activity
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.R // Import R for string resources if you use them
import com.example.synapse.databinding.FragmentChatRoomBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import kotlin.text.clear
import kotlin.text.isNotEmpty
import kotlin.text.isNullOrBlank
import kotlin.text.trim

class ChatRoomFragment : Fragment() {

    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    private val navArgs: ChatRoomFragmentArgs by navArgs()
    private val chatRoomViewModel: ChatRoomViewModel by viewModels()

    private lateinit var chatRoomPagerAdapter: ChatRoomPagerAdapter
    private lateinit var messageListAdapter: MessageListAdapter
    private var currentUserId: String? = null


    private var currentMessageType: String = MESSAGE_TYPE_IMPROVEMENT

    companion object {
        private const val TAG = "ChatRoomFragment"
        private const val MESSAGE_TYPE_IMPROVEMENT = "improvement" // Or R.string.tab_improvement_key
        private const val MESSAGE_TYPE_DRAWBACK = "drawback"       // Or R.string.tab_drawback_key
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView called.")
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called.")
        Log.d(TAG, "Received Group ID: ${navArgs.groupId}, Group Name: ${navArgs.groupName}")

        (activity as? AppCompatActivity)?.supportActionBar?.title = navArgs.groupName

        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null. Cannot initialize chat.")
            Toast.makeText(requireContext(), "Error: User not identified. Please restart.", Toast.LENGTH_LONG).show()
            // Consider disabling UI elements or navigating back
            binding.buttonSendMessage.isEnabled = false
            binding.editTextMessage.isEnabled = false
            return
        }

        setupTabsAndViewPager()
        setupMessagesRecyclerView()
        setupSendButton()
        observeViewModel()

        Log.d(TAG, "Initial load for groupId: ${navArgs.groupId}, messageType: $currentMessageType")
        chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
    }

    private fun setupTabsAndViewPager() {
        Log.d(TAG, "Setting up tabs and ViewPager2.")
        chatRoomPagerAdapter = ChatRoomPagerAdapter(this, navArgs.groupId)
        binding.viewPagerChatRoomTabs.adapter = chatRoomPagerAdapter
        Log.d(TAG, "ViewPager adapter item count: ${chatRoomPagerAdapter.itemCount}")

        TabLayoutMediator(binding.tabLayoutChatRoom, binding.viewPagerChatRoomTabs) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_improvement)
                1 -> getString(R.string.tab_drawback)
                else -> null
            }
            Log.d(TAG, "TabLayoutMediator: Configuring tab at position $position")
        }.attach()

        // Add a listener for tab selection
        binding.tabLayoutChatRoom.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    currentMessageType = when (it.position) {
                        0 -> MESSAGE_TYPE_IMPROVEMENT // Use your defined constant
                        1 -> MESSAGE_TYPE_DRAWBACK   // Use your defined constant
                        else -> MESSAGE_TYPE_IMPROVEMENT // Default or handle error
                    }
                    Log.d(TAG, "Tab selected: Position ${it.position}, MessageType: $currentMessageType")
                    // Load messages for the newly selected tab/message type
                    chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Optional: Handle tab unselection if needed
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Optional: Handle tab reselection if needed
            }
        })
        Log.d(TAG, "TabLayoutMediator attached and OnTabSelectedListener added.")
    }

    private fun setupMessagesRecyclerView() {
        Log.d(TAG, "Setting up messages RecyclerView.")
        messageListAdapter = MessageListAdapter(currentUserId!!) // currentUserId is checked not null before this
        binding.recyclerViewMessages.adapter = messageListAdapter
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerViewMessages.layoutManager = layoutManager
        binding.recyclerViewMessages.setHasFixedSize(true) // Optimization if item sizes don't change
        Log.d(TAG, "Messages RecyclerView adapter and layout manager set.")
    }

        // Implementation for getCurrentUserName
        private fun getCurrentUserName(): String {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val displayName = firebaseUser?.displayName
            val email = firebaseUser?.email

            // Prefer display name if available and not blank
            if (!displayName.isNullOrBlank()) {
                return displayName
            }

            // Fallback to part of the email if display name is not set
            if (!email.isNullOrBlank() && email.contains("@")) {
                return email.substringBefore("@")
            }

            // Further fallback if no display name and email is not usable
            return "User" // Or currentUserId if you want something unique but not necessarily a name
        }


    private fun setupSendButton() {
        binding.buttonSendMessage.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val userName = getCurrentUserName()
                // Pass the currentMessageType
                Log.d(TAG, "Sending message. GroupId: ${navArgs.groupId}, Type: $currentMessageType, Text: $messageText")
                chatRoomViewModel.sendMessage(navArgs.groupId, currentMessageType, messageText, userName)
                binding.editTextMessage.text.clear()
            } else {
                Log.d(TAG, "Send button clicked with empty message.")
            }
        }
    }


    private fun observeViewModel() {
        Log.d(TAG, "Observing ViewModel.")

        chatRoomViewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d(TAG, "Observed messages list. Count: ${messages.size}")
            messageListAdapter.submitList(messages) {
                // Scroll to the bottom to show the newest message after list is updated
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        chatRoomViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e(TAG, "ViewModel error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                chatRoomViewModel.clearErrorMessage() // Clear error after showing
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Potentially remove listeners or clear resources if not handled by ViewModel's onCleared
        Log.d(TAG, "onDestroyView called.")
        _binding = null // Crucial for preventing memory leaks with ViewBinding
    }
}
