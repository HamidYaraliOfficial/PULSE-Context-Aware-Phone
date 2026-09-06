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
import com.pulse.app.domain.model.RetentionCategory
import com.pulse.app.domain.model.RetentionPeriod
import com.pulse.app.domain.model.RetentionSetting
import com.pulse.app.domain.repository.RetentionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataRetentionViewModel @Inject constructor(
    private val retentionRepository: RetentionRepository,
) : ViewModel() {
    val settings: StateFlow<List<RetentionSetting>> =
        retentionRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPeriod(category: RetentionCategory, period: RetentionPeriod) =
        viewModelScope.launch { retentionRepository.setPeriod(category, period) }

    fun clearNow(category: RetentionCategory) = viewModelScope.launch { retentionRepository.clearNow(category) }
}

private fun categoryLabel(c: RetentionCategory): Int = when (c) {
    RetentionCategory.CONTEXT_HISTORY -> R.string.retention_context_history
    RetentionCategory.USAGE_ANALYTICS -> R.string.retention_usage_analytics
    RetentionCategory.NOTIFICATION_METADATA -> R.string.retention_notification_metadata
    RetentionCategory.EVENT_LOGS -> R.string.retention_event_logs
}

private fun periodLabel(p: RetentionPeriod): Int = when (p) {
    RetentionPeriod.SEVEN_DAYS -> R.string.retention_7_days
    RetentionPeriod.THIRTY_DAYS -> R.string.retention_30_days
    RetentionPeriod.NINETY_DAYS -> R.string.retention_90_days
    RetentionPeriod.FOREVER -> R.string.retention_forever
}

@Composable
fun DataRetentionScreen(viewModel: DataRetentionViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        settings.forEach { setting ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(categoryLabel(setting.category)), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RetentionPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = setting.period == period,
                                onClick = { viewModel.setPeriod(setting.category, period) },
                                label = { Text(stringResource(periodLabel(period))) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.clearNow(setting.category) }) {
                        Text(stringResource(R.string.retention_clear_now))
                    }
                }
            }
        }
    }
}
