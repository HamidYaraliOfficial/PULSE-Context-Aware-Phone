package com.pulse.app.ui.automations

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
import com.pulse.app.domain.model.AutomationRule
import com.pulse.app.domain.model.RuleExecutionRecord
import com.pulse.app.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutomationDetailViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val ruleId: String = checkNotNull(savedStateHandle["ruleId"])
    private val _rule = MutableStateFlow<AutomationRule?>(null)
    val rule: StateFlow<AutomationRule?> = _rule.asStateFlow()

    val history: StateFlow<List<RuleExecutionRecord>> = ruleRepository.executionHistory
        .map { list -> list.filter { it.ruleId == ruleId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { _rule.value = ruleRepository.getRule(ruleId) }
    }

    fun delete() = viewModelScope.launch { _rule.value?.let { ruleRepository.deleteRule(it.id) } }
}

@Composable
fun AutomationDetailScreen(viewModel: AutomationDetailViewModel = hiltViewModel()) {
    val rule by viewModel.rule.collectAsState()
    val history by viewModel.history.collectAsState()
    val current = rule ?: return

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(current.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.automation_trigger), style = MaterialTheme.typography.titleMedium)
        Text("${current.trigger.type.name} ${current.trigger.params}")
        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.automation_action), style = MaterialTheme.typography.titleMedium)
        current.actions.forEach { Text("• ${it.type.name} ${it.params}") }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column {
                Text(stringResource(R.string.automation_priority), style = MaterialTheme.typography.labelLarge)
                Text(current.priority.toString())
            }
            Column {
                Text(stringResource(R.string.automation_cooldown), style = MaterialTheme.typography.labelLarge)
                Text("${current.cooldownMinutes}m")
            }
        }
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.automation_history), style = MaterialTheme.typography.titleMedium)
        if (history.isEmpty()) {
            Text(stringResource(R.string.common_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            history.take(20).forEach { record ->
                ListItem(
                    headlineContent = { Text(record.result.name) },
                    supportingContent = { Text(record.actionsPerformed.joinToString { it.name }) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = viewModel::delete) { Text(stringResource(R.string.common_delete)) }
    }
}
