package com.pairplay.games.stability

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.models.Category
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Maintenir le téléphone stable. Score = 100 - (somme pondérée des mouvements).
 */
class StabilityGame : MiniGame {
    override val id = "stability"
    override val displayName = "Main Ferme"
    override val description = "Garde ton téléphone parfaitement immobile"
    override val category = Category.SENSOR
    override val durationMs = 10_000L

    @Composable
    override fun Content(seed: Long, onFinish: (Int) -> Unit) {
        val ctx = LocalContext.current
        val sensorManager = remember {
            ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

        var deviation by remember { mutableFloatStateOf(0f) }
        var remaining by remember { mutableLongStateOf(durationMs) }
        var penalty by remember { mutableFloatStateOf(0f) }

        val listener = remember {
            object : SensorEventListener {
                var lastMag: Float = 9.81f
                override fun onSensorChanged(e: SensorEvent) {
                    val mag = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
                    val delta = abs(mag - lastMag)
                    lastMag = mag
                    deviation = (deviation * 0.8f + delta * 0.2f)
                    if (delta > 0.4f) penalty += delta
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
        }

        DisposableEffect(Unit) {
            accelerometer?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }
            onDispose { sensorManager.unregisterListener(listener) }
        }

        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < durationMs) {
                remaining = durationMs - (System.currentTimeMillis() - start)
                delay(50)
            }
            val score = (100 - (penalty * 4).toInt()).coerceIn(0, 100)
            onFinish(score)
        }

        val pulse by animateFloatAsState(targetValue = 1f - (deviation / 10f).coerceIn(0f, 1f), label = "pulse")

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            Text("🤚 Main ferme", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Ne bouge plus !",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF22D3EE).copy(alpha = 0.9f * pulse),
                                Color(0xFF6366F1).copy(alpha = 0.2f)
                            ),
                            center = c,
                            radius = size.minDimension / 2f
                        ),
                        radius = size.minDimension / 2f * (0.6f + 0.4f * pulse)
                    )
                }
                Text(
                    "${remaining / 1000 + 1}s",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            LinearProgressIndicator(
                progress = { 1f - (remaining.toFloat() / durationMs) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
            )
            Text("Pénalité : ${penalty.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
