package com.example.synapse


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager // Ensure you have a LayoutManager
import com.example.synapse.databinding.FragmentBroadGroupBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Data class to represent a channel (optional, but good practice)
data class Channel(val id: String = "", val name: String = "", val description: String = "")

class BroadGroupFragment : Fragment() {

    private var _binding: FragmentBroadGroupBinding? = null // For view binding lifecycle
    private val binding get() = _binding!!

    private lateinit var departmentAdapter: DepartmentAdapter // Use your existing adapter name
    private val departmentsList = mutableListOf<Channel>() // To hold Channel objects

    private val db = FirebaseDatabase.getInstance().reference.child("channels")
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBroadGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchChannels()
    }

    private fun setupRecyclerView() {
        // Pass a lambda that takes a Channel object
        departmentAdapter = DepartmentAdapter(departmentsList) { channel ->
            val intent = Intent(requireContext(), GroupChatActivity::class.java)
            // Pass the channel ID or name to GroupChatActivity
            // The ID is generally better for Firebase paths
            intent.putExtra("group_id", channel.id)
            intent.putExtra("group_name", channel.name) // For display in GroupChatActivity
            startActivity(intent)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()) // Don't forget this!
            adapter = departmentAdapter
        }
    }

    private fun fetchChannels() {
        binding.progressBar.visibility = View.VISIBLE // Optional: Show a progress bar

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBar.visibility = View.GONE // Hide progress bar
                departmentsList.clear()
                if (snapshot.exists()) {
                    for (channelSnapshot in snapshot.children) {
                        // Assuming your channels have a "name" field
                        // And you want to use the key of the channel node as its ID
                        val channelId = channelSnapshot.key
                        val channelName = channelSnapshot.child("name").getValue(String::class.java)
                        // You could also deserialize into a Channel data class if you have more fields
                        // val channel = channelSnapshot.getValue(Channel::class.java)

                        if (channelId != null && channelName != null) {
                            departmentsList.add(Channel(id = channelId, name = channelName))
                        }
                    }
                    if (departmentsList.isEmpty()){
                        binding.emptyViewText.visibility = View.VISIBLE // Show "No channels" text
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyViewText.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                    departmentAdapter.notifyDataSetChanged()
                } else {
                    Log.d("BroadGroupFragment", "No channels found in Firebase")
                    binding.emptyViewText.visibility = View.VISIBLE // Show "No channels" text
                    binding.recyclerView.visibility = View.GONE
                    // Optionally show a message to the user
                    // Toast.makeText(requireContext(), "No channels available.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE // Hide progress bar
                Log.e("BroadGroupFragment", "Firebase database error: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load channels: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        db.addValueEventListener(valueEventListener) // Use addValueEventListener for real-time updates
    }

    override fun onDestroyView() {
        super.onDestroyView()
        db.removeEventListener(valueEventListener) // Important: Remove listener to prevent memory leaks
        _binding = null // Clear binding reference
    }
}
