package app.rilcy.finevolumetuner.service

import android.content.Intent
import app.rilcy.finevolumetuner.gainmath.GainProfiles
import app.rilcy.finevolumetuner.gainmath.TunerConfig
import app.rilcy.finevolumetuner.gainmath.TunerMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {

    private val boot = Intent.ACTION_BOOT_COMPLETED

    @Test
    fun starts_whenAutostartAndWasRunning() {
        val cfg = TunerConfig(mode = TunerMode.NORMAL, autostart = true, wasRunning = true)
        assertTrue(BootLogic.shouldStart(cfg, boot))
    }

    @Test
    fun noStart_whenAutostartDisabled() {
        val cfg = TunerConfig(autostart = false, wasRunning = true)
        assertFalse(BootLogic.shouldStart(cfg, boot))
    }

    @Test
    fun noStart_whenWasNotRunning() {
        val cfg = TunerConfig(autostart = true, wasRunning = false)
        assertFalse(BootLogic.shouldStart(cfg, boot))
    }

    @Test
    fun noStart_onOtherAction() {
        val cfg = TunerConfig(autostart = true, wasRunning = true)
        assertFalse(BootLogic.shouldStart(cfg, Intent.ACTION_POWER_CONNECTED))
    }

    @Test
    fun starts_onQuickboot() {
        val cfg = GainProfiles.DEFAULT.copy(autostart = true, wasRunning = true)
        assertTrue(BootLogic.shouldStart(cfg, "android.intent.action.QUICKBOOT_POWERON"))
    }
}
