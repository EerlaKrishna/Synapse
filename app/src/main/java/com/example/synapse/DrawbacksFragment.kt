package com.example.synapse

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synapse.databinding.FragmentDrawbacksBinding // Create this layout
import com.google.firebase.database.*

class DrawbacksFragment : Fragment() {

    private var _binding: FragmentDrawbacksBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter
    private val drawbackMessagesList = mutableListOf<Message>()
    private var groupId: String? = null

    private lateinit var dbRefDrawbacks: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): DrawbacksFragment {
            val fragment = DrawbacksFragment()
            val args = Bundle()
            args.putString(ARG_GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawbacksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        if (groupId == null) {
            Log.e("DrawbacksFragment", "Group ID is null. Cannot fetch messages.")
            binding.emptyViewTextDrawbacks.text = "Error: Group not specified."
            binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
            binding.progressBarDrawbacks.visibility = View.GONE
            return
        }

        dbRefDrawbacks = FirebaseDatabase.getInstance().getReference("messages")
            .child(groupId!!)
            .child("drawback_messages") // Key difference

        fetchDrawbackMessages()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(drawbackMessagesList)
        binding.recyclerViewDrawbacks.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun fetchDrawbackMessages() {
        binding.progressBarDrawbacks.visibility = View.VISIBLE
        binding.emptyViewTextDrawbacks.visibility = View.GONE

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarDrawbacks.visibility = View.GONE
                drawbackMessagesList.clear()
                if (snapshot.exists()) {
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        message?.let { drawbackMessagesList.add(it) }
                    }
                    if (drawbackMessagesList.isEmpty()) {
                        binding.emptyViewTextDrawbacks.text = "No drawbacks submitted yet."
                        binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
                    } else {
                        binding.emptyViewTextDrawbacks.visibility = View.GONE
                    }
                } else {
                    binding.emptyViewTextDrawbacks.text = "No drawbacks submitted yet."
                    binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
                }
                messageAdapter.updateMessages(drawbackMessagesList)
                if (drawbackMessagesList.isNotEmpty() && (binding.recyclerViewDrawbacks.layoutManager as LinearLayoutManager).reverseLayout) {
                    binding.recyclerViewDrawbacks.scrollToPosition(0)
                } else if (drawbackMessagesList.isNotEmpty()){
                    binding.recyclerViewDrawbacks.scrollToPosition(drawbackMessagesList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarDrawbacks.visibility = View.GONE
                Log.e("DrawbacksFragment", "Firebase error: ${error.message}")
                binding.emptyViewTextDrawbacks.text = "Failed to load drawbacks."
                binding.emptyViewTextDrawbacks.visibility = View.VISIBLE
            }
        }
        dbRefDrawbacks.orderByChild("timestamp").addValueEventListener(valueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbRefDrawbacks.isInitialized && ::valueEventListener.isInitialized) {
            dbRefDrawbacks.removeEventListener(valueEventListener)
        }
        _binding = null
    }
}