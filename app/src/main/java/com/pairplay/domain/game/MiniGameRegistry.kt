package com.pairplay.domain.game

import com.pairplay.domain.models.Category

/**
 * Registre des mini-jeux — point central d'extension (Plugin pattern).
 * Ajouter un jeu = 1 appel à [register]. Zéro modif ailleurs.
 */
interface MiniGameRegistry {
    fun register(factory: MiniGameFactory)
    fun all(): List<MiniGame>
    fun byId(id: String): MiniGame?
    fun byCategory(c: Category): List<MiniGame>
}
