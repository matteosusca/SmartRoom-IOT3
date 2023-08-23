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
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
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
            BluetoothConnectScreen()
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
        var deviceName by remember { mutableStateOf(TextFieldValue()) }
        var connecting by remember { mutableStateOf(false) }
        var errorText by remember { mutableStateOf("") }

        var btMessage by remember { mutableStateOf("") }
        var graph = Graph(10)
        val graphState = rememberUpdatedState(graph)

        val context = LocalContext.current

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = deviceName.text,
                onValueChange = { deviceName = TextFieldValue(it) },
                placeholder = { Text("Enter Device Name") },
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!arePermissionsGranted(requiredPermissions)) {
                        Log.d("CONNECTION", "Requesting permissions")
                        requestPermissions()
                        Log.d("CONNECTION", "Requested")
                        return@Button
                    }

                    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                        Log.e("CONNECTION", "Bluetooth is not available or turned off")
                        errorText = "Bluetooth is not available or turned off."
                    } else {
                        Log.d("CONNECTION", "Connecting...")
                        connecting = true
                        connectToDevice(
                            deviceName.text,
                            onError = { errorMessage -> errorText = errorMessage },
                            onMessageReceived = { message ->
                                Log.d("MESSAGE", message)
                                Log.d("MESSAGE", "Before: $btMessage")

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
                                            Log.d("JSONNNN", map["pir"].toString())
                                            Log.d("JSONNNN", map["light"].toString())
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

                                Log.d("MESSAGE", "After: $btMessage")
                            }
                        )
                    }
                },
                enabled = !connecting
            ) {
                Text(text = "Connect")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Error:")
            Text(text = errorText, color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(16.dp))

            ComposableGraph(values = graphState.value)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectToDevice(deviceName: String, onError: (String) -> Unit, onMessageReceived: (String) -> Unit) {
        Log.d("CONNECTION", "Trying to connect to $deviceName")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e("CONNECTION", "Error: no permissions")
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val targetDevice = pairedDevices?.find { it.name == deviceName }

        if (targetDevice == null) {
            onError("Device not found or not paired.")
            return
        }

        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
        val socket: BluetoothSocket

        try {
            socket = targetDevice.createRfcommSocketToServiceRecord(uuid)
            socket.connect()

            // Connection successful, handle further communication
            Log.d("CONNECTION", "Connected to $deviceName")

            // Stampa sul log ogni messaggio ricevuto dal seriale del dispositivo
            val inputStream = socket.inputStream
            val buffer = ByteArray(1024)

            val serReadThread = Thread {
                this.isThreadReading = true
                while (isThreadReading) {
                    try {
                        val bytes = inputStream.read(buffer)
                        val readMessage = String(buffer, 0, bytes)
                        //Log.d("MESSAGE", "Received: $readMessage")
                        onMessageReceived(readMessage)
                    } catch (e: IOException) {
                        onError("Error reading from Bluetooth socket.")
                        break
                    }
                }
                inputStream.close()
            }

            serReadThread.start()

        } catch (e: IOException) {
            onError("Error establishing Bluetooth connection.")
        }
    }
}
