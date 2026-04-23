package com.pairplay.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de sons léger basé sur ToneGenerator — aucun asset externe requis.
 * Victoire : séquence ascendante joyeuse. Défaite : tons descendants.
 */
@Singleton
class SoundManager @Inject constructor() {

    fun playVictory() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            Thread {
                tg.startTone(ToneGenerator.TONE_CDMA_PIP, 150); Thread.sleep(160)
                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); Thread.sleep(210)
                tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300); Thread.sleep(320)
                tg.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 400); Thread.sleep(420)
                tg.release()
            }.start()
        }
    }

    fun playDefeat() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            Thread {
                tg.startTone(ToneGenerator.TONE_CDMA_LOW_L, 300); Thread.sleep(320)
                tg.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 500); Thread.sleep(520)
                tg.release()
            }.start()
        }
    }
}
