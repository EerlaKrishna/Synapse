package com.example.synapse.chats

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope // Keep this if other methods use it
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.launch // Keep this if other methods use it



class BroadGroupViewModel(application: Application) : AndroidViewModel(application) {

    // This LiveData will be populated by the ValueEventListener for real-time updates
    // with denormalized last message info. This is what your Fragment should observe
    // for the main list display.
    private val _groups = MutableLiveData<List<Group>>()
    val groups: LiveData<List<Group>> = _groups

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val database = Firebase.database.reference
    // IMPORTANT: Use the correct path to your list of channels/groups in Firebase
    private val channelsRef = database.child("channels") // e.g., "channels" or "groups"
    private var channelsValueEventListener: ValueEventListener? = null

    // --- Existing ChatListItem related properties and functions ---
    // These manage a separate list (_chatList) with detailed unread count logic.
    // We will need to decide how/if to merge this with the `_groups` LiveData.
    private val _chatList = MutableLiveData<List<ChatListItem>>() // Your existing LiveData for ChatListItems
    val chatList: LiveData<List<ChatListItem>> = _chatList
    private val currentChatItemsMap = mutableMapOf<String, ChatListItem>() // Internal map for ChatListItems
    private val firebaseAuth = FirebaseAuth.getInstance()
    // --- End of ChatListItem related properties ---

    companion object {
        private const val TAG = "BroadGroupViewModel"
    }

    init {
        attachChannelsListener() // Start listening for real-time group updates for the main list
        // If you still need to initialize `currentChatItemsMap` for other purposes,
        // you might call a method here or it might be populated by other events.
    }

    // In BroadGroupViewModel.kt

    // In BroadGroupViewModel.kt

