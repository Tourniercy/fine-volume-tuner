package app.rilcy.finevolumetuner.vm

import app.rilcy.finevolumetuner.data.ConfigRepository
import app.rilcy.finevolumetuner.gainmath.AudioStreamLabel
import app.rilcy.finevolumetuner.gainmath.GainMath
import app.rilcy.finevolumetuner.gainmath.GainProfiles
import app.rilcy.finevolumetuner.gainmath.StreamConfig
import app.rilcy.finevolumetuner.gainmath.TunerConfig
import app.rilcy.finevolumetuner.gainmath.TunerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contrôle du service, injectable pour tests. */
interface ServiceControl {
    fun start()
    fun stop()
}

/** Wrapper testable de AudioManager. */
interface StreamMaxVolumeSource {
    fun maxVolume(streamType: Int): Int
}

data class TunerUiState(
    val config: TunerConfig = GainProfiles.DEFAULT,
    val engineOn: Boolean = false,
    val activeStream: AudioStreamLabel = AudioStreamLabel.MUSIC,
    val maxSteps: Map<AudioStreamLabel, Int> = emptyMap(),
)

class TunerViewModel(
    private val repo: ConfigRepository,
    private val service: ServiceControl,
    private val maxVolumeSource: StreamMaxVolumeSource,
) {

    private val _uiState = MutableStateFlow(TunerUiState(config = repo.load()))
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    init {
        refreshMaxSteps()
        val cfg = repo.load()
        _uiState.value = _uiState.value.copy(engineOn = cfg.wasRunning)
    }

    fun toggleEngine() {
        val on = !_uiState.value.engineOn
        if (on) service.start() else service.stop()
        updateConfig { it.copy(wasRunning = on) }
        _uiState.value = _uiState.value.copy(engineOn = on)
    }

    fun setMode(mode: TunerMode) = updateConfig { it.copy(mode = mode) }

    fun setAutostart(enabled: Boolean) = updateConfig { it.copy(autostart = enabled) }

    fun selectStream(stream: AudioStreamLabel) {
        _uiState.value = _uiState.value.copy(activeStream = stream)
    }

    fun setStreamOffset(stream: AudioStreamLabel, percent: Int) = updateConfig { cfg ->
        val sc = cfg.streams[stream] ?: StreamConfig()
        cfg.copy(streams = cfg.streams + (stream to sc.copy(offsetMb = GainMath.percentToMb(percent))))
    }

    fun setStepGain(stream: AudioStreamLabel, step: Int, percent: Int) = updateConfig { cfg ->
        val sc = cfg.streams[stream] ?: StreamConfig()
        val perStep = sc.perStepMb.toMutableList()
        while (perStep.size <= step) perStep.add(sc.offsetMb)
        perStep[step] = GainMath.percentToMb(percent)
        cfg.copy(streams = cfg.streams + (stream to sc.copy(perStepMb = perStep)))
    }

    fun reset() {
        repo.save(GainProfiles.DEFAULT)
        _uiState.value = TunerUiState(config = GainProfiles.DEFAULT).let { base ->
            base.copy(maxSteps = _uiState.value.maxSteps)
        }
    }

    private fun updateConfig(transform: (TunerConfig) -> TunerConfig) {
        val newCfg = transform(_uiState.value.config)
        repo.save(newCfg)
        _uiState.value = _uiState.value.copy(config = newCfg)
    }

    private fun refreshMaxSteps() {
        val maxes = AudioStreamLabel.entries.associateWith { maxVolumeSource.maxVolume(it.streamType) }
        _uiState.value = _uiState.value.copy(maxSteps = maxes)
    }
}
