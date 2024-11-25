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
    private val serverUrl = "ws://websocket.chhilif.com:8080"
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    var isConnected = false
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

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
