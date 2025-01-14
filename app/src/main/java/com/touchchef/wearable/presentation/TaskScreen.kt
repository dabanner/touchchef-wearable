package com.touchchef.wearable.presentation

import android.util.Log
import androidx.compose.foundation.background
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
import com.touchchef.wearable.presentation.theme.TouchChefTypography

@Composable
fun TaskScreen(
    taskHelpService: TaskHelpService,
    cookManagementService: CookManagementService,
    deviceId: String,
    taskName: String,
    onBack: () -> Unit
) {
    val activeRequest by taskHelpService.activeRequestFlow.collectAsState()
    val allPendingRequests by taskHelpService.pendingRequestsFlow.collectAsState()
    var showPendingRequests by remember { mutableStateOf(false) }
    var showCookSelectionDialog by remember { mutableStateOf(false) }
    val cooks by cookManagementService.cooksFlow.collectAsState()

    val incomingRequests by remember(allPendingRequests, deviceId) {
        derivedStateOf {
            allPendingRequests.filter { request ->
                request.status == TaskRequestStatus.PENDING && request.to == deviceId
            }
        }
    }

    val outgoingRequests by remember(allPendingRequests, deviceId) {
        derivedStateOf {
            allPendingRequests.filter { request ->
                request.status == TaskRequestStatus.PENDING && request.from == deviceId
            }
        }
    }

    LaunchedEffect(activeRequest, incomingRequests, outgoingRequests) {
        Log.d("TaskScreen", "Active Request Updated: $activeRequest")
        Log.d("TaskScreen", "Incoming requests: $incomingRequests")
        Log.d("TaskScreen", "Outgoing requests: $outgoingRequests")
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Active Task Section
        item {
            activeRequest?.let { request ->
                ActiveTaskSection(
                    helpRequest = request,
                    cooks = cooks,
                    currentDeviceId = deviceId,
                    onCancelTask = { taskHelpService.cancelTask(request.taskId, request) },
                    onCompleteTask = { taskHelpService.completeTask(request.taskId, request) }
                )
            }
        }

        // Pending Requests Section
        item {
            PendingRequestsSection(
                incomingCount = incomingRequests.size,
                outgoingCount = outgoingRequests.size,
                onShowRequests = { showPendingRequests = true }
            )
        }

        // Action Buttons
        item {
            ActionButtonsSection(
                onSpecificHelpClick = { showCookSelectionDialog = true },
                onGeneralHelpClick = { taskHelpService.notifyAllParticipants(taskName) },
                onBack = onBack
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    if (showCookSelectionDialog) {
        CookSelectionDialog(
            cooks = cooks.filter { it.deviceId != deviceId },
            onDismiss = { showCookSelectionDialog = false },
            onCookSelected = { cook ->
                taskHelpService.requestHelp(taskName, cook.deviceId)
                showCookSelectionDialog = false
            }
        )
    }

    if (showPendingRequests) {
        PendingRequestsDialog(
            incomingRequests = incomingRequests,
            outgoingRequests = outgoingRequests,
            onAccept = { request ->
                taskHelpService.acceptTask(request.taskId, request.from)
            },
            onDismiss = { request ->
                taskHelpService.clearRequest(request.taskId)
            },
            onClose = { showPendingRequests = false }
        )
    }
}

@Composable
private fun ActiveTaskSection(
    helpRequest: TaskHelpRequest,
    cooks: List<Cook>,
    currentDeviceId: String,
    onCancelTask: () -> Unit,
    onCompleteTask: () -> Unit
) {
    var showTaskActionsDialog by remember { mutableStateOf(false) }

    if (helpRequest.status == TaskRequestStatus.ACCEPTED) {
        val otherCookId = if (helpRequest.from == currentDeviceId) {
            helpRequest.to
        } else {
            helpRequest.from
        }

        val otherCook = cooks.find { it.deviceId == otherCookId }

        Card(
            onClick = { showTaskActionsDialog = true },
            modifier = Modifier.width(140.dp),
            backgroundPainter = CardDefaults.cardBackgroundPainter(
                startBackgroundColor = Color(0xFF525952),
                endBackgroundColor = Color(0xFF525952)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = Color(0xFF6FC96F),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tâche - ${helpRequest.taskId} -",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TouchChefTypography.bricolageGrotesque
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tâche avec ${otherCook?.name ?: "Inconnu"}",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )
            }
        }

        if (showTaskActionsDialog) {
            TaskActionsDialog(
                onDismiss = { showTaskActionsDialog = false },
                onCancel = {
                    onCancelTask()
                    showTaskActionsDialog = false
                },
                onComplete = {
                    onCompleteTask()
                    showTaskActionsDialog = false
                }
            )
        }
    }
}

@Composable
private fun TaskActionsDialog(
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        Card(
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Actions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE8FFE3))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Terminer la tâche",
                            fontSize = 12.sp,
                            color = Color(0xFF417E00),
                            fontFamily = TouchChefTypography.bricolageGrotesque
                        )
                        Text("✅", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFF4F4))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Annuler l'aide",
                            fontSize = 12.sp,
                            color = Color(0xFFFF2626),
                            fontFamily = TouchChefTypography.bricolageGrotesque
                        )
                        Text("❌", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0x4FFFFFFF))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Retour",
                            fontSize = 12.sp,
                            color = Color(0xFF000000),
                            fontFamily = TouchChefTypography.bricolageGrotesque
                        )
                        Text("⬅️", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRequestsSection(
    incomingCount: Int,
    outgoingCount: Int,
    onShowRequests: () -> Unit
) {
    Card(
        onClick = onShowRequests,
        modifier = Modifier.width(120.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFF2196F3),
            endBackgroundColor = Color(0xFF2196F3)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Demandes :",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TouchChefTypography.bricolageGrotesque
            )
            if (incomingCount > 0 || outgoingCount > 0) {
                Text(
                    text = "Reçues: $incomingCount  |  Envoyées: $outgoingCount",
                    fontSize = 10.sp,
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onSpecificHelpClick: () -> Unit,
    onGeneralHelpClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Chip(
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aide spécifique", fontSize = 10.sp,color = Color(0xFF0084FF),
                        fontFamily = TouchChefTypography.bricolageGrotesque)
                }
            },
            onClick = onSpecificHelpClick,
            modifier = Modifier.width(120.dp),
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFFE3F6FF))
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .width(120.dp)
                .height(32.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0x4FFFFFFF)
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Retour",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF000000),
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )
                Text(
                    text = "⬅️",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        /*Chip(
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aide générale", fontSize = 10.sp,color = Color(0xFF0084FF))
                }
            },
            onClick = onGeneralHelpClick,
            modifier = Modifier.width(120.dp),
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFFE3F6FF))
        )*/
    }
}


