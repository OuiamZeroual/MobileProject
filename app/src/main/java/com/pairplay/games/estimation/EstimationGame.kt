package com.pairplay.games.estimation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.models.Category
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

class EstimationGame : MiniGame {
    override val id = "estimation"
    override val displayName = "Compte-Points"
    override val description = "Estime le nombre de points affichés — 3 manches"
    override val category = Category.QNA
    override val durationMs = 25_000L

    @Composable
    override fun Content(seed: Long, onFinish: (Int) -> Unit) {
        val rng = remember(seed) { Random(seed) }
        var round by remember { mutableIntStateOf(0) }
        var target by remember { mutableIntStateOf(rng.nextInt(30, 120)) }
        var dots by remember { mutableStateOf(generateDots(target, rng)) }
        var guess by remember { mutableFloatStateOf(60f) }
        var totalScore by remember { mutableIntStateOf(0) }
        var remaining by remember { mutableLongStateOf(durationMs) }

        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < durationMs) {
                remaining = durationMs - (System.currentTimeMillis() - start)
                delay(80)
            }
            onFinish(totalScore.coerceAtMost(100))
        }

        fun submit() {
            val err = abs(guess.toInt() - target)
            val points = (40 - err).coerceAtLeast(0)
            totalScore += points
            round++
            if (round >= 3) {
                onFinish(totalScore.coerceAtMost(100))
            } else {
                target = rng.nextInt(30, 120)
                dots = generateDots(target, rng)
                guess = 60f
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🔢 Compte-Points", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("Manche ${round + 1}/3 • Total $totalScore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF0F172A))
                    dots.forEach { (x, y) ->
                        drawCircle(
                            Color(0xFF22D3EE),
                            radius = 8f,
                            center = Offset(x * size.width, y * size.height)
                        )
                    }
                }
            }

            Text("Ton estimation : ${guess.toInt()}",
                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Slider(
                value = guess,
                onValueChange = { guess = it },
                valueRange = 1f..200f
            )
            Button(
                onClick = ::submit,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Valider", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

            LinearProgressIndicator(
                progress = { 1f - (remaining.toFloat() / durationMs) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small)
            )
        }
    }

    private fun generateDots(n: Int, rng: Random): List<Pair<Float, Float>> =
        List(n) { rng.nextFloat() * 0.9f + 0.05f to rng.nextFloat() * 0.9f + 0.05f }
}
