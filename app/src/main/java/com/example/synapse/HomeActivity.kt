package com.example.synapse

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // For normal ViewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider // Required for activity-scoped ViewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.synapse.chats.BroadGroupViewModel
import com.example.synapse.chats.GroupData
import com.example.synapse.databinding.ActivityHomeBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// Assuming Message.kt exists:
// import com.example.synapse.models.Message // Make sure this path is correct

class HomeActivity : AppCompatActivity(),ChatNavigationListener {

    private val homeViewModel: HomeViewModel by viewModels()

    private var navController: NavController? = null
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val TAG = "HomeActivity" // For logging


    // --- ViewBinding and Core UI ---
    private lateinit var binding: ActivityHomeBinding
    private val tabTitles = arrayOf("Boards", "Direct Messages")

    // --- Authentication & Firebase ---
    lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var channelsMetadataValueListener: ValueEventListener? = null // Renamed for clarity
    private val groupMessageDataListeners = mutableMapOf<String, ValueEventListener>() // Key: "groupId/messageTypePath"
    private val activeFirebasePathsForListeners = mutableMapOf<String, DatabaseReference>() // Store refs for removal

    // --- Preferences & ViewModels ---
    private lateinit var messagePrefs: SharedPreferences
    private lateinit var broadGroupViewModel: BroadGroupViewModel // For Broad Groups chat list

    // --- Notification & Permission ---
    private var dmNotificationBellMenuItem: MenuItem? = null
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            handleNotificationPermissionResult(isGranted)
        }

    // --- Group Data Cache (ID -> GroupData(id, name)) ---
    // This cache is still useful for quick name lookups if needed,
    // though ViewModel becomes primary source for UI list items.
  private val groupDataCache= mutableMapOf<String,com.example.synapse.chats.GroupData>()

    // --- Tracking currently open broad group chat ---
    // To help BroadGroupFragment clear unread count onResume
    private var currentlyOpenBroadGroupId: String? = null


    // Helper data class for basic group info (name, id) - Keep this definition
    data class GroupData(val id: String, val name: String)

    companion object {
        private const val TAG = "HomeActivity"
        // To pass to GroupChatActivity and get back which group was opened
        const val EXTRA_OPENED_GROUP_ID = "extra_opened_group_id"
        const val EXTRA_TARGET_GROUP_ID = "com.example.synapse.TARGET_GROUP_ID"
        const val EXTRA_TARGET_GROUP_NAME = "com.example.synapse.TARGET_GROUP_NAME"
        const val EXTRA_LAUNCH_SOURCE = "com.example.synapse.LAUNCH_SOURCE"

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Enable edge-to-edge

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Setup ViewPager2 and TabLayout (Your existing setup)
        val viewPager = binding.viewPagerHome // Assuming ID is view_pager_home
        val tabLayout = binding.tabLayoutHome // Assuming ID is tab_layout_home
        val adapter = HomeViewPagerAdapter(this)
        viewPager.adapter = adapter

        com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "CHANNELS" // Or getString(R.string.channels)
                1 -> "DIRECT MESSAGES" // Or getString(R.string.direct_messages)
                else -> null
            }
        }.attach()

        // Setup Toolbar
        setSupportActionBar(binding.toolbarHome)

        initializeCoreComponents()
        setupUI() // Includes ViewPager and TabLayout

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment? // Replace with your NavHostFragment ID
        navController = navHostFragment?.navController


        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.broadGroupFragment // Add other top-level tab fragments if they are in this graph
            )
            // , binding.drawerLayout // If you have a DrawerLayout
        )
        navController?.let { setupActionBarWithNavController(it, appBarConfiguration) }

        if (navController == null) {
            Log.e("HomeActivity", "NavController not found from nav_host_fragment_activity_home. Ensure it's in the layout and correctly typed.")
        }

        navController?.addOnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id == R.id.chatRoomFragment) {
                Log.d(TAG, "Navigated to ChatRoomFragment. Hiding tab container, showing NavHost.")
                binding.tabContentContainer.visibility = View.GONE
                binding.navHostFragmentActivityHome.visibility = View.VISIBLE // Show the NavHost
            } else {
                Log.d(TAG, "Navigated away from ChatRoomFragment. Showing tab container, hiding NavHost.")
                binding.tabContentContainer.visibility = View.VISIBLE
                binding.navHostFragmentActivityHome.visibility = View.GONE // Hide the NavHost
            }
        }


        if (auth.currentUser == null) {
            Log.w(TAG, "User not logged in. Redirecting to LoginActivity.")
            navigateToLogin()
            return
        }

        // Fetch initial group list and then setup message listeners
        fetchBroadGroupMetadataAndSetupListeners()

        handleNotificationIntent(intent)

        Log.d(TAG, "onCreate completed.")
    }

    // In HomeActivity.kt
