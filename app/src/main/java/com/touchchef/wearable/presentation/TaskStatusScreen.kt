import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.touchchef.wearable.presentation.theme.TouchChefTypography

@Composable
fun TaskStatusScreen(
    avatarColor: String,
    onCompleted: () -> Unit,
    onCancelled: () -> Unit,
    onHelp: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            parseColor("#$avatarColor"),
                            parseColor("#$avatarColor")
                        )
                    )
                )
        ){
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Completed Button
                        Button(
                            onClick = onCompleted,
                            modifier = Modifier
                                .width(120.dp)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFE8FFE3)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Terminé",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF417E00),
                                    fontFamily = TouchChefTypography.bricolageGrotesque
                                )
                                Text(
                                    text = "✅",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TouchChefTypography.bricolageGrotesque
                                )
                            }
                        }

                        // Cancelled Button
                        Button(
                            onClick = onCancelled,
                            modifier = Modifier
                                .width(120.dp)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFFFF4F4)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Annulé",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF2626),
                                    fontFamily = TouchChefTypography.bricolageGrotesque
                                )
                                Text(
                                    text = "❌",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Help Button
                        Button(
                            onClick = onHelp,
                            modifier = Modifier
                                .width(120.dp)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFE3F6FF)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aide",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0084FF),
                                    fontFamily = TouchChefTypography.bricolageGrotesque
                                )
                                Text(
                                    text = "🤚",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Back Button
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
                    }
                }
            }
        }

    }
}

fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF87CEEB) // Default color if parsing fails
    }
}