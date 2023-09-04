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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                BluetoothConnectScreen()
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
    @Composable
    fun BluetoothConnectScreen() {
        var outMessage by remember { mutableStateOf("") }
        var connected by remember { mutableStateOf(false) }
        var connecting by remember { mutableStateOf(false) }
        var txt by remember { mutableStateOf("") }

        var btMessage by remember { mutableStateOf("") }
        var brightness by remember { mutableStateOf(0.0) }
        var blindsLevel by remember { mutableStateOf(0.0) }

        val buttonClick = { deviceName: String ->
            if (!arePermissionsGranted(requiredPermissions)) {
                Log.d("CONNECTION", "Requesting permissions")
                requestPermissions()
                Log.d("CONNECTION", "Requested")
            } else {

                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    Log.e("CONNECTION", "Bluetooth is not available or turned off")
                    txt = "Bluetooth is not available or turned off."
                    requestPermissions()
                } else {
                    Log.d("CONNECTION", "Connecting...")
                    connecting = true
                    connectToDevice(
                        deviceName,
                        onError = { }
                    )
                    connecting = false
                    connected = true
                    beginListeningForMessages { message ->
                        //Log.d("MESSAGE", message)

                        // Aggiungi il messaggio ricevuto al buffer
                        btMessage += message

                        // Cerca l'indice dell'inizio del primo messaggio nel buffer
                        var startIndex = btMessage.indexOf('|')

                        // Continua a cercare messaggi finché ce ne sono
                        while (startIndex != -1) {
                            // Cerca l'indice della fine del primo messaggio nel buffer
                            val endIndex = btMessage.indexOf('&', startIndex)

                            if (endIndex != -1) {
                                // Se la fine del messaggio è presente nel buffer, estrai il messaggio e gestiscilo
                                val json = btMessage.substring(startIndex + 1, endIndex)

                                try {
                                    val map = JSONObject(json)

                                    txt = map.toString()
                                    brightness = map["brightness"].toString().trim().toDouble() / 1024
                                } catch (e: JSONException) {
                                    Log.e("MESSAGE", "Error parsing JSON: $json")
                                }

                                // Rimuovi il messaggio dal buffer
                                btMessage = btMessage.substring(endIndex + 1)
                            } else {
                                // Se la fine del messaggio non è presente nel buffer, lascia il messaggio nel buffer e interrompi la ricerca
                                break
                            }

                            // Cerca l'indice dell'inizio del prossimo messaggio nel buffer
                            startIndex = btMessage.indexOf('|')
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Black, CircleShape)
                        .background(if (connected) Color.Green else (if (connecting) Color.Yellow else Color.Red))
                ) {

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = outMessage,
                onValueChange = { outMessage = it },
                enabled = connected
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        sendMessage(outMessage)
                    },
                    enabled = connected
                ) {
                    Text("Send custom message")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        sendMessage("|{\"lights\":\"on\"}&")
                    },
                    enabled = connected
                ) {
                    Text("Lights ON")
                }

                Button(
                    onClick = {
                        sendMessage("|{\"lights\":\"off\"}&")
                    },
                    enabled = connected
                ) {
                    Text("Lights OFF")
                }
            }

            Slider(
                value = blindsLevel.toFloat(),
                onValueChange = { blindsLevel = it.toDouble() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        sendMessage("|{\"blinds\":\"" + (blindsLevel * 100).toInt().toString() + "\"}&")
                    },
                    enabled = connected
                ) {
                    Text("Move blinds")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(modifier = Modifier.padding(horizontal = 5.dp, vertical = 0.dp), text = txt, color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(16.dp))

            Log.d("BRIGHTNESS", brightness.toString())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .padding(10.dp)
                    .border(2.dp, Color.Black),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .border(2.dp, Color.Red)
                        .fillMaxHeight(brightness.toFloat())
                        .fillMaxWidth()
                ) {

                }
            }

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
