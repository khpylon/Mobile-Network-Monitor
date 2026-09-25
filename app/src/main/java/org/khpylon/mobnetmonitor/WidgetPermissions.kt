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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat

@Composable
fun notificationPermission(context: Context) {
    val storage = Storage(context)

    val packageName = context.packageName

        // Can the app use notifications
    var notificationEnabled by remember {
        mutableStateOf(storage.isNotificationEnabled)
    }

    // Are notification permissions allowed?
    var notificationPermission by remember {
        mutableIntStateOf(storage.notificationPermission)
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
                storage.isNotificationEnabled = true
                storage.notificationPermission = StorageConstants.PERMISSION_GRANTED
            } else {
                var activity = context
                while (activity is ContextWrapper) {
                    if (activity is Activity) break
                    activity = activity.baseContext
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity as Activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    notificationPermissionPopup = true
                } else {
                    storage.notificationPermission = StorageConstants.PERMISSION_DENIED
                }
            }
            notificationEnabled = storage.isNotificationEnabled
            notificationPermission = storage.notificationPermission
        }

    // If request for notifications permissions fails, show a dialog
    if (notificationPermissionPopup) {
        InfoDialog(
            onDismissRequest = { notificationPermissionPopup = false },
            dialogTitle = stringResource(R.string.post_notifications_permission_title),
            dialogText =
                buildAnnotatedString {
                    append(stringResource(R.string.post_notifications_permission_text))
                },
            dismissText = stringResource(R.string.dismiss_button_text),
            confirmText = stringResource(R.string.try_again_button_text),
            onConfirmRequest = {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                notificationPermissionPopup = false
            },
        )
    }

    // Force request of notification permissions dialog
    val notificationSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if( context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                storage.notificationPermission = StorageConstants.PERMISSION_GRANTED
                notificationEnabled = storage.isNotificationEnabled
            } else {
                storage.notificationPermission = StorageConstants.PERMISSION_DENIED
            }
            notificationPermission = storage.notificationPermission
        }

    // Toggle for notifications
    OptionSwitchRow(
        tooltip = stringResource(R.string.notification_tooltip),
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(stringResource(R.string.show_notifications_on_network_detected))
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
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    storage.isNotificationEnabled = true
                    notificationEnabled = true
                } else {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                storage.isNotificationEnabled = false
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

@Composable
fun phonePermissions(context: Context) {
    val storage = Storage(context)

    val packageName = context.packageName

    // Can the app read the phone
    var readPhoneEnabled by remember {
        mutableStateOf(storage.isReadPhoneEnabled)
    }

    // Are read phone permissions allowed?
    var readPhonePermission by remember {
        mutableIntStateOf(storage.readPhonePermission)
    }

    // Is the user able to see the read phone permission dialog?
    var readPhonePermissionPopup by remember {
        mutableStateOf(false)
    }

    // Request read phone permissions if necessary
    val readPhoneLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                storage.isReadPhoneEnabled = true
                storage.readPhonePermission = StorageConstants.PERMISSION_GRANTED
            } else {
                var activity = context
                while (activity is ContextWrapper) {
                    if (activity is Activity) break
                    activity = activity.baseContext
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity as Activity,
                        Manifest.permission.READ_PHONE_STATE
                    )
                ) {
                    readPhonePermissionPopup = true
                } else {
                    storage.readPhonePermission = StorageConstants.PERMISSION_DENIED
                }
            }
            readPhoneEnabled = storage.isNotificationEnabled
            readPhonePermission = storage.notificationPermission
        }

    // If request for read phone permissions fails, show a dialog
    if (readPhonePermissionPopup) {
        InfoDialog(
            onDismissRequest = { readPhonePermissionPopup = false },
            dialogTitle = "Read Phone Permission",
            dialogText =
                buildAnnotatedString {
                    append(stringResource(R.string.post_notifications_permission_text))
                },
            dismissText = stringResource(R.string.dismiss_button_text),
            confirmText = stringResource(R.string.try_again_button_text),
            onConfirmRequest = {
                readPhoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                readPhonePermissionPopup = false
            },
        )
    }

    // Force request of notification permissions dialog,
    val readPhoneSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if( context.checkSelfPermission(
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                storage.readPhonePermission = StorageConstants.PERMISSION_GRANTED
                readPhoneEnabled = storage.isReadPhoneEnabled
            } else {
                storage.readPhonePermission = StorageConstants.PERMISSION_DENIED
            }
            readPhonePermission = storage.readPhonePermission
        }

    // Toggle for notifications
    OptionSwitchRow(
        tooltip = "Read the phone's status to determine if a mobile network is available.",
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append("Read mobile network info")
            }
            append("\n  Status: ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(
                    when (readPhonePermission) {
                        StorageConstants.PERMISSION_NOT_REQUESTED -> stringResource(R.string.not_requested)
                        StorageConstants.PERMISSION_DENIED -> stringResource(R.string.denied)
                        else -> context.getString(if (readPhoneEnabled) R.string.enabled_description else R.string.disabled_description)
                    }
                )
            }
        },
        isChecked = readPhonePermission == StorageConstants.PERMISSION_GRANTED && readPhoneEnabled,
        onClick = { value ->
            if (value) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    storage.isReadPhoneEnabled = true
                    readPhoneEnabled = true
                } else {
                    readPhoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                }
            } else {
                storage.isReadPhoneEnabled = false
                readPhoneEnabled = false
            }
        },
        onLongClick = {
            if (readPhonePermission == StorageConstants.PERMISSION_DENIED) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                readPhoneSettingsLauncher.launch(intent)
            }
        }
    )
}

