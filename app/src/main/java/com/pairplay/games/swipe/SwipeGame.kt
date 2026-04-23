package com.pairplay.games.swipe

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.models.Category
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

class SwipeGame : MiniGame {
    override val id = "swipe"
    override val displayName = "Swipe Express"
    override val description = "Swipe dans la direction demandée — le plus vite possible"
    override val category = Category.GESTURE
    override val durationMs = 15_000L

    private enum class Dir(val emoji: String, val icon: ImageVector) {
        UP("⬆", Icons.Filled.ArrowUpward),
        DOWN("⬇", Icons.Filled.ArrowDownward),
        LEFT("⬅", Icons.Filled.ArrowBack),
        RIGHT("➡", Icons.Filled.ArrowForward);
    }

    @Composable
    override fun Content(seed: Long, onFinish: (Int) -> Unit) {
        val rng = remember(seed) { Random(seed) }
        var target by remember { mutableStateOf(Dir.entries.random(rng)) }
        var hits by remember { mutableIntStateOf(0) }
        var misses by remember { mutableIntStateOf(0) }
        var remaining by remember { mutableLongStateOf(durationMs) }
        var feedback by remember { mutableStateOf<Boolean?>(null) }
        val feedbackAlpha by animateFloatAsState(
            targetValue = if (feedback != null) 1f else 0f, label = "fb"
        )

        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < durationMs) {
                remaining = durationMs - (System.currentTimeMillis() - start)
                delay(50)
            }
            val score = (hits * 10 - misses * 3).coerceIn(0, 100)
            onFinish(score)
        }

        LaunchedEffect(feedback) {
            if (feedback != null) { delay(300); feedback = null }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("👆 Swipe Express", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("✅ $hits", style = MaterialTheme.typography.titleMedium, color = Color(0xFF10B981))
                Text("❌ $misses", style = MaterialTheme.typography.titleMedium, color = Color(0xFFEF4444))
            }

            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .pointerInput(target) {
                        detectDragGestures(
                            onDragEnd = { /* no-op */ }
                        ) { change, drag ->
                            change.consume()
                            val (dx, dy) = drag
                            if (abs(dx) > 60f || abs(dy) > 60f) {
                                val detected = when {
                                    abs(dx) > abs(dy) && dx > 0 -> Dir.RIGHT
                                    abs(dx) > abs(dy) && dx < 0 -> Dir.LEFT
                                    dy > 0 -> Dir.DOWN
                                    else -> Dir.UP
                                }
                                if (detected == target) { hits++; feedback = true }
                                else { misses++; feedback = false }
                                target = Dir.entries.random(rng)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    target.icon, contentDescription = null,
                    modifier = Modifier.size(160.dp),
                    tint = Color.White
                )
                when (feedback) {
                    true -> Text("+10", fontSize = 64.sp, color = Color(0xFF10B981),
                        fontWeight = FontWeight.Black, modifier = Modifier.alpha(feedbackAlpha))
                    false -> Text("-3", fontSize = 64.sp, color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Black, modifier = Modifier.alpha(feedbackAlpha))
                    null -> Unit
                }
            }
            LinearProgressIndicator(
                progress = { 1f - (remaining.toFloat() / durationMs) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
            )
        }
    }
}
