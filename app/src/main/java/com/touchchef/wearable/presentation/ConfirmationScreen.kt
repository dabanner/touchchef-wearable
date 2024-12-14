package com.touchchef.wearable.presentation

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.google.gson.Gson
import com.touchchef.wearable.R
import com.touchchef.wearable.utils.FeedbackManager

@Composable
fun ConfirmationScreen(
    webSocketClient: WebSocketClient,
    deviceId: String,
    name: String,
    avatar: String,
    onConfirmation: () -> Unit
) {
    val bricolageGrotesque = FontFamily(
        Font(R.font.bricolagegrotesque_medium, FontWeight.Normal),
        Font(R.font.bricolagegrotesque_bold, FontWeight.Bold),
        Font(R.font.bricolagegrotesque_light, FontWeight.Light)
    )
    val avatarResource = when (avatar) {
        "1" -> R.drawable.one
        "2" -> R.drawable.two
        "3" -> R.drawable.three
        "4" -> R.drawable.four
        "5" -> R.drawable.five
        "6" -> R.drawable.six
        "7" -> R.drawable.seven
        "8" -> R.drawable.eight
        "9" -> R.drawable.nine
        else -> R.drawable.one
    }
    var isMessageSent = false
    val gson = Gson()
    val context = LocalContext.current
    val feedbackManager = remember { FeedbackManager(context) }
    var hasPlayedFeedback by remember { mutableStateOf(false) }

    val message = mapOf("type" to "confirmation", "to" to "angular", "from" to deviceId)
    val jsonString = gson.toJson(message)
    Log.d("ConfirmationScreen", "Sending JSON: $jsonString")
    webSocketClient.sendMessage(jsonString, onResult = { result ->
        isMessageSent = result
    })

    // Effet pour gérer le feedback
    LaunchedEffect(isMessageSent) {
        if (isMessageSent && !hasPlayedFeedback) {
            feedbackManager.playSuccessFeedback()
            hasPlayedFeedback = true
        }
    }

    // Cleanup lors de la destruction du composable
    DisposableEffect(Unit) {
        onDispose {
            feedbackManager.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        when {
            isMessageSent -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(avatarResource),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(130.dp)
                    )
                    Text(
                        text = "Bienvenue, $name !",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = bricolageGrotesque,
                        color = Color(0xFFFFC403)
                    )
                    Text(
                        text = "En attente du lancement de la partie...",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = bricolageGrotesque,
                        color = Color.Black
                    )
                }
            }
            else -> {
                Text(
                    text = "Erreur : Veuillez vous connecter au réseau WiFi du chef cuisinier.",
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = bricolageGrotesque,
                )
            }
        }

    }
    Button(onClick = onConfirmation) {
        Text("Confirmer")
    }
}
