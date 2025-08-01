package com.example.synapse

// ... other imports
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.view.MenuItemCompat // For setting action view
import androidx.lifecycle.observe
import com.example.synapse.viewmodel.UnreadCountViewModel

class HomeActivity : AppCompatActivity() {

    // ... (binding, auth, tabTitles, etc.)
    private val unreadCountViewModel: UnreadCountViewModel by viewModels()
    private var notificationBellMenuItem: MenuItem? = null // To hold reference to the menu item

    override fun onCreate(savedInstanceState: Bundle?) {
        // ... (super.onCreate, binding, auth, toolbar setup, ViewPager setup)

        // Observe unread count changes
        unreadCountViewModel.unreadDirectMessagesCount.observe(this) { count ->
            updateNotificationIconBadge(count)
        }

        // TODO: Initial fetch of unread count
        // unreadCountViewModel.setUnreadDirectMessagesCount(yourFetchedInitialCount)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu) // Assuming your logout, search, notification are here

        // --- Get reference to the notification menu item ---
        notificationBellMenuItem = menu?.findItem(R.id.action_notifications) // **Use your actual menu item ID**

        // --- Setup initial badge state ---
        // We need to wait for the menu to be created before setting the action view
        // So, we re-trigger an update if the LiveData already has a value
        unreadCountViewModel.unreadDirectMessagesCount.value?.let {
            updateNotificationIconBadge(it)
        }
        return true
    }

    private fun updateNotificationIconBadge(count: Int) {
        notificationBellMenuItem?.let { menuItem ->
            if (count > 0) {
                // Option 1: Set a custom action view with a badge
                menuItem.setActionView(R.layout.menu_item_notification_badge) // Create this layout
                val badgeTextView = menuItem.actionView?.findViewById<TextView>(R.id.notification_badge_text)
                badgeTextView?.text = count.toString()
                // Make sure the original icon is still visible or part of your custom layout
                // This custom view will replace the icon, so design it carefully.

                // Option 2: (Simpler, if your icon can be layered or you have different icon states)
                // menuItem.setIcon(R.drawable.ic_notifications_active) // A different drawable with a dot
            } else {
                // Option 1 Reset:
                menuItem.actionView = null // Remove custom view
                // menuItem.setIcon(R.drawable.ic_notifications_default) // Set back to default icon if using Option 1 and it was replaced

                // Option 2 Reset:
                // menuItem.setIcon(R.drawable.ic_notifications_default)
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            R.id.action_notifications -> { // **Use your actual menu item ID**
                // TODO: Navigate to a screen showing all notifications or handle click
                Toast.makeText(this, "Notifications icon clicked", Toast.LENGTH_SHORT).show()
                // Potentially, clicking this could take you to the Direct Messages tab
                // and clear the badge if that's the desired UX.
                // val dmTabIndex = tabTitles.indexOf("Direct Messages")
                // if (dmTabIndex != -1) {
                //    binding.viewPagerHome.currentItem = dmTabIndex
                //    unreadCountViewModel.clearUnreadDirectMessages() // Example: clear when icon is clicked
                // }
                true
            }
            R.id.action_search_dms -> { // **Use your actual menu item ID for DM search**
                // This will be handled by the DirectMessageFragment itself
                // We might not need to do anything here in HomeActivity if the search
                // icon is only relevant when the DirectMessageFragment is visible.
                // If it's a global search, then handle here.
                // For now, assume it's for DMs and will be handled by the fragment.
                Toast.makeText(this, "Search icon clicked (HomeActivity)", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // ... (performLogout, etc.)
}