// Assuming mainNavController is lateinit var and initialized

    override fun onSupportNavigateUp(): Boolean {
        // If navController is null, navigateUp isn't called, and the expression becomes null.
        // The Elvis operator then provides 'false', leading to super.onSupportNavigateUp().
        return navController?.navigateUp(appBarConfiguration) ?: false || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent) { // Added 'override'
        super.onNewIntent(intent)
        // Handle intent from notification if the activity is already running
        intent?.let {
            setIntent(it) // Update the activity's intent to the new one
            handleNotificationIntent(it)
            Log.d(TAG, "onNewIntent: Handled intent from notification.")
        }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val launchSource = intent.getStringExtra(EXTRA_LAUNCH_SOURCE)
        Log.d(TAG, "handleNotificationIntent: launchSource = $launchSource")

        if (launchSource == "notification_broad_group") {
            val groupId = intent.getStringExtra(EXTRA_TARGET_GROUP_ID)
            val groupName = intent.getStringExtra(EXTRA_TARGET_GROUP_NAME)
            // val targetMessageType = intent.getStringExtra("TARGET_MESSAGE_TYPE") // If you added this extra

            Log.d(TAG, "Notification intent received: GroupID='$groupId', GroupName='$groupName'")

            if (groupId != null && groupName != null) {
                if (navController == null) {
                    Log.e(TAG, "NavController is null in handleNotificationIntent. Cannot navigate.")
                    Toast.makeText(this, "Error opening chat. NavController not ready.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Ensure the user is logged in before attempting navigation
                if (auth.currentUser == null) {
                    Log.w(TAG, "User not logged in. Cannot navigate from notification. Redirecting to login.")
                    navigateToLogin() // Or show a message
                    return
                }

                try {
                    val currentDestinationId = navController?.currentDestination?.id
                    var shouldNavigate = true
                    if (currentDestinationId == R.id.chatRoomFragment) {
                        // If already in a chat room, check if it's the *same* chat room
                        val currentArgs = navController?.currentBackStackEntry?.arguments
                        val currentGroupId = currentArgs?.getString("groupId") // Assuming "groupId" is the arg name
                        if (currentGroupId == groupId) {
                            Log.d(TAG, "Already in the target chat room ($groupId). No navigation needed.")
                            shouldNavigate = false
                        } else {
                            Log.d(TAG, "In a different chat room. Navigating to target chat room ($groupId).")
                            // May need to pop the current chat room first if you don't want them stacked
                            // navController?.popBackStack() // Optional: depends on desired back stack behavior
                        }
                    }

                    if (shouldNavigate) {
                        // Create bundle for arguments
                        val bundle = Bundle().apply {
                            putString("groupId", groupId)
                            putString("groupName", groupName)
                            // putString("initialMessageType", targetMessageType) // If you pass this
                        }
                        // Ensure R.id.chatRoomFragment is the correct ID from your nav_graph.xml
                        navController?.navigate(R.id.chatRoomFragment, bundle)
                        Log.i(TAG, "Navigated to ChatRoomFragment for group: $groupName (ID: $groupId)")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Navigation to ChatRoomFragment failed from notification: ${e.message}", e)
                    Toast.makeText(this, "Could not open chat: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }

                // Important: Remove the extras so they aren't processed again on configuration change
                // or if the activity is brought to front without a new intent.
                intent.removeExtra(EXTRA_LAUNCH_SOURCE)
                intent.removeExtra(EXTRA_TARGET_GROUP_ID)
                intent.removeExtra(EXTRA_TARGET_GROUP_NAME)
                // intent.removeExtra("TARGET_MESSAGE_TYPE")
            } else {
                Log.w(TAG, "Notification intent missing groupId or groupName.")
            }
        }
    }



    override fun onResume() {
        super.onResume()
        // Check if we are returning from GroupChatActivity or if a group was opened
        // This helps the BroadGroupFragment to clear the unread count if it's visible
        intent.getStringExtra(EXTRA_OPENED_GROUP_ID)?.let { openedGroupId ->
            Log.d(TAG, "Resuming HomeActivity, group $openedGroupId was potentially opened.")
            broadGroupViewModel.markGroupAsRead(openedGroupId) // Update ViewModel state
            currentlyOpenBroadGroupId = openedGroupId // Set for fragment to pick up if it's active
            intent.removeExtra(EXTRA_OPENED_GROUP_ID) // Clear the extra after processing
        }

        // If the Broad Groups tab is currently selected, tell the fragment
        // to check and clear the unread count for the 'currentlyOpenBroadGroupId'
        if (binding.viewPagerHome.currentItem == tabTitles.indexOf("Broad Groups")) {
            val fragment = supportFragmentManager.findFragmentByTag("f${binding.viewPagerHome.currentItem}")
            if (fragment is BroadGroupFragment) {
                currentlyOpenBroadGroupId?.let {
                    fragment.markGroupAsReadInList(it)
                    clearCurrentlyOpenBroadGroupId() // Clear after fragment has handled it
                }
            }
        }
        Log.d(TAG, "onResume completed.")
    }


    override fun onDestroy() {
        super.onDestroy()
        clearAllFirebaseListeners()
        Log.d(TAG, "onDestroy: Listeners cleared.")
    }
    //endregion

    //region Initialization and UI Setup
    //==============================================================================================
    private fun initializeCoreComponents() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        messagePrefs = getSharedPreferences("broad_group_message_prefs", MODE_PRIVATE)

        // Initialize the ViewModel scoped to this Activity
        broadGroupViewModel = ViewModelProvider(this).get(BroadGroupViewModel::class.java)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbarHome)
        supportActionBar?.title = getString(R.string.app_name) // Or "Synapse"

        val adapter = HomeViewPagerAdapter(this) // Your existing adapter
        binding.viewPagerHome.adapter = adapter
        TabLayoutMediator(binding.tabLayoutHome, binding.viewPagerHome) { tab, position ->
            tab.text = tabTitles.getOrNull(position) ?: "Tab ${position + 1}"
        }.attach()

        binding.tabLayoutHome.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == tabTitles.indexOf("Broad Groups")) {
                    // When Broad Groups tab is selected, ensure the fragment (if it was just created or resumed)
                    // knows about any group that was just opened.
                    currentlyOpenBroadGroupId?.let { groupId ->
                        val fragment = supportFragmentManager.findFragmentByTag("f${tab.position}")
                        if (fragment is BroadGroupFragment) {
                            fragment.markGroupAsReadInList(groupId)
                            clearCurrentlyOpenBroadGroupId()
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    //endregion

    //region Notification Permission
    //==============================================================================================
    private fun askNotificationPermissionAndThenSetupMessageListeners() { // Renamed for clarity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "POST_NOTIFICATIONS permission already granted.")
                    setupAllBroadGroupMessageListeners()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.i(TAG, "Showing rationale for POST_NOTIFICATIONS permission.")
                    Toast.makeText(this, "Please grant notification permission to receive group updates.", Toast.LENGTH_LONG).show()
                    // Consider showing a more formal dialog here that explains why you need the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "No runtime notification permission needed for this Android version.")
            setupAllBroadGroupMessageListeners()
        }
    }

    private fun handleNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.d(TAG, "POST_NOTIFICATIONS permission granted by user.")
            setupAllBroadGroupMessageListeners()
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied by user.")
            Toast.makeText(this, "Notifications for group messages may not be shown as permission was denied.", Toast.LENGTH_LONG).show()
            // Still setup listeners for in-app UI updates even if system notifications are denied
            setupAllBroadGroupMessageListeners()
        }
    }
    //endregion

    //region Data Fetching & Listener Setup
    //==============================================================================================
    private fun fetchBroadGroupMetadataAndSetupListeners() {
        if (auth.currentUser == null) {
            Log.w(TAG, "Cannot fetch group metadata, user not logged in.")
            return
        }
        // Show loading state in BroadGroupFragment if it's active
        // broadGroupViewModel.setLoadingState(true) // You'd need to add this to ViewModel

        val groupsMetadataRef = database.getReference("channels") // Your node for group names/metadata

        // Clear existing listeners before fetching new metadata
        channelsMetadataValueListener?.let {
            groupsMetadataRef.removeEventListener(it)
        }
        clearAllGroupMessageListenersOnly() // Clear only message listeners, not this metadata listener

        Log.d(TAG, "Fetching broad group metadata from 'channels'.")
        channelsMetadataValueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedGroups = mutableListOf<com.example.synapse.chats.GroupData>()
                groupDataCache.clear()

                if (snapshot.exists()) {
                    for (groupSnapshot in snapshot.children) {
                        val groupId = groupSnapshot.key
                        val groupName = groupSnapshot.child("name").getValue(String::class.java)
                        if (groupId != null && groupName != null) {
                            val group = com.example.synapse.chats.GroupData(groupId, groupName)
                            fetchedGroups.add(group)
                            groupDataCache[groupId] = group // Update cache
                        } else {
                            Log.w(TAG, "Skipping group with null id or name: ${groupSnapshot.key}")
                        }
                    }
                    Log.d(TAG, "Fetched ${fetchedGroups.size} groups metadata.")
                } else {
                    Log.w(TAG, "No data found at 'channels' node.")
                }
                // Update ViewModel with the new list of groups
                // This will also trigger removal of groups in ViewModel not in fetchedGroups
                broadGroupViewModel.initializeOrUpdateGroups(fetchedGroups)

                // Now that groups are initialized in ViewModel, ask for permission (if needed)
                // and then setup message listeners for these groups.
                askNotificationPermissionAndThenSetupMessageListeners()
                // broadGroupViewModel.setLoadingState(false) // Update loading state
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch broad group metadata: ${error.message}")
                broadGroupViewModel.initializeOrUpdateGroups(emptyList()) // Clear groups in ViewModel on error
                // broadGroupViewModel.setLoadingState(false) // Update loading state
                // Consider showing an error message to the user
            }
        }
        groupsMetadataRef.addValueEventListener(channelsMetadataValueListener!!) // Listen for ongoing changes
    }

// HomeActivity.kt (Continued from setupAllBroadGroupMessageListeners())

    // This sets up listeners for messages within each group identified by fetchBroadGroupMetadata
    private fun setupAllBroadGroupMessageListeners() {
        if (auth.currentUser == null) {
            Log.w(TAG, "Cannot setup message listeners, user not logged in.")
            return
        }

        // Get the list of group IDs from the ViewModel or cache to ensure we listen to the correct set
        // Using groupDataCache as it's directly populated by fetchBroadGroupMetadata
        if (groupDataCache.isEmpty()) {
            Log.i(TAG, "No group data cached. No message listeners to set up.")
            // Ensure any old listeners are cleared if the cache becomes empty
            clearAllGroupMessageListenersOnly()
            return
        }

        Log.d(TAG, "Setting up message listeners for ${groupDataCache.size} groups based on cached metadata.")

        // Create a set of current listener keys to find and remove obsolete listeners
        val currentListenerKeys = mutableSetOf<String>()

        groupDataCache.values.forEach { groupData -> // groupData is GroupData(id, name)
            // IMPORTANT: Ensure groupData.name is a valid Firebase key!
            // It should not contain '.', '$', '#', '[', ']', '/', or ASCII control characters.
            if (isValidFirebaseKey(groupData.name)) {
                // Listen to "improvement_messages"
                val improvementKey = "${groupData.id}/improvement_messages" // Use ID for internal key
                listenForNewMessagesInGroup(groupData.id, groupData.name, "improvement_messages")
                currentListenerKeys.add(improvementKey)

                // Listen to "drawback_messages"
                val drawbackKey = "${groupData.id}/drawback_messages" // Use ID for internal key
                listenForNewMessagesInGroup(groupData.id, groupData.name, "drawback_messages")
                currentListenerKeys.add(drawbackKey)

                // Add other message types if necessary, following the same pattern
            } else {
                Log.e(TAG, "Skipping listener setup for group '${groupData.name}' (ID: ${groupData.id}) due to invalid characters in name for Firebase key.")
            }
        }

        // Remove listeners for groups/messageTypes that are no longer active
        val listenersToRemove = groupMessageDataListeners.keys.filterNot { it in currentListenerKeys }
        listenersToRemove.forEach { keyToRemove ->
            groupMessageDataListeners.remove(keyToRemove)?.let { listener ->
                activeFirebasePathsForListeners.remove(keyToRemove)?.removeEventListener(listener)
                Log.d(TAG, "Removed obsolete message listener for key: $keyToRemove")
            }
        }
    }

    private fun isValidFirebaseKey(name: String): Boolean {
        // Firebase keys cannot contain '.', '$', '#', '[', ']', '/', or ASCII control characters 0-31 or 127.
        return !name.contains(Regex("[.#$\\[\\]/]|[\u0000-\u001F\u007F]"))
    }

// HomeActivity.kt (Continued from listenForNewMessagesInGroup())

    // Modified to accept groupIdForMapping (for ViewModel) and groupNameForPath (for Firebase path)
    private fun listenForNewMessagesInGroup(
        groupIdForMapping: String, // The actual ID of the group
        groupNameForPath: String,  // The name used in the Firebase path (e.g., "messages/GroupName/...")
        messageTypePath: String    // e.g., "improvement_messages" or "drawback_messages"
    ) {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            Log.w(TAG, "User not logged in, cannot listen for messages in $groupNameForPath/$messageTypePath.")
            return
        }

        val firebasePath = "messages/$groupNameForPath/$messageTypePath"
        val groupMessagesRef = database.getReference(firebasePath)
        // Use a composite key that includes groupId and messageTypePath for uniqueness
        // This ensures that if two different groups happen to have the same messageTypePath (unlikely but possible),
        // their listeners are managed independently.
        val listenerCompositeKey = "$groupIdForMapping/$messageTypePath"

        // Remove existing listener for this specific path if it exists, before adding a new one
        groupMessageDataListeners[listenerCompositeKey]?.let { existingListener ->
            activeFirebasePathsForListeners[listenerCompositeKey]?.removeEventListener(existingListener)
            Log.d(TAG, "Removed existing message listener for key: $listenerCompositeKey before re-adding.")
        }
        // Also remove from active paths map to keep it clean
        activeFirebasePathsForListeners.remove(listenerCompositeKey)


        var lastSeenTimestampForNotifications = getLastSeenMessageTimestamp(listenerCompositeKey) // Use composite key for prefs
        Log.d(
            TAG,
            "Attaching listener for messages at path: $firebasePath. Last seen for notifications: $lastSeenTimestampForNotifications"
        )

        val newListener = object : ValueEventListener {
            private var initialDataProcessed = false
            private var latestTimestampInThisSnapshot: Long = 0L

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "No messages found for path $firebasePath.")
                    // If no messages, and the group exists in ViewModel, ensure it shows "No messages"
                    // The ViewModel's publishChatListUpdates will handle sorting and display
                    broadGroupViewModel.getChatListItem(groupIdForMapping)?.let {
                        if (it.lastMessageText != null || it.lastMessageTimestamp != 0L) { // Only update if it had a message before
                            broadGroupViewModel.updateGroupWithMessage(
                                groupId = groupIdForMapping,
                                groupName = groupDataCache[groupIdForMapping]?.name, // Get current name from cache
                                messageText = null, // Explicitly set to null or "No messages"
                                messageTimestamp = 0L, // Reset timestamp
                                senderName = null,
                                senderId = null,
                               // isNewUnreadOverride = false // Ensure no unread count
                            )
                        }
                    }
                    initialDataProcessed = true
                    return
                }

                var newMessagesForNotificationFoundInThisSnapshot = false
                latestTimestampInThisSnapshot = 0L
                var latestMessageForUI: Message? = null
                var latestMessageSenderNameForUI: String? = null

                // ... inside onDataChange ...
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    val messageId = messageSnapshot.key

                    if (message != null && messageId != null) {
                        val currentMessageTimestamp = message.timestamp // Keep as Long? initially

                        // Process for latest message for UI
                        if (currentMessageTimestamp != null) { // Check for null before comparison and assignment
                            if (currentMessageTimestamp > latestTimestampInThisSnapshot) {
                                latestTimestampInThisSnapshot = currentMessageTimestamp // Safe: currentMessageTimestamp is smart-cast to Long
                                latestMessageForUI = message
                                latestMessageSenderNameForUI = message.senderName
                            }
                        }

                        // --- START DEBUG LOGS FOR MESSAGE PROCESSING ---
                        // Use currentMessageTimestamp (which might be null) or message.timestamp directly for logging
                        Log.d(TAG, "HomeActivity: Received message for group '$groupNameForPath' (ID: $groupIdForMapping). Text: '${message.text}', Sender: '${message.senderName}', Timestamp: ${message.timestamp}")
                        // --- END DEBUG LOGS FOR MESSAGE PROCESSING ---

                        // System Notification Logic
                        if (currentMessageTimestamp != null) { // Check for null before comparison and use
                            if (currentMessageTimestamp > lastSeenTimestampForNotifications && initialDataProcessed) {
                                if (message.senderId != currentUserUid) {
                                    newMessagesForNotificationFoundInThisSnapshot = true
                                    Log.i(
                                        TAG,
                                        "NEW message for NOTIFICATION in group (name: $groupNameForPath, type: $messageTypePath): ${message.text}"
                                    )
                                    // currentMessageTimestamp is smart-cast to Long here
                                    val notificationId = (listenerCompositeKey.hashCode() + currentMessageTimestamp.hashCode()) % Int.MAX_VALUE
                                    NotificationHelper.showBroadGroupMessageNotification(
                                        applicationContext,
                                        groupIdForMapping,
                                        groupNameForPath,
                                        message.senderName ?: "Someone",
                                        message.text ?: "New message received",
                                        notificationId
                                    )
                                }
                            }
                        }
                    }
                } // End of snapshot.children.forEach

