package app.rilcy.finevolumetuner.vm

import app.rilcy.finevolumetuner.data.InMemoryConfigRepository
import app.rilcy.finevolumetuner.gainmath.AudioStreamLabel
import app.rilcy.finevolumetuner.gainmath.GainMath
import app.rilcy.finevolumetuner.gainmath.GainProfiles
import app.rilcy.finevolumetuner.gainmath.TunerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerViewModelTest {

    private class FakeService : ServiceControl {
        var started = 0
        var stopped = 0
        override fun start() { started++ }
        override fun stop() { stopped++ }
    }

    private class FakeMaxVolume : StreamMaxVolumeSource {
        override fun maxVolume(streamType: Int) = 15
    }

    @Test
    fun setStreamOffset_persistsAndUpdateState() {
        val repo = InMemoryConfigRepository()
        val vm = TunerViewModel(repo, FakeService(), FakeMaxVolume())
        vm.setStreamOffset(AudioStreamLabel.MUSIC, 80)
        assertEquals(GainMath.percentToMb(80), vm.uiState.value.config.streams[AudioStreamLabel.MUSIC]?.offsetMb)
        assertEquals(GainMath.percentToMb(80), repo.load().streams[AudioStreamLabel.MUSIC]?.offsetMb)
    }

    @Test
    fun setStepGain_modifiesPerStepInPER_STEP() {
        val repo = InMemoryConfigRepository()
        val vm = TunerViewModel(repo, FakeService(), FakeMaxVolume())
        vm.setMode(TunerMode.PER_STEP)
        vm.setStepGain(AudioStreamLabel.MUSIC, 2, 30)
        val sc = vm.uiState.value.config.streams[AudioStreamLabel.MUSIC]!!
        assertEquals(GainMath.percentToMb(30), sc.perStepMb[2])
        // padding: steps 0 et 1 remplis avec l'offset par défaut
        assertEquals(0, sc.perStepMb[0])
        assertEquals(0, sc.perStepMb[1])
    }

    @Test
    fun toggleEngine_startsServiceAndPersistsWasRunning() {
        val repo = InMemoryConfigRepository()
        val svc = FakeService()
        val vm = TunerViewModel(repo, svc, FakeMaxVolume())
        vm.toggleEngine()
        assertEquals(1, svc.started)
        assertTrue(repo.load().wasRunning)
        assertTrue(vm.uiState.value.engineOn)
        vm.toggleEngine()
        assertEquals(1, svc.stopped)
        assertFalse(repo.load().wasRunning)
    }

    @Test
    fun reset_restoresDefault() {
        val repo = InMemoryConfigRepository()
        val vm = TunerViewModel(repo, FakeService(), FakeMaxVolume())
        vm.setStreamOffset(AudioStreamLabel.ALARM, 90)
        vm.reset()
        assertEquals(GainProfiles.DEFAULT, vm.uiState.value.config)
        assertEquals(GainProfiles.DEFAULT, repo.load())
    }

    @Test
    fun setMode_persists() {
        val repo = InMemoryConfigRepository()
        val vm = TunerViewModel(repo, FakeService(), FakeMaxVolume())
        vm.setMode(TunerMode.PER_STEP)
        assertEquals(TunerMode.PER_STEP, repo.load().mode)
    }

    @Test
    fun autostart_toggles() {
        val repo = InMemoryConfigRepository()
        val vm = TunerViewModel(repo, FakeService(), FakeMaxVolume())
        vm.setAutostart(true)
        assertTrue(repo.load().autostart)
    }
}
