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
// Removed unused kotlin.text imports for clarity, add back if needed elsewhere

class GroupListAdapter(
    private val onGroupClicked: (Group) -> Unit
) : ListAdapter<Group, GroupListAdapter.GroupViewHolder>(GroupDiffCallback()) {

    companion object { // Good practice for TAG
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
        Log.d(TAG, "Binding group: ${group.name}, UnreadOthers: ${group.hasUnreadMessagesFromOthers}, UnreadCount: ${group.unreadCount}")
        holder.bind(group)
        holder.itemView.setOnClickListener { onGroupClicked(group) }
    }

    inner class GroupViewHolder(private val binding: ListItemGroupDisplayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Group) {
            Log.d(TAG, "Adapter: Binding group ID ${group.id}, Name: ${group.name}, LastMsgText: '${group.lastMessage?.text}', LastMsgTS: ${group.lastMessage?.timestamp}, UnreadOthers: ${group.hasUnreadMessagesFromOthers}") // DETAILED LOG
            binding.groupNameTextView.text = group.name
            val lastMessage = group.lastMessage

            // Last Message Text and Style
            if (lastMessage?.text != null) {
                binding.lastMessageTextView.text = lastMessage.text // Consider adding sender name if needed
                binding.lastMessageTextView.visibility = View.VISIBLE

                // --- NEW: BOLDING LOGIC ---
                if (group.hasUnreadMessagesFromOthers) {
                    binding.lastMessageTextView.setTypeface(null, Typeface.BOLD)
                    Log.d(TAG, "Group '${group.name}': Last message is BOLD.")
                } else {
                    binding.lastMessageTextView.setTypeface(null, Typeface.NORMAL)
                    Log.d(TAG, "Group '${group.name}': Last message is NORMAL.")
                }
                // --- END NEW: BOLDING LOGIC ---

            } else {
                binding.lastMessageTextView.text = itemView.context.getString(R.string.no_messages_yet)
                binding.lastMessageTextView.visibility = View.VISIBLE
                binding.lastMessageTextView.setTypeface(null, Typeface.NORMAL) // Ensure normal style
            }

            // Message Type
            if (lastMessage?.messageType != null) {
                val messageTypeString = lastMessage.messageType!!
                val formattedMessageType = messageTypeString.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                binding.messageTypeTextView.text = itemView.context.getString(R.string.message_type_prefix, formattedMessageType)
                binding.messageTypeTextView.visibility = View.VISIBLE
            } else {
                binding.messageTypeTextView.visibility = View.GONE
            }

            // Timestamp
            val lastMessageTimestamp = lastMessage?.timestamp // Assuming timestamp is Long?
            if (lastMessageTimestamp != null && lastMessageTimestamp > 0) {
                binding.timestampTextView.text = formatTimestamp(lastMessageTimestamp)
                binding.timestampTextView.visibility = View.VISIBLE
            } else {
                binding.timestampTextView.visibility = View.GONE
            }

            // --- UNREAD DOT INDICATOR LOGIC ---
            if (group.hasUnreadMessagesFromOthers) {
                binding.unreadDotIndicator.visibility = View.VISIBLE
                // Optionally hide the numerical unread count if the dot is shown
                // binding.unreadCountTextView.visibility = View.GONE
                Log.d(TAG, "Group '${group.name}': Unread dot VISIBLE.")
            } else {
                binding.unreadDotIndicator.visibility = View.GONE
                Log.d(TAG, "Group '${group.name}': Unread dot GONE.")
            }
            // --- END UNREAD DOT INDICATOR LOGIC ---

            // Numerical Unread Count (you might adjust visibility based on the dot)
            if (group.unreadCount > 0 && !group.hasUnreadMessagesFromOthers) { // Only show if dot isn't shown
                binding.unreadCountTextView.text = group.unreadCount.toString()
                binding.unreadCountTextView.visibility = View.VISIBLE
            } else if (group.unreadCount > 0 && group.hasUnreadMessagesFromOthers) {
                // If you want to show BOTH dot and count:
                binding.unreadCountTextView.text = group.unreadCount.toString()
                binding.unreadCountTextView.visibility = View.VISIBLE
                // If you want to HIDE count when dot is visible (as per previous comment):
                // binding.unreadCountTextView.visibility = View.GONE
            }
            else {
                binding.unreadCountTextView.visibility = View.GONE
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
            // Id is the unique identifier for a group item.
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            // Check all fields that, if changed, should trigger a UI update for the item.
            val contentsSame = oldItem.name == newItem.name &&
                    oldItem.description == newItem.description && // If description is displayed or affects layout
                    oldItem.lastMessage?.text == newItem.lastMessage?.text &&
                    oldItem.lastMessage?.timestamp == newItem.lastMessage?.timestamp &&
                    oldItem.lastMessage?.senderName == newItem.lastMessage?.senderName && // If sender name is displayed
                    oldItem.lastMessage?.messageType == newItem.lastMessage?.messageType && // If message type is displayed
                    oldItem.unreadCount == newItem.unreadCount &&
                    oldItem.hasUnreadMessagesFromOthers == newItem.hasUnreadMessagesFromOthers // Crucial for dot and bolding

            // Optional: Detailed logging for debugging differences
            if (!contentsSame) {
                Log.d("GroupDiffCallback", "areContentsTheSame: false for Group ID ${oldItem.id}")
                if (oldItem.name != newItem.name) {
                    Log.d("GroupDiffCallback", "--> Name changed: '${oldItem.name}' vs '${newItem.name}'")
                }
                if (oldItem.description != newItem.description) {
                    Log.d("GroupDiffCallback", "--> Description changed: '${oldItem.description}' vs '${newItem.description}'")
                }
                if (oldItem.lastMessage?.text != newItem.lastMessage?.text) {
                    Log.d("GroupDiffCallback", "--> LastMessage Text changed: '${oldItem.lastMessage?.text}' vs '${newItem.lastMessage?.text}'")
                }
                if (oldItem.lastMessage?.timestamp != newItem.lastMessage?.timestamp) {
                    Log.d("GroupDiffCallback", "--> LastMessage Timestamp changed: ${oldItem.lastMessage?.timestamp} vs ${newItem.lastMessage?.timestamp}")
                }
                if (oldItem.lastMessage?.senderName != newItem.lastMessage?.senderName) {
                    Log.d("GroupDiffCallback", "--> LastMessage SenderName changed: '${oldItem.lastMessage?.senderName}' vs '${newItem.lastMessage?.senderName}'")
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
            }
            return contentsSame
        }
    }
} // This closes the GroupListAdapter class