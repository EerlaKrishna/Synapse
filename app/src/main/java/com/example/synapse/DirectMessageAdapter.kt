package com.example.synapse.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.synapse.Conversation
import com.example.synapse.R // Import your R file

class DirectMessageAdapter(
    private var conversations: List<Conversation>,
    private val onItemClicked: (Conversation) -> Unit
) : RecyclerView.Adapter<DirectMessageAdapter.ConversationViewHolder>() {

    // Consider using DiffUtil for better performance with list updates
    fun updateData(newConversations: List<Conversation>) {
        conversations = newConversations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_direct_message_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.bind(conversation)
        holder.itemView.setOnClickListener { onItemClicked(conversation) }
    }

    override fun getItemCount(): Int = conversations.size

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.textViewLastMessageSnippet)
        private val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)
        private val unreadIndicator: View = itemView.findViewById(R.id.viewUnreadIndicator)
        private val avatarImageView: ImageView = itemView.findViewById(R.id.imageViewAvatar)

        fun bind(conversation: Conversation) {
            userNameTextView.text = conversation.participantsDisplayNames.joinToString(", ")
            lastMessageTextView.text = conversation.lastMessageSnippet
            timestampTextView.text = formatTimestamp(conversation.timestamp)

            if (conversation.isUnread) {
                unreadIndicator.visibility = View.VISIBLE
            } else {
                unreadIndicator.visibility = View.GONE
            }

            // Example using Glide (add Glide dependency if you use it)
            // Glide.with(itemView.context)
            //    .load(conversation.avatarUrl)
            //    .placeholder(R.drawable.ic_default_avatar) // Ensure you have this drawable
            //    .error(R.drawable.ic_default_avatar)      // Optional error placeholder
            //    .circleCrop() // Optional: for circular avatars
            //    .into(avatarImageView)

            // If no image loading library, you might set a default or hide it
            if (conversation.avatarUrl == null) {
                avatarImageView.setImageResource(R.drawable.ic_default_avatar)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            // A more robust date/time formatting is recommended for production
            return android.text.format.DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                android.text.format.DateUtils.MINUTE_IN_MILLIS,
                android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }
    }
}