@Composable
private fun PendingRequestsDialog(
    incomingRequests: List<TaskHelpRequest>,
    outgoingRequests: List<TaskHelpRequest>,
    onAccept: (TaskHelpRequest) -> Unit,
    onDismiss: (TaskHelpRequest) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onClose
    ) {
        Card(
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Demandes en attente",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )

                if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                    Text(
                        text = "Aucune demande",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = TouchChefTypography.bricolageGrotesque
                    )
                } else {
                    if (incomingRequests.isNotEmpty()) {
                        Text(
                            text = "Demandes reçues",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = TouchChefTypography.bricolageGrotesque
                        )
                        incomingRequests.forEach { request ->
                            RequestRow(
                                request = request,
                                showActions = true,
                                onAccept = onAccept,
                                onDismiss = onDismiss
                            )
                        }
                    }

                    if (outgoingRequests.isNotEmpty()) {
                        Text(
                            text = "Demandes envoyées",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = TouchChefTypography.bricolageGrotesque
                        )
                        outgoingRequests.forEach { request ->
                            RequestRow(
                                request = request,
                                showActions = false,
                                onAccept = onAccept,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(
    request: TaskHelpRequest,
    showActions: Boolean,
    onAccept: (TaskHelpRequest) -> Unit,
    onDismiss: (TaskHelpRequest) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tâche #${request.taskId}",
                fontSize = 12.sp,
                fontFamily = TouchChefTypography.bricolageGrotesque
            )
            if (showActions) {
                Text(
                    text = "De: ${request.from}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )
            } else {
                Text(
                    text = "Vers: ${request.to}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )
            }
        }

        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onAccept(request) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE8FFE3))
                ) {
                    Text("✅", fontSize = 12.sp)
                }
                Button(
                    onClick = { onDismiss(request) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFF4F4))
                ) {
                    Text("❌", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CookSelectionDialog(
    cooks: List<Cook>,
    onDismiss: () -> Unit,
    onCookSelected: (Cook) -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        Card(
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choisir un cuisinier",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = TouchChefTypography.bricolageGrotesque
                )

                cooks.forEach { cook ->
                    Button(
                        onClick = { onCookSelected(cook) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC403))
                    ) {
                        Text(cook.name, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}