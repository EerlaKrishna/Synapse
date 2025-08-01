package com.example.synapse

import android.app.Application
import android.util.Log

class SynapseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("SynapseApplication", "Application onCreate: Creating notification channels.")
        NotificationHelper.createNotificationChannel(applicationContext)
    }
}