package app.rilcy.finevolumetuner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.rilcy.finevolumetuner.R
import app.rilcy.finevolumetuner.audio.AudioEngine
import app.rilcy.finevolumetuner.audio.LoudnessEnhancerHandle
import app.rilcy.finevolumetuner.audio.VolumeObserver
import app.rilcy.finevolumetuner.data.PrefsRepository
import app.rilcy.finevolumetuner.gainmath.AudioStreamLabel
import app.rilcy.finevolumetuner.gainmath.GainMath
import app.rilcy.finevolumetuner.gainmath.TunerMode

/**
 * Service foreground (type specialUse) qui maintient le LoudnessEnhancer global
 * et réapplique le gain configuré à chaque changement de volume système.
 */
class TunerService : Service() {

    private lateinit var engine: AudioEngine
    private lateinit var observer: VolumeObserver
    private lateinit var repo: PrefsRepository

    override fun onCreate() {
        super.onCreate()
        repo = PrefsRepository(this)
        engine = AudioEngine { LoudnessEnhancerHandle() }
        observer = VolumeObserver(this) { streamType, newVolume ->
            onVolumeChanged(streamType, newVolume)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (engine.start()) {
            observer.register()
            applyGainFor(AudioStreamLabel.MUSIC)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observer.unregister()
        engine.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onVolumeChanged(streamType: Int, newVolume: Int) {
        val label = AudioStreamLabel.entries.firstOrNull { it.streamType == streamType }
            ?: return // stream inconnu → ignorer
        val cfg = repo.load()
        val sc = cfg.streams[label] ?: return
        val gain = when (cfg.mode) {
            TunerMode.NORMAL -> sc.offsetMb
            TunerMode.PER_STEP -> GainMath.gainForStep(sc, newVolume)
        }
        engine.applyGainMb(gain)
    }

    /** Au démarrage : applique le gain du stream musique au palier courant. */
    private fun applyGainFor(label: AudioStreamLabel) {
        val cfg = repo.load()
        val sc = cfg.streams[label] ?: return
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val vol = am.getStreamVolume(label.streamType)
        val gain = when (cfg.mode) {
            TunerMode.NORMAL -> sc.offsetMb
            TunerMode.PER_STEP -> GainMath.gainForStep(sc, vol)
        }
        engine.applyGainMb(gain)
    }

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val CHANNEL_ID = "fine_volume_tuner"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TunerService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TunerService::class.java))
        }
    }
}
