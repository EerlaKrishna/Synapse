package com.example.synapse

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit // Import TimeUnit

// Define view type constants
private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2

class MessageAdapter(
    private val currentUserId: String // Pass current user's ID
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val RECENT_THRESHOLD_MINUTES = 15L // 15 minutes
    }

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

        fun bind(message: Message, viewType: Int) {
            messageText.text = message.text
            messageTimestamp.text = message.timestamp?.let { formatTimestamp(it) }

            if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                // Logic for bolding recent received messages
                val messageTime = message.timestamp ?: 0L
                val currentTime = System.currentTimeMillis()
                val fifteenMinutesInMillis = TimeUnit.MINUTES.toMillis(RECENT_THRESHOLD_MINUTES)

                if (messageTime > 0 && (currentTime - messageTime) < fifteenMinutesInMillis) {
                    // Message is recent and received
                    messageText.setTypeface(null, Typeface.BOLD)
                    // Optionally, make sender name and timestamp bold too if desired
                    // messageSender?.setTypeface(null, Typeface.BOLD)
                    // messageTimestamp.setTypeface(null, Typeface.BOLD)
                } else {
                    // Message is older or not received (or timestamp invalid), ensure normal type
                    messageText.setTypeface(null, Typeface.NORMAL)
                    // messageSender?.setTypeface(null, Typeface.NORMAL)
                    // messageTimestamp.setTypeface(null, Typeface.NORMAL)
                }

            } else { // VIEW_TYPE_MESSAGE_SENT
                // Sent messages are not made bold based on this logic, ensure normal type
                messageText.setTypeface(null, Typeface.NORMAL)
                // messageTimestamp.setTypeface(null, Typeface.NORMAL)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            return try {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                val date = Date(timestamp)
                sdf.format(date)
            } catch (e: Exception) {
                "just now" // Fallback
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            // If you want the item to rebind when its "bold" status might change
            // due to time passing (even if content is same), you might need more complex logic here
            // or rely on the periodic refresh mechanism in the Fragment/Activity.
            // For now, default comparison is fine, onBindViewHolder handles the styling.
            return oldItem == newItem
        }
    }
}