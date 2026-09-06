package com.pulse.app.ui.modes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.domain.model.PulseMode
import com.pulse.app.domain.repository.ModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModesViewModel @Inject constructor(
    private val modeRepository: ModeRepository,
) : ViewModel() {
    val modes: StateFlow<List<PulseMode>> =
        modeRepository.modes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(mode: PulseMode) = viewModelScope.launch {
        if (mode.isActive) modeRepository.deactivate(mode.id) else modeRepository.activate(mode.id, "manual")
    }
}

@Composable
fun ModesScreen(onOpenMode: (String) -> Unit, viewModel: ModesViewModel = hiltViewModel()) {
    val modes by viewModel.modes.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(modes, key = { it.id }) { mode ->
            ElevatedCard(Modifier.fillMaxWidth(), onClick = { onOpenMode(mode.id) }) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(mode.name, style = MaterialTheme.typography.titleMedium)
                        Text(mode.type.name, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = mode.isActive, onCheckedChange = { viewModel.toggle(mode) })
                }
            }
        }
    }
}
