package com.pairplay.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairplay.domain.game.MiniGame
import com.pairplay.presentation.ui.components.GradientButton

@Composable
fun HomeScreen(
    games: List<MiniGame>,
    onSolo: () -> Unit,
    onHost: () -> Unit,
    onJoin: () -> Unit
) {
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(40.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.SportsEsports, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("PairPlay", fontSize = 42.sp, fontWeight = FontWeight.Black)
                        Text("Mini-jeux Bluetooth P2P",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                GradientButton(
                    text = "Mode Solo",
                    icon = Icons.Filled.PersonOutline,
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                    onClick = onSolo
                )
            }
            item {
                GradientButton(
                    text = "Héberger une partie",
                    icon = Icons.Filled.Bluetooth,
                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
                    onClick = onHost
                )
            }
            item {
                GradientButton(
                    text = "Rejoindre",
                    icon = Icons.Filled.Wifi,
                    colors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
                    onClick = onJoin
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("Pool de jeux (${games.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }

            items(games) { g -> GameCard(g) }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun GameCard(game: MiniGame) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(game.category.emoji, fontSize = 28.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(game.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(game.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(game.category.label,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp)
        }
    }
}
