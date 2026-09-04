package app.rilcy.finevolumetuner.gainmath

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GainMathTest {

    // --- clampMb : bornes strictes [-3000, 1500] ---

    @Test
    fun clamp() {
        assertEquals(-3000, GainMath.clampMb(-99999))
        assertEquals(1500, GainMath.clampMb(99999))
        assertEquals(750, GainMath.clampMb(750))
        assertEquals(-3000, GainMath.clampMb(GainMath.MIN_MB))
        assertEquals(1500, GainMath.clampMb(GainMath.MAX_MB))
    }

    // --- percentToMb : linéaire -3000 + p*45, clampé ---

    @Test
    fun percentRoundTrip() {
        assertEquals(-3000, GainMath.percentToMb(0))
        assertEquals(1500, GainMath.percentToMb(100))
        // -3000 + 66*45 = -3000 + 2970 = -30
        assertEquals(-30, GainMath.percentToMb(66))
    }

    @Test
    fun percentToMb_clamped() {
        assertEquals(-3000, GainMath.percentToMb(-5))
        assertEquals(1500, GainMath.percentToMb(120))
    }

    // --- mbToPercent : (mb + 3000) / 45, clampé [0, 100] ---

    @Test
    fun mbToPercent() {
        assertEquals(66, GainMath.mbToPercent(0))
        assertEquals(0, GainMath.mbToPercent(-3000))
        assertEquals(100, GainMath.mbToPercent(1500))
    }

    @Test
    fun mbToPercent_clamped() {
        assertEquals(0, GainMath.mbToPercent(-99999))
        assertEquals(100, GainMath.mbToPercent(99999))
    }

    // --- gainForStep ---

    @Test
    fun gainForStep_outOfList_fallsBackToOffset() {
        val cfg = StreamConfig(offsetMb = -120, perStepMb = listOf(0, -100, -200))
        assertEquals(-120, GainMath.gainForStep(cfg, 3))
        assertEquals(-120, GainMath.gainForStep(cfg, -1))
    }

    @Test
    fun gainForStep_usesPerStepValue() {
        val cfg = StreamConfig(offsetMb = -120, perStepMb = listOf(0, -100, -200))
        assertEquals(0, GainMath.gainForStep(cfg, 0))
        assertEquals(-100, GainMath.gainForStep(cfg, 1))
        assertEquals(-200, GainMath.gainForStep(cfg, 2))
    }

    @Test
    fun gainForStep_clamped() {
        val cfg = StreamConfig(offsetMb = -5000, perStepMb = listOf(99999))
        assertEquals(1500, GainMath.gainForStep(cfg, 0)) // 99999 clampé haut
        assertEquals(-3000, GainMath.gainForStep(cfg, 9))
    }

    // --- GainProfiles : sérialisation ---

    @Test
    fun toJson_fromJson_roundTrip() {
        val cfg = TunerConfig(
            mode = TunerMode.PER_STEP,
            autostart = true,
            wasRunning = true,
            streams = mapOf(
                AudioStreamLabel.MUSIC to StreamConfig(offsetMb = -60),
                AudioStreamLabel.RING to StreamConfig(
                    offsetMb = -120,
                    perStepMb = listOf(0, -30, -60)
                ),
            ),
        )
        val restored = GainProfiles.fromJson(GainProfiles.toJson(cfg))
        assertEquals(cfg, restored)
    }

    @Test
    fun fromJson_garbage_returnsDefault() {
        assertSame(GainProfiles.DEFAULT, GainProfiles.fromJson("{garbage"))
    }

    @Test
    fun fromJson_emptyObject_returnsDefault() {
        assertSame(GainProfiles.DEFAULT, GainProfiles.fromJson("{}"))
    }
}
