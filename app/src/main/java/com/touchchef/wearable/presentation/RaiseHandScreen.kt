package com.touchchef.wearable.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.touchchef.wearable.R

@Composable
fun RaiseHandScreen(
    name: String,
    avatar: String,
    backgroundColor: String,
    onDismiss: () -> Unit
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(parseColor("#$backgroundColor")),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            /*Image(
                painter = painterResource(avatarResource),
                contentDescription = "Avatar de $name",
                modifier = Modifier.size(90.dp)
            )*/

            Text(
                text = "🖐️",
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "RAISE YOUR HAND!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}