package com.example.synapse.chats

import android.graphics.Typeface // For setting bold text
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.synapse.R
import com.example.synapse.databinding.ListItemGroupDisplayBinding // Your ViewBinding class
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
class GroupListAdapter(
    private val onGroupClicked: (Group) -> Unit
) : ListAdapter<Group, GroupListAdapter.GroupViewHolder>(GroupDiffCallback()) {

    companion object {
        private const val TAG = "GroupListAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        Log.d(TAG, "onCreateViewHolder called")
        val binding = ListItemGroupDisplayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        Log.d(TAG, "Binding group: ${group.name}, ShowDot: ${group.showUnreadDot}, UnreadCount: ${group.unreadCount}")
        holder.bind(group)
        // It's generally better to set the click listener in onCreateViewHolder
        // to avoid creating new listener objects for every bind.
        // However, if you need the specific 'group' object directly, this is common.
        // For performance with many items, consider passing only the ID or position
        // from bind and having the listener in ViewHolder retrieve the item.
    }

    inner class GroupViewHolder(private val binding: ListItemGroupDisplayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init { // Set item click listener once when ViewHolder is created
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val group = getItem(position)
                    onGroupClicked(group)
                }
            }
        }

        fun bind(group: Group) {
            Log.d(TAG, "Adapter: Binding group ID ${group.id}, Name: ${group.name}, ShowDot: ${group.showUnreadDot}, LastMsgTS: ${group.lastMessage?.timestamp}")
            binding.groupNameTextView.text = group.name
            val lastMessage = group.lastMessage

            // Last Message Text and Style
            if (lastMessage?.text != null) {
                binding.lastMessageTextView.text = lastMessage.text
                binding.lastMessageTextView.visibility = View.VISIBLE

                // BOLDING LOGIC (using showUnreadDot or hasUnreadMessagesFromOthers, choose one or combine)
                // If the dot itself signifies "unread from others", then showUnreadDot is appropriate here.
                if (group.showUnreadDot) { // Or use group.hasUnreadMessagesFromOthers if that's preferred for bolding
                    binding.lastMessageTextView.setTypeface(null, Typeface.BOLD)
                    Log.d(TAG, "Group '${group.name}': Last message is BOLD.")
                } else {
                    binding.lastMessageTextView.setTypeface(null, Typeface.NORMAL)
                    Log.d(TAG, "Group '${group.name}': Last message is NORMAL.")
                }

            } else {
                binding.lastMessageTextView.text = itemView.context.getString(R.string.no_messages_yet)
                binding.lastMessageTextView.visibility = View.VISIBLE // Keep visible for "no messages" text
                binding.lastMessageTextView.setTypeface(null, Typeface.NORMAL) // Ensure normal style
            }

            // Message Type
            if (lastMessage?.messageType != null) {
                val messageTypeString = lastMessage.messageType!! // Safe due to null check
                val formattedMessageType = messageTypeString.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                binding.messageTypeTextView.text = itemView.context.getString(R.string.message_type_prefix, formattedMessageType)
                binding.messageTypeTextView.visibility = View.VISIBLE
            } else {
                binding.messageTypeTextView.visibility = View.GONE
            }

            // Timestamp
            val lastMessageTimestamp = lastMessage?.timestamp
            if (lastMessageTimestamp != null && lastMessageTimestamp > 0) {
                binding.timestampTextView.text = formatTimestamp(lastMessageTimestamp)
                binding.timestampTextView.visibility = View.VISIBLE
            } else {
                binding.timestampTextView.visibility = View.GONE
            }

            // --- UNREAD DOT INDICATOR LOGIC (using group.showUnreadDot) ---
            if (group.showUnreadDot) {
                binding.unreadDotIndicator.visibility = View.VISIBLE
                Log.d(TAG, "Group '${group.name}': Unread dot VISIBLE (showUnreadDot is true).")
            } else {
                binding.unreadDotIndicator.visibility = View.GONE
                Log.d(TAG, "Group '${group.name}': Unread dot GONE (showUnreadDot is false).")
            }
            // --- END UNREAD DOT INDICATOR LOGIC ---

            // Numerical Unread Count
            // Decide how you want the count and dot to interact.
            // Option 1: Show count only if dot is NOT shown.
            // if (group.unreadCount > 0 && !group.showUnreadDot) {
            // Option 2: Show count always if > 0, regardless of dot.
            if (group.unreadCount > 0) {
                binding.unreadCountTextView.text = group.unreadCount.toString()
                binding.unreadCountTextView.visibility = View.VISIBLE
                Log.d(TAG, "Group '${group.name}': Unread count VISIBLE (${group.unreadCount}).")
            } else {
                binding.unreadCountTextView.visibility = View.GONE
                Log.d(TAG, "Group '${group.name}': Unread count GONE.")
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val messageDate = Date(timestamp)
            return when {
                DateUtils.isToday(timestamp) -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(messageDate)
                isYesterday(timestamp) -> itemView.context.getString(R.string.yesterday)
                else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate)
            }
        }

        private fun isYesterday(timestamp: Long): Boolean {
            val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            return yesterdayCalendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) &&
                    yesterdayCalendar.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR)
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            // Check all fields that, if changed, should trigger a UI update for the item.
            val contentsSame = oldItem.name == newItem.name &&
                    // oldItem.description == newItem.description && // Only if displayed
                    oldItem.lastMessage?.text == newItem.lastMessage?.text &&
                    oldItem.lastMessage?.timestamp == newItem.lastMessage?.timestamp &&
