package org.khpylon.mobnetmonitor

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.ZoneId

class TelephonyService : Service() {

    // Expose the running state to the rest of the application
    companion object {
        var isRunning = false
            private set
    }

    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: ServiceStateCallback? = null
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "telephony_service_channel"

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // Start the service in the foreground
        startForeground(
            NOTIFICATION_ID,
            buildNotification("Monitoring network status..."),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )

        registerTelephonyCallback()

        // Sticky ensures the service restarts if killed by the system
        return START_STICKY
    }

    private fun registerTelephonyCallback() {
        if (telephonyCallback == null) {
            telephonyCallback = ServiceStateCallback()
            try {
                telephonyManager.registerTelephonyCallback(
                    ContextCompat.getMainExecutor(this),
                    telephonyCallback!!
                )
            } catch (e: SecurityException) {
                Log.e("TelephonyService", "Missing READ_PHONE_STATE permission", e)
                stopSelf()
            }
        }
    }

    private fun unregisterTelephonyCallback() {
        telephonyCallback?.let {
            telephonyManager.unregisterTelephonyCallback(it)
            telephonyCallback = null
        }
    }

    override fun onDestroy() {
        unregisterTelephonyCallback()
        super.onDestroy()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Inner class defining the listener
    private inner class ServiceStateCallback : TelephonyCallback(),
        TelephonyCallback.ServiceStateListener {
        override fun onServiceStateChanged(serviceState: ServiceState) {
            val statusText = when (serviceState.state) {
                ServiceState.STATE_IN_SERVICE -> "In Service - ${serviceState.operatorAlphaLong}"
                ServiceState.STATE_OUT_OF_SERVICE -> "No Network Service"
                ServiceState.STATE_EMERGENCY_ONLY -> "Emergency Calls Only"
                ServiceState.STATE_POWER_OFF -> "Radio Off (Airplane Mode)"
                else -> "Unknown State"
            }

            // TODO: After leaving Airplane mode, state changes to OUT_OF_SERVICE before changing to IN_SERVICE.
            // TODO: Need a way to recognize and not immediately play the alert.  Maybe remember the
            // TODO: time when when changes happen and if it's almost immediate ignore the event.
            // TODO: Can we write a FSM to also recognize this?

            val storage = Storage(applicationContext)

            // Get time of the last service state change
            val lastTime = storage.lastTime

            // Find time ten seconds prior from right now
            val nowTime = LocalDateTime.now(ZoneId.systemDefault())
            val thenTime = nowTime.minusSeconds(10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Store current time
            storage.lastTime = nowTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // If it hasn't been ten seconds since the last change, ignore it
            if (thenTime < lastTime) {
                Log.d(Constants.LOGTAG,
                    "TelephonyService.onServiceStateChanged(): mode change to $statusText, but less than 10 seconds elapsed"
                )
                return
            }

            // If state changes from "out of service" to "in service", play notification sound
            if (!storage.isConnected && serviceState.state == ServiceState.STATE_IN_SERVICE) {
                val intent = Intent(applicationContext, PlayAlarmService::class.java)
                startForegroundService(intent)
            }

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildNotification(statusText))

            // save current state
            storage.isConnected = serviceState.state == ServiceState.STATE_IN_SERVICE
            Log.d(Constants.LOGTAG,
                "TelephonyService.onServiceStateChanged(): mode change to $statusText"
            )

        }
    }

    // Helper to generate the ongoing notification
    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Network Monitor")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_dialog_info) // Replace with your app icon
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Telephony Monitor Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}