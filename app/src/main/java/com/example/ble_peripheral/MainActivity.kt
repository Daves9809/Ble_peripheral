package com.example.ble_peripheral

import android.Manifest
import android.app.DownloadManager.Request
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.node.modifierElementOf
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ble_peripheral.ui.theme.BLE_peripheralTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var gattServiceConn: GattServiceConn? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BLE_peripheralTheme {
                MainScreen()
            }

        }
    }

    override fun onStart() {
        super.onStart()

        val latestGattServiceConn = GattServiceConn()
        if (bindService(Intent(this, GattService::class.java), latestGattServiceConn, 0)) {
            gattServiceConn = latestGattServiceConn
        }
    }

    override fun onStop() {
        super.onStop()

        if (gattServiceConn != null) {
            unbindService(gattServiceConn!!)
            gattServiceConn = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // We only want the service around for as long as our app is being run on the device
        stopService(Intent(this, GattService::class.java))
    }

    class GattServiceConn : ServiceConnection {
        var binding: DeviceAPI? = null

        override fun onServiceDisconnected(name: ComponentName?) {
            binding = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binding = service as? DeviceAPI
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun MainScreen(mainViewModel: MainViewModel = hiltViewModel()) {

        val stateConnection: Boolean by mainViewModel.value.observeAsState(initial = false)

        RequestPermissions()
        var switchState by remember {
            mutableStateOf(false)
        }

        var readedData by remember {
            mutableStateOf("")
        }

        //initialize callback for read data from central
        gattServiceConn?.binding?.enableCallBacks {
            readedData = String(it.value!!)
        }

        val (focusRequester) = remember { FocusRequester.createRefs() }
        val focusManager = LocalFocusManager.current

        Column() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advertisement switch",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Switch(checked = switchState, onCheckedChange = {
                    switchState = it
                    if (it) {
                        startForegroundService(
                            Intent(
                                this@MainActivity,
                                GattService::class.java
                            )
                        )
                    } else
                        stopService(Intent(this@MainActivity, GattService::class.java))
                })
            }
            OutlinedTextField(value = "$stateConnection", label = {
                Text(text = "Connection State")
            }, onValueChange = {}, enabled = false, modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var textValue by remember {
                    mutableStateOf("Default")
                }

                OutlinedTextField(value = textValue,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    onValueChange = {
                        textValue = it
                        gattServiceConn?.binding?.setMyCharacteristicValue(it)
                    },
                    label = {
                        Text(text = "Send Value")
                    })
                OutlinedTextField(value = readedData,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    enabled = false,
                    onValueChange = {
                    },
                    label = {
                        Text(text = "Received value")
                    })

            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissions() {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    })
}
