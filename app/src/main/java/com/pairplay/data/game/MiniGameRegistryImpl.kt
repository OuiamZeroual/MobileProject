package com.pairplay.data.game

import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.game.MiniGameFactory
import com.pairplay.domain.game.MiniGameRegistry
import com.pairplay.domain.models.Category
import java.util.concurrent.ConcurrentHashMap

class MiniGameRegistryImpl : MiniGameRegistry {
    private val factories = ConcurrentHashMap<String, MiniGameFactory>()

    override fun register(factory: MiniGameFactory) {
        val probe = factory()
        factories[probe.id] = factory
    }

    override fun all(): List<MiniGame> = factories.values.map { it() }

    override fun byId(id: String): MiniGame? = factories[id]?.invoke()

    override fun byCategory(c: Category): List<MiniGame> = all().filter { it.category == c }
}
