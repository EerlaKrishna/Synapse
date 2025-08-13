package com.example.synapse.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.example.synapse.R
import com.example.synapse.home.HomeActivity
import com.google.firebase.messaging.FirebaseMessagingService // Make sure this import is present
import com.google.firebase.messaging.RemoteMessage     // And this one for the parameter

// ... other imports

class MyFirebaseMessagingService : FirebaseMessagingService() { // Use the simple name here

    companion object {
        private const val TAG = "MyFirebaseMsgService" // Example TAG
        private const val CHANNEL_ID = "default_channel_id" // Example Channel ID
        private const val NOTIFICATION_ID = 0 // Example Notification ID
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) { // Use simple name here too
        Log.d(TAG, "From: ${remoteMessage.from}")

        // ... (rest of your onMessageReceived logic)
        var notificationTitle = "New Message"
        var notificationBody = "You have a new message."

        remoteMessage.notification?.let {
            notificationTitle = it.title ?: notificationTitle
            notificationBody = it.body ?: notificationBody
        }
        remoteMessage.data["title"]?.let { notificationTitle = it }
        remoteMessage.data["body"]?.let { notificationBody = it }

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentUnread = prefs.getInt("unread_dm_count", 0)
        prefs.edit { // Using Kotlin extension function for SharedPreferences
            putInt("unread_dm_count", currentUnread + 1)
            // apply() is called automatically by edit {}
        }
        Log.d(TAG, "Incremented unread DM count to: ${currentUnread + 1}")

        sendNotification(notificationTitle, notificationBody)
    }

    private fun sendNotification(title: String, messageBody: String) {
        // ... (your sendNotification logic)
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, pendingIntentFlag)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_synapse_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New Messages",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
        // TODO: Implement this method to send token to your app server.
    }
}