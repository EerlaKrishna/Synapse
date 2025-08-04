package com.example.synapse.chats

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.R
import com.example.synapse.databinding.FragmentChatRoomBinding
import com.example.synapse.Message // Ensure this is imported
import com.example.synapse.MessageAdapter
import com.google.android.material.tabs.TabLayout
// Removed: import com.google.android.material.tabs.TabLayoutMediator - No longer needed
import com.google.firebase.auth.FirebaseAuth

class ChatRoomFragment : Fragment() {

    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    private val navArgs: ChatRoomFragmentArgs by navArgs()
    private val chatRoomViewModel: ChatRoomViewModel by viewModels()

    // Removed: private lateinit var chatRoomPagerAdapter: ChatRoomPagerAdapter
    private lateinit var messageListAdapter: MessageAdapter
    private var currentUserId: String? = null

    private var currentMessageType: String = MESSAGE_TYPE_IMPROVEMENT

    companion object {
        private const val TAG = "ChatRoomFragment"
        private const val MESSAGE_TYPE_IMPROVEMENT = "improvement_messages" // Match Firebase node
        private const val MESSAGE_TYPE_DRAWBACK = "drawback_messages"       // Match Firebase node
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
            Toast.makeText(requireContext(), getString(R.string.error_user_not_identified), Toast.LENGTH_LONG).show()
            binding.buttonSendMessage.isEnabled = false
            binding.editTextMessage.isEnabled = false
            return
        }

        setupTabs() // Renamed from setupTabsAndViewPager
        setupMessagesRecyclerView()
        setupSendButton()
        observeViewModel()

        // Ensure the first tab is selected visually and data is loaded for it
        binding.tabLayoutChatRoom.getTabAt(0)?.select()
        // The onTabSelected listener should trigger the initial load.
        // If you find it doesn't reliably (e.g., if tabs are added after listener is set),
        // you can add an explicit initial load here, checking if messages are already loaded.
        if (chatRoomViewModel.messages.value.isNullOrEmpty() && binding.tabLayoutChatRoom.selectedTabPosition == 0) {
            Log.d(TAG, "Explicit initial load for default tab (Improvement). GroupId: ${navArgs.groupId}")
            currentMessageType = MESSAGE_TYPE_IMPROVEMENT // Ensure it's set before load
            chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
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
            Log.d(TAG, "Tabs already exist or are defined in XML. Ensuring text is set. Count: ${binding.tabLayoutChatRoom.tabCount}")
        }

        binding.tabLayoutChatRoom.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val previousMessageType = currentMessageType
                    currentMessageType = when (it.position) {
                        0 -> MESSAGE_TYPE_IMPROVEMENT
                        1 -> MESSAGE_TYPE_DRAWBACK
                        else -> {
                            Log.w(TAG, "Unknown tab selected at position: ${it.position}, defaulting to IMPROVEMENT.")
                            MESSAGE_TYPE_IMPROVEMENT
                        }
                    }
                    if (previousMessageType != currentMessageType) {
                        Log.d(TAG, "Tab selected: Position ${it.position}, New MessageType: $currentMessageType")
                        messageListAdapter.submitList(emptyList())
                        chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
                    } else {
                        Log.d(TAG, "Tab reselected or message type unchanged ($currentMessageType). Data load handled by onTabReselected or initial load.")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "Tab reselected: Position ${tab?.position}, MessageType: $currentMessageType")
                messageListAdapter.submitList(emptyList())
                chatRoomViewModel.loadMessages(navArgs.groupId, currentMessageType)
            }
        })
        Log.d(TAG, "OnTabSelectedListener added to TabLayout.")
    }

    private fun setupMessagesRecyclerView() {
        Log.d(TAG, "Setting up messages RecyclerView for ChatRoomFragment.")
        messageListAdapter = MessageAdapter(currentUserId!!)
        binding.recyclerViewMessages.adapter = messageListAdapter
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerViewMessages.layoutManager = layoutManager
        Log.d(TAG, "Messages RecyclerView adapter and layout manager set for ChatRoomFragment.")
    }

    private fun getCurrentUserName(): String {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return firebaseUser?.displayName?.takeIf { it.isNotBlank() }
            ?: firebaseUser?.email?.takeIf { it.contains("@") }?.substringBefore("@")
            ?: "User"
    }

    private fun setupSendButton() {
        binding.buttonSendMessage.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val userName = getCurrentUserName()
                Log.d(TAG, "Sending message. GroupId: ${navArgs.groupId}, Type: $currentMessageType, Text: $messageText, User: $userName")
                chatRoomViewModel.sendMessage(
                    groupId = navArgs.groupId,
                    messageType = currentMessageType,
                    text = messageText,
                    senderName = userName
                )
                binding.editTextMessage.text.clear()
                hideKeyboard()
            } else {
                Log.d(TAG, "Send button clicked with empty message.")
                Toast.makeText(requireContext(), getString(R.string.message_empty_cannot_send), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "Observing ViewModel for ChatRoomFragment.")

        chatRoomViewModel.messages.observe(viewLifecycleOwner) { messages ->
            // Ensure the messages being submitted are for the currently selected tab.
            // This check is important because the LiveData might emit old data
            // from a previous messageType if not handled carefully in ViewModel or here.
            // However, since loadMessages is called on tab change, this should generally be fine.
            Log.d(TAG, "Observed messages list for $currentMessageType. Count: ${messages.size}")
            messageListAdapter.submitList(messages.toList()) { // Create a new list for DiffUtil
                // Scroll to bottom only if new messages were added or list is not empty
                // and the user hasn't scrolled up manually (more complex to check,
                // for now, always scroll if list is updated and not empty).
                if (messages.isNotEmpty()) {
                    // Check if the last visible item is close to the end of the list
                    // to avoid auto-scrolling if the user has scrolled up.
                    val layoutManager = binding.recyclerViewMessages.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = messageListAdapter.itemCount

                    // Auto-scroll if near the bottom or if it's an initial load/small list
                    if (lastVisibleItemPosition == -1 || // Nothing visible yet
                        (totalItemCount - lastVisibleItemPosition) <= 5 || // Near the bottom
                        lastVisibleItemPosition == totalItemCount - 2) { // If a new item was added at the end
                        binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        chatRoomViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e(TAG, "ViewModel error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                chatRoomViewModel.clearErrorMessage() // Important to prevent re-showing on config change
            }
        }

        chatRoomViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "isLoading state changed: $isLoading")
            binding.progressBarMessages.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Optionally, disable input fields while loading to prevent sending messages
            // when the list might not be fully up-to-date or during an operation.
            binding.editTextMessage.isEnabled = !isLoading
            binding.buttonSendMessage.isEnabled = !isLoading
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called.")
        // Important to prevent memory leaks with RecyclerView
        binding.recyclerViewMessages.adapter = null
        _binding = null
    }
}