package com.example.synapse.chats


import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.synapse.R
import com.google.firebase.auth.FirebaseAuth

class CreateGroupDialogFragment : DialogFragment() {

    // Use activityViewModels to share ViewModel with BroadGroupFragment and underlying Activity
    private val broadGroupViewModel: BroadGroupViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { currentActivity ->
            val builder = AlertDialog.Builder(currentActivity)
            val inflater = currentActivity.layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_create_group, null)
            val groupNameEditText = dialogView.findViewById<EditText>(R.id.edit_text_group_name_dialog)
            // val groupDescriptionEditText = dialogView.findViewById<EditText>(R.id.edit_text_group_description_dialog) // If you added description

            builder.setView(dialogView)
                .setTitle("Create New Group")
                .setPositiveButton("Create") { _, _ ->
                    val groupName = groupNameEditText.text.toString().trim()
                    // val groupDescription = groupDescriptionEditText.text.toString().trim() // If you added description

                    if (groupName.isNotEmpty()) {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            // Call ViewModel to create the group
                            // Pass description if you have it: broadGroupViewModel.createNewGroup(groupName, groupDescription, currentUser.uid)
                            broadGroupViewModel.createNewGroup(groupName, currentUser.uid)
                        } else {
                            // Handle case where user is not logged in, though this dialog
                            // should ideally only be shown if the user is authenticated.
                            Toast.makeText(context, "Error: User not authenticated.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        groupNameEditText.error = "Group name cannot be empty"
                        // Prevent dialog from closing if validation fails by not calling dismiss explicitly
                        // For more robust validation, you might override the button's OnClickListener later.
                        Toast.makeText(context, "Group name cannot be empty.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null for CreateGroupDialogFragment")
    }
}