package com.touchchef.wearable.presentation

import android.util.Log
import okhttp3.*

class WebSocketClient {
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient()

    fun connect(url: String) {
        Log.i("Connecting", "Currently connecting to ws")
        val request = Request.Builder().url(url).build()
        Log.i("Connecting", request.toString())

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Connection opened
            }


            override fun onMessage(webSocket: WebSocket, text: String) {
                // Received message
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Connection closing
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Connection error
            }
        })

        Log.i("Connecting", webSocket.toString())
    }

    fun sendMessage(message: String) {
        Log.i("Sending", message)
        val result = webSocket.send(message)
        Log.i("Was it sent ?", result.toString())
    }

    fun disconnect() {
        webSocket.close(1000, "Disconnecting")
    }
}