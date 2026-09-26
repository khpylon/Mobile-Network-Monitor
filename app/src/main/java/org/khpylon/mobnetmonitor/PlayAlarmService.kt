package org.khpylon.mobnetmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PlayAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "RingtoneServiceChannel"
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == "STOP_RINGTONE") {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground immediately with the correct Service Type
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Play the ringtone
        playRingtone()

        return START_STICKY // Kept alive if killed by low memory
    }

    private fun playRingtone() {
        if (mediaPlayer == null) {

            // Just in case we're already playing a sound, stop it
            releasePlayer()

            // Get the ringtone to use
            val storage = Storage(applicationContext)
            val ringtoneUri = storage.ringTone

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, ringtoneUri)

                // Configure audio attributes for a Ringtone stream
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                setOnPreparedListener { mp ->
                    start()
                }
                setOnCompletionListener { mp ->
                    releasePlayer()
                }

                // USAGE_NOTIFICATION apparently does not loop, but just to be sure
                isLooping = false

                prepare()
            }
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }


    private fun createNotification(): Notification {
        // Intent to open Main Activity when clicking notification
        val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intent to stop the ringtone from the notification
        val stopIntent = Intent(this, PlayAlarmService::class.java).apply {
            action = "STOP_RINGTONE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Incoming Alert")
            .setContentText("Ringtone is playing...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_media_ff, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true) // Cannot be dismissed by swiping
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Ringtone Playback Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for background ringtone playback"
                setSound(null, null) // Silent because MediaPlayer handles audio
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Safely stop and release player resources to avoid memory leaks
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Binding not required for this use case
    }

}