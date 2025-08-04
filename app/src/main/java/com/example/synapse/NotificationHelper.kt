package com.example.synapse

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat // Keep if you add permission checks here, otherwise can be removed
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val BROAD_GROUP_CHANNEL_ID = "broad_group_messages_channel"
    private const val BROAD_GROUP_CHANNEL_NAME = "Broad Group Messages"
    private const val BROAD_GROUP_CHANNEL_DESC = "Notifications for new messages in broad groups"

    // Constants for Intent Extras, should match what HomeActivity expects
    // It's good practice to define these in a companion object of HomeActivity
    // or a shared constants file. For now, we'll define them here for clarity.
    const val EXTRA_TARGET_GROUP_ID = "com.example.synapse.TARGET_GROUP_ID"
    const val EXTRA_TARGET_GROUP_NAME = "com.example.synapse.TARGET_GROUP_NAME"
    const val EXTRA_LAUNCH_SOURCE = "com.example.synapse.LAUNCH_SOURCE" // To indicate it's from a notification

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BROAD_GROUP_CHANNEL_ID,
                BROAD_GROUP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = BROAD_GROUP_CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Notification channel created.")
        }
    }

    fun showBroadGroupMessageNotification(
        context: Context,
        groupId: String,
        groupName: String,
        senderName: String,
        messageSnippet: String,
        notificationId: Int
    ) {
        // Intent to open HomeActivity when notification is tapped.
        // HomeActivity will then navigate to the correct ChatRoomFragment.
        val intent = Intent(context, HomeActivity::class.java).apply { // CHANGED HERE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Add extras that HomeActivity can use to navigate to the specific chat
            putExtra(EXTRA_TARGET_GROUP_ID, groupId)
            putExtra(EXTRA_TARGET_GROUP_NAME, groupName)
            putExtra(EXTRA_LAUNCH_SOURCE, "notification_broad_group")
            // If you need to specify which tab (improvement/drawback), add another extra here.
            // e.g., putExtra("TARGET_MESSAGE_TYPE", "improvement_messages")
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        // Use notificationId as the requestCode for PendingIntent to ensure uniqueness if groupId/groupName could be the same for different notifications
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, notificationId, intent, pendingIntentFlag)

        val builder = NotificationCompat.Builder(context, BROAD_GROUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_image) // Replace with your notification icon
            .setContentTitle(groupName)
            .setContentText("$senderName: $messageSnippet")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("broad_group_notifications")

        val notificationManagerCompat = NotificationManagerCompat.from(context)

        if (notificationManagerCompat.areNotificationsEnabled()) {
            // Consider adding a check for POST_NOTIFICATIONS permission if targeting API 33+
            // and you haven't requested it elsewhere before calling this.
            // However, areNotificationsEnabled() implicitly checks this on API 33+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                // Optionally, inform the user or log that permission is missing.
                // The actual permission request should happen in an Activity context.
                return
            }
            notificationManagerCompat.notify(notificationId, builder.build())
            Log.d("NotificationHelper", "Notification shown for group $groupName, id $notificationId")
        } else {
            Log.w("NotificationHelper", "Notifications are disabled for this app.")
        }
    }
    // ... (rest of the object)
}