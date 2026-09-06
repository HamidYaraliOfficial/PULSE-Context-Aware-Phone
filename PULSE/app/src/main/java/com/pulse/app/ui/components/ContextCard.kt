package com.pulse.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pulse.app.R
import com.pulse.app.domain.model.ContextState
import com.pulse.app.domain.model.SignalContribution

/** Animated card showing the current detected Context + confidence + confirm/reject/correct actions. */
@Composable
fun ContextCard(
    context: ContextState?,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onCorrect: () -> Unit,
    onWhy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.home_current_context), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ConfidenceIndicator(confidencePercent = context?.confidencePercent ?: 0)
                Column {
                    Text(
                        text = context?.displayLabel ?: stringResource(R.string.home_no_active_mode),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    TextButton(onClick = onWhy) { Text(stringResource(R.string.home_why)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onConfirm) { Text(stringResource(R.string.home_confirm_context)) }
                OutlinedButton(onClick = onCorrect) { Text(stringResource(R.string.home_correct_context)) }
                OutlinedButton(onClick = onReject) { Text(stringResource(R.string.home_reject_context)) }
            }
        }
    }
}

@Composable
fun ExplainabilityPanel(signals: List<SignalContribution>, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(20.dp)) {
        Text(stringResource(R.string.explain_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.explain_signals_used), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        signals.sortedByDescending { it.weight }.forEach { s ->
            ListItem(
                headlineContent = { Text(s.humanReadable) },
                supportingContent = { Text(s.value) },
                trailingContent = { Text("+${(s.weight * 100).toInt()}%") },
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose) { Text(stringResource(R.string.explain_close)) }
    }
}
