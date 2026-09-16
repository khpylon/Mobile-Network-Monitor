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
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.khpylon.mobnetmonitor.ui.theme.MyApplicationTheme

object Constants {
    const val LOGTAG = "934TXS"
}
class MainActivity : ComponentActivity() {
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
        storage.isConnected = state == ServiceState.STATE_IN_SERVICE

        if (!TelephonyService.isRunning) {
            Log.d(Constants.LOGTAG, "MainActivity.onCreate(): starting Telephony service")
            val startIntent = Intent(applicationContext, TelephonyService::class.java)
            ContextCompat.startForegroundService(applicationContext, startIntent)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}