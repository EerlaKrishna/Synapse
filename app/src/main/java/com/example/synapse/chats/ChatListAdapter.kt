package com.example.synapse.chats // Or your actual adapter package

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.synapse.R // For colors
import com.example.synapse.chats.ChatListItem // Your ChatListItem model
import com.example.synapse.databinding.ItemChatListBinding // Your item layout binding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Define a click listener interface
interface OnChatClickListener {
    fun onChatClicked(chatListItem: ChatListItem)
}

class ChatListAdapter(private val onChatClickListener: OnChatClickListener) :
    ListAdapter<ChatListItem, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val TAG = "ChatListAdapter"
    }

    // Formatter for the timestamp - consider making this more sophisticated
    // to show "Yesterday", day of the week, or date for older messages.
    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val messageDate = Date(timestamp)
        val currentDate = Date()

        val diffMillis = currentDate.time - messageDate.time
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffDays == 0L -> { // Today
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(messageDate)
            }
            diffDays == 1L -> "Yesterday" // Yesterday
            diffDays < 7L -> SimpleDateFormat("EEE", Locale.getDefault()).format(messageDate) // Day of week
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(messageDate) // Date like "Jan 5"
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = getItem(position)
        // Log.d(TAG, "Binding item at position $position: Group='${item.groupName}', Unread='${item.unreadCount}'") // Optional: for verbose logging
        holder.bind(item)
    }

    inner class ChatViewHolder(private val binding: ItemChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition // Use bindingAdapterPosition for safety
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = getItem(position)
                    // Log.d(TAG, "Item clicked: ${clickedItem.groupName}") // Optional log
                    onChatClickListener.onChatClicked(clickedItem)
                }
            }
        }

        fun bind(item: ChatListItem) {
            binding.textViewChatName.text = item.groupName
            binding.textViewLastMessage.text = item.lastMessageText ?: "No messages yet" // Fallback

            // Timestamp
            if (item.lastMessageTimestamp > 0L) {
                binding.textViewTimestamp.text = formatTimestamp(item.lastMessageTimestamp)
                binding.textViewTimestamp.visibility = View.VISIBLE
            } else {
                binding.textViewTimestamp.visibility = View.INVISIBLE // Or GONE, depending on layout
            }

            // Unread count badge
            if (item.unreadCount > 0) {
                binding.textViewUnreadCount.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()
                binding.textViewUnreadCount.visibility = View.VISIBLE
                // Make last message bold and change text color if unread
                binding.textViewLastMessage.setTypeface(null, Typeface.BOLD)
                binding.textViewLastMessage.setTextColor(ContextCompat.getColor(binding.root.context, R.color.unread_message_text_color)) // Define this color
                binding.textViewChatName.setTypeface(null, Typeface.BOLD) // Optionally make chat name bold too
            } else {
                binding.textViewUnreadCount.visibility = View.GONE
                // Reset to normal style if read
                binding.textViewLastMessage.setTypeface(null, Typeface.NORMAL)
                binding.textViewLastMessage.setTextColor(ContextCompat.getColor(binding.root.context, R.color.read_message_text_color)) // Define this color
                binding.textViewChatName.setTypeface(null, Typeface.NORMAL)
            }

            // TODO: Add logic for group image/icon if you have one (e.g., using Glide or Coil)
            // binding.imageViewChatIcon.setImage...
        }
    }

    // DiffUtil.ItemCallback implementation
    class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            // Items are the same if their IDs are the same
            return oldItem.groupId == newItem.groupId
        }

        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            // Contents are the same if all relevant displayed fields are the same
            // This is CRUCIAL for DiffUtil to know when to rebind a ViewHolder
            return oldItem.groupName == newItem.groupName &&
                    oldItem.lastMessageText == newItem.lastMessageText &&
                    oldItem.lastMessageTimestamp == newItem.lastMessageTimestamp &&
                    oldItem.unreadCount == newItem.unreadCount &&
                    oldItem.lastMessageSenderName == newItem.lastMessageSenderName
            // Add any other fields that affect the UI of the list item
        }
    }
}