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
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartroom.utils.ComposableGraph
import com.example.smartroom.utils.Graph
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

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    fun BluetoothConnectScreen() {
        var outMessage by remember { mutableStateOf("") }
        var connecting by remember { mutableStateOf(false) }
        var txt by remember { mutableStateOf("") }

        var btMessage by remember { mutableStateOf("") }
        var light by remember { mutableStateOf(0.0) }

        val context = LocalContext.current

        val buttonClick = { deviceName: String ->
            if (!arePermissionsGranted(requiredPermissions)) {
                Log.d("CONNECTION", "Requesting permissions")
                requestPermissions()
                Log.d("CONNECTION", "Requested")
            } else {

                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    Log.e("CONNECTION", "Bluetooth is not available or turned off")
                    txt = "Bluetooth is not available or turned off."
                } else {
                    Log.d("CONNECTION", "Connecting...")
                    connecting = true
                    connectToDevice(
                        deviceName,
                        onError = { }
                    )
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
                                    txt = "" + map["pir"] + ", " + map["light"]
                                    light = map["light"].toString().trim().toDouble() / 1024
                                } catch (e: IOException) {
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
            TextField(
                value = outMessage,
                onValueChange = { outMessage = it },
                enabled = connecting
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        buttonClick("DSD TECH HC-05")
                    },
                    enabled = !connecting
                ) {
                    Text(text = "Connect to DSD TECH HC-05")
                }

                Button(
                    onClick = {
                        buttonClick("HC-06")
                    },
                    enabled = !connecting
                ) {
                    Text(text = "Connect to HC-06")
                }
            }

            Button(
                onClick = {
                    sendMessage(outMessage)
                },
                enabled = connecting
            ) {
                Text("Send message")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(modifier = Modifier.padding(horizontal = 5.dp, vertical = 0.dp), text = txt, color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(16.dp))

            Log.d("LIGHT", light.toString())

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
                        .fillMaxHeight(light.toFloat())
                        .fillMaxWidth()
                ) {

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
        if (socket.isConnected) {
            val outputStream = socket.outputStream
            outputStream.write(message.toByteArray())
            outputStream.flush()
            Log.d("MESSAGE-SND", message)
        }
    }
}
