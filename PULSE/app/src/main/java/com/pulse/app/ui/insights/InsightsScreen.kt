package com.pulse.app.ui.insights

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
import com.pulse.app.domain.model.AnalyticsPeriod
import com.pulse.app.domain.model.AnalyticsSummary
import com.pulse.app.domain.model.UsagePatternInsight
import com.pulse.app.domain.repository.AnalyticsRepository
import com.pulse.app.domain.repository.ContextRepository
import com.pulse.app.context.PatternDetectionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val contextRepository: ContextRepository,
    private val patternDetectionEngine: PatternDetectionEngine,
) : ViewModel() {
    private val _period = MutableStateFlow(AnalyticsPeriod.WEEKLY)
    val period: StateFlow<AnalyticsPeriod> = _period.asStateFlow()

    private val _summary = MutableStateFlow<AnalyticsSummary?>(null)
    val summary: StateFlow<AnalyticsSummary?> = _summary.asStateFlow()

    private val _patterns = MutableStateFlow<List<UsagePatternInsight>>(emptyList())
    val patterns: StateFlow<List<UsagePatternInsight>> = _patterns.asStateFlow()

    init { refresh() }

    fun selectPeriod(period: AnalyticsPeriod) {
        _period.value = period
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        _summary.value = analyticsRepository.summary(_period.value)
        val history = contextRepository.timeline.first()
        _patterns.value = patternDetectionEngine.detectPatterns(history)
    }
}

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val period by viewModel.period.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val patterns by viewModel.patterns.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SingleChoiceSegmentedButtonRow {
            AnalyticsPeriod.entries.forEachIndexed { index, p ->
                SegmentedButton(
                    selected = period == p,
                    onClick = { viewModel.selectPeriod(p) },
                    shape = SegmentedButtonDefaults.itemShape(index, AnalyticsPeriod.entries.size),
                ) {
                    Text(
                        when (p) {
                            AnalyticsPeriod.DAILY -> stringResource(R.string.insights_daily)
                            AnalyticsPeriod.WEEKLY -> stringResource(R.string.insights_weekly)
                            AnalyticsPeriod.MONTHLY -> stringResource(R.string.insights_monthly)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        summary?.let { s ->
            val metrics = listOf(
                stringResource(R.string.insights_focus_time) to "${s.focusMinutes}m",
                stringResource(R.string.insights_distraction_count) to s.distractionCount.toString(),
                stringResource(R.string.insights_notification_load) to s.notificationLoad.toString(),
                stringResource(R.string.insights_mode_switches) to s.modeSwitches.toString(),
                stringResource(R.string.insights_active_hours) to "${s.activeHours}h",
            )
            metrics.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, value) ->
                        ElevatedCard(Modifier.weight(1f)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(value, style = MaterialTheme.typography.headlineSmall)
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.insights_patterns), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.insights_pattern_disclaimer), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        if (patterns.isEmpty()) {
            Text(stringResource(R.string.insights_no_data))
        } else {
            patterns.forEach { p ->
                ListItem(
                    headlineContent = { Text(p.descriptionParams.entries.joinToString { "${it.key}=${it.value}" }) },
                    trailingContent = { AssistChip(onClick = {}, label = { Text("${(p.confidence * 100).toInt()}%") }) },
                )
            }
        }
    }
}
