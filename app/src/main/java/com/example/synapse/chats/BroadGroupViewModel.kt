package com.example.synapse.chats

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.synapse.AppConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import org.json.JSONObject

class BroadGroupViewModel(application: Application) : AndroidViewModel(application) {

    private val _groups = MutableLiveData<List<Group>>()
    val groups: LiveData<List<Group>> = _groups

    private val _isAdmin = MutableLiveData<Boolean>()
    val isAdmin: LiveData<Boolean> = _isAdmin

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // --- SharedPreferences for Unread State Persistence ---
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("unread_group_states", Context.MODE_PRIVATE)
    }
    private val PREF_KEY_CHAT_ITEMS = "chat_items_map_data"
    // --- End SharedPreferences ---

    private val database = Firebase.database.reference
    // IMPORTANT: Use the correct path to your list of channels/groups in Firebase
    private val channelsRef = database.child("channels") // e.g., "channels" or "groups"
    private var channelsValueEventListener: ValueEventListener? = null

    private val _groupCreatedEvent = MutableLiveData<Pair<String, String?>?>()
    val groupCreatedEvent: LiveData<Pair<String, String?>?> = _groupCreatedEvent

    // --- Add isLoading LiveData ---
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    // --- End isLoading LiveData ---

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
        Log.d(TAG, "VM: ViewModel initialized. Attaching listeners.")
        checkCurrentUserAdminStatus() // Check admin status on init
        loadChatItemsMapFromPrefs()
        attachChannelsListener() // Start listening for real-time group updates for the main list

        // If you still need to initialize `currentChatItemsMap` for other purposes,
        // you might call a method here or it might be populated by other events.
    }

    // --- Function to check and update admin status ---
    fun checkCurrentUserAdminStatus() {
        val firebaseUser = firebaseAuth.currentUser // Use the existing firebaseAuth instance
        if (firebaseUser != null) {
            val isCurrentUserAdmin = AppConfig.ADMIN_UIDS.contains(firebaseUser.uid)
            _isAdmin.postValue(isCurrentUserAdmin)
            Log.d(TAG, "VM: Current user UID: ${firebaseUser.uid}, Is Admin: $isCurrentUserAdmin")
        } else {
            _isAdmin.postValue(false) // No user logged in, so not an admin
            Log.d(TAG, "VM: No user logged in, cannot determine admin status.")
        }
    }
    // --- End Function to check admin status ---

    // --- Add this method ---
    fun onGroupCreatedEventHandled() {
        _groupCreatedEvent.value = null
    }

    private fun getPrefKeyForCurrentUser(): String? {
        val currentUserId = firebaseAuth.currentUser?.uid
        return if (currentUserId != null) {
            "chat_items_map_data_$currentUserId"
        } else {
            null // Or handle as an error, though returning null is safer for conditional checks
        }
    }
    // --- Persistence Logic for currentChatItemsMap ---
    private fun loadChatItemsMapFromPrefs() {
        val prefKey = getPrefKeyForCurrentUser()
        if (prefKey == null) {
            Log.w(TAG, "VM: Cannot load chat items from prefs, user not logged in or key generation failed.")
            currentChatItemsMap.clear() // Ensure map is clear
            return
        }

        val jsonString = prefs.getString(prefKey, null)
        if (jsonString != null) {
            try {
                val jsonObject = JSONObject(jsonString)
                val tempMap = mutableMapOf<String, ChatListItem>()
                jsonObject.keys().forEach { parsedGroupId -> // Renamed to avoid confusion with ChatListItem.groupId
                    val itemJson = jsonObject.getJSONObject(parsedGroupId)

                    val chatListItem = ChatListItem() // Use the no-argument constructor
                    chatListItem.groupId = parsedGroupId // Set the groupId property
                    chatListItem.groupName = itemJson.optString("groupName", "Group $parsedGroupId")
                    chatListItem.unreadCount = itemJson.optInt("unreadCount", 0)
                    chatListItem.lastMessageTimestampProcessedForUnread = itemJson.optLong("lastMessageTimestampProcessedForUnread", 0L)
                    chatListItem.lastMessageText = itemJson.optString("lastMessageText", null)
                    chatListItem.lastMessageTimestamp = itemJson.optLong("lastMessageTimestamp", 0L)
                    // Your ChatListItem has lastMessageSenderName, not lastMessageSenderId
                    chatListItem.lastMessageSenderName = itemJson.optString("lastMessageSenderName", null)

                    tempMap[parsedGroupId] = chatListItem
                }
                currentChatItemsMap.clear()
                currentChatItemsMap.putAll(tempMap)
                Log.i(TAG, "VM: Loaded ${currentChatItemsMap.size} chat items from SharedPreferences for key $prefKey.")
            } catch (e: Exception) {
                Log.e(TAG, "VM: Error loading chat items from SharedPreferences for key $prefKey", e)
                currentChatItemsMap.clear()
            }
        } else {
            Log.i(TAG, "VM: No saved chat items found in SharedPreferences for key $prefKey.")
            currentChatItemsMap.clear()
        }
    }


    private fun saveChatItemsMapToPrefs() {
        val prefKey = getPrefKeyForCurrentUser()
        if (prefKey == null) {
            Log.w(TAG, "VM: Cannot save chat items to prefs, user not logged in or key generation failed.")
            return
        }

        val jsonObject = JSONObject()
        currentChatItemsMap.forEach { (mapGroupId, chatListItem) -> // mapGroupId is the key from the map
            val itemJson = JSONObject()
            // Use chatListItem.groupId if it's guaranteed to be the same as mapGroupId,
            // otherwise, it's safer to use mapGroupId as the key for the JSON object.
            // For consistency, let's assume chatListItem.groupId is correctly populated.

            itemJson.put("groupName", chatListItem.groupName)
            itemJson.put("unreadCount", chatListItem.unreadCount)
            itemJson.put("lastMessageTimestampProcessedForUnread", chatListItem.lastMessageTimestampProcessedForUnread)
            itemJson.put("lastMessageText", chatListItem.lastMessageText)
            itemJson.put("lastMessageTimestamp", chatListItem.lastMessageTimestamp)
            // Your ChatListItem has lastMessageSenderName
            itemJson.put("lastMessageSenderName", chatListItem.lastMessageSenderName)

            jsonObject.put(chatListItem.groupId, itemJson) // Use chatListItem.groupId as the key in JSON
        }
        try {
            prefs.edit().putString(prefKey, jsonObject.toString()).apply()
            Log.d(TAG, "VM: Saved ${currentChatItemsMap.size} chat items to SharedPreferences for key $prefKey.")
        } catch (e: Exception) {
            Log.e(TAG, "VM: Error saving chat items to SharedPreferences for key $prefKey", e)
        }
    }

    // In BroadGroupViewModel.kt

    private fun attachChannelsListener() {
        // Ensure any existing listener is removed before attaching a new one
        channelsValueEventListener?.let {
            channelsRef.removeEventListener(it)
            Log.d(TAG, "VM: Removed existing channels listener.")
        }
        channelsValueEventListener = null

        Log.d(TAG, "VM: Attaching new channels listener. Setting isLoading to true.")
        _isLoading.value = true // Indicate loading starts before attaching the listener

        channelsValueEventListener = channelsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "VM: attachChannelsListener - onDataChange. Path: ${snapshot.ref}")
                val newGroupsFromFirebase = mutableListOf<Group>()
                val currentUserId = firebaseAuth.currentUser?.uid
                var errorOccurredInLoop = false
                var chatListItemMapModified = false // To track if SharedPreferences need updating

                if (currentUserId == null) {
                    Log.e(TAG, "VM: Current user is null in attachChannelsListener. Cannot process groups.")
                    _error.postValue("User session error. Please restart.")
                    _groups.postValue(emptyList())
                    // Optionally clear local state if user is null
                    if (currentChatItemsMap.isNotEmpty()) {
                        currentChatItemsMap.clear()
                        saveChatItemsMapToPrefs()
                    }
                    _chatList.postValue(emptyList()) // Also update _chatList if it's still used
                    _isLoading.value = false
                    return
                }

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    Log.w(TAG, "VM: No channels data found at ${snapshot.ref}. Clearing local state.")
                    if (currentChatItemsMap.isNotEmpty()) {
                        currentChatItemsMap.clear()
                        chatListItemMapModified = true
                    }
                    _groups.postValue(emptyList())
                    _chatList.postValue(emptyList()) // Also update _chatList if it's still used
                    if (chatListItemMapModified) {
                        saveChatItemsMapToPrefs()
                    }
                    _isLoading.value = false // Loading finished (no data found)
                    return
                }

                val groupIdsFromFirebase = mutableSetOf<String>()

                snapshot.children.forEach { groupSnapshot ->
                    val groupId = groupSnapshot.key
                    if (groupId == null) {
                        Log.e(TAG, "VM: groupSnapshot key (groupId) is null. Skipping.")
                        return@forEach
                    }
                    groupIdsFromFirebase.add(groupId)

                    val group = groupSnapshot.getValue(Group::class.java)
                    if (group == null) {
                        Log.e(TAG, "VM: Failed to parse Group for ID: $groupId. Value: ${groupSnapshot.value}")
                        errorOccurredInLoop = true
                        return@forEach
                    }
                    group.id = groupId // Ensure ID is set

                    // --- Retrieve or initialize ChatListItem for this group ---
                    var chatListItem = currentChatItemsMap[groupId]
                    if (chatListItem == null) {
                        Log.d(TAG, "VM: No ChatListItem for group $groupId. Creating new default in-memory item.")
                        // For a brand new group never seen by the client, all messages are effectively "new"
                        // until the user interacts. So, lastMessageTimestampProcessedForUnread starts at 0.
                        val newDefaultItem = ChatListItem(
                            groupId = group.id,
                            groupName = group.name,
                            lastMessageText = group.lastMessage?.text,
                            lastMessageTimestamp = group.lastMessage?.timestamp ?: 0L,
                            lastMessageSenderName = group.lastMessage?.senderName,
                            unreadCount = 0, // Will be updated if new messages arrive and are processed
                            lastMessageTimestampProcessedForUnread = 0L // User hasn't "processed" anything for this new group yet
                        )
                        currentChatItemsMap[group.id] = newDefaultItem
                        chatListItem = newDefaultItem // Use the newly created item for subsequent logic
                        chatListItemMapModified = true
                    } else {
                        // If ChatListItem exists, ensure its details are up-to-date with the group from Firebase
                        // (e.g., name might have changed, or last message details if not updated elsewhere)
                        var itemUpdated = false
                        if (chatListItem.groupName != group.name) {
                            chatListItem.groupName = group.name
                            itemUpdated = true
                        }
                        // Update last message details in ChatListItem if they differ from the group's last message
                        // This is a good place to sync if ChatListItem might not be updated by other means (e.g. direct message listeners)
                        val groupLastMsgTs = group.lastMessage?.timestamp ?: 0L
                        if (chatListItem.lastMessageTimestamp != groupLastMsgTs) {
                            chatListItem.lastMessageText = group.lastMessage?.text
                            chatListItem.lastMessageTimestamp = groupLastMsgTs
                            chatListItem.lastMessageSenderName = group.lastMessage?.senderName
                            itemUpdated = true
                        }
                        if (itemUpdated) {
                            chatListItemMapModified = true
                        }
                    }
                    // --- End ChatListItem retrieval/initialization ---

// ... inside snapshot.children.forEach { groupSnapshot ->
// ... after 'val group = groupSnapshot.getValue(Group::class.java)'
// ... and after 'var chatListItem = currentChatItemsMap[groupId]' block

                    // --- Dot Logic ---
                    val lastMessageInGroup = group.lastMessage // Get the LastMessage object
                    val currentLastMessageTimestamp = lastMessageInGroup?.timestamp ?: 0L // Timestamp of the latest message in the group
                    val processedTimestampForUnread = chatListItem.lastMessageTimestampProcessedForUnread

                    Log.d(TAG, "VM: CHECKING DOT LOGIC for Group ${group.id}: Name='${group.name}'")
                    Log.d(TAG, "VM: Current User ID: $currentUserId")
                    Log.d(TAG, "VM: Last Message Sender ID: ${lastMessageInGroup?.senderId}")
                    Log.d(TAG, "VM: Last Message Timestamp (currentLastMessageTimestamp): $currentLastMessageTimestamp")
                    Log.d(TAG, "VM: ChatListItem Processed Timestamp (processedTimestampForUnread): $processedTimestampForUnread")

                    val isSenderNotCurrentUser = lastMessageInGroup?.senderId != null && lastMessageInGroup.senderId != currentUserId
                    Log.d(TAG, "VM: Is Sender NOT Current User? $isSenderNotCurrentUser")

                    var shouldShowDot = false
                    if (currentLastMessageTimestamp > 0 && isSenderNotCurrentUser) {
                        Log.d(TAG, "VM: Conditions (last msg exists AND senderId != currentUserId) ARE TRUE.")
                        if (currentLastMessageTimestamp > processedTimestampForUnread) {
                            shouldShowDot = true
                            Log.d(TAG, "VM: Timestamp condition (current > processed) IS TRUE. shouldShowDot = true")
                        } else {
                            Log.d(TAG, "VM: Timestamp condition (current > processed) IS FALSE. shouldShowDot remains false. ProcessedTS might be equal or newer.")
                        }
                    } else {
                        Log.d(TAG, "VM: Conditions (last msg exists AND senderId != currentUserId) ARE FALSE. Last msg might be null, from current user, or senderId is null. shouldShowDot remains false.")
                    }
                    group.showUnreadDot = shouldShowDot
                    Log.d(TAG, "VM: FINAL ShowDot for group ${group.id}: ${group.showUnreadDot}")
                    // --- End Dot Logic ---

                    // --- Update Group object with unread info from ChatListItem ---
                    // The unreadCount on the Group object should reflect the detailed unreadCount from ChatListIte
                    // ... rest of your group processing logic

                    //--- Update Group object with unread info from ChatListItem ---
                    // The unreadCount on the Group object should reflect the detailed unreadCount from ChatListItem.
                    group.unreadCount = chatListItem.unreadCount
                    val isLastMsgFromAnother = group.lastMessage?.senderId != null &&
                            group.lastMessage?.senderId != currentUserId
                    // hasUnreadMessagesFromOthers is true if there's any unread message AND the last one is from someone else.
                    // Or, more simply, if the unread count (managed by ChatListItem) is > 0 and last msg is from other.
                    group.hasUnreadMessagesFromOthers = (group.unreadCount > 0 && isLastMsgFromAnother)
                    // --- End Update Group object ---

                    Log.d(TAG, "VM: Processed Group ID ${group.id}: Name='${group.name}', " +
                            "LMsg='${group.lastMessage?.text?.take(20)}', LMsgTS='${group.lastMessage?.timestamp}', " +
                            "ProcessedTS='${chatListItem.lastMessageTimestampProcessedForUnread}', " +
                            "ShowDot=${group.showUnreadDot}, UnreadCount=${group.unreadCount}, HasUnreadOthers=${group.hasUnreadMessagesFromOthers}")

                    newGroupsFromFirebase.add(group)
                } // End of snapshot.children.forEach

                if (errorOccurredInLoop) {
                    _error.postValue("Error parsing some group data. Check logs.")
                }

                // Handle groups that were in currentChatItemsMap but are no longer in Firebase
                val groupIdsToRemoveFromMap = currentChatItemsMap.keys.filterNot { it in groupIdsFromFirebase }
                if (groupIdsToRemoveFromMap.isNotEmpty()) {
                    groupIdsToRemoveFromMap.forEach { groupIdToRemove ->
                        currentChatItemsMap.remove(groupIdToRemove)
                        Log.d(TAG, "VM: Removed ChatListItem for deleted group ID '$groupIdToRemove'.")
                    }
                    chatListItemMapModified = true // Map was modified
                }

                if (chatListItemMapModified) {
                    saveChatItemsMapToPrefs() // Save if the map was changed (new items or removals)
                }

                val sortedGroups = newGroupsFromFirebase.sortedByDescending { it.lastMessage?.timestamp ?: it.timestamp ?: 0L }
                _groups.postValue(sortedGroups)

                // Update _chatList separately if it's still actively used by another part of your UI
                // and is derived from currentChatItemsMap
                publishChatListUpdates()
                _isLoading.value = false
            }

            // In BroadGroupViewModel.kt (continuing attachChannelsListener's onCancelled)

            // In BroadGroupViewModel.kt (within attachChannelsListener's ValueEventListener)

            override fun onCancelled(error: DatabaseError) {
                // Use toString() for the full path, or .key for just the last segment.
                // For a listener attached to 'channelsRef', toString() gives the full URL.
                val pathInfo = channelsRef.toString() // More robust way to get the path information

                Log.e(TAG, "VM: Firebase listener for channels cancelled: ${error.message} at path: $pathInfo", error.toException())
                _isLoading.value = false // Loading finished (due to cancellation/error)

            }
        })
    }

    // Ensure publishChatListUpdates uses the current state of currentChatItemsMap
    fun publishChatListUpdates() {
        val sortedChatListItems = currentChatItemsMap.values.toList()
            .sortedByDescending { it.lastMessageTimestamp }
        Log.d(TAG, "VM: Publishing _chatList updates. Count: ${sortedChatListItems.size}.")
        _chatList.postValue(sortedChatListItems)
    }
