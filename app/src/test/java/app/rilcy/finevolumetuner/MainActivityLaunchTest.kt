package app.rilcy.finevolumetuner

import android.app.Application
import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reproduit le crash au lancement : construit MainActivity via Robolectric
 * (contourne le souci de résolution d'Intent du manifest debug).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], packageName = "app.rilcy.finevolumetuner")
class MainActivityLaunchTest {

    @Test
    fun buildActivity_noCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        controller.setup() // create + start + resume → déclenche onCreate + Compose
        assert(!activity.isFinishing)
    }
}
