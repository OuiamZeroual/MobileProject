package com.pairplay.games.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.models.Category
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Quiz éclair : 5 questions visuelles avec 4 choix chacune. */
class QuizGame : MiniGame {
    override val id = "quiz"
    override val displayName = "Quiz Éclair"
    override val description = "5 questions, 4s par question — le meilleur score gagne"
    override val category = Category.QNA
    override val durationMs = 25_000L
    private val perQuestionMs = 5_000L

    private data class Q(val emoji: String, val prompt: String, val choices: List<String>, val answer: Int)

    private val pool = listOf(
        Q("🐙", "Combien de bras ?", listOf("6", "8", "10", "12"), 1),
        Q("🌍", "Quelle planète est la 3ᵉ ?", listOf("Mars", "Vénus", "Terre", "Jupiter"), 2),
        Q("🎨", "Quelle couleur primaire ?", listOf("Vert", "Orange", "Rouge", "Violet"), 2),
        Q("🦒", "Le plus grand animal terrestre ?", listOf("Éléphant", "Girafe", "Hippopotame", "Rhinocéros"), 0),
        Q("⚡", "Unité du courant ?", listOf("Volt", "Watt", "Ampère", "Ohm"), 2),
        Q("🎵", "Combien de touches sur un piano ?", listOf("76", "88", "92", "102"), 1),
        Q("🏔️", "Plus haut sommet ?", listOf("K2", "Everest", "Kilimandjaro", "Mont Blanc"), 1),
        Q("🌡️", "Eau bout à ?", listOf("90°C", "100°C", "110°C", "120°C"), 1),
    )

    @Composable
    override fun Content(seed: Long, onFinish: (Int) -> Unit) {
        val rng = remember(seed) { Random(seed) }
        val questions = remember(seed) { pool.shuffled(rng).take(5) }
        var idx by remember { mutableIntStateOf(0) }
        var score by remember { mutableIntStateOf(0) }
        var remaining by remember { mutableLongStateOf(perQuestionMs) }
        var selected by remember { mutableIntStateOf(-1) }

        LaunchedEffect(idx) {
            selected = -1
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < perQuestionMs && selected == -1) {
                remaining = perQuestionMs - (System.currentTimeMillis() - start)
                delay(50)
            }
            if (selected == -1) {
                if (idx < questions.lastIndex) idx++ else onFinish(score.coerceAtMost(100))
            }
        }

        val q = questions.getOrNull(idx) ?: return

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("❓ Quiz Éclair", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("Q ${idx + 1}/${questions.size} • Score $score",
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(
                Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text(q.emoji, fontSize = 96.sp) }

            Text(q.prompt, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)

            q.choices.forEachIndexed { i, choice ->
                val bg = when {
                    selected == -1 -> MaterialTheme.colorScheme.surfaceVariant
                    i == q.answer -> Color(0xFF10B981)
                    i == selected -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    Modifier.fillMaxWidth().height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable(enabled = selected == -1) {
                            selected = i
                            if (i == q.answer) score += 20
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(choice, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = if (selected != -1 && (i == q.answer || i == selected)) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LaunchedEffect(selected) {
                if (selected != -1) {
                    delay(600)
                    if (idx < questions.lastIndex) idx++
                    else onFinish(score.coerceAtMost(100))
                }
            }

            Spacer(Modifier.weight(1f))
            LinearProgressIndicator(
                progress = { 1f - (remaining.toFloat() / perQuestionMs) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small)
            )
        }
    }
}
