package com.pulse.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.R
import com.pulse.app.context.ContextEngine
import com.pulse.app.domain.model.ContextState
import com.pulse.app.domain.model.DetectedActivityType
import com.pulse.app.domain.model.NormalizedContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simulation / Test Mode. Builds a synthetic [NormalizedContext] and runs it
 * straight through the real inference engine via [ContextEngine.previewSimulated]
 * — the same evaluator used on real signals — without ever touching a live
 * device sensor or executing an action, per the spec's safety requirement.
 */
@HiltViewModel
class SimulationModeViewModel @Inject constructor(
    private val contextEngine: ContextEngine,
) : ViewModel() {
    var headphones by mutableStateOf(false)
    var isMeeting by mutableStateOf(false)
    var isCharging by mutableStateOf(false)
    var isStill by mutableStateOf(true)
    var timeMinutes by mutableStateOf(10 * 60)
    var result by mutableStateOf<ContextState?>(null)
        private set

    fun run() = viewModelScope.launch {
        val synthetic = NormalizedContext(
            timeOfDayMinutes = timeMinutes,
            dayOfWeek = 1,
            isCharging = isCharging,
            headphonesConnected = headphones,
            calendarEventIsMeetingLike = isMeeting,
            isDeviceStill = isStill,
            detectedActivity = if (isStill) DetectedActivityType.STILL else DetectedActivityType.WALKING,
        )
        result = contextEngine.previewSimulated(synthetic)
    }
}

@Composable
fun SimulationModeScreen(viewModel: SimulationModeViewModel = hiltViewModel()) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.simulation_subtitle), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        SwitchRow("Headphones connected", viewModel.headphones) { viewModel.headphones = it }
        SwitchRow("Calendar meeting active", viewModel.isMeeting) { viewModel.isMeeting = it }
        SwitchRow("Charging", viewModel.isCharging) { viewModel.isCharging = it }
        SwitchRow("Device still", viewModel.isStill) { viewModel.isStill = it }

        Spacer(Modifier.height(16.dp))
        Button(onClick = viewModel::run) { Text(stringResource(R.string.simulation_run)) }
        Spacer(Modifier.height(16.dp))

        viewModel.result?.let { r ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.simulation_result), style = MaterialTheme.typography.titleMedium)
                    Text("${r.displayLabel} — ${r.confidencePercent}%")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.simulation_disclaimer), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
