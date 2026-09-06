package com.pulse.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.R
import com.pulse.app.domain.model.PermissionFeatureState
import com.pulse.app.domain.model.PermissionStatus
import com.pulse.app.domain.model.PulsePermission
import com.pulse.app.domain.repository.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionCenterViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository,
) : ViewModel() {
    val states: StateFlow<List<PermissionFeatureState>> =
        permissionRepository.permissionStates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { viewModelScope.launch { permissionRepository.refreshOsGrantedStatus() } }

    fun setEnabled(permission: PulsePermission, enabled: Boolean) =
        viewModelScope.launch { permissionRepository.setFeatureEnabled(permission, enabled) }
}

private fun labelRes(p: PulsePermission): Pair<Int, Int> = when (p) {
    PulsePermission.NOTIFICATIONS -> R.string.perm_notifications to R.string.perm_notifications_desc
    PulsePermission.LOCATION -> R.string.perm_location to R.string.perm_location_desc
    PulsePermission.ACTIVITY_RECOGNITION -> R.string.perm_activity_recognition to R.string.perm_activity_recognition_desc
    PulsePermission.SENSORS -> R.string.perm_sensors to R.string.perm_sensors_desc
    PulsePermission.USAGE_ACCESS -> R.string.perm_usage_access to R.string.perm_usage_access_desc
    PulsePermission.CALENDAR -> R.string.perm_calendar to R.string.perm_calendar_desc
    PulsePermission.BLUETOOTH -> R.string.perm_bluetooth to R.string.perm_bluetooth_desc
    PulsePermission.NOTIFICATION_ACCESS -> R.string.perm_notification_access to R.string.perm_notification_access_desc
    PulsePermission.DND_ACCESS -> R.string.perm_dnd_access to R.string.perm_dnd_access_desc
}

@Composable
fun PermissionCenterScreen(viewModel: PermissionCenterViewModel = hiltViewModel()) {
    val states by viewModel.states.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.perm_center_subtitle),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        LazyColumn {
            items(states, key = { it.permission }) { state ->
                val (label, desc) = labelRes(state.permission)
                ListItem(
                    headlineContent = { Text(stringResource(label)) },
                    supportingContent = { Text(stringResource(desc)) },
                    trailingContent = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(
                                when (state.status) {
                                    PermissionStatus.GRANTED -> stringResource(R.string.perm_status_granted)
                                    PermissionStatus.DENIED -> stringResource(R.string.perm_status_denied)
                                    PermissionStatus.NOT_REQUESTED -> stringResource(R.string.perm_status_not_requested)
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Switch(
                                checked = state.featureEnabled && state.status == PermissionStatus.GRANTED,
                                enabled = state.status == PermissionStatus.GRANTED,
                                onCheckedChange = { viewModel.setEnabled(state.permission, it) },
                            )
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
