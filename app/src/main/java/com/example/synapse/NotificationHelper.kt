package com.example.synapse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.privacysandbox.tools.core.generator.build

object NotificationHelper {

    private const val BROAD_GROUP_CHANNEL_ID = "broad_group_messages_channel"
    private const val BROAD_GROUP_CHANNEL_NAME = "Broad Group Messages"
    private const val BROAD_GROUP_CHANNEL_DESC = "Notifications for new messages in broad groups"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BROAD_GROUP_CHANNEL_ID,
                BROAD_GROUP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Or other importance levels
            ).apply {
                description = BROAD_GROUP_CHANNEL_DESC
                // Configure other channel properties if needed (sound, vibration, etc.)
                // enableLights(true)
                // lightColor = Color.RED
                // enableVibration(true)
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
        senderName: String, // Or "New message" if sender is not readily available
        messageSnippet: String,
        notificationId: Int // Unique ID for each notification (e.g., derive from groupId or use a counter)
    ) {
        // Intent to open when notification is tapped
        val intent = Intent(context, GroupChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(GroupChatActivity.EXTRA_GROUP_ID, groupId)
            putExtra(GroupChatActivity.EXTRA_GROUP_NAME, groupName)
            // You might want to add which tab to open (improvements/drawbacks)
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, notificationId /* requestCode */, intent, pendingIntentFlag)

        val builder = NotificationCompat.Builder(context, BROAD_GROUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon) // Replace with your notification icon
            .setContentTitle(groupName) // e.g., "Project Phoenix"
            .setContentText("$senderName: $messageSnippet") // e.g., "Alice: New idea for the UI"
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss notification when tapped
            .setGroup("broad_group_notifications") // Optional: Group notifications

        // For summary notification (Android 7.0+)
        // val summaryNotification = NotificationCompat.Builder(context, BROAD_GROUP_CHANNEL_ID)
        //     .setContentTitle("New Group Messages")
        //     .setContentText("You have new messages in broad groups")
        //     .setSmallIcon(R.drawable.ic_notification_icon)
        //     .setGroup("broad_group_notifications")
        //     .setGroupSummary(true)
        //     .build()

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (this.areNotificationsEnabled()) { // Check if notifications are enabled by user
                    // TODO: Request POST_NOTIFICATIONS permission if not granted
                    notify(notificationId, builder.build())
                    // notify(SUMMARY_ID_BROAD_GROUP, summaryNotification) // If using summary
                } else {
                    Log.w("NotificationHelper", "Notifications are disabled by the user.")
                    // Optionally inform the user or guide them to settings
                }
            } else {
                notify(notificationId, builder.build())
                // notify(SUMMARY_ID_BROAD_GROUP, summaryNotification) // If using summary
            }
        }
        Log.d("NotificationHelper", "Notification shown for group $groupName, id $notificationId")
    }

    // You'll need a unique ID for the summary if you use it
    // private const val SUMMARY_ID_BROAD_GROUP = 0
}