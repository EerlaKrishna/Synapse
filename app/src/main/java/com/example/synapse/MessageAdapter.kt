package com.example.synapse


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synapse.databinding.ItemChatMessageBinding // You'll need to create this layout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private var messages: List<Message>) : // Made messages var to allow updates
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.binding.messageContentText.text = message.content
        // Format the timestamp (optional)
        holder.binding.messageTimestampText.text = formatTimestamp(message.timestamp)
        // IMPORTANT: DO NOT DISPLAY message.sessionId
    }

    override fun getItemCount(): Int = messages.size

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val date = Date(timestamp)
            sdf.format(date)
        } catch (e: Exception) {
            "just now" // Fallback
        }
    }

    // Function to update the list of messages
    fun updateMessages(newMessages: List<Message>) {
        this.messages = newMessages
        notifyDataSetChanged() // Consider using DiffUtil for better performance
    }
}