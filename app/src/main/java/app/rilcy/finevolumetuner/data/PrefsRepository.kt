package app.rilcy.finevolumetuner.data

import android.content.Context
import app.rilcy.finevolumetuner.gainmath.GainProfiles
import app.rilcy.finevolumetuner.gainmath.TunerConfig

interface ConfigRepository {
    fun load(): TunerConfig
    fun save(cfg: TunerConfig)
}

/** Persistance SharedPreferences + JSON via GainProfiles. */
class PrefsRepository(context: Context) : ConfigRepository {

    private val prefs =
        context.getSharedPreferences("fine_volume_tuner", Context.MODE_PRIVATE)

    override fun load(): TunerConfig {
        val raw = prefs.getString(KEY, null) ?: return GainProfiles.DEFAULT
        return GainProfiles.fromJson(raw)
    }

    override fun save(cfg: TunerConfig) {
        prefs.edit().putString(KEY, GainProfiles.toJson(cfg)).apply()
    }

    companion object {
        private const val KEY = "config"
    }
}

/** Impl en mémoire pour tests et previews. */
class InMemoryConfigRepository(
    var config: TunerConfig = GainProfiles.DEFAULT,
) : ConfigRepository {
    override fun load(): TunerConfig = config
    override fun save(cfg: TunerConfig) {
        config = cfg
    }
}
