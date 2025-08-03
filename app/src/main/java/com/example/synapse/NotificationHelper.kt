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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

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
            .setSmallIcon(R.drawable.notification_image) // Replace with your notification icon
            .setContentTitle(groupName) // e.g., "Project Phoenix"
            .setContentText("$senderName: $messageSnippet") // e.g., "Alice: New idea for the UI"
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss notification when tapped
            .setGroup("broad_group_notifications") // Optional: Group notifications
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        // Check if notifications are enabled for the app.
        // This covers both runtime permission on API 33+ and user settings on all versions.
        if (notificationManagerCompat.areNotificationsEnabled()) {
            // The lint warning is about this call.
            // By checking areNotificationsEnabled() first, we address the spirit of the warning.
            // For API 33+, if POST_NOTIFICATIONS is not granted, areNotificationsEnabled() will be false.
            notificationManagerCompat.notify(notificationId, builder.build())
            Log.d("NotificationHelper", "Notification shown for group $groupName, id $notificationId")

            // If using summary notification:
            // val summaryNotification = NotificationCompat.Builder(context, BROAD_GROUP_CHANNEL_ID)
            //     // ... build summary ...
            //     .build()
            // notificationManagerCompat.notify(SUMMARY_ID_BROAD_GROUP, summaryNotification)

        } else {
            // This log is important. It tells you why a notification might not appear.
            Log.w("NotificationHelper", "Notifications are disabled for this app (either permission denied on API 33+ or disabled in settings).")
            // Optionally, you could have a mechanism here to inform the user more directly,
            // but usually, the place that *requests* the permission (HomeActivity) is better for that.
        }
    }
    // private const val SUMMARY_ID_BROAD_GROUP = 0
}