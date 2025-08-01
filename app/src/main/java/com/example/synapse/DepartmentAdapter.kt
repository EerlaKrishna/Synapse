package com.example.synapse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synapse.databinding.ItemDepartmentBinding // Assuming you use ViewBinding

// DepartmentAdapter now takes a list of Channel objects
class DepartmentAdapter(
    private val items: List<Channel>, // Changed from List<String>
    private val onClick: (Channel) -> Unit // Changed from (String) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDepartmentBinding) : RecyclerView.ViewHolder(binding.root)
    // No need to findViewByID if ItemDepartmentBinding has 'departmentName'

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDepartmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = items[position]
        holder.binding.departmentName.text = channel.name // Display the channel name
        holder.itemView.setOnClickListener { onClick(channel) } // Pass the whole Channel object
    }
}