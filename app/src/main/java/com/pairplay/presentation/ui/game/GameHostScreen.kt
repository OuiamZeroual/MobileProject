package com.pairplay.presentation.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.models.GamePhase

/**
 * Conteneur de mini-jeu. Affiche la barre de manche + délègue le rendu à [MiniGame.Content].
 */
@Composable
fun GameHostScreen(
    phase: GamePhase,
    game: MiniGame?,
    seed: Long,
    onFinishGame: (gameId: String, score: Int) -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            when (phase) {
                is GamePhase.Playing -> {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Manche ${phase.index + 1}/${phase.total}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            game?.category?.label ?: "",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedContent(
                        targetState = game?.id,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "game"
                    ) { gameId ->
                        val current = game
                        if (current != null && current.id == gameId) {
                            current.Content(seed) { score -> onFinishGame(current.id, score) }
                        }
                    }
                }
                is GamePhase.Syncing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Synchronisation des scores…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
