package app.rilcy.finevolumetuner.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeObserverTest {

    @Test
    fun parseValues_nullDefaults_toMusicAndMinusOne() {
        assertEquals(3 to -1, VolumeObserver.parseValues(null, null))
    }

    @Test
    fun parseValues_validExtras() {
        assertEquals(2 to 7, VolumeObserver.parseValues(2, 7))
    }

    @Test
    fun parseValues_typeOnly() {
        assertEquals(4 to -1, VolumeObserver.parseValues(4, null))
    }

    @Test
    fun parseExtras_nullBundle() {
        assertEquals(3 to -1, VolumeObserver.parseExtras(null))
    }
}
