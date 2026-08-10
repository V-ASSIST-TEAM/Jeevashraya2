package com.jeevashraya.j2rescue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jeevashraya.j2rescue.ui.screens.HomeScreen
import com.jeevashraya.j2rescue.ui.screens.RescueScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onStartRescue = {
                    navController.navigate(Screen.Rescue.route)
                }
            )
        }
        composable(route = Screen.Rescue.route) {
            RescueScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
