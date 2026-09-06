package com.pulse.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.pulse.app.R
import com.pulse.app.ui.automations.AutomationDetailScreen
import com.pulse.app.ui.automations.AutomationsScreen
import com.pulse.app.ui.home.HomeScreen
import com.pulse.app.ui.insights.InsightsScreen
import com.pulse.app.ui.modes.ModeDetailScreen
import com.pulse.app.ui.modes.ModesScreen
import com.pulse.app.ui.navigation.PulseDestination
import com.pulse.app.ui.navigation.PulseRoutes
import com.pulse.app.ui.settings.AppearanceSettingsScreen
import com.pulse.app.ui.settings.DataRetentionScreen
import com.pulse.app.ui.settings.PermissionCenterScreen
import com.pulse.app.ui.settings.SettingsScreen
import com.pulse.app.ui.settings.SimulationModeScreen
import com.pulse.app.ui.timeline.TimelineScreen

@Composable
fun PulseApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = PulseDestination.bottomBarItems.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        PulseDestination.bottomBarItems.firstOrNull { it.route == currentRoute }
                            ?.let { stringResource(it.labelRes) } ?: stringResource(R.string.app_name),
                    )
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    PulseDestination.bottomBarItems.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                            label = { Text(stringResource(dest.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = PulseDestination.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable(PulseDestination.Home.route) { HomeScreen() }
            composable(PulseDestination.Timeline.route) { TimelineScreen() }
            composable(PulseDestination.Modes.route) {
                ModesScreen(onOpenMode = { id -> navController.navigate(PulseRoutes.modeDetail(id)) })
            }
            composable(PulseDestination.Automations.route) {
                AutomationsScreen(onOpenRule = { id -> navController.navigate(PulseRoutes.automationDetail(id)) })
            }
            composable(PulseDestination.Insights.route) { InsightsScreen() }
            composable(PulseDestination.Settings.route) {
                SettingsScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(
                PulseRoutes.MODE_DETAIL,
                arguments = listOf(navArgument("modeId") { type = NavType.StringType }),
            ) { ModeDetailScreen() }
            composable(
                PulseRoutes.AUTOMATION_DETAIL,
                arguments = listOf(navArgument("ruleId") { type = NavType.StringType }),
            ) { AutomationDetailScreen() }
            composable(PulseRoutes.PERMISSION_CENTER) { PermissionCenterScreen() }
            composable(PulseRoutes.APPEARANCE_SETTINGS) { AppearanceSettingsScreen() }
            composable(PulseRoutes.DATA_RETENTION) { DataRetentionScreen() }
            composable(PulseRoutes.SIMULATION_MODE) { SimulationModeScreen() }
        }
    }
}
