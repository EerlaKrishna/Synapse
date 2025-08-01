package com.example.synapse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.synapse.databinding.ActivityHomeBinding // Import the generated binding class
import com.google.android.material.tabs.TabLayoutMediator // Import for TabLayout
import com.google.firebase.auth.FirebaseAuth // Import for logout

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding // Declare binding variable
    private lateinit var auth: FirebaseAuth // Declare Firebase Auth

    // Define your tab titles - make sure this matches the number of fragments in HomeViewPagerAdapter
    private val tabTitles = arrayOf("Broad Groups", "Direct Messages")

    private val unreadCountViewModel: UnreadCountViewModel by viewModels()
    private var notificationBellMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater) // Inflate the layout
        setContentView(binding.root) // Set the content view

        auth = FirebaseAuth.getInstance() // Initialize Firebase Auth

        // Toolbar setup
        setSupportActionBar(binding.toolbarHome) // Set the toolbar
        supportActionBar?.title = "Synapse" // Optional: Set a title for the Toolbar

        // ViewPager and TabLayout setup
        val adapter = HomeViewPagerAdapter(this) // 'this' is the FragmentActivity
        binding.viewPagerHome.adapter = adapter
        TabLayoutMediator(binding.tabLayoutHome, binding.viewPagerHome) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // Observe unread count changes
        unreadCountViewModel.unreadDirectMessagesCount.observe(this) { count ->
            updateNotificationIconBadge(count)
        }

        // TODO: Initial fetch of unread count from your data source
        // This should happen after your user is authenticated and you have access to their data.
        // For example, if you fetch this from a repository:
        // unreadCountViewModel.loadInitialCounts(auth.currentUser?.uid)
        // Or if you get it directly:
        // val initialCount = getInitialUnreadCountFromSomewhere()
        // unreadCountViewModel.setUnreadDirectMessagesCount(initialCount)
        Log.d("HomeActivity", "onCreate: ViewModel observation setup and UI initialized.")
        // Simulate initial fetch for testing if you don't have a data source yet
        // unreadCountViewModel.setUnreadDirectMessagesCount(3) // Example initial count
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        notificationBellMenuItem = menu?.findItem(R.id.action_notifications)
        // Observe the value directly here as well, in case it's already set when menu is created
        // This ensures the badge is updated if the menu is recreated (e.g., on configuration change)
        unreadCountViewModel.unreadDirectMessagesCount.value?.let {
            updateNotificationIconBadge(it)
        }
        return true
    }

    private fun updateNotificationIconBadge(count: Int) {
        notificationBellMenuItem?.let { menuItem ->
            if (count > 0) {
                menuItem.setActionView(R.layout.menu_item_notification_badge)
                val badgeTextView = menuItem.actionView?.findViewById<TextView>(R.id.notification_badge_text)
                badgeTextView?.text = count.toString()
                // If your menu_item_notification_badge is just the TextView and doesn't include the icon,
                // you might need to ensure the original icon is still visible or set it programmatically.
                // For example, if your R.id.action_notifications in home_menu.xml has an icon:
                // menuItem.setIcon(R.drawable.ic_notifications_default) // or your specific notification icon
            } else {
                menuItem.actionView = null
                // Optionally reset the icon if it was changed or part of the action view
                // menuItem.setIcon(R.drawable.ic_notifications_default)
            }
        }
        Log.d("HomeActivity", "updateNotificationIconBadge called with count: $count")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            R.id.action_notifications -> {
                Toast.makeText(this, "Notifications icon clicked", Toast.LENGTH_SHORT).show()
                // Example: Navigate to the Direct Messages tab when notifications are clicked
                val dmTabIndex = tabTitles.indexOf("Direct Messages")
                if (dmTabIndex != -1) { // Check if "Direct Messages" tab exists
                    binding.viewPagerHome.currentItem = dmTabIndex
                }
                // Optionally, clear the unread count when the user views notifications
                // unreadCountViewModel.clearUnreadDirectMessages()
                true
            }
            R.id.action_search_dms -> {
                // This action is specific to Direct Messages.
                // It's often better handled by the DirectMessageFragment itself,
                // especially if the search UI (e.g., SearchView) is part of that fragment's toolbar contribution.
                // If it's a global search trigger, you can handle it here.
                Toast.makeText(this, "Search icon clicked (HomeActivity - consider if fragment should handle)", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performLogout() {
        auth.signOut() // Sign out from Firebase
        // Clear any local session data if you have more than just SharedPreferences for sessionId
        getSharedPreferences("session", MODE_PRIVATE).edit().remove("sessionId").apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
        startActivity(intent)
        finish() // Finish HomeActivity
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}