// oldItem.last
// In GroupListAdapter.kt (continuing GroupDiffCallback)

                    // oldItem.lastMessage?.senderName == newItem.lastMessage?.senderName && // Only if displayed
                    oldItem.lastMessage?.messageType == newItem.lastMessage?.messageType && // Only if displayed
                    oldItem.unreadCount == newItem.unreadCount && // For the numerical count
                    oldItem.hasUnreadMessagesFromOthers == newItem.hasUnreadMessagesFromOthers && // If used for bolding or other logic
                    oldItem.showUnreadDot == newItem.showUnreadDot // CRUCIAL for the dot indicator

            // Optional: Detailed logging for debugging differences
            if (!contentsSame) {
                Log.d("GroupDiffCallback", "areContentsTheSame: false for Group ID ${oldItem.id} ('${oldItem.name}')")
                if (oldItem.name != newItem.name) {
                    Log.d("GroupDiffCallback", "--> Name changed: '${oldItem.name}' vs '${newItem.name}'")
                }
                if (oldItem.lastMessage?.text != newItem.lastMessage?.text) {
                    Log.d("GroupDiffCallback", "--> LastMessage Text changed: '${oldItem.lastMessage?.text}' vs '${newItem.lastMessage?.text}'")
                }
                if (oldItem.lastMessage?.timestamp != newItem.lastMessage?.timestamp) {
                    Log.d("GroupDiffCallback", "--> LastMessage Timestamp changed: ${oldItem.lastMessage?.timestamp} vs ${newItem.lastMessage?.timestamp}")
                }
                if (oldItem.lastMessage?.messageType != newItem.lastMessage?.messageType) {
                    Log.d("GroupDiffCallback", "--> LastMessage MessageType changed: '${oldItem.lastMessage?.messageType}' vs '${newItem.lastMessage?.messageType}'")
                }
                if (oldItem.unreadCount != newItem.unreadCount) {
                    Log.d("GroupDiffCallback", "--> UnreadCount changed: ${oldItem.unreadCount} vs ${newItem.unreadCount}")
                }
                if (oldItem.hasUnreadMessagesFromOthers != newItem.hasUnreadMessagesFromOthers) {
                    Log.d("GroupDiffCallback", "--> HasUnreadMessagesFromOthers changed: ${oldItem.hasUnreadMessagesFromOthers} vs ${newItem.hasUnreadMessagesFromOthers}")
                }
                if (oldItem.showUnreadDot != newItem.showUnreadDot) {
                    Log.d("GroupDiffCallback", "--> showUnreadDot changed: ${oldItem.showUnreadDot} vs ${newItem.showUnreadDot}")
                }
            }
            return contentsSame
        }
    }
}