package org.khpylon.mobnetmonitor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.app.ActivityCompat


@Composable
fun NotificationPermission(context: Context) {
    val storage = Storage(context)
    HandlePermission(
        context = context,
        permission = Manifest.permission.POST_NOTIFICATIONS,
        onReadEnable = {
            storage.isNotificationEnabled
        },
        onWriteEnable = { value ->
            storage.isNotificationEnabled = value
        },
        onReadPermission = {
            storage.notificationPermission
        },
        onWritePermission = { value ->
            storage.notificationPermission = value
        },
        dialogText = stringResource(R.string.post_notifications_permission_text),
        dialogTitle = stringResource(R.string.post_notifications_permission_title),
        descText = stringResource(R.string.show_notifications_on_network_detected),
        tooltipText = stringResource(R.string.notification_tooltip)
    )
}

//@Composable
//fun accessLocationPermissions(context: Context) {
//    val storage = Storage(context)
//    HandlePermission(
//        context = context,
//        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
//        onReadEnable = {
//            storage.isAccessLocationEnabled
//        },
//        onWriteEnable = { value ->
//            storage.isAccessLocationEnabled = value
//        },
//        onReadPermission = {
//            storage.accessLocationPermission
//        },
//        onWritePermission = { value ->
//            storage.accessLocationPermission = value
//        },
//        dialogText = "This permission is needed to access the phone state, read location information... dunno why it's needed.",
//        dialogTitle = "Read phone location",
//        descText = "Access phone's location",
//        tooltipText = "Read the phone's location"
//    )
//}

@Composable
fun HandlePermission(
    context: Context,
    permission: String,
    onReadEnable: () -> Boolean,
    onWriteEnable: (Boolean) -> Unit,
    onReadPermission: () -> Int,
    onWritePermission: (Int) -> Unit,
    dialogTitle: String,
    dialogText: String,
    tooltipText: String,
    descText: String,
) {
    val packageName = context.packageName

        // Can the app use notifications
    var notificationEnabled by remember {
        mutableStateOf(onReadEnable())
    }

    // Are notification permissions allowed?
    var notificationPermission by remember {
        mutableIntStateOf(onReadPermission())
    }

    // Is the user able to see the notification permission dialog?
    var notificationPermissionPopup by remember {
        mutableStateOf(false)
    }

    // Request notification permissions if necessary
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                onWriteEnable(true)
                onWritePermission(StorageConstants.PERMISSION_GRANTED)
            } else {
                var activity = context
                while (activity is ContextWrapper) {
                    if (activity is Activity) break
                    activity = activity.baseContext
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity as Activity,
                        permission
                    )
                ) {
                    notificationPermissionPopup = true
                } else {
                    onWritePermission(StorageConstants.PERMISSION_DENIED)
                }
            }
            notificationEnabled = onReadEnable()
            notificationPermission = onReadPermission()
        }

    // If request for notifications permissions fails, show a dialog
    if (notificationPermissionPopup) {
        InfoDialog(
            onDismissRequest = { notificationPermissionPopup = false },
            dialogTitle = dialogTitle,
            dialogText = buildAnnotatedString {  append(dialogText)},
            dismissText = stringResource(R.string.dismiss_button_text),
            confirmText = stringResource(R.string.try_again_button_text),
            onConfirmRequest = {
                notificationLauncher.launch(permission)
                notificationPermissionPopup = false
            },
        )
    }

    // Force request of notification permissions dialog
    val notificationSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if( context.checkSelfPermission(
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onWritePermission(StorageConstants.PERMISSION_GRANTED)
                notificationEnabled = onReadEnable()
            } else {
                onWritePermission(StorageConstants.PERMISSION_DENIED)
            }
            notificationPermission = onReadPermission()
        }

    // Toggle for notifications
    OptionSwitchRow(
        tooltip = tooltipText,
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(descText)
            }
            append("\n  Status: ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(
                    when (notificationPermission) {
                        StorageConstants.PERMISSION_NOT_REQUESTED -> stringResource(R.string.not_requested)
                        StorageConstants.PERMISSION_DENIED -> stringResource(R.string.denied)
                        else -> context.getString(if (notificationEnabled) R.string.enabled_description else R.string.disabled_description)
                    }
                )
            }
        },
        isChecked = notificationPermission == StorageConstants.PERMISSION_GRANTED && notificationEnabled,
        onClick = { value ->
            if (value) {
                if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                    onWriteEnable(true)
                    notificationEnabled = true
                } else {
                    notificationLauncher.launch(permission)
                }
            } else {
                onWriteEnable(false)
                notificationEnabled = false
            }
        },
        onLongClick = {
            if (notificationPermission == StorageConstants.PERMISSION_DENIED) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                notificationSettingsLauncher.launch(intent)
            }
        }
    )
}

