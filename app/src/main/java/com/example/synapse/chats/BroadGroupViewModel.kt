package com.example.synapse.chats

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth

class BroadGroupViewModel(application: Application) : AndroidViewModel(application) {

    private val _chatList = MutableLiveData<List<ChatListItem>>()
    val chatList: LiveData<List<ChatListItem>> = _chatList

    private val currentChatItemsMap = mutableMapOf<String, ChatListItem>()
    private val firebaseAuth = FirebaseAuth.getInstance()
    // It's generally safer to get currentUser?.uid when needed, as auth state can change.
    // However, for frequent access within a method, caching it at the start of the method is fine.

    companion object {
        private const val TAG = "BroadGroupViewModel"
    }

    fun initializeOrUpdateGroups(groups: List<GroupData>) {
        val currentUserId = firebaseAuth.currentUser?.uid // Get fresh UID
        Log.d(TAG, "VM: initializeOrUpdateGroups called with ${groups.size} groups. CurrentUser: $currentUserId")
        var listChanged = false
        val incomingGroupIds = groups.map { it.id }.toSet()
        val currentGroupIdsInMap = currentChatItemsMap.keys.toMutableSet()

        // Add new groups or update existing ones
        groups.forEach { groupData ->
            val existingItem = currentChatItemsMap[groupData.id]
            if (existingItem == null) {
                val newItem = ChatListItem(
                    groupId = groupData.id,
                    groupName = groupData.name
                    // lastMessageText, lastMessageTimestamp, lastMessageSenderName, unreadCount
                    // and lastMessageTimestampProcessedForUnread will default to initial values
                    // as per ChatListItem data class definition.
                )
                currentChatItemsMap[groupData.id] = newItem
                listChanged = true
                Log.d(TAG, "VM: Initialized new group in ViewModel: ID='${groupData.id}', Name='${groupData.name}'")
            } else {
                // Update name if it changed
                if (existingItem.groupName != groupData.name) {
                    existingItem.groupName = groupData.name
                    listChanged = true
                    Log.d(TAG, "VM: Updated group name for ID='${groupData.id}' to '${groupData.name}'")
                }
            }
        }

        // Remove groups that are no longer in the fetched list
        val groupsToRemove = currentGroupIdsInMap - incomingGroupIds // Groups in map but not in new list
        if (groupsToRemove.isNotEmpty()) {
            groupsToRemove.forEach { groupIdToRemove ->
                currentChatItemsMap.remove(groupIdToRemove)
                Log.d(TAG, "VM: Removed group from ViewModel: ID='$groupIdToRemove'")
            }
            listChanged = true
        }

        if (listChanged) {
            Log.d(TAG, "VM: List changed during group initialization/update. Publishing.")
            publishChatListUpdates()
        } else {
            Log.d(TAG, "VM: No list changes during group initialization/update required publishing.")
        }
    }

