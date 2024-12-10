package com.touchchef.wearable.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    deviceId: String
) {
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showCookSelectionDialog by remember { mutableStateOf(false) }
    val helpRequest by taskHelpService.helpRequestFlow.collectAsState()

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
                        .width(120.dp)
                        .padding(bottom = 8.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF5EFF61),
                        endBackgroundColor = Color(0xFF5EFF61),
                    ),
                    contentColor = Color.Black,
                ) {
                    Text(
                        text = "Tâche #${request.taskId}, assistant : ${request.to}",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
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
                modifier = Modifier.width(120.dp)
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
                modifier = Modifier.width(120.dp)
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
                    Button(
                        onClick = {
                            taskHelpService.requestHelp("123", "Cuisinier 1")
                            showCookSelectionDialog = false
                        },
                        modifier = Modifier.size(width = 100.dp, height = 30.dp)
                    ) {
                        Text("Cuisinier 1", fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            taskHelpService.requestHelp("123", "Cuisinier 2")
                            showCookSelectionDialog = false
                        },
                        modifier = Modifier.size(width = 100.dp, height = 30.dp)
                    ) {
                        Text("Cuisinier 2", fontSize = 10.sp)
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
                            modifier = Modifier.size(width = 60.dp, height = 30.dp)
                        ) {
                            Text("Oui", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { showNotificationDialog = false },
                            modifier = Modifier.size(width = 60.dp, height = 30.dp)
                        ) {
                            Text("Non", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}