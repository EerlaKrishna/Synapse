package com.example.synapse

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.chats.BroadGroupViewModel
import com.example.synapse.chats.Group
import com.example.synapse.chats.GroupListAdapter // Your adapter
import com.example.synapse.databinding.FragmentBroadGroupBinding

// Remove the OnGroupClickListener interface if it was defined here for the fragment to implement
// interface OnGroupClickListener {
//    fun onGroupClicked(group: Group)
// }

// ChatNavigationListener should be defined (as it was before)
interface ChatNavigationListener {
    fun onNavigateToChatRoom(groupId: String, groupName: String?)
}

class BroadGroupFragment : Fragment() { // REMOVE ", OnGroupClickListener" if it was there

    private var navigationListener: ChatNavigationListener? = null
    private var _binding: FragmentBroadGroupBinding? = null
    private val binding get() = _binding!!

    private val broadGroupViewModel: BroadGroupViewModel by activityViewModels()
    private lateinit var groupListAdapter: GroupListAdapter

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
        setupRecyclerView() // Call setupRecyclerView
        observeViewModel()
    }

    fun markGroupAsReadInList(groupId: String) {
        Log.d(TAG, "Fragment: Attempting to mark group $groupId as read via ViewModel.")
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
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigationListener = null
        Log.d(TAG, "Detached ChatNavigationListener.")
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Fragment: setupRecyclerView called")
        // Initialize the adapter, passing a lambda for click handling
        groupListAdapter = GroupListAdapter { group -> // This is the lambda
            // This code block is the implementation of (Group) -> Unit
            Log.d(TAG, "Fragment: Group item clicked via lambda: GroupID='${group.id}', Name='${group.name}'")
            broadGroupViewModel.markGroupAsRead(group.id)
            navigationListener?.onNavigateToChatRoom(group.id, group.name)
            Log.d(TAG, "Navigation request sent to listener for group: ${group.name}")
        }

        binding.recyclerViewBroadGroups.apply {
            adapter = groupListAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
        Log.d(TAG, "Fragment: RecyclerView and Adapter setup complete.")
    }
// In BroadGroupFragment.kt

    private fun observeViewModel() {
        Log.d(TAG, "Fragment: Setting up ViewModel observer for 'groups'.")
        broadGroupViewModel.groups.observe(viewLifecycleOwner) { groups ->
            Log.d(TAG, "Fragment: 'groups' LiveData updated in Fragment. Item count: ${groups.size}")
            if (groups.isNotEmpty()) {
                groups.take(3).forEachIndexed { index, group ->
                    // --- MODIFIED LOGGING LINE START ---
                    Log.d(
                        TAG,
                        "Fragment: Group $index in list: ID='${group.id}', Name='${group.name}', " +
                                "LastMsg='${group.lastMessage?.text?.take(30)}...', " + // Access via group.lastMessage?.text
                                "Sender='${group.lastMessage?.senderName}', " +           // Access via group.lastMessage?.senderName
                                "Unread='${group.unreadCount}', " +
                                "TS='${group.lastMessage?.timestamp}'"                  // Access via group.lastMessage?.timestamp
                    )
                    // --- MODIFIED LOGGING LINE END ---
                }
            }

            // Check if the list submitted to the adapter actually contains the new data
            // --- MODIFIED LOGGING LINE FOR SUBMITLIST START ---
            Log.d(TAG, "Fragment: Submitting list to adapter. First item's last message TS (if any): ${groups.firstOrNull()?.lastMessage?.timestamp}")
            // --- MODIFIED LOGGING LINE FOR SUBMITLIST END ---
            groupListAdapter.submitList(groups.toList()) {
                Log.d(
                    TAG,
                    "Fragment: submitList completed. Current list size in adapter: ${groupListAdapter.itemCount}"
                )
            }

            if (groups.isEmpty()) {
                binding.recyclerViewBroadGroups.visibility = View.GONE
                binding.textViewNoChatsPlaceholder.visibility = View.VISIBLE
                Log.d(TAG, "Fragment: Displaying empty group list message.")
            } else {
                binding.recyclerViewBroadGroups.visibility = View.VISIBLE
                binding.textViewNoChatsPlaceholder.visibility = View.GONE
                Log.d(TAG, "Fragment: Displaying group list.")
            }
        }

        broadGroupViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e(TAG, "ViewModel error: $it")
                Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                // broadGroupViewModel.clearError() // If you implement this
            }
        }
    }

    // If BroadGroupFragment was implementing OnGroupClickListener,
    // this method is NO LONGER NEEDED because the lambda handles the click directly.
    // override fun onGroupClicked(group: Group) {
    //    Log.d(TAG, "Fragment: Group item clicked: GroupID='${group.id}', Name='${group.name}'")
    //    broadGroupViewModel.markGroupAsRead(group.id)
    //    navigationListener?.onNavigateToChatRoom(group.id, group.name)
    //    Log.d(TAG, "Navigation request sent to listener for group: ${group.name}")
    // }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewBroadGroups.adapter = null
        _binding = null
        Log.d(TAG, "onDestroyView called, binding set to null")
    }
}