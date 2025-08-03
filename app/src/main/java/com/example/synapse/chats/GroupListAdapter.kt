package com.example.synapse.chats

// import android.icu.util.Calendar // Using java.util.Calendar
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.synapse.R
import com.example.synapse.databinding.ListItemGroupDisplayBinding
import java.text.SimpleDateFormat
import java.util.Calendar // Using java.util.Calendar
import java.util.Date     // Using java.util.Date
import java.util.Locale   // Using java.util.Locale


class GroupListAdapter(
    private val onGroupClicked: (Group) -> Unit
) : ListAdapter<Group, GroupListAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ListItemGroupDisplayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding) // Pass binding to ViewHolder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        holder.bind(group) // Call bind on the holder instance
        holder.itemView.setOnClickListener { onGroupClicked(group) }
    }

    // In GroupListAdapter.kt

// ... (other parts of GroupListAdapter like onCreateViewHolder, onBindViewHolder) ...

    inner class GroupViewHolder(private val binding: ListItemGroupDisplayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Group) {
            binding.groupNameTextView.text = group.name

            // --- MODIFIED SECTION START ---
            if (group.lastMessage?.text != null) { // Access via group.lastMessage?.text
                binding.lastMessageTextView.text = group.lastMessage!!.text // Use !! if you're sure it's not null after check, or ?. also
                binding.lastMessageTextView.visibility = View.VISIBLE
            } else {
                binding.lastMessageTextView.text = itemView.context.getString(R.string.no_messages_yet)
                binding.lastMessageTextView.visibility = View.VISIBLE // Or GONE, depending on desired behavior
            }

            // Access via group.lastMessage?.timestamp
            val lastMessageTimestamp = group.lastMessage?.timestamp
            if (lastMessageTimestamp != null && lastMessageTimestamp > 0) {
                binding.timestampTextView.text = formatTimestamp(lastMessageTimestamp) // Pass the Long value
                binding.timestampTextView.visibility = View.VISIBLE
            } else {
                binding.timestampTextView.visibility = View.GONE
            }
            // --- MODIFIED SECTION END ---

            if (group.unreadCount > 0) {
                binding.unreadCountTextView.text = group.unreadCount.toString()
                binding.unreadCountTextView.visibility = View.VISIBLE
            } else {
                binding.unreadCountTextView.visibility = View.GONE
            }

            // Optional: Display sender name if you have a TextView for it
            // group.lastMessage?.senderName?.let { senderName ->
            //     binding.senderNameTextView.text = senderName
            //     binding.senderNameTextView.visibility = View.VISIBLE
            // } ?: run {
            //     binding.senderNameTextView.visibility = View.GONE
            // }
        }

        // formatTimestamp and isYesterday methods remain the same
        private fun formatTimestamp(timestamp: Long): String {
            val messageDate = Date(timestamp)

            return when {
                DateUtils.isToday(timestamp) -> {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(messageDate)
                }
                isYesterday(timestamp) -> {
                    itemView.context.getString(R.string.yesterday)
                }
                else -> {
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate)
                }
            }
        }

        private fun isYesterday(timestamp: Long): Boolean {
            val yesterdayCalendar = Calendar.getInstance()
            yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1)

            val messageCalendar = Calendar.getInstance()
            messageCalendar.timeInMillis = timestamp

            return yesterdayCalendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) &&
                    yesterdayCalendar.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR)
        }
    }

    // GroupDiffCallback is already updated correctly in your provided code.
    class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        // ... (your correctly updated areItemsTheSame and areContentsTheSame)
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            val contentsSame = oldItem.name == newItem.name &&
                    oldItem.description == newItem.description &&
                    oldItem.lastMessage?.text == newItem.lastMessage?.text &&
                    oldItem.lastMessage?.timestamp == newItem.lastMessage?.timestamp &&
                    oldItem.lastMessage?.senderName == newItem.lastMessage?.senderName &&
                    oldItem.unreadCount == newItem.unreadCount

            if (!contentsSame) {
                Log.d("GroupDiffCallback", "areContentsTheSame: false for ID ${oldItem.id}")
                // ... (your detailed logging) ...
            }
            return contentsSame
        }
    }
}