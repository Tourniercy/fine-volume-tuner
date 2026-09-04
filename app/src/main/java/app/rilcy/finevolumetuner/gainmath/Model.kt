package app.rilcy.finevolumetuner.gainmath

import kotlinx.serialization.Serializable

enum class AudioStreamLabel(val streamType: Int, val labelFr: String, val labelEn: String) {
    MUSIC(3, "Musique", "Music"),
    RING(2, "Sonnerie", "Ringtone"),
    ALARM(4, "Alarme", "Alarm"),
    NOTIFICATION(5, "Notifications", "Notifications"),
    VOICE_CALL(0, "Appel", "Call"),
    SYSTEM(1, "Système", "System"),
}

enum class TunerMode { NORMAL, PER_STEP }

@Serializable
data class StreamConfig(
    val offsetMb: Int = 0,
    val perStepMb: List<Int> = emptyList(),
)

@Serializable
data class TunerConfig(
    val mode: TunerMode = TunerMode.NORMAL,
    val autostart: Boolean = false,
    val wasRunning: Boolean = false,
    val streams: Map<AudioStreamLabel, StreamConfig> =
        AudioStreamLabel.entries.associateWith { StreamConfig() },
)
