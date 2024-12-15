package com.touchchef.wearable.presentation

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

class WebSocketClient {
    private val serverUrl = "ws://websocket.chhilif.com/ws"
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    var isConnected = false
    private var deviceId: String = ""

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()


    private var onTaskMessageReceived: ((Map<String, Any>) -> Unit)? = null

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())



    fun initialize(deviceId: String) {
        Log.d("WebSocketClient", "Device ID : ${deviceId}")
        this.deviceId = deviceId
        Log.d("WebSocketClient", "Initialized with deviceId: $deviceId")
    }

    fun connect(
        onConnected: () -> Unit,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit,
        onTaskMessage: (Map<String, Any>) -> Unit,
    ) {
        if (deviceId.isEmpty()) {
            Log.e("WebSocketClient", "DeviceId not initialized")
            onError("DeviceId not initialized: $deviceId")
            return
        }

        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketClient", "WebSocket connection opened")
                isConnected = true
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson(text, Map::class.java) as Map<String, Any>
                    val type = message["type"] as? String
                    val to = message["to"] as? String

                    Log.d("WebSocketClient", "Received raw message on device $deviceId: $text")

                    // Check if this is a task-related message for this device
                    if ((to == deviceId || to == "all" || to == "allWatches") &&
                        type in listOf("help_request", "task_notification", "task_acceptance")) {
                        Log.d("WebSocketClient", "Processing task message for device $deviceId")
                        handler.post { onTaskMessage(message) }
                    } else {
                        // Handle non-task messages
                        handler.post { onMessage(text) }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Error processing message", e)
                    handler.post { onMessage(text) }
                }
            }


            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketClient", "WebSocket failure: ${t.message}", t)
                isConnected = false
                onError(t.message ?: "Unknown error")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "WebSocket closed: $reason")
                isConnected = false
            }
        })
    }

    fun sendMessage(message: String, onResult: (Boolean) -> Unit) {
        if (!isConnected || webSocket == null) {
            Log.e("WebSocketClient", "Failed to send message - not connected")
            onResult(false)
            return
        }

        try {
            val success = webSocket!!.send(message)
            Log.d("WebSocketClient", "Sent message: $message, success: $success")
            onResult(success)
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Error sending message", e)
            onResult(false)
        }
    }

    fun sendJson(message: Any, onResult: (Boolean) -> Unit) {
        if (!isConnected || webSocket == null) {
            Log.e("WebSocketClient", "Failed to send JSON - not connected")
            onResult(false)
            return
        }

        try {
            // Force include deviceId in outgoing messages
            val messageWithDeviceId = when (message) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val mutableMap = (message as Map<String, Any>).toMutableMap()
                    mutableMap["from"] = deviceId  // Always set deviceId as "from"
                    mutableMap
                }
                else -> message
            }

            val jsonString = gson.toJson(messageWithDeviceId)
            Log.d("WebSocketClient", "Sending JSON: $jsonString")
            val success = webSocket!!.send(jsonString)
            onResult(success)
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Error sending JSON message", e)
            onResult(false)
        }
    }
}