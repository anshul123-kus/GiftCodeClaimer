package com.giftclaimer.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.giftclaimer.app.data.AppDatabase

class GiftClaimerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Clipboard Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when the clipboard monitor is running"
            }

            val resultChannel = NotificationChannel(
                CHANNEL_RESULTS,
                "Claim Results",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows gift code claim results"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(resultChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "clipboard_monitor_service"
        const val CHANNEL_RESULTS = "claim_results"
    }
}
