package app.rilcy.finevolumetuner.audio

interface EffectHandle {
    /** Applique le gain (millibels). Peut lancer une exception si l'effet échoue. */
    @Throws(Exception::class)
    fun setGainMb(mb: Int)

    fun release()
}

/** Wrap d'un LoudnessEnhancer attaché à la session 0 (output mix global). */
class LoudnessEnhancerHandle : EffectHandle {

    private val enhancer = android.media.audiofx.LoudnessEnhancer(0)

    init {
        enhancer.enabled = true
    }

    override fun setGainMb(mb: Int) {
        enhancer.setTargetGain(mb)
    }

    override fun release() {
        enhancer.release()
    }
}