    fun updateGroupWithMessage(
        groupId: String,
        groupName: String?,
        messageText: String?,
        messageTimestamp: Long,
        senderName: String?,
        senderId: String?,
        isNewUnreadOverride: Boolean? = null
    ) {
        val currentUserId = firebaseAuth.currentUser?.uid // Get fresh UID
        Log.d(TAG, "VM updateGroupWithMessage: START - GroupID='${groupId}', Msg='${messageText?.take(30)}...', SenderID='${senderId}', TS='${messageTimestamp}', CurrentUser: $currentUserId")

        var itemChangedOverall = false // Tracks if any change occurred that requires publishing
        val item = currentChatItemsMap[groupId]

        if (item == null) {
            Log.d(TAG, "VM: Group '$groupId' not found in map. Creating new ChatListItem.")
            val newUnreadCount = if (senderId != currentUserId && messageText != null) 1 else 0
            val newItem = ChatListItem(
                groupId = groupId,
                groupName = groupName ?: "Group $groupId", // Fallback name
                lastMessageText = messageText,
                lastMessageTimestamp = messageTimestamp,
                lastMessageSenderName = senderName,
                unreadCount = newUnreadCount,
                // If it's unread, this message timestamp is the one processed for unread.
                lastMessageTimestampProcessedForUnread = if (newUnreadCount > 0) messageTimestamp else 0L
            )
            currentChatItemsMap[groupId] = newItem
            itemChangedOverall = true // Item was added, so list changed
            Log.d(TAG, "VM: New group '$groupId' added via message. Unread: $newUnreadCount.")
            // No need to call publishChatListUpdates() here, it will be called at the end if itemChangedOverall is true
        } else {
            // Existing item, check for updates
            var specificPropertyChanged = false

            // Update last message details if this message is newer
            if (messageTimestamp > item.lastMessageTimestamp) {
                item.lastMessageText = messageText ?: "Attachment"
                item.lastMessageTimestamp = messageTimestamp
                item.lastMessageSenderName = senderName
                specificPropertyChanged = true
                itemChangedOverall = true
                Log.d(TAG, "VM: Updated last message for '$groupId'. New TS: $messageTimestamp. Old TS was: ${item.lastMessageTimestamp}.") // Log old TS for clarity
            }

            // Update group name if provided and different
            if (groupName != null && item.groupName != groupName) {
                item.groupName = groupName
                specificPropertyChanged = true
                itemChangedOverall = true
                Log.d(TAG, "VM: Updated group name for '$groupId' to '$groupName'.")
            }

            // --- Unread Count Logic ---
            val shouldIncrementUnreadLogReason = StringBuilder()
            var shouldActuallyIncrement = false

            if (isNewUnreadOverride == true) {
                shouldActuallyIncrement = true
                shouldIncrementUnreadLogReason.append("Forced by isNewUnreadOverride. ")
            } else {
                if (senderId != currentUserId) {
                    if (messageText != null) { // Meaningful message
                        // Check if this message is genuinely newer than the last one that incremented unread count,
                        // AND it is the message that is currently setting the lastMessageTimestamp for the chat item.
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
                item.lastMessageTimestampProcessedForUnread = messageTimestamp // Mark this timestamp as processed for unread
                // specificPropertyChanged = true // This was already set if unread count changed
                itemChangedOverall = true // Unread count changed, so overall item changed
                Log.d(TAG, "VM: Incremented unread for '$groupId'. New count: ${item.unreadCount}. Reason: ${shouldIncrementUnreadLogReason.toString().trim()}")
            } else {
                // Only log if no increment happened and it wasn't an override, and there was a potential reason logged
                if (isNewUnreadOverride != true && shouldIncrementUnreadLogReason.isNotBlank()) {
                    Log.d(TAG, "VM: Did NOT increment unread for '$groupId'. Reason: ${shouldIncrementUnreadLogReason.toString().trim()}")
                } else if (isNewUnreadOverride != true && senderId == currentUserId) {
                    // Explicit log if it's simply because the sender is the current user and no other complex condition failed
                    Log.d(TAG, "VM: Did NOT increment unread for '$groupId' because sender is current user.")
                }
            }
            // --- End Unread Count Logic ---
        } // End of else (item != null)

        if (itemChangedOverall) {
            Log.d(TAG, "VM: Overall item changed for '$groupId'. Publishing updates.")
            publishChatListUpdates()
        } else {
            Log.d(TAG, "VM: No overall changes detected for '$groupId' that require publishing.")
        }
        Log.d(TAG, "VM updateGroupWithMessage: END - GroupID='${groupId}'")
    }

    fun markGroupAsRead(groupId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM: markGroupAsRead called for GroupID='${groupId}'. CurrentUser: $currentUserId")
        currentChatItemsMap[groupId]?.let { item ->
            if (item.unreadCount > 0) {
                item.unreadCount = 0
                // When a group is marked as read, we should also update
                // lastMessageTimestampProcessedForUnread to the current latest message's timestamp.
                // This prevents an old message (that was part of the unread batch) from re-triggering
                // unread count if it's processed again after marking as read.
                item.lastMessageTimestampProcessedForUnread = item.lastMessageTimestamp
                Log.d(TAG, "VM: Marked group '$groupId' as read. Unread set to 0. LastProcessedTS set to ${item.lastMessageTimestamp}. Publishing.")
                publishChatListUpdates()
            } else {
                Log.d(TAG, "VM: Group '$groupId' already has 0 unread messages. No changes made.")
            }
        } ?: Log.d(TAG, "VM: markGroupAsRead: GroupID='$groupId' not found in map.")
    }

    fun getChatListItem(groupId: String): ChatListItem? {
        return currentChatItemsMap[groupId]
    }

    private fun publishChatListUpdates() {
        // Sort by last message timestamp (descending) before publishing
        val sortedList = currentChatItemsMap.values.toList().sortedByDescending { it.lastMessageTimestamp }
        Log.d(TAG, "VM: Publishing chat list updates. Count: ${sortedList.size}. Items: ${sortedList.joinToString { it.groupId + " (Unread:" + it.unreadCount + ", TS:" + it.lastMessageTimestamp + ")" }}")
        _chatList.postValue(sortedList) // Use postValue as updates can come from background threads
    }

    fun clearAllData() {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM: clearAllData called. CurrentUser: $currentUserId")
        currentChatItemsMap.clear()
        _chatList.postValue(emptyList())
        Log.d(TAG, "VM: Cleared all data from BroadGroupViewModel.")
    }
}