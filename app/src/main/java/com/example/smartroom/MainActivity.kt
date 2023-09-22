package com.example.smartroom

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartroom.ui.theme.SmartRoomTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var socket: BluetoothSocket

    @RequiresApi(Build.VERSION_CODES.S)
    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    private val permissionRequestCode = 42 // You can choose any value

    private val btPerm = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Log.d("BT-TEST", it.toString())
    }

    private var isThreadReading = false

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartRoomTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    var outMessage by remember { mutableStateOf("") }
                    var connected by remember { mutableStateOf(false) }
                    var connecting by remember { mutableStateOf(false) }

                    var snackbarVisible by remember { mutableStateOf(false) }
                    var snackBarText by remember { mutableStateOf("") }

                    var btMessage by remember { mutableStateOf("") }

                    var receivedMessage by remember { mutableStateOf("") }
                    var lightsState by remember { mutableStateOf(false) }
                    var blindsState by remember { mutableStateOf(100.0) }

                    var pagerState = rememberPagerState()

                    val buttonClick = { deviceName: String ->
                        if (!arePermissionsGranted(requiredPermissions)) {
                            Log.d("CONNECTION", "Requesting permissions")
                            requestPermissions()
                            Log.d("CONNECTION", "Requested")
                        } else {

                            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                                Log.e("CONNECTION", "Bluetooth is not available or turned off")
                                snackBarText = "Bluetooth is not available or turned off"
                                requestPermissions()
                            } else {
                                Log.d("CONNECTION", "Connecting...")
                                connecting = true
                                connected = connectToDevice(
                                    deviceName,
                                    onError = { }
                                )
                                connecting = false

                                beginListeningForMessages { message ->
                                    btMessage += message

                                    if (btMessage.contains('&')) {
                                        Log.d("RCV-MESSAGE", btMessage)

                                        /*btMessage = btMessage.substring(
                                            btMessage.indexOf('{'),
                                            btMessage.indexOf('}')
                                        )*/
                                        receivedMessage = btMessage

                                        btMessage = btMessage.replace("|", "")
                                        btMessage = btMessage.replace("&", "")
                                        btMessage = btMessage.replace("\n", "")

                                        try {
                                            val json = JSONObject(btMessage)
                                            if (json.has("lights")) {
                                                lightsState = json.getString("lights") == "on"
                                            }
                                            if (json.has("blinds")) {
                                                blindsState = json.getString("blinds").toDouble() / 100
                                            }
                                            btMessage = ""
                                        } catch (e: JSONException) {
                                            Log.e("JSON", "Exception")
                                            btMessage = ""
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (snackbarVisible) {
                        Snackbar(
                            modifier = Modifier.fillMaxSize(),
                            action = {
                                TextButton(onClick = { snackbarVisible = false }) {
                                    Text("Close")
                                }
                            },
                            content = {
                                Text(snackBarText)
                            }
                        )
                    }


                    if (!connected && !connecting) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "SmartRoom controller",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    modifier = Modifier.padding(7.dp),
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            buttonClick("DSD TECH HC-05")
                                        }
                                    },
                                    enabled = !connected
                                ) {
                                    Text(text = "Connect to DSD TECH HC-05")
                                }

                                Button(
                                    modifier = Modifier.padding(7.dp),
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            buttonClick("HC-06")
                                        }
                                    },
                                    enabled = !connected
                                ) {
                                    Text(text = "Connect to HC-06")
                                }
                            }
                        }
                    } else {
                        HorizontalPager(
                            pageCount = 2,
                            state = pagerState,
                            key = { "Test$it" }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                TopAppBar(title = { Text(text = if (it == 0) "Lights" else (if (it == 1) "Blinds" else "Graph")) })
                                if (it == 0) {
                                    ElevatedButton(
                                        modifier = Modifier
                                            .fillMaxSize(0.6f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape),
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (lightsState) Color.Blue else Color.DarkGray
                                        ),
                                        elevation = ButtonDefaults.elevatedButtonElevation(15.dp),
                                        onClick = {
                                            lightsState = if (lightsState) {
                                                sendMessage("|{\"lights\":\"off\"}&")
                                                false
                                            } else {
                                                sendMessage("|{\"lights\":\"on\"}&")
                                                true
                                            }
                                        }
                                    ) {
                                        Image(
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            painter = painterResource(id = R.drawable.baseline_power_settings_new_24),
                                            contentDescription = "Toggle lights"
                                        )
                                    }
                                } else if (it == 1) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Top
                                    ) {
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(0.65f),
                                            value = blindsState.toFloat(),
                                            onValueChange = {
                                                blindsState = it.toDouble()
                                            },
                                            onValueChangeFinished = {
                                                sendMessage(
                                                    "|{\"blinds\":\"" + (blindsState * 100).toInt()
                                                        .toString() + "\"}&"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        /*Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "SmartRoom controller",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(7.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Status", style = MaterialTheme.typography.labelMedium)

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.Black, CircleShape)
                                        .background(if (connected) Color.Green else (if (connecting) Color.Yellow else Color.Red))
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lights",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .background(if (lightsState) Color.Cyan else Color.Blue)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    modifier = Modifier.padding(7.dp),
                                    onClick = {
                                        sendMessage("|{\"lights\":\"on\"}&")
                                    },
                                    enabled = connected
                                ) {
                                    Text("On")
                                }

                                Button(
                                    modifier = Modifier.padding(7.dp),
                                    onClick = {
                                        sendMessage("|{\"lights\":\"off\"}&")
                                    },
                                    enabled = connected
                                ) {
                                    Text("Off")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Blinds", style = MaterialTheme.typography.labelMedium)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    modifier = Modifier.fillMaxWidth(0.65f),
                                    value = blindsLevel.toFloat(),
                                    onValueChange = { blindsLevel = it.toDouble() }
                                )

                                Button(
                                    modifier = Modifier.padding(7.dp),
                                    onClick = {
                                        sendMessage(
                                            "|{\"blinds\":\"" + (blindsLevel * 100).toInt()
                                                .toString() + "\"}&"
                                        )
                                    },
                                    enabled = connected
                                ) {
                                    Text("Move blinds")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Custom message", style = MaterialTheme.typography.labelMedium)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = receivedMessage,
                                    onValueChange = { outMessage = it },
                                    enabled = connected
                                )

                                Button(
                                    modifier = Modifier.padding(7.dp),
                                    onClick = {
                                        sendMessage(outMessage)
                                    },
                                    enabled = connected
                                ) {
                                    Text("Send")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Button(
                                    onClick = {
                                        disconnect()
                                        connected = false
                                    },
                                    enabled = connected
                                ) {
                                    Text("Disconnect")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                        }*/
                    }
                }
            }
        }

        // Check and request permissions
        if (!arePermissionsGranted(requiredPermissions)) {
            ActivityCompat.requestPermissions(this, requiredPermissions, permissionRequestCode)
        }
    }

    private fun arePermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermissions() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val requestBtPermCode = 123
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    requestBtPermCode
                )
                // Callback per grabbare il risultato?
            }
            // Se è stato dato il permesso soprastante, allora chiedi di attivare il bluetooth
            btPerm.launch(enableBtIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectToDevice(deviceName: String, onError: (String) -> Unit): Boolean {
        Log.d("CONNECTION", "Trying to connect to $deviceName")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("CONNECTION", "Error: no permissions")
            return false
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val targetDevice = pairedDevices?.find { it.name == deviceName }

        if (targetDevice == null) {
            onError("Device not found or not paired.")
            return false
        }

        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID

        try {
            socket = targetDevice.createRfcommSocketToServiceRecord(uuid)
            socket.connect()

            // Connection successful, handle further communication
            Log.d("CONNECTION", "Connected to $deviceName")

        } catch (e: IOException) {
            onError("Error establishing Bluetooth connection.")
            return false
        }
        return true
    }

    private fun disconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            socket.close()
        }
    }

    private fun beginListeningForMessages(onMessageReceived: (String) -> Unit) {
        val inputStream = socket.inputStream
        val buffer = ByteArray(1024)

        val serReadThread = Thread {
            this.isThreadReading = true
            while (isThreadReading) {
                try {
                    val bytes = inputStream.read(buffer)
                    val readMessage = String(buffer, 0, bytes)
                    //Log.d("MESSAGE-RCV", readMessage)
                    onMessageReceived(readMessage)
                } catch (e: IOException) {
                    break
                }
            }
            inputStream.close()
        }
        serReadThread.start()
    }

    private fun sendMessage(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (socket.isConnected) {
                val outputStream = socket.outputStream
                withContext(Dispatchers.IO) {
                    outputStream.write(message.toByteArray())
                }
                Log.d("MESSAGE-SND", message)
            }
        }
    }
}