// ... (previous code from onDataChange) ...

                // Update ViewModel with the latest message from this snapshot
                latestMessageForUI?.let { msg ->
                    // --- START DEBUG LOGS BEFORE VIEWMODEL UPDATE ---
                    Log.d(TAG, "HomeActivity: Updating ViewModel for group '$groupNameForPath' (ID: $groupIdForMapping) with latest message. Text: '${msg.text}', Sender: '${latestMessageSenderNameForUI}', Timestamp: ${msg.timestamp}")
                    // --- END DEBUG LOGS BEFORE VIEWMODEL UPDATE ---

                    broadGroupViewModel.updateGroupWithMessage(
                        groupId = groupIdForMapping,
                        groupName = groupDataCache[groupIdForMapping]?.name, // Get current name from cache
                        messageText = msg.text,
                        // Provide a default (e.g., 0L or current time if appropriate) if msg.timestamp can be null
                        // and your ViewModel expects a non-null Long.
                        // If ViewModel can handle Long?, then msg.timestamp is fine.
                        // Assuming ViewModel's updateGroupWithMessage expects a non-null Long for timestamp:
                        messageTimestamp = msg.timestamp ?: 0L, // Or handle null appropriately
                        senderName = latestMessageSenderNameForUI,
                        senderId = msg.senderId
                    )

                    // --- START DEBUG LOGS AFTER VIEWMODEL UPDATE ---
                    Log.d(TAG, "HomeActivity: Called broadGroupViewModel.updateGroupWithMessage for group '$groupNameForPath' (ID: $groupIdForMapping)")
                    // --- END DEBUG LOGS AFTER VIEWMODEL UPDATE ---
                }



                if (newMessagesForNotificationFoundInThisSnapshot && latestTimestampInThisSnapshot > lastSeenTimestampForNotifications) {
                    saveLastSeenMessageTimestamp(listenerCompositeKey, latestTimestampInThisSnapshot)
                    lastSeenTimestampForNotifications = latestTimestampInThisSnapshot
                    Log.d(TAG, "Updated lastSeenTimestampForNotifications for $listenerCompositeKey to $lastSeenTimestampForNotifications")
                }

                if (!initialDataProcessed) {
                    if (latestTimestampInThisSnapshot > 0 && latestTimestampInThisSnapshot > lastSeenTimestampForNotifications) {
                        saveLastSeenMessageTimestamp(listenerCompositeKey, latestTimestampInThisSnapshot)
                        lastSeenTimestampForNotifications = latestTimestampInThisSnapshot
                        Log.d(
                            TAG,
                            "Initial data processed for $listenerCompositeKey. Set lastSeenTimestampForNotifications to $lastSeenTimestampForNotifications."
                        )
                    }
                    initialDataProcessed = true
                }
            } // End of onDataChange

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    TAG,
                    "Listener cancelled for messages at path: $firebasePath. Error: ${error.message}"
                )
                // Optionally remove from maps if listener is permanently cancelled
                groupMessageDataListeners.remove(listenerCompositeKey)
                activeFirebasePathsForListeners.remove(listenerCompositeKey)
            }
        }
        groupMessageDataListeners[listenerCompositeKey] = newListener
        activeFirebasePathsForListeners[listenerCompositeKey] = groupMessagesRef // Store ref for removal
        groupMessagesRef.orderByChild("timestamp").limitToLast(50).addValueEventListener(newListener) // Listen to last N messages
        // Consider .limitToLast(1) if you only ever care about the very last message for the list item display
        // and handle unread counts separately. However, for notifications, you might need more.
        // The current ViewModel logic updates based on the latest message in the snapshot.
    }

    //region SharedPreferences for Message Timestamps
    //==============================================================================================
    private fun saveLastSeenMessageTimestamp(listenerKey: String, timestamp: Long) {
        messagePrefs.edit().putLong("ts_$listenerKey", timestamp).apply()
        Log.d(TAG, "Saved last seen timestamp for $listenerKey: $timestamp")
    }

    private fun getLastSeenMessageTimestamp(listenerKey: String): Long {
        val timestamp = messagePrefs.getLong("ts_$listenerKey", 0L)
        Log.d(TAG, "Retrieved last seen timestamp for $listenerKey: $timestamp")
        return timestamp
    }

    //region Listener Cleanup
    //==============================================================================================
    private fun clearAllFirebaseListeners() {
        Log.d(TAG, "Clearing all Firebase listeners.")
        // Clear metadata listener
        channelsMetadataValueListener?.let {
            // Ensure you have the correct reference for removal.
            // If groupsMetadataRef is not a class member, you might need to re-obtain it or store it.
            // For simplicity, assuming 'database' is initialized:
            val groupsMetadataRef = database.getReference("channels") // Or your actual path
            groupsMetadataRef.removeEventListener(it)
            channelsMetadataValueListener = null // Nullify after removal
            Log.d(TAG, "Removed channels metadata listener.")
        }
        // Clear all group message listeners
        clearAllGroupMessageListenersOnly()
    }

    private fun clearAllGroupMessageListenersOnly() {
        Log.d(TAG, "Clearing all ${groupMessageDataListeners.size} group message data listeners.")
        // Iterate over a copy of keys to avoid ConcurrentModificationException if modification happens elsewhere
        val keysToRemove = activeFirebasePathsForListeners.keys.toList()
        keysToRemove.forEach { key ->
            val listener = groupMessageDataListeners.remove(key)
            val ref = activeFirebasePathsForListeners.remove(key)
            listener?.let { ref?.removeEventListener(it) }
        }
        // Ensure maps are definitely empty
        groupMessageDataListeners.clear()
        activeFirebasePathsForListeners.clear()
        Log.d(TAG, "All group message data listeners cleared.")
    }

    //region Menu Handling (Your existing logic, ensure IDs match)
    //==============================================================================================
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)

        // DM Notification Badge Setup
        dmNotificationBellMenuItem = menu?.findItem(R.id.action_notifications)
        // Initial update for DM badge based on ViewModel's current value
       // updateDirectMessageNotificationBadge(unreadCountViewModel.unreadDirectMessagesCount.value ?: 0)

        // Search View Setup
        val searchItem = menu?.findItem(R.id.action_search_dms) // Use your search item ID
        val searchView = searchItem?.actionView as? SearchView

        searchView?.queryHint = getString(R.string.search_groups_hint) // Example: "Search groups..."
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                homeViewModel.setSearchQuery(query)
                searchView.clearFocus() // Optional: hide keyboard
                Log.d(TAG, "Search submitted: $query")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                homeViewModel.setSearchQuery(newText)
                Log.d(TAG, "Search query changed: $newText")
                return true
            }
        })

        // Optional: Handle search view expansion/collapse to clear query
        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                // Optional: Perform actions when search is expanded
                return true // Return true to allow expansion
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                homeViewModel.setSearchQuery(null) // Clear search query when collapsed
                Log.d(TAG, "Search collapsed, query cleared.")
                return true // Return true to allow collapse
            }
        })
        return true
    }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            R.id.action_notifications -> { // This is for DMs
                Toast.makeText(this, " Messages notifications clicked", Toast.LENGTH_SHORT).show()
                val dmTabIndex = tabTitles.indexOf("Direct Messages")
                if (dmTabIndex != -1) {
                    binding.viewPagerHome.currentItem = dmTabIndex
                }
                // unreadCountViewModel.clearUnreadDirectMessages() // If you want to clear on bell click
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    //region Navigation and Logout
    //==============================================================================================
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java) // Ensure LoginActivity is correct
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun performLogout() {
        Log.d(TAG, "Performing logout.")
        auth.signOut()
        clearAllFirebaseListeners() // Important to stop listening to data for the logged-out user
        messagePrefs.edit().clear().apply()
        Log.d(TAG, "Cleared broad_group_message_prefs.")

        broadGroupViewModel.clearAllData() // Clear data from the ViewModel
        navigateToLogin()
    }
    //endregion

    //region Public methods for Fragments or other Activities
    //==============================================================================================

    // Called by BroadGroupFragment (or GroupChatActivity via result/intent extra)
    // when a group chat is opened/viewed.
    fun groupWasOpened(groupId: String) {
        Log.d(TAG, "Group $groupId was opened. Marking as read in ViewModel.")
        broadGroupViewModel.markGroupAsRead(groupId)
        // No need to directly call fragment here, ViewModel observation will handle UI update.
    }

    // Used by BroadGroupFragment in its onResume to know which group to mark as read
    // if HomeActivity was brought to front after a chat was opened.
    fun getCurrentlyOpenBroadGroupId(): String? {
        return currentlyOpenBroadGroupId
    }

    fun clearCurrentlyOpenBroadGroupId() {
        currentlyOpenBroadGroupId = null
    }

    override fun onNavigateToChatRoom(groupId: String, groupName: String?) {
        Log.d("HomeActivity", "Received navigation request to chat room: ID='$groupId', Name='$groupName'")
        if (navController == null) {
            Log.e("HomeActivity", "Cannot navigate: Main NavController is null.")
            Toast.makeText(this, "Error: Could not open chat.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
                putString("groupName", groupName)
            }
            // Replace R.id.chatRoomFragment with your actual destination ID for the chat room
            // or an action ID that leads to it.
            navController?.navigate(R.id.chatRoomFragment, bundle) // Or your specific action ID

            Log.d("HomeActivity", "Navigation to ChatRoomFragment initiated.")

        } catch (e: Exception) { // Catch generic Exception to see any navigation issue
            Log.e("HomeActivity", "Navigation to ChatRoomFragment failed.", e)
            Toast.makeText(this, "Error opening chat room.", Toast.LENGTH_SHORT).show()
        }
    }

}