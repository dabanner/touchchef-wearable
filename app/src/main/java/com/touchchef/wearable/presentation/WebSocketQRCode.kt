package com.touchchef.wearable.presentation

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.google.gson.Gson
import com.touchchef.wearable.R
import com.touchchef.wearable.data.DevicePreferences
import kotlinx.coroutines.flow.first

@Composable
fun WebSocketQRCode(
    webSocketClient: WebSocketClient,
    context: android.content.Context,
    qrCodeGenerator: QRCodeGenerator,
    navigateToConfirmationScreen: (String, String, String) -> Unit // Cette fonction permet de naviguer vers l'écran de confirmation
) {
    val isConnected = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val gson = Gson()
    val devicePreferences = DevicePreferences(context)
    var cachedDeviceId = "null"

    val bricolageGrotesque = FontFamily(
        Font(R.font.bricolagegrotesque_medium, FontWeight.Normal),
        Font(R.font.bricolagegrotesque_bold, FontWeight.Bold),
        Font(R.font.bricolagegrotesque_light, FontWeight.Light)
    )

    LaunchedEffect(Unit) {
        devicePreferences.deviceId.first()?.let {cachedDeviceId = it}
        webSocketClient.connect(
            onConnected = {
                val message = mapOf("type" to "testConnection")
                webSocketClient.sendJson( message,  onResult = { success ->
                    if (success) {
                        isConnected.value = true
                        Log.d("WebSocket", "Connected to WebSocket server")
                    } else {
                        errorMessage.value = "Erreur : Veuillez vous connecter au réseau WiFi du chef cuisinier."
                        Log.e("WebSocket", "Failed to send message")
                    }
                })
            },
            onMessage = { message ->
                Log.d("WebSocket", "Received message: $message")
                // Désérialisation du message
                val response = gson.fromJson(message, Map::class.java)
                val to = response["to"] as? String
                val type = response["type"] as? String

                // Vérification si le `to` correspond à notre `deviceId`
                if (to == cachedDeviceId && type == "addCook") {
                    // On reçoit les infos du cuisinier
                    val name = response["name"] as? String ?: "Inconnu"
                    val avatar = response["avatar"] as? String ?: "a.png"

                    // On navigue vers l'écran de confirmation avec le nom et l'avatar
                    navigateToConfirmationScreen(name, avatar, cachedDeviceId)
                }
            },
            onError = { message ->
                errorMessage.value = message
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        when {
            errorMessage.value != null -> {
                Text(
                    text = errorMessage.value!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = bricolageGrotesque
                )
            }

            isConnected.value -> {
                qrCodeGenerator.initialize(context)

                val qrCodeBitmap = remember {
                    qrCodeGenerator.generateDeviceQRCode(context, 200)
                }

                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(140.dp)
                    )

                    Text(
                        text = "Scan moi chef \uD83E\uDDD1\u200D\uD83C\uDF73 !",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = bricolageGrotesque
                    )
                }
            }

            else -> {
                Text(
                    text = "Connexion en cours...",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = bricolageGrotesque
                )
            }
        }
    }
}
