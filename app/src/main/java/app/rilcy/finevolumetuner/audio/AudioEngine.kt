package app.rilcy.finevolumetuner.audio

import app.rilcy.finevolumetuner.gainmath.GainMath

/**
 * Moteur audio : crée et détient l'effet global. Ne crash jamais —
 * toute exception est capturée dans [lastError] et stoppe le moteur.
 */
class AudioEngine(private val handleFactory: () -> EffectHandle) {

    private var handle: EffectHandle? = null

    var running: Boolean = false
        private set

    var lastError: String? = null
        private set

    fun start(): Boolean {
        if (running) return true
        return try {
            handle = handleFactory()
            running = true
            lastError = null
            true
        } catch (e: Exception) {
            handle = null
            running = false
            lastError = e.message ?: e.javaClass.simpleName
            false
        }
    }

    fun applyGainMb(mb: Int) {
        val h = handle ?: return
        try {
            h.setGainMb(GainMath.clampMb(mb))
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            stop()
        }
    }

    fun stop() {
        try {
            handle?.release()
        } catch (_: Exception) {
            // release best-effort
        }
        handle = null
        running = false
    }
}
