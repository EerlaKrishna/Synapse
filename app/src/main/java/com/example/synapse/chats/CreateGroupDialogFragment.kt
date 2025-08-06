package com.example.synapse.chats

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.activity
import com.example.synapse.R // Make sure this import is correct
import com.google.firebase.auth.FirebaseAuth

class CreateGroupDialogFragment : DialogFragment() {

    private val broadGroupViewModel: BroadGroupViewModel by activityViewModels()
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val TAG = "CreateGroupDialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { currentActivity ->
            val builder = AlertDialog.Builder(currentActivity)
            val inflater = currentActivity.layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_create_group, null) // Your provided layout

            val groupNameEditText = dialogView.findViewById<EditText>(R.id.edit_text_group_name_dialog)

            builder.setView(dialogView)
                .setTitle(getString(R.string.create_new_group_dialog_title))
                .setPositiveButton(getString(R.string.create_button_text)) { _, _ ->
                    val groupName = groupNameEditText.text.toString().trim()

                    if (groupName.isNotEmpty()) {
                        val currentUser = firebaseAuth.currentUser
                        if (currentUser != null) {
                            val creatorId = currentUser.uid
                            Log.d(TAG, "Attempting to create group. Name: '$groupName', CreatorID: '$creatorId'")

                            // --- Member Selection Logic (Placeholder) ---
                            // TODO: This layout (dialog_create_group.xml) does not yet have UI
                            //       for selecting members. You will need to add UI elements
                            //       (e.g., RecyclerView, ChipGroup) to the XML and then
                            //       implement the logic here to retrieve selected member UIDs.
                            val selectedMemberIds = emptyList<String>() // Placeholder!
                            Log.w(TAG, "No member selection UI in dialog_create_group.xml. Passing empty list for memberIds.")
                            // --- End Member Selection Logic ---

                            broadGroupViewModel.createNewGroup(
                                groupName = groupName,
                                creatorId = creatorId,
                                description = null, // No description field in the current XML
                                memberIdsFromDialog = selectedMemberIds
                            )
                        } else {
                            Log.e(TAG, "User not authenticated. Cannot create group.")
                            Toast.makeText(context, getString(R.string.error_user_not_authenticated), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        groupNameEditText.error = getString(R.string.error_group_name_cannot_be_empty)
                        Toast.makeText(context, getString(R.string.error_group_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(getString(R.string.cancel_button_text)) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null for CreateGroupDialogFragment")
    }
}