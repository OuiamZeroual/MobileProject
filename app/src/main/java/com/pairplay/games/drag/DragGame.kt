package com.pairplay.games.drag

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.models.Category
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.random.Random

class DragGame : MiniGame {
    override val id = "drag"
    override val displayName = "Trace Parfaite"
    override val description = "Attrape la cible et dépose-la au centre — 15s"
    override val category = Category.GESTURE
    override val durationMs = 15_000L

    @Composable
    override fun Content(seed: Long, onFinish: (Int) -> Unit) {
        val rng = remember(seed) { Random(seed) }

        var remaining by remember { mutableLongStateOf(durationMs) }
        var score by remember { mutableIntStateOf(0) }
        var ballX by remember { mutableFloatStateOf(0.2f) }
        var ballY by remember { mutableFloatStateOf(0.3f) }
        var targetX by remember { mutableFloatStateOf(0.75f) }
        var targetY by remember { mutableFloatStateOf(0.75f) }
        var boxW by remember { mutableFloatStateOf(1f) }
        var boxH by remember { mutableFloatStateOf(1f) }

        fun shuffle() {
            targetX = 0.15f + rng.nextFloat() * 0.7f
            targetY = 0.15f + rng.nextFloat() * 0.7f
            ballX = 0.15f + rng.nextFloat() * 0.7f
            ballY = 0.15f + rng.nextFloat() * 0.7f
        }

        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < durationMs) {
                remaining = durationMs - (System.currentTimeMillis() - start)
                delay(50)
            }
            onFinish(score.coerceAtMost(100))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎯 Trace Parfaite", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("Score : $score", style = MaterialTheme.typography.titleMedium)

            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF312E81))
                        )
                    )
                    .onSizeChanged {
                        boxW = it.width.toFloat().coerceAtLeast(1f)
                        boxH = it.height.toFloat().coerceAtLeast(1f)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            ballX = (ballX + drag.x / boxW).coerceIn(0.05f, 0.95f)
                            ballY = (ballY + drag.y / boxH).coerceIn(0.05f, 0.95f)
                            val dx = ballX - targetX; val dy = ballY - targetY
                            if (hypot(dx, dy) < 0.08f) {
                                score += 10
                                shuffle()
                            }
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        Color(0xFF10B981).copy(alpha = 0.35f),
                        radius = size.minDimension * 0.12f,
                        center = Offset(size.width * targetX, size.height * targetY)
                    )
                    drawCircle(
                        Color(0xFF10B981),
                        radius = size.minDimension * 0.06f,
                        center = Offset(size.width * targetX, size.height * targetY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFFACC15), Color(0xFFF59E0B)),
                            center = Offset(size.width * ballX, size.height * ballY),
                            radius = size.minDimension * 0.08f
                        ),
                        radius = size.minDimension * 0.08f,
                        center = Offset(size.width * ballX, size.height * ballY)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { 1f - (remaining.toFloat() / durationMs) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
            )
        }
    }
}

