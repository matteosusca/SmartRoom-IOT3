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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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
        Toast.makeText(this, if (it.resultCode == RESULT_OK) "Bluetooth enabled" else "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
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
                    var connected by remember { mutableStateOf(false) }
                    var connecting by remember { mutableStateOf(false) }

                    var btMessage by remember { mutableStateOf("") }

                    var receivedMessage by remember { mutableStateOf("") }
                    var lightsState by remember { mutableStateOf(false) }
                    var blindsState by remember { mutableStateOf(100.0) }

                    val pagerState = rememberPagerState()

                    val buttonClick = { deviceName: String ->
                        if (!arePermissionsGranted(requiredPermissions)) {
                            Log.d("CONNECTION", "Requesting permissions")
                            requestPermissions()
                            Log.d("CONNECTION", "Requested")
                        } else {

                            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                                Log.e("CONNECTION", "Bluetooth is not available or turned off")
                                requestPermissions()
                            } else {
                                Log.d("CONNECTION", "Connecting...")
                                connecting = true
                                connected = connectToDevice(
                                    deviceName,
                                    onError = { message ->
                                        Log.e("CONNECTION", message)
                                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                    }
                                )
                                connecting = false

                                beginListeningForMessages { message ->
                                    btMessage += message

                                    if (btMessage.contains('&')) {
                                        Log.d("RCV-MESSAGE", btMessage)
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
                                            Log.e("JSON", "Exception while parsing JSON")
                                            btMessage = ""
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
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
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Status",
                                            style = MaterialTheme.typography.labelMedium
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color.Black, CircleShape)
                                                .background(if (connected) Color.Green else (if (connecting) Color.Yellow else Color.Red))
                                        )
                                    }
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

                                Divider(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp))

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

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Top
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    TopAppBar(
                                                        title = {
                                                            Text(
                                                                text = if (it == 0) "Lights" else "Blinds",
                                                                style = MaterialTheme.typography.titleLarge
                                                            )
                                                        }
                                                    )
                                                    if (it == 0) {
                                                        ElevatedButton(
                                                            modifier = Modifier
                                                                .fillMaxSize(0.6f)
                                                                .aspectRatio(1f)
                                                                .clip(CircleShape),
                                                            colors = ButtonDefaults.elevatedButtonColors(
                                                                containerColor = if (lightsState)
                                                                    Color.hsl(125f, 0.85f, 0.45f)
                                                                else
                                                                    Color.hsl(125f, 0.85f, 0.2f)
                                                            ),
                                                            elevation = ButtonDefaults.elevatedButtonElevation(
                                                                15.dp
                                                            ),
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
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Open",
                                                                style = MaterialTheme.typography.labelLarge
                                                            )
                                                            Text(
                                                                text = "Closed",
                                                                style = MaterialTheme.typography.labelLarge
                                                            )
                                                        }
                                                        Slider(
                                                            modifier = Modifier.fillMaxWidth(0.92f),
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
                                                if (it == 0) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Bottom
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(25.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Swipe right to control blinds",
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                            Image(
                                                                painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
                                                                contentDescription = "Right arrow"
                                                            )
                                                        }
                                                        Text(
                                                            text = "If you manually turn on/off the lights, they will remain in that state for a certain amount of seconds, regardless of movements within the room.",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                } else if (it == 1) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Bottom
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(25.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.baseline_keyboard_arrow_left_24),
                                                                contentDescription = "Left arrow"
                                                            )
                                                            Text(
                                                                text = "Swipe left to control lights",
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                        }
                                                        Text(
                                                            text = "Blinds automatically open when someone enters the room for the first time of the day, then they will be totally manual.",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
            }
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
            Log.d("CONNECTION", "Connected to $deviceName")
        } catch (e: IOException) {
            onError("Cannot connect to device $deviceName")
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
