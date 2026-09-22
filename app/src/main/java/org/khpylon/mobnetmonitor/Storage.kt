package org.khpylon.mobnetmonitor

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.telephony.ServiceState
import androidx.core.net.toUri

object StorageConstants {
    const val TAG = "storage"
    const val TELEPHONY_STATE = "telephonyState"
    const val LAST_TIME = "lastTime"
    const val RINGTONE_URI = "ringToneUri"
    const val NEW_INSTALL = "newInstall"
    const val LAST_APP_VERSION: String = "last_app_version"
    const val CURRENT_APP_VERSION: String = "current_app_version"
    const val FIRST_APP_VERSION: String = "2026.09-18"
    const val NOTIFICATION_STATUS: String = "notificationStatus"
    const val NOTIFICATION_PERMISSION: String = "notificationPermissions"
    const val PERMISSION_NOT_REQUESTED: Int = 0
    const val PERMISSION_GRANTED: Int = 1
    const val PERMISSION_DENIED: Int = 2

}

class Storage(private val context: Context) {
    private fun commitWait(edit: SharedPreferences.Editor) {
        for (i in 0..9) {
            if (edit.commit()) {
                return
            }
        }
    }

    var serviceState: Int
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getInt(StorageConstants.TELEPHONY_STATE, ServiceState.STATE_IN_SERVICE)
        }
        set(state) {
            val edit =
                context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putInt(StorageConstants.TELEPHONY_STATE, state)
            commitWait(edit)
        }

    var lastTime: Long
        get() {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            return pref.getLong(StorageConstants.LAST_TIME, 0)
        }
        set(time) {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            val edit = pref.edit()
            // Store data. you may also use putFloat(), putInt(), putLong() as requirement
            edit.putLong(StorageConstants.LAST_TIME, time)
            // Commit the changes
            commitWait(edit)
        }

    var ringTone: Uri
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            val defaultUri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
            val uri = pref.getString(StorageConstants.RINGTONE_URI, defaultUri)!!.toUri()

            // Make sure the URI still exists
            try {
                context.contentResolver.openInputStream(uri)?.close()
                return uri
            }
            catch (_: Exception) {
                return defaultUri.toUri()
            }
        }
        set(uri) {
            val edit =
                context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putString(StorageConstants.RINGTONE_URI, uri.toString())
            commitWait(edit)
        }

    var newInstall: Boolean
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getBoolean(StorageConstants.NEW_INSTALL, true)
        }
        set(isNewInstall) {
            val edit =
                context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putBoolean(StorageConstants.NEW_INSTALL, isNewInstall)
            commitWait(edit)
        }

    val lastAppVersion: String
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getString(
                StorageConstants.LAST_APP_VERSION,
                StorageConstants.FIRST_APP_VERSION
            ).toString()
        }

    // Track versions of the app.  This is used to display most recent release notes
    fun updateAppVersion() {
        val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
        val edit = pref.edit()

        // Get prior app version and save it
        val lastAppVersion =
            pref.getString(StorageConstants.CURRENT_APP_VERSION, StorageConstants.FIRST_APP_VERSION)
                .toString()
        edit.putString(StorageConstants.LAST_APP_VERSION, lastAppVersion)

        val buildVersion = BuildConfig.VERSION_NAME
        // Make sure there's no ".debug" on end of current version string
        val currentAppVersion = if (buildVersion.endsWith(".debug"))
            buildVersion.substring(buildVersion.length - 6)
        else
            buildVersion

        // Save the current version
        edit.putString(StorageConstants.CURRENT_APP_VERSION, currentAppVersion)
        commitWait(edit)
    }

    var isNotificationEnabled: Boolean
        get() {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            return pref.getBoolean(StorageConstants.NOTIFICATION_STATUS, false)
        }
        set(value) {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            val edit = pref.edit()
            // Store data. you may also use putFloat(), putInt(), putLong() as requirement
            edit.putBoolean(StorageConstants.NOTIFICATION_STATUS, value)
            // Commit the changes
            commitWait(edit)
        }


    var notificationPermission: Int
        get() {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            return pref.getInt(StorageConstants.NOTIFICATION_PERMISSION, StorageConstants.PERMISSION_NOT_REQUESTED)
        }
        set(id) {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            val edit = pref.edit()
            // Store data. you may also use putFloat(), putInt(), putLong() as requirement
            edit.putInt(StorageConstants.NOTIFICATION_PERMISSION, id)
            // Commit the changes
            commitWait(edit)
        }

    }