    private fun attachChannelsListener() {
        channelsValueEventListener = channelsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "VM: attachChannelsListener - onDataChange triggered. Snapshot value: ${snapshot.value}")
                val newGroups = mutableListOf<Group>()
                var errorOccurredInLoop = false

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    Log.d(TAG, "VM: No channels data found or snapshot is empty.")
                    _groups.postValue(emptyList()) // Post empty list if no data
                    return
                }

                snapshot.children.forEach { groupSnapshot ->
                    val groupId = groupSnapshot.key
                    Log.d(TAG, "VM: Processing groupSnapshot for ID: $groupId. Value: ${groupSnapshot.value}")

                    if (groupId == null) {
                        Log.e(TAG, "VM: groupSnapshot key (groupId) is null. Skipping.")
                        return@forEach // continue to next iteration
                    }

                    // Attempt to automatically map the entire groupSnapshot to the Group data class
                    // This relies on your Group and LastMessage data classes matching Firebase structure.
                    val group = groupSnapshot.getValue(Group::class.java)

                    if (group == null) {
                        Log.e(TAG, "VM: Failed to parse Group object for ID: $groupId. Snapshot value: ${groupSnapshot.value}. Check Group data class structure and Firebase data.")
                        errorOccurredInLoop = true
                        return@forEach // continue to next iteration
                    }

                    group.id = groupId // Ensure ID is set from the key, as getValue() won't populate it from the key

                    // Log the automatically parsed group, including the nested lastMessage
                    // The Group's toString() method (default for data classes) will show its properties.
                    Log.d(TAG, "VM: Successfully parsed Group for ID: $groupId. Group data: $group")
                    Log.d(TAG, "VM: Parsed LastMessage for group $groupId: ${group.lastMessage}")


                    // --- Unread Count Logic ---
                    // This part remains the same, assuming currentChatItemsMap is still how you
                    // track unread counts separately or augment the Group object.
                    val chatListItem = currentChatItemsMap[groupId] // Assuming currentChatItemsMap is up-to-date
                    group.unreadCount = chatListItem?.unreadCount ?: 0
                    Log.d(TAG, "VM: Group ID: $groupId, Unread count set to: ${group.unreadCount}")
                    // --- End Unread Count Logic ---

                    newGroups.add(group)
                }

                if (errorOccurredInLoop) {
                    // You might want to decide if you post partial data or an error state.
                    // For now, it posts whatever was successfully parsed.
                    _error.postValue("Error parsing some group data. Check logs for details.")
                }

                // Sort by the timestamp within the nested lastMessage object.
                // Handle cases where lastMessage or its timestamp might be null.
                val sortedGroups = newGroups.sortedByDescending { it.lastMessage?.timestamp ?: 0L }

                Log.d(TAG, "VM: Posting ${sortedGroups.size} groups to _groups LiveData. First group in sorted list (if any): ${sortedGroups.firstOrNull()}")
                _groups.postValue(sortedGroups)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "VM: Firebase listener for channels cancelled: ${error.message}")
                _error.postValue("Failed to load group data: ${error.message}")
            }
        })
    }


    // --- Your existing ChatListItem management methods ---
    // These methods (initializeOrUpdateGroups, updateGroupWithMessage, markGroupAsRead, publishChatListUpdates, clearAllData)
    // will continue to update `currentChatItemsMap` and `_chatList`.
    // The `attachChannelsListener` will now also try to pick up `unreadCount` from `currentChatItemsMap`.
    // For this to be perfectly in sync, `publishChatListUpdates` might also need to trigger a re-evaluation
    // or re-fetch for `_groups` or you ensure that any change in `currentChatItemsMap` that affects unread count
    // also triggers a new emission of `_groups` if `attachChannelsListener` doesn't pick it up immediately.

    fun initializeOrUpdateGroups(groupsData: List<GroupData>) {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM: initializeOrUpdateGroups called with ${groupsData.size} groups. CurrentUser: $currentUserId")
        var listChanged = false
        val incomingGroupIds = groupsData.map { it.id }.toSet()
        val currentGroupIdsInMap = currentChatItemsMap.keys.toMutableSet()

        // Add new groups or update existing ones in currentChatItemsMap
        groupsData.forEach { groupData ->
            val existingItem = currentChatItemsMap[groupData.id]
            if (existingItem == null) {
                val newItem = ChatListItem(
                    groupId = groupData.id,
                    groupName = groupData.name
                    // lastMessageText, lastMessageTimestamp, etc., will default or be updated by updateGroupWithMessage
                )
                currentChatItemsMap[groupData.id] = newItem
                listChanged = true
                Log.d(TAG, "VM: Initialized new ChatListItem in ViewModel: ID='${groupData.id}', Name='${groupData.name}'")
            } else {
                // Update name if it changed
                if (existingItem.groupName != groupData.name) {
                    existingItem.groupName = groupData.name
                    listChanged = true
                    Log.d(TAG, "VM: Updated ChatListItem name for ID='${groupData.id}' to '${groupData.name}'")
                }
            }
        }

        // Remove groups from currentChatItemsMap that are no longer in the fetched list
        val groupsToRemove = currentGroupIdsInMap - incomingGroupIds // Groups in map but not in new list
        if (groupsToRemove.isNotEmpty()) {
            groupsToRemove.forEach { groupIdToRemove ->
                currentChatItemsMap.remove(groupIdToRemove)
                Log.d(TAG, "VM: Removed ChatListItem from ViewModel: ID='$groupIdToRemove'")
            }
            listChanged = true
        }

        if (listChanged) {
            Log.d(TAG, "VM: List changed during group initialization/update. Publishing ChatListItem updates.")
            publishChatListUpdates() // This updates _chatList

            // **Important for UI consistency with _groups LiveData:**
            // If `_groups` is the primary source for your RecyclerView, and unread counts
            // from `currentChatItemsMap` are important for that display, you might need
            // to trigger a re-evaluation of `_groups` here or ensure `attachChannelsListener`
            // picks up the changes.
            // A simple way to force `_groups` to re-evaluate with potentially updated unread counts
            // from `currentChatItemsMap` is to re-post its current value, or re-fetch if necessary.
            // However, the current `attachChannelsListener` already merges unread counts.
            // If `publishChatListUpdates` changes unread counts, the next time `attachChannelsListener`
            // fires (due to a Firebase data change), it will pick up the new unread counts.
            // For immediate reflection, you might consider:
            // 1. Having `publishChatListUpdates` also trigger a refresh of `_groups` data.
            // 2. Modifying `attachChannelsListener` to be callable on demand to refresh `_groups`.
            // For now, the unread count in `_groups` will update when Firebase data changes.
        } else {
            Log.d(TAG, "VM: No ChatListItem changes during group initialization/update required publishing.")
        }
    }

    fun updateGroupWithMessage(
        groupId: String,
        groupName: String?, // Optional: if the message source can also update group name
        messageText: String?,
        messageTimestamp: Long,
        senderName: String?,
        senderId: String?,
        isNewUnreadOverride: Boolean? = null // For special cases like push notifications
    ) {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM updateGroupWithMessage: START - GroupID='${groupId}', Msg='${messageText?.take(30)}...', SenderID='${senderId}', TS='${messageTimestamp}', CurrentUser: $currentUserId")

        var itemChangedOverall = false
        val item = currentChatItemsMap[groupId]

        if (item == null) {
            Log.d(TAG, "VM: Group '$groupId' not found in map for ChatListItem. Creating new.")
            val newUnreadCount = if (senderId != currentUserId && messageText != null) 1 else 0
            val newItem = ChatListItem(
                groupId = groupId,
                groupName = groupName ?: "Group $groupId", // Fallback name
                lastMessageText = messageText,
                lastMessageTimestamp = messageTimestamp,
                lastMessageSenderName = senderName,
                unreadCount = newUnreadCount,
                lastMessageTimestampProcessedForUnread = if (newUnreadCount > 0) messageTimestamp else 0L
            )
            currentChatItemsMap[groupId] = newItem
            itemChangedOverall = true
            Log.d(TAG, "VM: New ChatListItem for '$groupId' added via message. Unread: $newUnreadCount.")
        } else {
            // Existing item, check for updates
            if (messageTimestamp > item.lastMessageTimestamp) {
                item.lastMessageText = messageText ?: "Attachment" // Handle null message text
                item.lastMessageTimestamp = messageTimestamp
                item.lastMessageSenderName = senderName
                itemChangedOverall = true
                Log.d(TAG, "VM: Updated last message for ChatListItem '$groupId'. New TS: $messageTimestamp.")
            }

            if (groupName != null && item.groupName != groupName) {
                item.groupName = groupName
                itemChangedOverall = true
                Log.d(TAG, "VM: Updated group name for ChatListItem '$groupId' to '$groupName'.")
            }

            // --- Unread Count Logic for ChatListItem ---
            val shouldIncrementUnreadLogReason = StringBuilder()
            var shouldActuallyIncrement = false

            if (isNewUnreadOverride == true) {
                shouldActuallyIncrement = true
                shouldIncrementUnreadLogReason.append("Forced by isNewUnreadOverride. ")
            } else {
                if (senderId != currentUserId) {
                    if (messageText != null) { // Only count meaningful messages
                        if (messageTimestamp > item.lastMessageTimestampProcessedForUnread && messageTimestamp == item.lastMessageTimestamp) {
                            shouldActuallyIncrement = true
                            shouldIncrementUnreadLogReason.append("Valid new message for unread. ")
                        } else {
                            if (messageTimestamp <= item.lastMessageTimestampProcessedForUnread) {
                                shouldIncrementUnreadLogReason.append("Msg TS ($messageTimestamp) not > last processed TS for unread (${item.lastMessageTimestampProcessedForUnread}). ")
                            }
                            if (messageTimestamp != item.lastMessageTimestamp && senderId != currentUserId && messageText != null) {
                                shouldIncrementUnreadLogReason.append("Msg TS ($messageTimestamp) is not the current last msg TS (${item.lastMessageTimestamp}) that would trigger unread. ")
                            }
                        }
                    } else {
                        shouldIncrementUnreadLogReason.append("Msg text is null. ")
                    }
                } else {
                    shouldIncrementUnreadLogReason.append("Msg from current user ('$senderId'). ")
                }
            }

            if (shouldActuallyIncrement) {
                item.unreadCount++
                item.lastMessageTimestampProcessedForUnread = messageTimestamp
                itemChangedOverall = true
                Log.d(TAG, "VM: Incremented unread for ChatListItem '$groupId'. New count: ${item.unreadCount}. Reason: ${shouldIncrementUnreadLogReason.toString().trim()}")
            } else {
                if (isNewUnreadOverride != true && shouldIncrementUnreadLogReason.isNotBlank() && shouldIncrementUnreadLogReason.toString() != "Msg from current user ('$senderId'). ") {
                    Log.d(TAG, "VM: Did NOT increment unread for ChatListItem '$groupId'. Reason: ${shouldIncrementUnreadLogReason.toString().trim()}")
                } else if (isNewUnreadOverride != true && senderId == currentUserId) {
                    Log.d(TAG, "VM: Did NOT increment unread for ChatListItem '$groupId' because sender is current user.")
                }
            }
            // --- End Unread Count Logic ---
        }

        if (itemChangedOverall) {
            Log.d(TAG, "VM: Overall ChatListItem changed for '$groupId'. Publishing ChatListItem updates.")
            publishChatListUpdates() // This updates _chatList

            // **Important for UI consistency with _groups LiveData:**
            // If the unread count in `currentChatItemsMap` was modified, and your UI
            // (observing `_groups`) needs to reflect this change immediately without waiting
            // for the next Firebase data event, you might need to trigger a manual refresh
            // of the `_groups` LiveData.
            // One way is to make `attachChannelsListener` re-fetch or re-process.
            // A simpler, though less efficient way if done too often, is to re-post the current
            // list to `_groups` after updating its items with new unread counts.
            // Example (if you need immediate unread count updates in the `_groups` list):
            // val currentGroups = _groups.value?.map { group ->
            //     currentChatItemsMap[group.id]?.let { chatListItem ->
            //         group.copy(unreadCount = chatListItem.unreadCount)
            //     } ?: group
            // }
            // _groups.postValue(currentGroups)
            // However, the current setup in `attachChannelsListener` will pick up the unread count
            // from `currentChatItemsMap` whenever it processes data. This might be sufficient.

        } else {
            Log.d(TAG, "VM: No overall ChatListItem changes detected for '$groupId' that require publishing.")
        }
        Log.d(TAG, "VM updateGroupWithMessage: END - GroupID='${groupId}'")
    }

    fun markGroupAsRead(groupId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM: markGroupAsRead called for ChatListItem GroupID='${groupId}'. CurrentUser: $currentUserId")
        currentChatItemsMap[groupId]?.let { item ->
            if (item.unreadCount > 0) {
                item.unreadCount = 0
                item.lastMessageTimestampProcessedForUnread = item.lastMessageTimestamp // Important for future unread logic
                Log.d(TAG, "VM: Marked ChatListItem '$groupId' as read. Unread set to 0. LastProcessedTS set to ${item.lastMessageTimestamp}. Publishing ChatListItem updates.")
                publishChatListUpdates() // This updates _chatList

                // See comment in updateGroupWithMessage about potentially refreshing _groups
                // if immediate reflection of unreadCount = 0 is needed in the UI observing `_groups`.
            } else {
                Log.d(TAG, "VM: ChatListItem '$groupId' already has 0 unread messages. No changes made.")
            }
        } ?: Log.d(TAG, "VM: markGroupAsRead: ChatListItem GroupID='$groupId' not found in map.")
    }

    // This method is specific to the ChatListItem system.
    fun getChatListItem(groupId: String): ChatListItem? {
        return currentChatItemsMap[groupId]
    }

    // In BroadGroupViewModel.kt

    private fun publishChatListUpdates() {
        // Sorts and posts updates to _chatList (the LiveData for ChatListItems)
        // _chatList is presumably a List<ChatListItem>, and ChatListItem still has direct lastMessageTimestamp
        val sortedChatListItems = currentChatItemsMap.values.toList().sortedByDescending { it.lastMessageTimestamp }
        Log.d(TAG, "VM: Publishing _chatList updates. Count: ${sortedChatListItems.size}. Items: ${sortedChatListItems.joinToString { it.groupId + " (Unread:" + it.unreadCount + ", TS:" + it.lastMessageTimestamp + ")" }}")
        _chatList.postValue(sortedChatListItems)

        // If _groups is the primary display list and needs to reflect unread count changes from
        // currentChatItemsMap immediately, you might trigger a refresh here.
        // The `attachChannelsListener` (which you've updated) will now get the lastMessage details
        // directly from Firebase, including its timestamp.
        // This section is primarily for syncing unreadCount from currentChatItemsMap to the _groups LiveData.

        _groups.value?.let { currentGroupList ->
            val updatedGroupList = currentGroupList.map { group ->
                currentChatItemsMap[group.id]?.let { chatListItem ->
                    // Create a new Group object with the updated unreadCount.
                    // The lastMessage object within the group remains as it was from Firebase.
                    group.copy(unreadCount = chatListItem.unreadCount)
                } ?: group // If no corresponding ChatListItem, keep the group as is.
            }
                // Re-sort the _groups list based on its lastMessage.timestamp,
                // as the unread count change doesn't affect sorting order here,
                // but it's good practice if other modifications could.
                // If attachChannelsListener is the sole source of truth for lastMessage details,
                // this re-sort might even use the existing group.lastMessage.timestamp.
                .sortedByDescending { it.lastMessage?.timestamp ?: 0L } // MODIFIED HERE

            _groups.postValue(updatedGroupList)
            Log.d(TAG, "VM: Refreshed _groups with updated unread counts from currentChatItemsMap and re-sorted.")
        }
    }

    fun clearAllData() {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM: clearAllData called. CurrentUser: $currentUserId")
        currentChatItemsMap.clear()
        _chatList.postValue(emptyList())
        // _groups will be cleared by the listener if the Firebase node becomes empty or on next update.
        // If you want to clear it immediately:
        _groups.postValue(emptyList())
        Log.d(TAG, "VM: Cleared all data from BroadGroupViewModel (ChatListItems and Groups).")
    }

    override fun onCleared() {
        super.onCleared()
        // Remove Firebase listener to prevent memory leaks
        channelsValueEventListener?.let {
            channelsRef.removeEventListener(it)
        }
        Log.d(TAG, "BroadGroupViewModel cleared and Firebase listener removed.")
    }
}