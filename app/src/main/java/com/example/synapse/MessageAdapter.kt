package com.example.synapse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// import com.example.synapse.databinding.ItemChatMessageBinding // Not used anymore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Define view type constants
private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2

class MessageAdapter(
    private val currentUserId: String // Pass current user's ID
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) { // Pass the callback instance

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            layoutInflater.inflate(R.layout.list_item_message_sent, parent, false)
        } else { // VIEW_TYPE_MESSAGE_RECEIVED
            layoutInflater.inflate(R.layout.list_item_message_received, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message, getItemViewType(position))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textView_message_text)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.textView_message_timestamp)
        private val messageSender: TextView? = itemView.findViewById(R.id.textView_message_sender)

        fun bind(message: Message, viewType: Int) {
            messageText.text = message.text // message.text is already String?
            // Use the timestamp directly as it's Long, not Long? in your Message class
            messageTimestamp.text = message.timestamp?.let { formatTimestamp(it) }


            if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                messageSender?.text = message.senderName
                messageSender?.visibility = View.VISIBLE
            } else { // VIEW_TYPE_MESSAGE_SENT
                messageSender?.visibility = View.GONE
            }
        }

        private fun formatTimestamp(timestamp: Long): String { // Changed to Long from Long?
            // if (timestamp == null) return "just now" // No longer needed if timestamp is not nullable
            return try {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                val date = Date(timestamp)
                sdf.format(date)
            } catch (e: Exception) {
                "just now" // Fallback
            }
        }
    }

    // Using ListAdapter's DiffUtil for better performance
    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            // Use the 'id' field from your Message class
            return oldItem.id == newItem.id // <--- CHANGED FROM messageId to id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}