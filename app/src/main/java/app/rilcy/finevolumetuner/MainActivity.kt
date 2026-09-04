package app.rilcy.finevolumetuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import app.rilcy.finevolumetuner.ui.theme.FineVolumeTunerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FineVolumeTunerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(text = "Fine Volume Tuner")
                }
            }
        }
    }
}
