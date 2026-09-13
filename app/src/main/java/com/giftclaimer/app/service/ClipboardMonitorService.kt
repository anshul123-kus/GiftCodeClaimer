package com.giftclaimer.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.giftclaimer.app.GiftClaimerApp
import com.giftclaimer.app.R
import com.giftclaimer.app.ui.MainActivity
import com.giftclaimer.app.util.CodeDetector
import com.giftclaimer.app.util.Constants
import com.giftclaimer.app.webview.CodeSubmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ClipboardMonitorService : Service() {

    private val TAG = "ClipboardMonitor"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var lastClipContent = ""
    private val seenCodes = mutableSetOf<String>()
    private var checkInterval = Constants.DEFAULT_CHECK_INTERVAL
    private var codeSubmitter: CodeSubmitter? = null
    private var isRunning = false

    private val clipboardCheckRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkClipboard()
                handler.postDelayed(this, checkInterval)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        checkInterval = prefs.getString(Constants.PREF_CHECK_INTERVAL, "500")?.toLongOrNull()
            ?: Constants.DEFAULT_CHECK_INTERVAL
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            Constants.ACTION_CODE_DETECTED -> {
                val code = intent.getStringExtra(Constants.EXTRA_CODE)
                if (code != null && code !in seenCodes) {
                    seenCodes.add(code)
                    processCode(code)
                }
                return START_STICKY
            }
        }

        startForeground(Constants.SERVICE_NOTIFICATION_ID, createNotification())
        isRunning = true

        // Initialize code submitter
        if (codeSubmitter == null) {
            codeSubmitter = CodeSubmitter(this)
        }

        // Start clipboard monitoring
        handler.post(clipboardCheckRunnable)

        // Send broadcast that service started
        sendBroadcast(Intent("com.giftclaimer.SERVICE_STATUS_CHANGED").apply {
            putExtra("is_running", true)
            setPackage(packageName)
        })

        Log.d(TAG, "Service started, monitoring clipboard every ${checkInterval}ms")
        return START_STICKY
    }

    private fun checkClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return

            val text = clip.getItemAt(0).text?.toString() ?: return
            if (text == lastClipContent) return

            lastClipContent = text
            val codes = CodeDetector.findCodes(text)

            for (code in codes) {
                if (code !in seenCodes) {
                    seenCodes.add(code)
                    Log.d(TAG, "New code detected: $code")
                    processCode(code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking clipboard", e)
        }
    }

    private fun processCode(code: String) {
        serviceScope.launch {
            try {
                // Broadcast to UI
                sendBroadcast(Intent(Constants.ACTION_CODE_DETECTED).apply {
                    putExtra(Constants.EXTRA_CODE, code)
                    setPackage(packageName)
                })

                codeSubmitter?.submitCodeToAllProfiles(code)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing code: $code", e)
            }
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ClipboardMonitorService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GiftClaimerApp.CHANNEL_SERVICE)
            .setContentTitle("Gift Code Claimer")
            .setContentText("Monitoring clipboard for gift codes...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_notification, "Stop", pendingStop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(clipboardCheckRunnable)
        codeSubmitter?.destroy()
        serviceScope.cancel()

        // Send broadcast that service stopped
        sendBroadcast(Intent("com.giftclaimer.SERVICE_STATUS_CHANGED").apply {
            putExtra("is_running", false)
            setPackage(packageName)
        })

        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
