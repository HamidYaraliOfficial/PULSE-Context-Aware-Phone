package com.pulse.app.ui.automations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.R
import com.pulse.app.domain.model.ActionType
import com.pulse.app.domain.model.AutomationRule
import com.pulse.app.domain.model.RuleAction
import com.pulse.app.domain.model.Trigger
import com.pulse.app.domain.model.TriggerType
import com.pulse.app.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutomationsViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
) : ViewModel() {
    val rules: StateFlow<List<AutomationRule>> =
        ruleRepository.rules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEnabled(id: String, enabled: Boolean) = viewModelScope.launch { ruleRepository.setEnabled(id, enabled) }

    fun createExample() = viewModelScope.launch {
        ruleRepository.saveRule(
            AutomationRule(
                name = "Silence after 22:30 while charging",
                trigger = Trigger(TriggerType.TIME, mapOf("time" to "22:30")),
                condition = com.pulse.app.domain.model.ConditionNode.Leaf(
                    "is_charging", com.pulse.app.domain.model.ComparisonOperator.EQUALS, "true",
                ),
                actions = listOf(RuleAction(ActionType.SET_DND, mapOf("filter" to "alarms"))),
                priority = 5,
                cooldownMinutes = 60,
            ),
        )
    }
}

@Composable
fun AutomationsScreen(onOpenRule: (String) -> Unit, viewModel: AutomationsViewModel = hiltViewModel()) {
    val rules by viewModel.rules.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::createExample) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.automation_new)) }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rules, key = { it.id }) { rule ->
                ElevatedCard(Modifier.fillMaxWidth(), onClick = { onOpenRule(rule.id) }) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(rule.name, style = MaterialTheme.typography.titleMedium)
                            Text("${stringResource(R.string.automation_priority)}: ${rule.priority}", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = rule.enabled, onCheckedChange = { viewModel.setEnabled(rule.id, it) })
                    }
                }
            }
        }
    }
}
