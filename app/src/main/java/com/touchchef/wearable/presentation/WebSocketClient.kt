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
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var onTaskMessageReceived: ((Map<String, Any>) -> Unit)? = null

    fun connect(
        onConnected: () -> Unit,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit,
        onTaskMessage: ((Map<String, Any>) -> Unit)? = null
    ) {
        this.onTaskMessageReceived = onTaskMessage

        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson(text, Map::class.java) as Map<String, Any>
                    when (message["type"] as? String) {
                        "help_request", "task_notification", "task_acceptance" -> {
                            onTaskMessageReceived?.invoke(message)
                        }
                        else -> onMessage(text)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Error parsing message", e)
                    onMessage(text)
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
}
