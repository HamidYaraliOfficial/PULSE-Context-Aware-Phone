package com.pulse.app.ui.modes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.R
import com.pulse.app.domain.model.PulseMode
import com.pulse.app.domain.model.WeeklySchedule
import com.pulse.app.domain.repository.ModeRepository
import com.pulse.app.ui.components.ScheduleEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModeDetailViewModel @Inject constructor(
    private val modeRepository: ModeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val modeId: String = checkNotNull(savedStateHandle["modeId"])
    private val _mode = MutableStateFlow<PulseMode?>(null)
    val mode: StateFlow<PulseMode?> = _mode.asStateFlow()

    init {
        viewModelScope.launch { _mode.value = modeRepository.getMode(modeId) }
    }

    fun updateSchedule(schedule: WeeklySchedule) = viewModelScope.launch {
        val current = _mode.value ?: return@launch
        val updated = current.copy(schedule = schedule)
        modeRepository.saveMode(updated)
        _mode.value = updated
    }

    fun toggleActive() = viewModelScope.launch {
        val current = _mode.value ?: return@launch
        if (current.isActive) modeRepository.deactivate(current.id) else modeRepository.activate(current.id, "manual")
        _mode.value = modeRepository.getMode(current.id)
    }
}

@Composable
fun ModeDetailScreen(viewModel: ModeDetailViewModel = hiltViewModel()) {
    val mode by viewModel.mode.collectAsState()
    val current = mode ?: return

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(current.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::toggleActive) {
            Text(stringResource(if (current.isActive) R.string.mode_disable else R.string.mode_enable))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.mode_actions), style = MaterialTheme.typography.titleMedium)
        current.actions.forEach { action ->
            ListItem(headlineContent = { Text(action.type.name) }, supportingContent = { Text(action.params.entries.joinToString { "${it.key}=${it.value}" }) })
        }
        Spacer(Modifier.height(24.dp))
        ScheduleEditor(schedule = current.schedule, onScheduleChange = viewModel::updateSchedule)
    }
}
