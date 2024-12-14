package com.touchchef.wearable.presentation

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

data class WebSocketMessage(
    val from: String,
    val to: String,
    val type: String,
    val assignedTask: AssignedTask? = null
)

data class TaskData(
    val type: String,
    val quantity: Int
)

// Updated WebSocketClient
class WebSocketClient {
    private val serverUrl = "ws://websocket.chhilif.com/ws"
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    var isConnected = false
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    // Add a list of message listeners
    private val messageListeners = mutableListOf<(WebSocketMessage) -> Unit>()

    fun addMessageListener(listener: (WebSocketMessage) -> Unit) {
        messageListeners.add(listener)
    }

    fun removeMessageListener(listener: (WebSocketMessage) -> Unit) {
        messageListeners.remove(listener)
    }

    fun connect(
        onConnected: () -> Unit,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessage(text)
                // Parse the message and notify listeners
                try {
                    val message = gson.fromJson(text, WebSocketMessage::class.java)
                    messageListeners.forEach { listener ->
                        listener(message)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message: $text", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                onError(t.message ?: "Unknown error")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
            }
        })
    }

    fun sendMessage(message: String, onResult: (Boolean) -> Unit) {
        if (isConnected && webSocket != null) {
            val success = webSocket!!.send(message)
            onResult(success)
        } else {
            onResult(false)
        }
    }

    fun sendJson(message: Any, onResult: (Boolean) -> Unit) {
        if (webSocket != null) {
            val jsonString = gson.toJson(message)
            Log.d("WebSocket", "Sending JSON: $jsonString")
            val success = webSocket!!.send(jsonString)
            onResult(success)
        } else {
            onResult(false)
        }
    }

    // Add cleanup method
    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        messageListeners.clear()
    }
}
