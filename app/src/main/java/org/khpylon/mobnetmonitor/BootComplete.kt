package org.khpylon.mobnetmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootComplete : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action.equals(Intent.ACTION_BOOT_COMPLETED, ignoreCase = true) ||
            action.equals(Intent.ACTION_MY_PACKAGE_REPLACED, ignoreCase = true) )
        {

            // When a new version of the app is loaded
            if (action.equals(Intent.ACTION_MY_PACKAGE_REPLACED, ignoreCase = true) )
            {
                // Display the release notes when app is opened
                val storage = Storage(context)
                storage.newInstall = true
            }

            // Start listener for telephone state changes
            Log.d(Constants.LOGTAG, "BootComplete.onReceive(): starting Telephony service")
            val startIntent = Intent(context, TelephonyService::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }
    }
}
