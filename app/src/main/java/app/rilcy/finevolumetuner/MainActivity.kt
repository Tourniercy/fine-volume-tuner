package app.rilcy.finevolumetuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rilcy.finevolumetuner.audio.AudioManagerWrapper
import app.rilcy.finevolumetuner.data.PrefsRepository
import app.rilcy.finevolumetuner.service.TunerService
import app.rilcy.finevolumetuner.ui.MainScreen
import app.rilcy.finevolumetuner.ui.theme.FineVolumeTunerTheme
import app.rilcy.finevolumetuner.vm.ServiceControl
import app.rilcy.finevolumetuner.vm.TunerViewModel

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* refusable */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val serviceControl = object : ServiceControl {
                    override fun start() {
                        requestNotifPermissionIfNeeded()
                        TunerService.start(this@MainActivity)
                    }

                    override fun stop() = TunerService.stop(this@MainActivity)
                }
                return TunerViewModel(
                    repo = PrefsRepository(this@MainActivity),
                    service = serviceControl,
                    maxVolumeSource = AudioManagerWrapper(this@MainActivity),
                ) as T
            }
        }

        setContent {
            FineVolumeTunerTheme {
                val vm: TunerViewModel = viewModel(factory = vmFactory)
                MainScreen(vm)
            }
        }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
