package com.pairplay.presentation.ui.result

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairplay.domain.models.SessionResult
import com.pairplay.presentation.ui.components.GradientButton

@Composable
fun ResultScreen(
    result: SessionResult,
    myId: String,
    playerName: (String) -> String,
    onRestart: () -> Unit
) {
    val iWin = !result.isDraw && result.winnerId == myId
    var triggered by remember { mutableStateOf<Boolean>(false) }
    LaunchedEffect(Unit) { triggered = true }
    val scaleAnim = animateFloatAsState(
        targetValue = if (triggered) 1f else 0.3f,
        animationSpec = tween(600), label = "scale"
    )

    val bg = when {
        result.isDraw -> listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
        iWin -> listOf(Color(0xFF10B981), Color(0xFF06B6D4))
        else -> listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(bg)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            Icon(
                imageVector = when {
                    result.isDraw -> Icons.Filled.SentimentSatisfied
                    iWin -> Icons.Filled.EmojiEvents
                    else -> Icons.Filled.SentimentDissatisfied
                },
                contentDescription = null,
                modifier = Modifier.size(120.dp).graphicsLayer(scaleX = scaleAnim.value, scaleY = scaleAnim.value),
                tint = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = when {
                    result.isDraw -> "Match nul !"
                    iWin -> "Victoire !"
                    else -> "Défaite"
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            val winnerName = result.winnerId?.let(playerName) ?: "—"
            Text(
                text = if (result.isDraw) "Aucun gagnant" else "Gagnant : $winnerName",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Totaux", fontWeight = FontWeight.Bold,
                        color = Color.White, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    result.perPlayerTotals.entries
                        .sortedByDescending { it.value }
                        .forEach { (pid, total) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(playerName(pid),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp)
                                Text("$total pts",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 20.sp)
                            }
                        }
                }
            }

            Spacer(Modifier.height(24.dp))
            GradientButton(
                text = "Rejouer",
                icon = Icons.Filled.Replay,
                colors = listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.15f)),
                onClick = onRestart
            )
        }
    }
}
