package app.rilcy.finevolumetuner.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat

/**
 * Observe les changements de volume système (action cachée mais stable
 * depuis API 24 : android.media.VOLUME_CHANGED_ACTION).
 */
class VolumeObserver(
    private val context: Context,
    private val onVolumeChanged: (streamType: Int, newVolume: Int) -> Unit,
) : BroadcastReceiver() {

    private var registered = false

    override fun onReceive(receiverContext: Context?, intent: Intent?) {
        if (intent?.action != ACTION_VOLUME_CHANGED) return
        val (streamType, newVolume) = parseExtras(intent.extras)
        if (newVolume < 0) return
        onVolumeChanged(streamType, newVolume)
    }

    fun register() {
        if (registered) return
        ContextCompat.registerReceiver(
            context,
            this,
            IntentFilter(ACTION_VOLUME_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { context.unregisterReceiver(this) }
        registered = false
    }

    companion object {
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val EXTRA_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"

        /** Parse JVM-testable : défauts (MUSIC=3, -1). */
        fun parseValues(streamType: Int?, streamValue: Int?): Pair<Int, Int> =
            (streamType ?: 3) to (streamValue ?: -1)

        /** Parse du Bundle du broadcast (délègue à [parseValues]). */
        @JvmStatic
        fun parseExtras(extras: Bundle?): Pair<Int, Int> =
            parseValues(
                extras?.getInt(EXTRA_STREAM_TYPE),
                extras?.getInt(EXTRA_STREAM_VALUE),
            )
    }
}
