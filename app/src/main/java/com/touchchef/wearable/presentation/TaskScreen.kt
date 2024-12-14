package com.touchchef.wearable.presentation

import android.text.style.BulletSpan
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog

@Composable
fun TaskScreen(
    taskHelpService: TaskHelpService,
    cookManagementService: CookManagementService,
    deviceId: String
) {
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showCookSelectionDialog by remember { mutableStateOf(false) }
    val helpRequest by taskHelpService.helpRequestFlow.collectAsState()
    val cooks by cookManagementService.cooksFlow.collectAsState()

    fun getCookName(deviceId: String): String {
        return cooks.find { it.deviceId == deviceId }?.name ?: deviceId
    }


    LaunchedEffect(helpRequest) {
        helpRequest?.let { request ->
            if (request.from != deviceId) {
                showNotificationDialog = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Info Card - Now just informative
            helpRequest?.let { request ->
                Card(
                    onClick = {},
                    modifier = Modifier
                        .width(140.dp)
                        .wrapContentHeight(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF525952),
                        endBackgroundColor = Color(0xFF525952),
                    ),

                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Bullet point
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Color(0xFF6FC96F),
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Task information
                        Text(
                            text = buildString {
                                append("Tâche #")
                                append(request.taskId)
                                append(", assistant : ")
                                append(getCookName(request.to))
                            },
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }

                }
            }
            // Bouton pour demander de l'aide à un cuisinier spécifique
            Chip(
                label = {
                    Text(
                        "Aide spécifique",
                        fontSize = 12.sp
                    )
                },
                onClick = { showCookSelectionDialog = true },
                modifier = Modifier.width(120.dp),
                colors = ChipDefaults.chipColors(
                    backgroundColor = Color(0xFFFFC403),
                ),

            )

            // Bouton pour notifier tous les cuisiniers
            Chip(
                label = {
                    Text(
                        "Aide générale",
                        fontSize = 12.sp
                    )
                },
                onClick = { taskHelpService.notifyAllParticipants("123") },
                modifier = Modifier.width(120.dp),
                colors = ChipDefaults.chipColors(
                    backgroundColor = Color(0xFFFFC403),
                ),
            )

        }
    }

    // Dialog pour choisir un cuisinier spécifique
    if (showCookSelectionDialog) {
        Dialog(
            showDialog = showCookSelectionDialog,
            onDismissRequest = { showCookSelectionDialog = false }
        ) {
            Card(
                modifier = Modifier.padding(4.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choisir un cuisinier",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Liste des cuisiniers (exemple avec des données fictives)
                    cooks.filter { cook -> cook.deviceId != deviceId }.forEach { cook ->
                        Button(
                            onClick = {
                                taskHelpService.requestHelp("123", cook.deviceId)
                                showCookSelectionDialog = false
                            },
                            modifier = Modifier.size(width = 100.dp, height = 30.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFFFC403)
                            )
                        ) {
                            Text(cook.name, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Dialog pour les notifications reçues
    if (showNotificationDialog) {
        Dialog(
            showDialog = showNotificationDialog,
            onDismissRequest = { showNotificationDialog = false }
        ) {
            Card(
                modifier = Modifier.padding(4.dp),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Nouvelle aide",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    helpRequest?.let { request ->
                        Text(
                            text = if (request.to == "all")
                                "Aide générale demandée"
                            else
                                "Aide directe demandée",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                helpRequest?.let { request ->
                                    taskHelpService.acceptTask(request.taskId, request.from)
                                }
                                showNotificationDialog = false
                            },
                            modifier = Modifier.size(width = 60.dp, height = 30.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF8BC34A)
                            )
                        ) {
                            Text("Oui", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { showNotificationDialog = false },
                            modifier = Modifier.size(width = 60.dp, height = 30.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFE91E1E)
                            )
                        ) {
                            Text("Non", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}