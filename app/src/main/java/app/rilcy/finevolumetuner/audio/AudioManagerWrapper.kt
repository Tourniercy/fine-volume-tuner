package app.rilcy.finevolumetuner.audio

import android.content.Context
import android.media.AudioManager
import app.rilcy.finevolumetuner.vm.StreamMaxVolumeSource

class AudioManagerWrapper(context: Context) : StreamMaxVolumeSource {

    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun maxVolume(streamType: Int): Int =
        am.getStreamMaxVolume(streamType).takeIf { it > 0 } ?: 15
}
