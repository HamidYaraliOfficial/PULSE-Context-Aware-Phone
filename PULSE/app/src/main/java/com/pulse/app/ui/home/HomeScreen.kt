package com.pulse.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pulse.app.R
import com.pulse.app.domain.model.RecommendationDecision
import com.pulse.app.ui.components.ContextCard
import com.pulse.app.ui.components.ExplainabilityPanel

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showExplain by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ContextCard(
                context = state.currentContext,
                onConfirm = viewModel::confirmContext,
                onReject = viewModel::rejectContext,
                onCorrect = viewModel::correctContext,
                onWhy = { showExplain = true },
            )
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.home_active_mode), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.activeMode?.name ?: stringResource(R.string.home_no_active_mode),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(stringResource(R.string.home_battery), style = MaterialTheme.typography.titleMedium)
                        Text("${state.batteryLevel}%" + if (state.isCharging) " ⚡" else "")
                    }
                    Icon(Icons.Filled.BatteryFull, contentDescription = null)
                }
            }
        }

        if (state.recommendations.isNotEmpty()) {
            item { Text(stringResource(R.string.home_suggested_actions), style = MaterialTheme.typography.titleMedium) }
            items(state.recommendations) { rec ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rec.titleKey.replace('_', ' '))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { viewModel.resolveRecommendation(rec.id, RecommendationDecision.ACCEPTED) }) {
                                Text(stringResource(R.string.common_confirm))
                            }
                            OutlinedButton(onClick = { viewModel.resolveRecommendation(rec.id, RecommendationDecision.DISMISSED) }) {
                                Text(stringResource(R.string.common_cancel))
                            }
                        }
                    }
                }
            }
        }

        item { Text(stringResource(R.string.home_quick_actions), style = MaterialTheme.typography.titleMedium) }
        item {
            LazyColumn(Modifier.height((state.allModes.size * 64).dp.coerceAtMost(320.dp))) {
                items(state.allModes) { mode ->
                    ListItem(
                        headlineContent = { Text(mode.name) },
                        trailingContent = {
                            Switch(
                                checked = mode.isActive,
                                onCheckedChange = { checked ->
                                    if (checked) viewModel.activateMode(mode.id) else viewModel.deactivateMode(mode.id)
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    if (showExplain) {
        ModalBottomSheet(onDismissRequest = { showExplain = false }) {
            ExplainabilityPanel(
                signals = state.currentContext?.signals ?: emptyList(),
                onClose = { showExplain = false },
            )
        }
    }
}
