package com.example.synapse

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.adapter.DirectMessageAdapter // Your adapter
import com.example.synapse.databinding.FragmentDirectMessageBinding
// Assuming Conversation is in the same package or imported correctly
// import com.example.synapse.model.Conversation

class DirectMessageFragment : Fragment() {

    private var _binding: FragmentDirectMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var directMessageAdapter: DirectMessageAdapter
    private val allConversationsList = mutableListOf<Conversation>() // Master list

    private var currentSearchQuery: String? = null
    private var searchView: SearchView? = null

    private val SEARCH_QUERY_KEY = "directMessageSearchQueryKey"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectMessageBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            currentSearchQuery = savedInstanceState.getString(SEARCH_QUERY_KEY)
        }

        setupRecyclerView()
        setupMenu()

        binding.fabNewDirectMessage.setOnClickListener {
            Log.d("DirectMessageFragment", "FAB Clicked!")
            Toast.makeText(context, "New DM: Implement action.", Toast.LENGTH_LONG).show()
            // TODO: Navigation to start a new DM
        }

        showLoadingState()
        loadDirectMessagesFromDataSource()

        return view
    }

    private fun setupRecyclerView() {
        binding.recyclerViewDirectMessages.layoutManager = LinearLayoutManager(context)
        // Your adapter takes the list in constructor, but we'll update it via updateData
        directMessageAdapter = DirectMessageAdapter(emptyList()) { conversation ->
            Log.d("DirectMessageFragment", "Clicked on conversation: ${conversation.id}")
            Toast.makeText(requireContext(), "Clicked: ${conversation.participantsDisplayNames.joinToString()}", Toast.LENGTH_SHORT).show()
            // TODO: Handle conversation click (navigation, mark as read, etc.)
        }
        binding.recyclerViewDirectMessages.adapter = directMessageAdapter
        Log.d("DirectMessageFragment", "RecyclerView setup complete.")
    }

// END OF PART 1 OF 3



// START OF PART 2 OF 3 (Continuing DirectMessageFragment class)

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val searchItem = menu.findItem(R.id.action_search_dms)
                searchView = searchItem?.actionView as? SearchView

                searchView?.queryHint = "Search conversations..."
                searchView?.isSubmitButtonEnabled = false

                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        searchView?.clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        currentSearchQuery = newText?.trim()
                        applyFilterToConversations()
                        return true
                    }
                })

                if (!currentSearchQuery.isNullOrEmpty()) {
                    searchItem?.expandActionView()
                    searchView?.setQuery(currentSearchQuery, false)
                    // searchView?.clearFocus() // Optional: remove focus after restoring
                } else {
                    searchItem?.collapseActionView()
                }

                searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        currentSearchQuery = null
                        applyFilterToConversations()
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_search_dms -> true
                    else -> false
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                val searchItem = menu.findItem(R.id.action_search_dms)
                searchItem?.isVisible = allConversationsList.isNotEmpty()
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showLoadingState() {
        binding.progressBarDirectMessages.visibility = View.VISIBLE
        binding.recyclerViewDirectMessages.visibility = View.GONE
        binding.textViewNoDirectMessages.visibility = View.GONE
    }

    private fun showEmptyState(message: String) {
        binding.progressBarDirectMessages.visibility = View.GONE
        binding.recyclerViewDirectMessages.visibility = View.GONE
        binding.textViewNoDirectMessages.visibility = View.VISIBLE
        binding.textViewNoDirectMessages.text = message
    }

    private fun showDataState(filteredList: List<Conversation>) {
        binding.progressBarDirectMessages.visibility = View.GONE
        binding.recyclerViewDirectMessages.visibility = View.VISIBLE
        binding.textViewNoDirectMessages.visibility = View.GONE
        directMessageAdapter.updateData(filteredList) // Uses your adapter's method
    }

// END OF PART 2 OF 3


