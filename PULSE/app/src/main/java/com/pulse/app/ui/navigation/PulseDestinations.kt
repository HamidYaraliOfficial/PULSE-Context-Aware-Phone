package com.pulse.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.pulse.app.R

sealed class PulseDestination(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Home : PulseDestination("home", R.string.nav_home, Icons.Filled.Home)
    data object Timeline : PulseDestination("timeline", R.string.nav_timeline, Icons.Filled.Timeline)
    data object Modes : PulseDestination("modes", R.string.nav_modes, Icons.Filled.Widgets)
    data object Automations : PulseDestination("automations", R.string.nav_automations, Icons.Filled.Bolt)
    data object Insights : PulseDestination("insights", R.string.nav_insights, Icons.Filled.Insights)
    data object Settings : PulseDestination("settings", R.string.nav_settings, Icons.Filled.Settings)

    companion object {
        val bottomBarItems = listOf(Home, Timeline, Modes, Automations, Insights, Settings)
    }
}

object PulseRoutes {
    const val MODE_DETAIL = "mode_detail/{modeId}"
    const val AUTOMATION_DETAIL = "automation_detail/{ruleId}"
    const val PERMISSION_CENTER = "permission_center"
    const val APPEARANCE_SETTINGS = "appearance_settings"
    const val DATA_RETENTION = "data_retention"
    const val SIMULATION_MODE = "simulation_mode"

    fun modeDetail(modeId: String) = "mode_detail/$modeId"
    fun automationDetail(ruleId: String) = "automation_detail/$ruleId"
}
