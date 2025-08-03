
package com.example.synapse.chats // Or your adapter package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.synapse.databinding.ItemMessageSentBinding // Assuming you have this
// import com.example.synapse.databinding.ItemMessageReceivedBinding // And this for received messages
import com.example.synapse.Message // Create this data class
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Create a simple Message data class (e.g., in a 'models' package)
// package com.example.synapse.models
// data class Message(
//    val id: String = "",
//    val text: String? = null,
//    val senderId: String? = null,
//    val timestamp: Long? = null
//    // You might add isSentByCurrentUser in the ViewModel or when observing
// )

class MessageListAdapter(private val currentUserId: String) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                // TODO: Create ItemMessageReceivedBinding and ReceivedMessageViewHolder
                // val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
                // ReceivedMessageViewHolder(binding)

                // For now, as a placeholder if ItemMessageReceivedBinding doesn't exist,
                // we'll reuse the sent layout. This is NOT ideal for a real chat app.
                // You should create a distinct layout and ViewHolder for received messages.
                val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
                // You'd ideally adjust alignment or background for received messages here or in the ViewHolder
                SentMessageViewHolder(binding) // Replace with ReceivedMessageViewHolder and its binding
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            // is ReceivedMessageViewHolder -> holder.bind(message) // Uncomment when you have this
        }
    }

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return ""
        // Example: "10:30 AM"
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.textViewMessageBody.text = message.text
            binding.textViewMessageTimestamp.text = formatTimestamp(message.timestamp)
        }
    }

    // TODO: Create ReceivedMessageViewHolder
    // inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
    //     fun bind(message: Message) {
    //         binding.textViewMessageBody.text = message.text // Assuming ID is the same
    //         binding.textViewMessageTimestamp.text = formatTimestamp(message.timestamp) // Assuming ID is the same
    //     }
    // }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}