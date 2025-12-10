package com.example.esdl_term_project

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.esdl_term_project.ui.theme.Esdl_term_projectTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManagerWrapper: com.example.esdl_term_project.BluetoothManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (!granted) {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter
        } else {
            BluetoothAdapter.getDefaultAdapter()
        }
        
        bluetoothManagerWrapper = com.example.esdl_term_project.BluetoothManager(adapter)

        setContent {
            Esdl_term_projectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val connectionState by bluetoothManagerWrapper.connectionState.collectAsState()
                    val gameStatus by bluetoothManagerWrapper.gameStatus.collectAsState()
                    val scope = rememberCoroutineScope()

                    GameScreen(
                        connectionState = connectionState,
                        gameStatus = gameStatus,
                        onConnect = { device ->
                            scope.launch { bluetoothManagerWrapper.connect(device) }
                        },
                        onDisconnect = { bluetoothManagerWrapper.disconnect() },
                        onSendMessage = { message ->
                             scope.launch { bluetoothManagerWrapper.sendMessage(message) }
                        },
                        modifier = Modifier.padding(innerPadding),
                        onCheckPermissions = { checkPermissions() },
                        bluetoothAdapter = adapter
                    )
                }
            }
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
             requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

@Composable
fun GameScreen(
    connectionState: ConnectionState,
    gameStatus: GameStatus?,
    onConnect: (android.bluetooth.BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCheckPermissions: () -> Unit,
    bluetoothAdapter: BluetoothAdapter?
) {
    var showDeviceList by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --- Header Section ---
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "RETRO DUCK HUNT",
            fontSize = 30.sp, // Reduced from 36.sp to fit one line
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp, // Reduced spacing
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth() // Ensure it takes full width for centering
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Target Practice Simulator",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        // --- Connection Status Card ---
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = when(connectionState) {
                    is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.2f)
                    is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha=0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (val state = connectionState) {
                            is ConnectionState.Connected -> "Bluetooth connected"
                            is ConnectionState.Connecting -> "Connecting..."
                            is ConnectionState.Disconnected -> "Bluetooth disconnected"
                            is ConnectionState.Error -> "Error: ${state.message}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Action Button next to status
                if (connectionState is ConnectionState.Disconnected || connectionState is ConnectionState.Error) {
                    Button(
                        onClick = { 
                            onCheckPermissions()
                            showDeviceList = true 
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("CONNECT")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onDisconnect() },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("DISCONNECT")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Game Dashboard ---
        if (gameStatus != null || connectionState is ConnectionState.Connected) {
             GameDashboard(gameStatus, onSendMessage)
        } else {
             Box(
                 modifier = Modifier.fillMaxWidth().weight(1f),
                 contentAlignment = Alignment.Center
             ) {
                 Text(
                     text = "Connect to device to start",
                     style = MaterialTheme.typography.bodyLarge,
                     color = MaterialTheme.colorScheme.outline
                 )
             }
        }
        
        if (showDeviceList) {
            DeviceListDialog(
                adapter = bluetoothAdapter,
                onDismiss = { showDeviceList = false },
                onDeviceSelected = { device ->
                    showDeviceList = false
                    onConnect(device)
                }
            )
        }
    }
}

@Composable
fun GameDashboard(status: GameStatus?, onSendMessage: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CURRENT SCORE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${status?.score ?: 0}",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary, // Retro Green?
                    lineHeight = 96.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time Card (Smaller)
        Card(
            modifier = Modifier.fillMaxWidth(0.6f),
             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val remainingTime = status?.time ?: 0
                val timerColor = if (remainingTime <= 5) {
                    MaterialTheme.colorScheme.error // Red color for warning
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }

                Text(
                    text = "TIME REMAINING",
                    style = MaterialTheme.typography.labelSmall,
                    color = timerColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${remainingTime}s",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = timerColor,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onSendMessage("CMD,START\n") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("START GAME")
            }
            
            Button(
                onClick = { onSendMessage("CMD,RESET\n") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("RESET GAME")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceListDialog(
    adapter: BluetoothAdapter?,
    onDismiss: () -> Unit,
    onDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit
) {
    if (adapter == null || !adapter.isEnabled) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
            title = { Text("Bluetooth Unavailable") },
            text = { Text("Please enable Bluetooth on your device.") },
            icon = { Text("⚠️") } // Simple text icon if vector not available
        )
        return
    }

    val pairedDevices = try {
        adapter.bondedDevices.toList()
    } catch (e: SecurityException) {
        emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
             TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Select Device", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                if (pairedDevices.isEmpty()) {
                    item { 
                        Text(
                            "No paired devices found.\nPlease pair your HC-06 module in Android Settings first.",
                            modifier = Modifier.padding(8.dp)
                        ) 
                    }
                } else {
                    items(pairedDevices) { device ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = device.name ?: "Unknown Device",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

// Helper annotation for simplified permission handling in this view logic
@SuppressLint("MissingPermission")
private fun getPairedDevices(adapter: BluetoothAdapter): List<android.bluetooth.BluetoothDevice> {
    return adapter.bondedDevices.toList()
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    Esdl_term_projectTheme {
        GameScreen(
            connectionState = ConnectionState.Connected("STM32-Game"),
            gameStatus = GameStatus(score = 1500, time = 45), // Set to 5 to trigger red warning
            onConnect = {},
            onDisconnect = {},
            onSendMessage = {},
            onCheckPermissions = {},
            bluetoothAdapter = null
        )
    }
}