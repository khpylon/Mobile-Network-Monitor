package org.khpylon.mobnetmonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.khpylon.mobnetmonitor.ui.theme.MobNetMonitorTheme
import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


object Constants {
    const val LOGTAG = "934TXS"
}
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val attributionContext = createAttributionContext(getString(R.string.location_attribution_label))

        val telephonyManager = attributionContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        val state = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED ) {

            telephonyManager.serviceState?.state ?: ServiceState.STATE_IN_SERVICE
        } else {
            ServiceState.STATE_IN_SERVICE
        }

        val storage = Storage(applicationContext)
        storage.serviceState = state

        if (!TelephonyService.isRunning) {
            Log.d(Constants.LOGTAG, "MainActivity.onCreate(): starting Telephony service")
            val startIntent = Intent(applicationContext, TelephonyService::class.java)
            ContextCompat.startForegroundService(applicationContext, startIntent)
        }

        if (applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && storage.notificationPermission == StorageConstants.PERMISSION_GRANTED
        ) {
            storage.notificationPermission = StorageConstants.PERMISSION_DENIED
        } else if (applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            && storage.notificationPermission != StorageConstants.PERMISSION_GRANTED
        ) {
            storage.notificationPermission = StorageConstants.PERMISSION_GRANTED
        }


//        if (ContextCompat.checkSelfPermission(
//                this, Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
////            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
//                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
//                    Manifest.permission.POST_NOTIFICATIONS
//                )
////            }
//        }

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
                    Manifest.permission.READ_PHONE_STATE
                )
            }
        }

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }

        setContent {
            MobNetMonitorTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(applicationContext.getString(R.string.app_name)) },
                            actions = {}
                        )
                    },
                    modifier = Modifier.fillMaxSize())
                { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoDialog(
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit = {},
    dialogTitle: String,
    dialogText: AnnotatedString,
    confirmText: String? = null,
    dismissText: String? = null,
) {
    AlertDialog(
        icon = {
            Icon(painter = painterResource(R.drawable.info_24px), contentDescription = "")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(
                text = dialogText,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        onDismissRequest = { onDismissRequest() },

        // Only use confirm and dismiss if a button text exists
        confirmButton = {
            if (confirmText != null) {
                TextButton(
                    onClick = {
                        onConfirmRequest()
                    }
                ) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            if (dismissText != null) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text(dismissText)
                }
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    val context = LocalContext.current.applicationContext
    val storage = Storage(context)

    var selectedRingtoneUri by remember { mutableStateOf<Uri?>(storage.ringTone) }

    val ringtone = RingtoneManager.getRingtone(context, selectedRingtoneUri)
    var selectedRingtoneTitle by remember { mutableStateOf(ringtone.getTitle(context)) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                // Retrieve the selected ringtone URI
                val uri =
                    intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI,Uri::class.java)
                selectedRingtoneUri = uri
                storage.ringTone = uri!!
                val ringtone = RingtoneManager.getRingtone(context, uri)
                // Extract the user-friendly title
                selectedRingtoneTitle = ringtone?.getTitle(context) ?: "Unknown Ringtone"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {


        Text(
            text = buildAnnotatedString {
                append ("This app monitors the status of your cellular network connection, ")
                append ("and should you lose network service for a period of time will sound ")
                append("an alert when service is re-established.")
            },
            modifier = modifier
        )

        Spacer(
            modifier = Modifier
                .padding(horizontal = 8.dp)
        )

        WidgetPermissions(context)
        Box(
            modifier = Modifier
                .clickable(
                    true, "label",
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_RINGTONE
                            )
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                selectedRingtoneUri
                            )
                        }
                        ringtoneLauncher.launch(intent)
                    }
                )
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append("Alert ringtone (tap to change)")
                    }
                    append("\n  ")
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(selectedRingtoneTitle)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobNetMonitorTheme {
        Greeting("Android")
    }
}

