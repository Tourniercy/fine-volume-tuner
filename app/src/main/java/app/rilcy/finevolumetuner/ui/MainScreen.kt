package app.rilcy.finevolumetuner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.rilcy.finevolumetuner.R
import app.rilcy.finevolumetuner.gainmath.AudioStreamLabel
import app.rilcy.finevolumetuner.gainmath.GainMath
import app.rilcy.finevolumetuner.gainmath.TunerMode
import app.rilcy.finevolumetuner.vm.TunerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: TunerViewModel) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Text(
                        if (state.engineOn) stringResource(R.string.engine_on)
                        else stringResource(R.string.engine_off),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.engineOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Switch(
                        checked = state.engineOn,
                        onCheckedChange = { vm.toggleEngine() },
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ModeSelector(state.config.mode, vm::setMode) }

            items(AudioStreamLabel.entries.toList()) { stream ->
                StreamCard(
                    stream = stream,
                    selected = state.activeStream == stream,
                    offsetPercent = GainMath.mbToPercent(
                        state.config.streams[stream]?.offsetMb ?: 0,
                    ),
                    mode = state.config.mode,
                    steps = state.config.streams[stream]?.perStepMb ?: emptyList(),
                    maxSteps = state.maxSteps[stream] ?: 15,
                    onSelect = { vm.selectStream(stream) },
                    onOffsetChange = { vm.setStreamOffset(stream, it) },
                    onStepChange = { step, pct -> vm.setStepGain(stream, step, pct) },
                )
            }

            item { SettingsRow(state.config.autostart, vm::setAutostart, vm::reset) }
        }
    }
}

@Composable
private fun ModeSelector(mode: TunerMode, onMode: (TunerMode) -> Unit) {
    Column {
        Text(
            stringResource(R.string.mode_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == TunerMode.NORMAL,
                onClick = { onMode(TunerMode.NORMAL) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.mode_normal)) }
            SegmentedButton(
                selected = mode == TunerMode.PER_STEP,
                onClick = { onMode(TunerMode.PER_STEP) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.mode_per_step)) }
        }
    }
}

@Composable
private fun StreamCard(
    stream: AudioStreamLabel,
    selected: Boolean,
    offsetPercent: Int,
    mode: TunerMode,
    steps: List<Int>,
    maxSteps: Int,
    onSelect: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    onStepChange: (Int, Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (mode == TunerMode.PER_STEP) it.selectable(selected, role = Role.Button, onClick = onSelect) else it },
            ) {
                Text(streamEmoji(stream), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Text(streamLabel(stream), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    formatGain(offsetPercent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = offsetPercent.toFloat(),
                onValueChange = { onOffsetChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
            )
            if (mode == TunerMode.PER_STEP && selected) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.per_step_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                steps(maxSteps).forEachIndexed { step, _ ->
                    val pct = GainMath.mbToPercent(steps.getOrElse(step) { 0 })
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.step_n, step + 1),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(52.dp),
                        )
                        Slider(
                            value = pct.toFloat(),
                            onValueChange = { onStepChange(step, it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatGain(pct),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(64.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(autostart: Boolean, onAutostart: (Boolean) -> Unit, onReset: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.autostart_label), Modifier.weight(1f))
                Switch(checked = autostart, onCheckedChange = onAutostart)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onReset) {
                Text(stringResource(R.string.reset_label))
            }
        }
    }
}

private fun steps(count: Int): List<Int> = List(count.coerceAtMost(30)) { it }

private fun streamEmoji(stream: AudioStreamLabel): String = when (stream) {
    AudioStreamLabel.MUSIC -> "🎵"
    AudioStreamLabel.RING -> "🔔"
    AudioStreamLabel.ALARM -> "⏰"
    AudioStreamLabel.NOTIFICATION -> "💬"
    AudioStreamLabel.VOICE_CALL -> "📞"
    AudioStreamLabel.SYSTEM -> "⚙️"
}

@Composable
private fun streamLabel(stream: AudioStreamLabel): String =
    if (Locale.getDefault().language == "fr") stream.labelFr else stream.labelEn

private fun formatGain(percent: Int): String {
    val mb = GainMath.percentToMb(percent)
    val db = mb / 100.0
    return if (db >= 0) String.format(Locale.US, "+%.1f dB", db)
    else String.format(Locale.US, "%.1f dB", db)
}
