package com.machadothi.templateapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.machadothi.templateapp.ui.screen.address.AddressScreen
import com.machadothi.templateapp.ui.screen.dashboard.DashboardScreen
import com.machadothi.templateapp.ui.screen.devicescan.DeviceScanScreen
import com.machadothi.templateapp.ui.screen.filter.FiltersScreen
import com.machadothi.templateapp.ui.screen.find.FindScreen
import com.machadothi.templateapp.ui.screen.graph.GraphScreen
import com.machadothi.templateapp.ui.screen.graph.GraphType
import com.machadothi.templateapp.ui.screen.graph.humidity.HumidityGraphScreen
import com.machadothi.templateapp.ui.screen.graph.temperature.TemperatureGraphScreen
import com.machadothi.templateapp.ui.screen.jog.JogScreen
import com.machadothi.templateapp.ui.screen.provision.ProvisionScreen
import com.machadothi.templateapp.ui.screen.sensor.SensorsScreen
import com.machadothi.templateapp.ui.screen.target.TargetScreen

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Find(auto = true),
        modifier = modifier
    ) {
        composable<NavRoutes.Find> {
            val canGoBack = navController.previousBackStackEntry != null
            FindScreen(
                onConnected = { navController.replaceWith(NavRoutes.Dashboard) },
                onSetUpNew = { navController.navigate(NavRoutes.DeviceScan) },
                onEnterAddress = { navController.navigate(NavRoutes.Address) },
                onBack = if (canGoBack) ({ navController.popBackStack() }) else null,
            )
        }
        composable<NavRoutes.DeviceScan> {
            // Reached from the dashboard ("Set up again"), Back returns there with
            // the old address intact; reached on first launch, there is nowhere to go back to.
            val canGoBack = navController.previousBackStackEntry != null
            DeviceScanScreen(
                onDeviceSelected = { navController.navigate(NavRoutes.Provision(it.address)) },
                onUseAddress = { navController.navigate(NavRoutes.Address) },
                onBack = if (canGoBack) ({ navController.popBackStack() }) else null,
            )
        }
        composable<NavRoutes.Provision> {
            ProvisionScreen(
                onDone = { navController.replaceWith(NavRoutes.Dashboard) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<NavRoutes.Address> {
            AddressScreen(
                onDone = { navController.replaceWith(NavRoutes.Dashboard) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<NavRoutes.Dashboard> {
            DashboardScreen(
                onJog = { navController.navigate(NavRoutes.Jog) },
                onTarget = { navController.navigate(NavRoutes.Target) },
                onSetupAgain = { navController.navigate(NavRoutes.DeviceScan) },
                onFind = { auto -> navController.navigate(NavRoutes.Find(auto)) },
            )
        }
        composable<NavRoutes.Jog> {
            JogScreen(onDone = { navController.popBackStack() })
        }
        composable<NavRoutes.Target> {
            TargetScreen(
                onJog = { navController.navigate(NavRoutes.Jog) },
                onBack = { navController.popBackStack() },
            )
        }

        // The original sensor demo.
        composable<NavRoutes.Sensors> {
            SensorsScreen(
                onFiltersSelected = {
                    navController.navigate(NavRoutes.Graph)
                }
            )
        }
        composable<NavRoutes.Filter> {
            FiltersScreen()
        }
        composable<NavRoutes.Graph> {
            GraphScreen(onGraphSelected = { graph ->
                when (graph) {
                    GraphType.TEMPERATURE -> navController.navigate(NavRoutes.Graph.Temperature)
                    GraphType.HUMIDITY -> navController.navigate(NavRoutes.Graph.Humidity)
                }
            })
        }
        composable<NavRoutes.Graph.Humidity> {
            HumidityGraphScreen()
        }
        composable<NavRoutes.Graph.Temperature> {
            TemperatureGraphScreen()
        }
    }
}

/** Navigate and clear the whole back stack: Back should not return to setup. */
private fun NavHostController.replaceWith(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
