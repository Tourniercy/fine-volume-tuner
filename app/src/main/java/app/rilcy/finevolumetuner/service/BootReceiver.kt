package app.rilcy.finevolumetuner.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.rilcy.finevolumetuner.data.PrefsRepository
import app.rilcy.finevolumetuner.gainmath.TunerConfig

/** Logique de boot, JVM-testable. */
object BootLogic {
    fun shouldStart(cfg: TunerConfig, action: String?): Boolean =
        cfg.autostart && cfg.wasRunning &&
            (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON")
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (BootLogic.shouldStart(PrefsRepository(context).load(), intent.action)) {
            TunerService.start(context)
        }
    }
}
