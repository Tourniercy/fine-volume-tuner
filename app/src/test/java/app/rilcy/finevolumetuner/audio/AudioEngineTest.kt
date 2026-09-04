package app.rilcy.finevolumetuner.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEngineTest {

    private class FakeHandle(val failOnSet: Boolean = false) : EffectHandle {
        var released = false
            private set
        var lastGain: Int? = null
            private set

        override fun setGainMb(mb: Int) {
            if (failOnSet) throw RuntimeException("boom")
            lastGain = mb
        }

        override fun release() {
            released = true
        }
    }

    @Test
    fun start_setsRunning() {
        val engine = AudioEngine { FakeHandle() }
        assertTrue(engine.start())
        assertTrue(engine.running)
    }

    @Test
    fun doubleStart_isIdempotent() {
        var creations = 0
        val engine = AudioEngine { creations++; FakeHandle() }
        engine.start()
        engine.start()
        assertEquals(1, creations)
    }

    @Test
    fun applyGain_clampedToBounds() {
        val fake = FakeHandle()
        val engine = AudioEngine { fake }
        engine.start()
        engine.applyGainMb(99999)
        assertEquals(1500, fake.lastGain)
        engine.applyGainMb(-99999)
        assertEquals(-3000, fake.lastGain)
        engine.applyGainMb(500)
        assertEquals(500, fake.lastGain)
    }

    @Test
    fun exceptionInHandle_stopsEngine_noCrash() {
        val engine = AudioEngine { FakeHandle(failOnSet = true) }
        engine.start()
        engine.applyGainMb(500) // doit attraper, pas crasher
        assertFalse(engine.running)
        assertNotNull(engine.lastError)
    }

    @Test
    fun stop_releasesHandle() {
        val fake = FakeHandle()
        val engine = AudioEngine { fake }
        engine.start()
        engine.stop()
        assertTrue(fake.released)
        assertFalse(engine.running)
    }
}
