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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.chats.BroadGroupViewModel
import com.example.synapse.chats.CreateGroupDialogFragment
import com.example.synapse.chats.Group
import com.example.synapse.chats.GroupListAdapter // Your adapter
import com.example.synapse.databinding.FragmentBroadGroupBinding

interface ChatNavigationListener {
    fun onNavigateToChatRoom(groupId: String, groupName: String?)
}

class BroadGroupFragment : Fragment() {

    private var navigationListener: ChatNavigationListener? = null
    private var _binding: FragmentBroadGroupBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private var allGroups: List<Group> = emptyList()

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
        observeViewModels()
        setupFab()
    }
    private fun setupFab() {
        binding.fabAddNewGroup.setOnClickListener {
            Log.d(TAG, "FAB clicked to add new group.")
            // Show the CreateGroupDialogFragment
            CreateGroupDialogFragment().show(parentFragmentManager, "CreateGroupDialog")
        }
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

    private fun observeViewModels() {
        Log.d(TAG, "Fragment: Setting up ViewModel observers.")

        // --- Observe Admin Status from BroadGroupViewModel ---
        broadGroupViewModel.isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            Log.d(TAG, "Admin status observed: $isAdmin")
            binding.fabAddNewGroup.visibility = if (isAdmin) View.VISIBLE else View.GONE
            if (isAdmin) {
                Log.d(TAG, "User is Admin. Showing Create Group FAB.")
            } else {
                Log.d(TAG, "User is NOT Admin. Hiding Create Group FAB.")
            }
        }
        // --- End Observe Admin Status ---

        // --- Observe isLoading status from BroadGroupViewModel ---
        broadGroupViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Fragment: isLoading state observed: $isLoading")
            if (isLoading) {
                binding.progressBarBroadGroups.visibility = View.VISIBLE
                // Optionally hide RecyclerView and placeholder while loading initial data
                // binding.recyclerViewBroadGroups.visibility = View.GONE
                // binding.textViewNoChatsPlaceholder.visibility = View.GONE
            } else {
                binding.progressBarBroadGroups.visibility = View.GONE
                // Visibility of RecyclerView and placeholder will be handled by filterAndDisplayGroups
                // after data is loaded. If allGroups is empty after loading,
                // filterAndDisplayGroups will show the placeholder.
            }
        }
        // --- End Observe isLoading Status ---

        // Observe the full list of groups from BroadGroupViewModel
        broadGroupViewModel.groups.observe(viewLifecycleOwner) { groups ->
            Log.d(TAG, "Fragment: 'groups' LiveData updated in Fragment. Original item count: ${groups?.size ?: 0}")
            allGroups = groups ?: emptyList() // Store the full list

            // Log first few items from the original list for verification
            if (allGroups.isNotEmpty()) {
                allGroups.take(3).forEachIndexed { index, group ->
                    Log.d(
                        TAG,
                        "Fragment: Original Group $index: ID='${group.id}', Name='${group.name}', " +
                                "LastMsg='${group.lastMessage?.text?.take(30)}...', " +
                                "Unread='${group.unreadCount}', " +
                                "UnreadFromOthers='${group.hasUnreadMessagesFromOthers}'" // <-- ADD THIS
                    )
                }
            }

            // Apply current search query to the new full list
            val currentQuery = homeViewModel.searchQuery.value
            Log.d(TAG, "Full group list updated. Applying current search query: '$currentQuery'")
            filterAndDisplayGroups(currentQuery)
        }

        // Observe the search query from HomeViewModel
        homeViewModel.searchQuery.observe(viewLifecycleOwner, Observer { query ->
            Log.d(TAG, "Fragment: Search query LiveData updated to: '$query'")
            // Filter the existing 'allGroups' list with the new query
            filterAndDisplayGroups(query)
        })

        // Error observation (remains the same)
        broadGroupViewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            errorMessage?.let {
                Log.e(TAG, "ViewModel error: $it")
                Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            }
        })

        // Group creation event observation
        broadGroupViewModel.groupCreatedEvent.observe(viewLifecycleOwner, Observer { eventData ->
            // This code inside the lambda runs WHEN the event occurs
            eventData?.let { data -> // Use ?.let for safety if eventData can be null initially
                val groupId = data.first    // Assuming eventData is Pair<String, String>
                val groupName = data.second
                Log.i(TAG, "Observed groupCreatedEvent for group: $groupName (ID: $groupId)")
                Toast.makeText(context, "Group '$groupName' created successfully!", Toast.LENGTH_SHORT).show()

                // CORRECT PLACE: Call this *after* you've handled the event (shown the Toast)
                // and *inside* the observer's lambda.
                broadGroupViewModel.onGroupCreatedEventHandled()
            }
        })
        Log.d(TAG, "ViewModel observers set up.")
        // DO NOT call onGroupCreatedEventHandled() here anymore.
    }


    private fun filterAndDisplayGroups(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            Log.d(TAG, "Query is blank, displaying all ${allGroups.size} groups.")
            allGroups // If query is empty, show all groups
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            Log.d(TAG, "Filtering groups with query: '$lowerCaseQuery'")
            allGroups.filter { group ->
                // Adjust your filtering logic as needed:
                // Search in group name (case-insensitive)
                (group.name?.lowercase()?.contains(lowerCaseQuery) == true)
                // Example: also search in last message text (if available and desired)
                // || (group.lastMessage?.text?.lowercase()?.contains(lowerCaseQuery) == true)
                // Example: also search in another field if your Group object has it
                // || (group.description?.lowercase()?.contains(lowerCaseQuery) == true)
            }
        }

        Log.d(TAG, "Submitting list to adapter. Filtered count: ${filteredList.size}. Query: '$query'")
        if (filteredList.isNotEmpty()) {
            filteredList.take(3).forEachIndexed { index, group -> // Log first 3 filtered items
                Log.d(TAG, "Fragment: Filtered Group $index to display: ID='${group.id}', Name='${group.name}', UnreadFromOthers='${group.hasUnreadMessagesFromOthers}'")             }
        } else {
            Log.d(TAG, "Fragment: Filtered list is empty.")
        }

        // Submit the filtered list to the adapter.
        // Using toList() creates a new list instance, which is good for DiffUtil if your adapter uses it.
        groupListAdapter.submitList(filteredList.toList()) {
            // This callback is executed after the list diffing and updates are complete.
            // Useful for logging or triggering animations.
            Log.d(TAG, "Fragment: submitList for filtered data completed. Adapter item count: ${groupListAdapter.itemCount}")

            // Optional: Scroll to the top of the list when a new search is performed
            // and results are found. This can be a good user experience.
            // if (filteredList.isNotEmpty() && !query.isNullOrBlank()) {
            //    binding.recyclerViewBroadGroups.scrollToPosition(0)
            // }
        }

        // Update placeholder visibility based on whether the filtered list is empty.
        if (filteredList.isEmpty()) {
            binding.recyclerViewBroadGroups.visibility = View.GONE
            binding.textViewNoChatsPlaceholder.visibility = View.VISIBLE
            // Set appropriate text for the placeholder based on whether there was a search query.
            binding.textViewNoChatsPlaceholder.text = if (query.isNullOrBlank()) {
                // Ensure you have these string resources defined in your strings.xml file
                getString(R.string.no_groups_placeholder) // e.g., "No groups yet. Create one!"
            } else {
                getString(R.string.no_search_results_placeholder) // e.g., "No groups found for your search."
            }
            Log.d(TAG, "Fragment: Displaying placeholder: '${binding.textViewNoChatsPlaceholder.text}'")
        } else {
            binding.recyclerViewBroadGroups.visibility = View.VISIBLE
            binding.textViewNoChatsPlaceholder.visibility = View.GONE
            Log.d(TAG, "Fragment: Displaying filtered group list.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Important to prevent memory leaks with RecyclerView adapters
        binding.recyclerViewBroadGroups.adapter = null
        _binding = null // Crucial for view binding in Fragments
        Log.d(TAG, "onDestroyView called, binding and adapter set to null")
    }
}