package com.jeevashraya.j2rescue.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Rescue : Screen("rescue")
}
