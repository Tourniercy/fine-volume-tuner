package app.rilcy.finevolumetuner.gainmath

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GainMath {
    const val MIN_MB = -3000
    const val MAX_MB = 1500

    fun clampMb(v: Int): Int = v.coerceIn(MIN_MB, MAX_MB)

    /** Linéaire : -3000 + p*45, clampé sur [MIN_MB, MAX_MB]. */
    fun percentToMb(p: Int): Int = clampMb(MIN_MB + p * 45)

    /** (mb + 3000) / 45, clampé sur [0, 100]. */
    fun mbToPercent(mb: Int): Int = ((mb - MIN_MB) / 45).coerceIn(0, 100)

    /** PER_STEP : perStepMb[step], sinon offsetMb — toujours clampé. */
    fun gainForStep(cfg: StreamConfig, step: Int): Int =
        clampMb(cfg.perStepMb.getOrNull(step) ?: cfg.offsetMb)
}

object GainProfiles {
    val DEFAULT: TunerConfig = TunerConfig()

    @Serializable
    private data class Dto(
        val mode: TunerMode = TunerMode.NORMAL,
        val autostart: Boolean = false,
        val wasRunning: Boolean = false,
        val streams: Map<String, StreamConfig> =
            AudioStreamLabel.entries.associate { it.name to StreamConfig() },
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(cfg: TunerConfig): String = json.encodeToString(
        Dto(
            mode = cfg.mode,
            autostart = cfg.autostart,
            wasRunning = cfg.wasRunning,
            streams = cfg.streams.mapKeys { it.key.name },
        ),
    )

    fun fromJson(s: String): TunerConfig = try {
        val dto = json.decodeFromString<Dto>(s)
        val cfg = TunerConfig(
            mode = dto.mode,
            autostart = dto.autostart,
            wasRunning = dto.wasRunning,
            streams = dto.streams.entries.associate { (name, sc) ->
                AudioStreamLabel.valueOf(name) to sc
            },
        )
        if (cfg == DEFAULT) DEFAULT else cfg
    } catch (_: Exception) {
        DEFAULT
    }
}
