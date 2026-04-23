package com.pairplay.domain.game

import androidx.compose.runtime.Composable
import com.pairplay.domain.models.Category

/**
 * Contrat d'un mini-jeu. Tout jeu ajouté au projet n'a qu'à :
 *   1. implémenter cette interface
 *   2. être enregistré dans le MiniGameRegistry
 *
 * Le GameEngine ne dépend d'AUCUN jeu concret : extension sans modification.
 * (Open/Closed Principle — Strategy pattern)
 */
interface MiniGame {
    /** Identifiant unique et stable — utilisé dans le protocole Bluetooth. */
    val id: String
    val displayName: String
    val description: String
    val category: Category
    val durationMs: Long

    /** UI Compose propre au jeu. Appelle [onFinish] quand le joueur a terminé. */
    @Composable
    fun Content(seed: Long, onFinish: (score: Int) -> Unit)
}

/** Factory fonctionnelle — permet à chaque jeu d'être créé à la volée. */
typealias MiniGameFactory = () -> MiniGame
