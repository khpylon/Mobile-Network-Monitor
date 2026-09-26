package org.khpylon.mobnetmonitor

import android.Manifest
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
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

//            // Without necessary permissions, don't try to do anything
//            if (checkSelfPermission(
//                    Manifest.permission.READ_PHONE_STATE
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                Log.e("TelephonyService", "Missing READ_PHONE_STATE permission")
//                return
//            }
//            else
                if (checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("TelephonyService", "Missing ACCESS_COARSE_LOCATION permission")
                return
            }

            // Ignore Airplane mode changes; they don't seem to tell anything useful
            if (serviceState.state == ServiceState.STATE_POWER_OFF) {
                Log.d(
                    Constants.LOGTAG,
                    "TelephonyService.onServiceStateChanged(): ignoring change to STATE_POWER_OFF"
                )
            } else {
                val storage = Storage(applicationContext)

                // Get time of the last service state change
                val lastTime = storage.lastTime

                // Find time ten seconds prior from right now
                val nowTime = LocalDateTime.now(ZoneId.systemDefault())
                val thenTime = nowTime.minusSeconds(10).atZone(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli()

                // If state changes from "out of service" to "in service", play notification sound
                if (storage.serviceState != ServiceState.STATE_IN_SERVICE && serviceState.state == ServiceState.STATE_IN_SERVICE) {
                    // If it hasn't been ten seconds since the last change, ignore it
                    if (thenTime < lastTime) {
                        Log.d(
                            Constants.LOGTAG,
                            "TelephonyService.onServiceStateChanged(): less than 10 seconds elapsed since last change, so not playing alarm"
                        )
                    } else {
                        Log.d(
                            Constants.LOGTAG,
                            "TelephonyService.onServiceStateChanged(): playing alarm"
                        )
                        val intent = Intent(applicationContext, PlayAlarmService::class.java)
                        startForegroundService(intent)
                    }
                }

                val statusText = when (serviceState.state) {
                    ServiceState.STATE_IN_SERVICE -> "In Service"
                    ServiceState.STATE_OUT_OF_SERVICE -> "No Network Service"
                    ServiceState.STATE_EMERGENCY_ONLY -> "Emergency Calls Only"
                    ServiceState.STATE_POWER_OFF -> "Radio Off (Airplane Mode)"
                    else -> "Unknown State"
                }

                // Send notification
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(statusText))

                // if state changes, save the new state and current time
                if (storage.serviceState != serviceState.state) {
                    storage.serviceState = serviceState.state
                    storage.lastTime =
                        nowTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    Log.d(
                        Constants.LOGTAG,
                        "TelephonyService.onServiceStateChanged(): mode change to $statusText"
                    )
                }
            }
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