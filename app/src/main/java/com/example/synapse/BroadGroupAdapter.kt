package com.example.synapse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BroadGroupAdapter(private val onItemClicked: (GroupDisplayInfo) -> Unit) :
    ListAdapter<GroupDisplayInfo, BroadGroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_group_display, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val groupInfo = getItem(position)
        holder.bind(groupInfo)
        holder.itemView.setOnClickListener {
            onItemClicked(groupInfo)
        }
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupNameTextView: TextView = itemView.findViewById(R.id.group_name_text_view)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.last_message_text_view)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestamp_text_view)
        private val unreadCountTextView: TextView = itemView.findViewById(R.id.unread_count_text_view)

        fun bind(groupInfo: GroupDisplayInfo) {
            groupNameTextView.text = groupInfo.name

            val currentUserName = (itemView.context as? HomeActivity)?.auth?.currentUser?.displayName
            val lastMessagePrefix = if (groupInfo.lastMessageSenderName != null &&
                groupInfo.lastMessageSenderName != currentUserName &&
                groupInfo.lastMessageSenderName!!.isNotEmpty()) {
                "${groupInfo.lastMessageSenderName}: "
            } else if (groupInfo.lastMessageSenderName != null && groupInfo.lastMessageSenderName == currentUserName) {
                "You: " // Optional: Prefix with "You: " if current user sent the last message
            }
            else {
                ""
            }
            lastMessageTextView.text = "$lastMessagePrefix${groupInfo.lastMessageText}"

            if (groupInfo.lastMessageTimestamp > 0) {
                timestampTextView.text = formatTimestamp(groupInfo.lastMessageTimestamp)
                timestampTextView.visibility = View.VISIBLE
            } else {
                timestampTextView.visibility = View.GONE
            }

            if (groupInfo.unreadCount > 0) {
                unreadCountTextView.text = groupInfo.unreadCount.toString()
                unreadCountTextView.visibility = View.VISIBLE
            } else {
                unreadCountTextView.visibility = View.GONE
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            return when {
                // Same day: HH:mm
                messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
                // Yesterday: "Yesterday"
                messageDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        messageDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> {
                    itemView.context.getString(R.string.timestamp_yesterday) // Add <string name="timestamp_yesterday">Yesterday</string> to strings.xml
                }
                // Same year, older than yesterday: MMM d (e.g., Apr 12)
                messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
                }
                // Different year: MMM d, yyyy (e.g., Apr 12, 2023)
                else -> {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<GroupDisplayInfo>() {
        override fun areItemsTheSame(oldItem: GroupDisplayInfo, newItem: GroupDisplayInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroupDisplayInfo, newItem: GroupDisplayInfo): Boolean {
            return oldItem == newItem // Relies on GroupDisplayInfo being a data class
        }
    }
}