package com.pairplay.di

import android.content.Context
import com.pairplay.core.bluetooth.BluetoothGatewayImpl
import com.pairplay.core.engine.GameEngine
import com.pairplay.data.game.MiniGameRegistryImpl
import com.pairplay.data.local.ScoreDao
import com.pairplay.data.local.ScoreDatabase
import com.pairplay.data.repository.ScoreRepositoryImpl
import com.pairplay.domain.bluetooth.BluetoothGateway
import com.pairplay.domain.game.MiniGameRegistry
import com.pairplay.domain.repository.ScoreRepository
import com.pairplay.games.blow.BlowGame
import com.pairplay.games.drag.DragGame
import com.pairplay.games.estimation.EstimationGame
import com.pairplay.games.quiz.QuizGame
import com.pairplay.games.stability.StabilityGame
import com.pairplay.games.swipe.SwipeGame
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideRegistry(): MiniGameRegistry = MiniGameRegistryImpl().apply {
        // === Enregistrement des jeux — UNIQUE point de modification lors de l'ajout ===
        register { StabilityGame() }
        register { BlowGame() }
        register { SwipeGame() }
        register { DragGame() }
        register { EstimationGame() }
        register { QuizGame() }
    }

    @Provides @Singleton
    fun provideGateway(@ApplicationContext ctx: Context): BluetoothGateway =
        BluetoothGatewayImpl(ctx)

    @Provides @Singleton
    fun provideEngine(registry: MiniGameRegistry, gateway: BluetoothGateway): GameEngine =
        GameEngine(registry, gateway).also { it.listen() }

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): ScoreDatabase = ScoreDatabase.build(ctx)

    @Provides
    fun provideDao(db: ScoreDatabase): ScoreDao = db.scoreDao()

    @Provides @Singleton
    fun provideScoreRepo(dao: ScoreDao): ScoreRepository = ScoreRepositoryImpl(dao)
}