@Composable
fun accessLocationPermissions(context: Context) {
    val storage = Storage(context)

    val packageName = context.packageName

    // Can the app access location?
    var accessLocationEnabled by remember {
        mutableStateOf(storage.isAccessLocationEnabled)
    }

    // Are access location permissions allowed?
    var accessLocationPermission by remember {
        mutableIntStateOf(storage.accessLocationPermission)
    }

    // Is the user able to see the access location permission dialog?
    var accessLocationPermissionPopup by remember {
        mutableStateOf(false)
    }

    // Request access location permissions if necessary
    val accessLocationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                storage.isAccessLocationEnabled = true
                storage.accessLocationPermission = StorageConstants.PERMISSION_GRANTED
            } else {
                var activity = context
                while (activity is ContextWrapper) {
                    if (activity is Activity) break
                    activity = activity.baseContext
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity as Activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    accessLocationPermissionPopup = true
                } else {
                    storage.accessLocationPermission = StorageConstants.PERMISSION_DENIED
                }
            }
            accessLocationEnabled = storage.isAccessLocationEnabled
            accessLocationPermission = storage.accessLocationPermission
        }

    // If request for access location permissions fails, show a dialog
    if (accessLocationPermissionPopup) {
        InfoDialog(
            onDismissRequest = { accessLocationPermissionPopup = false },
            dialogTitle = "Read Phone Location",
            dialogText =
                buildAnnotatedString {
                    append(stringResource(R.string.post_notifications_permission_text))
                },
            dismissText = stringResource(R.string.dismiss_button_text),
            confirmText = stringResource(R.string.try_again_button_text),
            onConfirmRequest = {
                accessLocationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                accessLocationPermissionPopup = false
            },
        )
    }

    // Force request of access location permissions dialog
    val readPhoneSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if( context.checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                storage.accessLocationPermission = StorageConstants.PERMISSION_GRANTED
                accessLocationEnabled = storage.isReadPhoneEnabled
            } else {
                storage.accessLocationPermission = StorageConstants.PERMISSION_DENIED
            }
            accessLocationPermission = storage.accessLocationPermission
        }

    // Toggle for access location
    OptionSwitchRow(
        tooltip = "Read the phone's location.",
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append("Access phone's location")
            }
            append("\n  Status: ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(
                    when (accessLocationPermission) {
                        StorageConstants.PERMISSION_NOT_REQUESTED -> stringResource(R.string.not_requested)
                        StorageConstants.PERMISSION_DENIED -> stringResource(R.string.denied)
                        else -> context.getString(if (accessLocationEnabled) R.string.enabled_description else R.string.disabled_description)
                    }
                )
            }
        },
        isChecked = accessLocationPermission == StorageConstants.PERMISSION_GRANTED && accessLocationEnabled,
        onClick = { value ->
            if (value) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    storage.isAccessLocationEnabled = true
                    accessLocationEnabled = true
                } else {
                    accessLocationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            } else {
                storage.isAccessLocationEnabled = false
                accessLocationEnabled = false
            }
        },
        onLongClick = {
            if (accessLocationPermission == StorageConstants.PERMISSION_DENIED) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                readPhoneSettingsLauncher.launch(intent)
            }
        }
    )
}