// Make sure this LiveData is defined if you want to signal creation success to the UI
// private val _groupCreatedEvent = MutableLiveData<Pair<String, String?>>()
// val groupCreatedEvent: LiveData<Pair<String, String?>> = _groupCreatedEvent

    // In BroadGroupViewModel.kt

// Make sure this LiveData is defined if you want to signal creation success to the UI
// private val _groupCreatedEvent = MutableLiveData<Pair<String, String?>>() // Pair of <groupId, groupName>
// val groupCreatedEvent: LiveData<Pair<String, String?>> = _groupCreatedEvent

    fun createNewGroup(
        groupName: String,
        creatorId: String, // This MUST be one of the hardcoded admin UIDs for the write to succeed
        description: String? = null,
        memberIdsFromDialog: List<String> // Renamed to clarify origin
    ) {
        Log.d(
            TAG,
            "VM: createNewGroup called. Name: $groupName, Creator: $creatorId, Initial Members from Dialog: $memberIdsFromDialog"
        )

        // Using 'channelsRef' as per your comment, assuming it's the correct ref for channels/groups
        val newGroupId = channelsRef.push().key

        if (newGroupId == null) {
            _error.postValue("Couldn't get a unique ID for the new channel.")
            Log.e(TAG, "VM: createNewGroup - Firebase push key was null.")
            return
        }

        // For admin-created public channels, initially, memberIds might be empty or just include the creator.
        // The current dialog sends an empty list for memberIdsFromDialog.
        // Adding the creator to memberIds by default is a common pattern.
        val initialMemberIdsForFirebase = (memberIdsFromDialog + creatorId).distinct()
       // val membersMap = initialMemberIdsForFirebase.associateWith { true }

        Log.d(TAG, "VM: Final members map for new channel '$newGroupId': $initialMemberIdsForFirebase")

        val groupData = mapOf(
            "id" to newGroupId,
            "name" to groupName,
            "description" to description, // Will be omitted if null due to mapOfNotNull
            "memberIds" to initialMemberIdsForFirebase,    // Store as map { "uid1": true, "uid2": true }
            "lastMessage" to null,        // Explicitly null for a new channel, can be omitted too
            "createdBy" to creatorId,     // Crucial: This UID must be in the admin list in rules
            "timestamp" to ServerValue.TIMESTAMP
            // "unreadCount" is not typically set at channel creation.
        )

        channelsRef.child(newGroupId).setValue(groupData)
            .addOnSuccessListener {
                Log.i(
                    TAG,
                    "VM: New channel '$groupName' successfully created in Firebase with ID: $newGroupId by $creatorId"
                )
                // If _groupCreatedEvent is used, post to it. Example:
                _groupCreatedEvent.postValue(Pair(newGroupId, groupName.trim()))
            }
            .addOnFailureListener { e ->
                Log.e(
                    TAG,
                    "VM: Failed to create new channel '$groupName' in Firebase. Error: ${e.message}",
                    e
                )
                _error.postValue("Failed to create channel: ${e.message}")
            }
    }

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
            saveChatItemsMapToPrefs() // <<< ADD THIS LINE
            publishChatListUpdates() // This updates _chatList

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
            saveChatItemsMapToPrefs()
            publishChatListUpdates() // This updates _chatList

        } else {
            Log.d(TAG, "VM: No overall ChatListItem changes detected for '$groupId' that require publishing.")
        }
        Log.d(TAG, "VM updateGroupWithMessage: END - GroupID='${groupId}'")
    }

    // In BroadGroupViewModel.kt

    fun markGroupAsRead(groupId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM: markGroupAsRead called for GroupID='${groupId}'. CurrentUser: $currentUserId")

        currentChatItemsMap[groupId]?.let { item ->
            val groupFromLiveData = _groups.value?.find { it.id == groupId }
            // Use the timestamp from the actual last message in the group if available,
            // otherwise, use the last message timestamp stored in the ChatListItem.
            val latestTimestampToProcess = groupFromLiveData?.lastMessage?.timestamp
                ?: item.lastMessageTimestamp // Fallback to ChatListItem's last message timestamp

            var itemChangedInMap = false
            var groupInListNeedsUpdate = false

            if (item.unreadCount > 0) {
                item.unreadCount = 0
                itemChangedInMap = true
                Log.d(TAG, "VM: ChatListItem '$groupId' unreadCount set to 0.")
            }

            // Always update lastMessageTimestampProcessedForUnread to the latest known message timestamp
            // when marking as read. This ensures that even if unreadCount was already 0,
            // the "processed" point is correctly set for future dot logic.
            if (latestTimestampToProcess > item.lastMessageTimestampProcessedForUnread) {
                item.lastMessageTimestampProcessedForUnread = latestTimestampToProcess
                itemChangedInMap = true
                Log.d(TAG, "VM: ChatListItem '$groupId' lastMessageTimestampProcessedForUnread updated to $latestTimestampToProcess.")
            }

            if (itemChangedInMap) {
                Log.d(TAG, "VM: ChatListItem '$groupId' updated. Unread: ${item.unreadCount}, ProcessedTS: ${item.lastMessageTimestampProcessedForUnread}.")
                saveChatItemsMapToPrefs() // Persist changes to ChatListItem
                groupInListNeedsUpdate = true // Since ChatListItem changed, the Group in _groups might need UI update for dot/count
            }

            // Update the specific Group object in the _groups LiveData
            _groups.value?.let { currentGroups ->
                var groupFoundAndModified = false
                val updatedGroups = currentGroups.map { group ->
                    if (group.id == groupId) {
                        // Even if itemChangedInMap was false (e.g. unread already 0),
                        // we ensure showUnreadDot is false after explicitly marking as read.
                        if (group.showUnreadDot || group.unreadCount != 0 || group.hasUnreadMessagesFromOthers) {
                            groupFoundAndModified = true
                            group.copy(
                                showUnreadDot = false, // Explicitly set dot to false
                                unreadCount = 0,       // Ensure unread count is 0
                                hasUnreadMessagesFromOthers = false // And this too
                            )
                        } else {
                            group // No change needed for this group's unread status
                        }
                    } else {
                        group
                    }
                }
                // Only post if a group was actually modified or if the underlying ChatListItem data changed
                if (groupFoundAndModified || groupInListNeedsUpdate) {
                    // Inside BroadGroupViewModel.kt (continuing markGroupAsRead)

                    _groups.postValue(updatedGroups.sortedByDescending { it.lastMessage?.timestamp ?: it.timestamp ?: 0L })
                    Log.d(TAG, "VM: Refreshed _groups after markGroupAsRead for ID $groupId. Dot should be off.")
                }
            }

            // Update _chatList as well if its data (like unreadCount) changed
            if (itemChangedInMap) {
                publishChatListUpdates()
            }

        } ?: Log.w(TAG, "VM: markGroupAsRead: ChatListItem GroupID='$groupId' not found in map.")
    }

    // ... (rest of your BroadGroupViewModel, including onCleared, etc.)
    // This method is specific to the ChatListItem system.
    fun getChatListItem(groupId: String): ChatListItem? {
        return currentChatItemsMap[groupId]
    }

    fun clearAllData() {
        val currentUserId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "VM: clearAllData called. CurrentUser: $currentUserId")
        currentChatItemsMap.clear()
        _chatList.postValue(emptyList())
        // _groups will be cleared by the listener if the Firebase node becomes empty or on next update.
        // If you want to clear it immediately:
        _groups.postValue(emptyList())
        // Clear SharedPreferences for the current user
        val prefKey = getPrefKeyForCurrentUser()
        if (prefKey != null) {
            prefs.edit().remove(prefKey).apply()
            Log.d(TAG, "VM: Cleared SharedPreferences for key $prefKey.")
        } else {
            Log.w(TAG, "VM: Could not clear SharedPreferences, user not logged in or key generation failed.")
        }
        Log.d(TAG, "VM: Cleared all data from BroadGroupViewModel (ChatListItems, Groups, and relevant SharedPreferences).")
    }


    override fun onCleared() {
        super.onCleared()
        // Remove Firebase listener to prevent memory leaks
        channelsValueEventListener?.let {
            channelsRef.removeEventListener(it)
        }
        channelsValueEventListener = null // Ensure it's nullified
        Log.d(TAG, "BroadGroupViewModel cleared and Firebase listener removed.")
    }
}