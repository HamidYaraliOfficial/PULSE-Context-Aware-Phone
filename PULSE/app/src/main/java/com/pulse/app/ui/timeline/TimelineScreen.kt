package com.pulse.app.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.R
import com.pulse.app.domain.model.ContextState
import com.pulse.app.domain.repository.ContextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val contextRepository: ContextRepository,
) : ViewModel() {
    val timeline: StateFlow<List<ContextState>> =
        contextRepository.timeline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: String) = viewModelScope.launch { contextRepository.deleteEvent(id) }
}

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val events by viewModel.timeline.collectAsState()
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    if (events.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(stringResource(R.string.timeline_empty))
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events, key = { it.id }) { event ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(event.displayLabel, style = MaterialTheme.typography.titleMedium)
                        val startTime = event.startedAt.atZone(ZoneId.systemDefault()).format(formatter)
                        val endTime = event.endedAt?.atZone(ZoneId.systemDefault())?.format(formatter) ?: "…"
                        Text("$startTime – $endTime · ${event.confidencePercent}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { viewModel.delete(event.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.timeline_delete_event))
                    }
                }
            }
        }
    }
}
