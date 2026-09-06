package com.pulse.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pulse.app.R
import com.pulse.app.ui.navigation.PulseRoutes

private data class SettingsRow(val labelRes: Int, val route: String)

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit) {
    val rows = listOf(
        SettingsRow(R.string.perm_center_title, PulseRoutes.PERMISSION_CENTER),
        SettingsRow(R.string.settings_appearance, PulseRoutes.APPEARANCE_SETTINGS),
        SettingsRow(R.string.retention_title, PulseRoutes.DATA_RETENTION),
        SettingsRow(R.string.simulation_title, PulseRoutes.SIMULATION_MODE),
    )
    LazyColumn(Modifier.fillMaxSize()) {
        items(rows) { row ->
            ListItem(
                headlineContent = { Text(stringResource(row.labelRes)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickableRow { onNavigate(row.route) },
            )
            HorizontalDivider()
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
