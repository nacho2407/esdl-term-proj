package com.example.esdl_term_project

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.UUID

// Standard UUID for SPP (Serial Port Profile)
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

class BluetoothManager(private val bluetoothAdapter: BluetoothAdapter?) {

    private var socket: BluetoothSocket? = null
    private var isRunning = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _gameStatus = MutableStateFlow<GameStatus?>(null)
    val gameStatus: StateFlow<GameStatus?> = _gameStatus

    private val _messageLog = MutableStateFlow<List<String>>(emptyList())
    val messageLog: StateFlow<List<String>> = _messageLog

    suspend fun sendMessage(message: String) {
        if (!isRunning || socket?.isConnected != true) return
        
        try {
            withContext(Dispatchers.IO) {
                socket?.outputStream?.write(message.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            _connectionState.value = ConnectionState.Error("Send Failed")
        }
    }

    suspend fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        try {
            withContext(Dispatchers.IO) {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
            }
            _connectionState.value = ConnectionState.Connected(device.name ?: "Unknown")
            isRunning = true
            startListening()
        } catch (e: IOException) {
            e.printStackTrace()
            _connectionState.value = ConnectionState.Error("Connection Failed")
            socket?.close()
            socket = null
        }
    }

    private fun startListening() {
        Thread {
            val buffer = ByteArray(1024)
            val inputStream: InputStream? = socket?.inputStream
            val messageBuffer = StringBuilder()

            while (isRunning && socket?.isConnected == true) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val chunk = String(buffer, 0, bytes)
                        messageBuffer.append(chunk)

                        // Process all complete lines in the buffer
                        var newlineIndex = messageBuffer.indexOf("\n")
                        while (newlineIndex != -1) {
                            val line = messageBuffer.substring(0, newlineIndex).trim()
                            if (line.isNotEmpty()) {
                                // Add to log (keep last 50)
                                val currentLog = _messageLog.value.toMutableList()
                                currentLog.add(0, "[RX] $line")
                                if (currentLog.size > 50) currentLog.removeAt(currentLog.lastIndex)
                                _messageLog.value = currentLog

                                handleIncomingMessage(line)
                            }
                            messageBuffer.delete(0, newlineIndex + 1)
                            newlineIndex = messageBuffer.indexOf("\n")
                        }
                    }
                } catch (e: IOException) {
                    _connectionState.value = ConnectionState.Disconnected
                    isRunning = false
                    break
                }
            }
        }.start()
    }

    private fun handleIncomingMessage(message: String) {
        // message is now guaranteed to be a single trimmed line
        val status = MessageParser.parseStatus(message)
        if (status != null) {
            _gameStatus.value = status
        }
    }

    fun disconnect() {
        isRunning = false
        try {
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        _connectionState.value = ConnectionState.Disconnected
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
