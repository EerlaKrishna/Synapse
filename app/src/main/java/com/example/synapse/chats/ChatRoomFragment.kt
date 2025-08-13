package com.example.synapse.chats

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.R
import com.example.synapse.databinding.FragmentChatRoomBinding
import com.example.synapse.MessageAdapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class ChatRoomFragment : Fragment() {

    // Binding
    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!! // Non-null accessor

    // Navigation Arguments & ViewModel
    private val navArgs: ChatRoomFragmentArgs by navArgs()
    private val chatRoomViewModel: ChatRoomViewModel by viewModels()

    // RecyclerView Adapter & User Info
    private lateinit var messageListAdapter: MessageAdapter
    private var currentUserId: String? = null

    // Chat State
    private var currentMessageType: String = MESSAGE_TYPE_IMPROVEMENT

    // For periodic refresh of message list (bolding recent messages)
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null
    private val REFRESH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1) // Refresh every 1 minute

    companion object {
        private const val TAG = "ChatRoomFragment"
        private const val MESSAGE_TYPE_IMPROVEMENT = "improvement_messages"
        private const val MESSAGE_TYPE_DRAWBACK = "drawback_messages"
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

        // Set the channel name to the new TextView
        binding.textViewChannelNameHeader.text = navArgs.groupName ?: getString(R.string.default_channel_name) // Use a string resource for default


        //(activity as? AppCompatActivity)?.supportActionBar?.title = navArgs.groupName

        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null. Cannot initialize chat.")
            Toast.makeText(requireContext(), getString(R.string.error_user_not_identified), Toast.LENGTH_LONG).show()
            binding.buttonSendMessage.isEnabled = false
            binding.editTextMessage.isEnabled = false
            return
        }
        setupBackButton() // Add this call
        setupTabs()
        setupMessagesRecyclerView()
        setupSendButton()
        observeViewModel()

        if (binding.tabLayoutChatRoom.tabCount > 0) {
            binding.tabLayoutChatRoom.getTabAt(0)?.select()
        }
        if (chatRoomViewModel.messages.value.isNullOrEmpty()) {
            val initialMessageType = if (binding.tabLayoutChatRoom.selectedTabPosition == 1) {
                MESSAGE_TYPE_DRAWBACK
            } else {
                MESSAGE_TYPE_IMPROVEMENT // Default to improvement
            }
            currentMessageType = initialMessageType // Ensure currentMessageType is set
            Log.d(TAG, "Explicit initial load for tab position ${binding.tabLayoutChatRoom.selectedTabPosition}. Type: $currentMessageType. GroupId: ${navArgs.groupId}")
            chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::messageListAdapter.isInitialized && messageListAdapter.itemCount > 0 && currentUserId != null) {
            startPeriodicRefresh()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called.")
        binding.recyclerViewMessages.adapter = null
        _binding = null
        stopPeriodicRefresh() // Ensure handler callbacks are removed
    }

    // New function to setup the back button
    private fun setupBackButton() {
        binding.buttonBackArrow.setOnClickListener {
            // findNavController().navigateUp() // This is usually sufficient
            // OR, if you want to be more explicit about popping the current fragment:
            findNavController().popBackStack()
            Log.d(TAG, "Back button clicked, navigating up.")
        }
    }

    private fun setupTabs() {
        Log.d(TAG, "Setting up tabs.")
        if (binding.tabLayoutChatRoom.tabCount == 0) {
            binding.tabLayoutChatRoom.addTab(
                binding.tabLayoutChatRoom.newTab().setText(getString(R.string.tab_improvement))
            )
            binding.tabLayoutChatRoom.addTab(
                binding.tabLayoutChatRoom.newTab().setText(getString(R.string.tab_drawback))
            )
            Log.d(TAG, "Tabs added programmatically.")
        } else {
            binding.tabLayoutChatRoom.getTabAt(0)?.text = getString(R.string.tab_improvement)
            binding.tabLayoutChatRoom.getTabAt(1)?.text = getString(R.string.tab_drawback)
            Log.d(TAG, "Tabs already exist. Ensuring text is set. Count: ${binding.tabLayoutChatRoom.tabCount}")
        }

        binding.tabLayoutChatRoom.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val previousMessageType = currentMessageType
                    currentMessageType = when (it.position) {
                        0 -> MESSAGE_TYPE_IMPROVEMENT
                        1 -> MESSAGE_TYPE_DRAWBACK
                        else -> {
                            Log.w(TAG, "Unknown tab selected: ${it.position}, defaulting to IMPROVEMENT.")
                            MESSAGE_TYPE_IMPROVEMENT
                        }
                    }
                    if (previousMessageType != currentMessageType) {
                        Log.d(TAG, "Tab selected: Pos ${it.position}, New Type: $currentMessageType")
                        messageListAdapter.submitList(emptyList())
                        stopPeriodicRefresh()
                        chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
                    } else {
                        Log.d(TAG, "Tab reselected or type unchanged ($currentMessageType).")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "Tab reselected: Pos ${tab?.position}, Type: $currentMessageType")
                messageListAdapter.submitList(emptyList())
                stopPeriodicRefresh()
                chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
            }
        })
        Log.d(TAG, "OnTabSelectedListener added.")
    }

    private fun setupMessagesRecyclerView() {
        Log.d(TAG, "Setting up messages RecyclerView.")
        currentUserId?.let { userId ->
            messageListAdapter = MessageAdapter(userId) // Pass currentUserId
            binding.recyclerViewMessages.adapter = messageListAdapter
            val layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            binding.recyclerViewMessages.layoutManager = layoutManager
            binding.recyclerViewMessages.itemAnimator = null // To prevent flickering on refresh
            Log.d(TAG, "Messages RecyclerView setup complete.")
        } ?: run {
            Log.e(TAG, "Cannot setup RecyclerView: currentUserId is null.")
        }
    }

    private fun setupSendButton() {
        binding.buttonSendMessage.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val userName = getCurrentUserName()
                Log.d(TAG, "Sending: Group ${navArgs.groupId}, Type $currentMessageType, Text '$messageText', User '$userName'")
                chatRoomViewModel.sendMessage(
                    groupId = navArgs.groupId,
                    messageType = currentMessageType,
                    text = messageText,
                    senderName = userName
                )
                binding.editTextMessage.text.clear()
                hideKeyboard()
            } else {
                Log.d(TAG, "Send button: emptymessage.")
                Toast.makeText(requireContext(), getString(R.string.message_empty_cannot_send), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentUserName(): String {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return firebaseUser?.displayName?.takeIf { it.isNotBlank() }
            ?: firebaseUser?.email?.takeIf { it.contains("@") }?.substringBefore("@")
            ?: getString(R.string.default_anonymous_user_name) // Ensure this string resource exists
    }


// End of Part 1
// Continued from Part 1...

    private fun observeViewModel() {
        Log.d(TAG, "Observing ViewModel.")

        chatRoomViewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d(TAG, "Observed messages for $currentMessageType. Count: ${messages.size}")
            messageListAdapter.submitList(messages.toList()) {
                if (messages.isNotEmpty()) {
                    val layoutManager = binding.recyclerViewMessages.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = messageListAdapter.itemCount

                    if (lastVisibleItemPosition == -1 ||
                        (totalItemCount - lastVisibleItemPosition) <= 5 ||
                        lastVisibleItemPosition == totalItemCount - 2 ||
                        layoutManager.stackFromEnd) {
                        binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
            // Manage periodic refresh based on message list
            if (messages.isNotEmpty() && viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                startPeriodicRefresh()
            } else {
                stopPeriodicRefresh()
            }
        }

        chatRoomViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e(TAG, "ViewModel error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                chatRoomViewModel.clearErrorMessage()
            }
        }

        chatRoomViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "isLoading state: $isLoading")
            binding.progressBarMessages.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.editTextMessage.isEnabled = !isLoading
            binding.buttonSendMessage.isEnabled = !isLoading
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun startPeriodicRefresh() {
        if (currentUserId == null || !::messageListAdapter.isInitialized) {
            Log.w(TAG, "Cannot start periodic refresh: User ID or adapter not ready.")
            return
        }

        stopPeriodicRefresh() // Stop any existing runnable
        Log.d(TAG, "Starting periodic refresh for message list.")
        refreshRunnable = Runnable {
            if (isAdded && view != null && _binding != null) { // Ensure fragment is still valid
                Log.d(TAG, "Periodic refresh: Re-submitting list to adapter.")
                // Re-submit the current list. The adapter's onBindViewHolder will
                // re-evaluate the bold status for each message.
                messageListAdapter.submitList(messageListAdapter.currentList.toList())
            }
            // Schedule the next refresh only if the fragment is still in a resumed state
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                refreshHandler.postDelayed(refreshRunnable!!, REFRESH_INTERVAL_MS)
            } else {
                Log.d(TAG, "Periodic refresh: Not rescheduling as fragment is not resumed.")
            }
        }
        // Post the first execution
        refreshHandler.postDelayed(refreshRunnable!!, REFRESH_INTERVAL_MS)
    }

    private fun stopPeriodicRefresh() {
        refreshRunnable?.let {
            Log.d(TAG, "Stopping periodic refresh for message list.")
            refreshHandler.removeCallbacks(it)
        }
        refreshRunnable = null // Clear the runnable
    }
}
// End of Part 2