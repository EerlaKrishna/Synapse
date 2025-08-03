package com.example.synapse // Or your actual fragment package

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
// import androidx.recyclerview.widget.DividerItemDecoration // Uncomment if you want dividers
import com.example.synapse.R
import com.example.synapse.chats.BroadGroupViewModel
import com.example.synapse.chats.ChatListAdapter
import com.example.synapse.chats.ChatListItem
import com.example.synapse.chats.OnChatClickListener
import com.example.synapse.databinding.FragmentBroadGroupBinding
// import com.google.android.material.snackbar.Snackbar // Uncomment if you use Snackbar




class BroadGroupFragment : Fragment(), OnChatClickListener {


    private var navigationListener: ChatNavigationListener? = null

    private var _binding: FragmentBroadGroupBinding? = null
    private val binding get() = _binding!! // This is safe as long as you only access it between onCreateView and onDestroyView

    // Use activityViewModels as the ViewModel is likely shared with HomeActivity for data updates
    private val broadGroupViewModel: BroadGroupViewModel by activityViewModels()

    private lateinit var chatListAdapter: ChatListAdapter

    companion object {
        private const val TAG = "BroadGroupFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBroadGroupBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView called")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        setupRecyclerView()
        observeViewModel()

        // Example: Setup for a FloatingActionButton to create a new group (if you have one)
        // binding.fabNewGroup.setOnClickListener {
        //     try {
        //         // Ensure this action ID exists in your navigation graph
        //         val action = BroadGroupFragmentDirections.actionBroadGroupFragmentToCreateNewGroupFragment()
        //         findNavController().navigate(action)
        //     } catch (e: IllegalStateException) {
        //         Log.e(TAG, "Navigation failed: NavController not found for FAB.", e)
        //         Toast.makeText(context, "Could not navigate. Please try again.", Toast.LENGTH_SHORT).show()
        //     } catch (e: IllegalArgumentException) {
        //          Log.e(TAG, "Navigation failed: Action/Destination not found for FAB.", e)
        //          Toast.makeText(context, "Navigation target not found.", Toast.LENGTH_SHORT).show()
        //     }
        // }
    }

    /**
     * Called from HomeActivity to indicate a group should be marked as read in the list
     * (e.g., if the chat was opened directly via a notification and this fragment is visible).
     */
    fun markGroupAsReadInList(groupId: String) {
        Log.d(TAG, "Fragment: Attempting to mark group $groupId as read via ViewModel (from HomeActivity).")
        // The ViewModel will update its LiveData, and the observer will refresh the list.
        broadGroupViewModel.markGroupAsRead(groupId)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ChatNavigationListener) {
            navigationListener = context
            Log.d(TAG, "Attached ChatNavigationListener from Activity.")
        } else {
            Log.e(TAG, "$context must implement ChatNavigationListener")

            throw ClassCastException("$context must implement ChatNavigationListener")
                       // throw RuntimeException("$context must implement ChatNavigationListener") // Or handle more gracefully
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigationListener = null
        Log.d(TAG, "Detached ChatNavigationListener.")
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Fragment: setupRecyclerView called")
        // Initialize the adapter, passing 'this' fragment as the click listener
        chatListAdapter = ChatListAdapter(this) // 'this' implements OnChatClickListener

        binding.recyclerViewBroadGroups.apply {
            adapter = chatListAdapter
            layoutManager = LinearLayoutManager(context)
            // Optional: Add ItemDecoration for dividers
            // addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            setHasFixedSize(true) // Optimization if item sizes don't change
        }
        Log.d(TAG, "Fragment: RecyclerView and Adapter setup complete.")
    }

    private fun observeViewModel() {
        Log.d(TAG, "Fragment: Setting up ViewModel observer.")
        broadGroupViewModel.chatList.observe(viewLifecycleOwner) { chatListItems ->
            Log.d(TAG, "Fragment: chatList LiveData updated. Item count: ${chatListItems.size}")
            if (chatListItems.isNotEmpty()) {
                val first = chatListItems[0]
                Log.d(
                    TAG,
                    "Fragment: First item example: Group='${first.groupName}', LastMsg='${
                        first.lastMessageText?.take(
                            30
                        )
                    }...', Unread='${first.unreadCount}', TS='${first.lastMessageTimestamp}'"
                )
            }

            // Submit a new copy of the list for DiffUtil to work correctly.
            chatListAdapter.submitList(chatListItems.toList()) {
                // Callback after list is submitted and diffing is done.
                // You could scroll to a specific position here if needed.
                Log.d(
                    TAG,
                    "Fragment: submitList completed. Current list size in adapter: ${chatListAdapter.itemCount}"
                )
            }

            // Show/hide empty state view
            if (chatListItems.isEmpty()) {
                binding.recyclerViewBroadGroups.visibility = View.GONE
                // Ensure you have this ID in your fragment_broad_group.xml
                binding.textViewNoChatsPlaceholder.visibility = View.VISIBLE
                Log.d(TAG, "Fragment: Displaying empty chat list message.")
            } else {
                binding.recyclerViewBroadGroups.visibility = View.VISIBLE
                binding.textViewNoChatsPlaceholder.visibility = View.GONE
                Log.d(TAG, "Fragment: Displaying chat list.")
            }
        }

        // Optional: Observe loading state from ViewModel
        // broadGroupViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
        //     binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // }
    }

    // Implementation of OnChatClickListener from ChatListAdapter
    override fun onChatClicked(chatListItem: ChatListItem) {
        Log.d(
            TAG,
            "Fragment: Chat item clicked: GroupID='${chatListItem.groupId}', Name='${chatListItem.groupName}'"
        )

        // 1. Mark the group as read in the ViewModel.
        // The ViewModel updates LiveData, and the observer refreshes the list UI.
        broadGroupViewModel.markGroupAsRead(chatListItem.groupId)

        // Communicate to Activity to handle navigation
        navigationListener?.onNavigateToChatRoom(chatListItem.groupId, chatListItem.groupName)
        Log.d(TAG, "Navigation request sent to listener for group: ${chatListItem.groupName}")



    }



    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewBroadGroups.adapter = null // Important to prevent memory leaks with RecyclerView
        _binding = null
        Log.d(TAG, "onDestroyView called, binding set to null")
    }
}