// START OF PART 3 OF 3 (Continuing DirectMessageFragment class)

    private fun loadDirectMessagesFromDataSource() {
        Log.d("DirectMessageFragment", "loadDirectMessagesFromDataSource called")
        // showLoadingState() // Already called before this

        // Simulate network delay or database query
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("DirectMessageFragment", "Simulated data fetch finished.")
            val fetchedConversations = mutableListOf<Conversation>()
            // TODO: Replace with your actual data fetching logic (e.g., from Firebase, local DB)
            // Example dummy data:
            fetchedConversations.add(Conversation(id="conv1", participantsDisplayNames = listOf("Alice", "You"), lastMessageSnippet = "Hey, how are you doing today?", timestamp = System.currentTimeMillis() - 120000, isUnread = true))
            fetchedConversations.add(Conversation(id="conv2", participantsDisplayNames = listOf("Bob Johnson"), lastMessageSnippet = "Sounds good, see you tomorrow then!", timestamp = System.currentTimeMillis() - 240000))
            fetchedConversations.add(Conversation(id="conv3", participantsDisplayNames = listOf("Charlie Brown", "Lucy"), lastMessageSnippet = "Meeting at 3 PM. Don't be late!", timestamp = System.currentTimeMillis() - 360000, isUnread = true))
            fetchedConversations.add(Conversation(id="conv4", participantsDisplayNames = listOf("Diana Prince"), lastMessageSnippet = "Can you send me the report?", timestamp = System.currentTimeMillis() - 480000))
            fetchedConversations.add(Conversation(id="conv5", participantsDisplayNames = listOf("Edward Nigma"), lastMessageSnippet = "Riddle me this...", timestamp = System.currentTimeMillis() - 600000, isUnread = false))


            allConversationsList.clear()
            allConversationsList.addAll(fetchedConversations.sortedByDescending { it.timestamp })

            applyFilterToConversations() // This will also update UI based on initial load
            activity?.invalidateOptionsMenu() // To update menu (e.g., search icon visibility)

        }, 1500) // 1.5-second delay
    }

    private fun applyFilterToConversations() {
        val filteredList: List<Conversation>
        val query = currentSearchQuery

        if (query.isNullOrEmpty()) {
            filteredList = ArrayList(allConversationsList) // Create a new list for the adapter
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            filteredList = allConversationsList.filter { conversation ->
                val matchesParticipants = conversation.participantsDisplayNames.any { name ->
                    name.lowercase().contains(lowerCaseQuery)
                }
                val matchesMessage = conversation.lastMessageSnippet.lowercase().contains(lowerCaseQuery)
                matchesParticipants || matchesMessage
            }
        }

        if (allConversationsList.isEmpty()) {
            showEmptyState("No direct messages yet.")
        } else if (filteredList.isEmpty() && !query.isNullOrEmpty()) {
            showEmptyState("No results found for \"$query\"")
        } else if (filteredList.isEmpty() && query.isNullOrEmpty() && allConversationsList.isNotEmpty()){
            // This case means allConversationsList has items, but somehow filteredList is empty with no query.
            // This shouldn't typically happen if logic is correct, but as a fallback:
            showEmptyState("No messages to display.")
        }
        else {
            showDataState(filteredList)
        }
        Log.d("DirectMessageFragment", "Filtered. Query: '$query', Displayed: ${filteredList.size}, Total: ${allConversationsList.size}")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current search query, so it can be restored in onCreateView or onViewStateRestored
        if (currentSearchQuery != null) {
            outState.putString(SEARCH_QUERY_KEY, currentSearchQuery)
        }
    }

    override fun onDestroyView() {
        // It's important to clear the listener for the SearchView specifically from the
        // instance we captured in setupMenu, especially if the menu is activity-owned.
        searchView?.setOnQueryTextListener(null)
        searchView = null // Release the reference

        // Clear the binding reference to prevent memory leaks
        _binding = null
        Log.d("DirectMessageFragment", "onDestroyView called, binding and searchView listener cleared.")
        super.onDestroyView()
    }
} // End of DirectMessageFragment class

// END OF PART 3 OF 3