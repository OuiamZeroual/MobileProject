package com.pairplay.games.blow

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
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
import androidx.core.content.ContextCompat
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.models.Category
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.log10
import kotlin.math.max

class BlowGame : MiniGame {
    override val id = "blow"
    override val displayName = "Coup de Vent"
    override val description = "Souffle le plus fort possible dans le micro"
    override val category = Category.SENSOR
    override val durationMs = 8_000L

    @Composable
    override fun Content(seed: Long, onFinish: (Int) -> Unit) {
        val ctx = LocalContext.current
        var amplitude by remember { mutableIntStateOf(0) }
        var totalBlow by remember { mutableFloatStateOf(0f) }
        var peak by remember { mutableIntStateOf(0) }
        var remaining by remember { mutableLongStateOf(durationMs) }

        val hasMic = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        LaunchedEffect(Unit) {
            val recorder = if (hasMic) runCatching {
                @Suppress("DEPRECATION")
                val r = MediaRecorder()
                r.setAudioSource(MediaRecorder.AudioSource.MIC)
                r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                val out = File.createTempFile("blow", ".3gp", ctx.cacheDir)
                r.setOutputFile(out.absolutePath)
                r.prepare(); r.start(); r
            }.getOrNull() else null

            val start = System.currentTimeMillis()
            try {
                while (System.currentTimeMillis() - start < durationMs) {
                    val a = recorder?.maxAmplitude ?: 0
                    amplitude = a
                    peak = max(peak, a)
                    // Ne compte que les souffles significatifs
                    if (a > 2500) totalBlow += (a / 1000f) * 0.05f
                    remaining = durationMs - (System.currentTimeMillis() - start)
                    delay(50)
                }
            } finally {
                runCatching { recorder?.stop() }
                runCatching { recorder?.release() }
            }
            val score = (totalBlow * 10).toInt().coerceIn(0, 100)
            onFinish(if (hasMic) score else 50)
        }

        val level = (20 * log10(max(1, amplitude).toDouble()) / 100.0).toFloat().coerceIn(0f, 1f)
        val anim by animateFloatAsState(targetValue = level, label = "blow")

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            Text("💨 Coup de Vent", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Souffle dans le micro !", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFEF4444).copy(alpha = 0.3f)),
                            center = c,
                            radius = size.minDimension / 2f * (0.4f + 0.6f * anim)
                        ),
                        radius = size.minDimension / 2f * (0.4f + 0.6f * anim)
                    )
                }
                Text("${(anim * 100).toInt()}", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }

            LinearProgressIndicator(
                progress = { 1f - (remaining.toFloat() / durationMs) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
            )
            if (!hasMic) Text("⚠️ Permission micro refusée — score par défaut", color = MaterialTheme.colorScheme.error)
        }
    }
}
