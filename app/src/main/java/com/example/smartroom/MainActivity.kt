package com.example.smartroom

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.smartroom.ui.theme.SmartRoomTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btPerm = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("BT-TEST", it.toString())
        }

        setContent {
            SmartRoomTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")

                    val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
                    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
                    if (bluetoothAdapter == null) {
                        Text("Device doesn't support bluetooth")
                    }

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
    SmartRoomTheme {
        Greeting("Android")
    }
}