package com.odom.lottogogo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.odom.lottogogo.ui.generate.GenerateScreen
import com.odom.lottogogo.ui.home.HomeScreen
import com.odom.lottogogo.ui.mytickets.MyTicketsScreen
import com.odom.lottogogo.ui.scan.ScanScreen
import com.odom.lottogogo.ui.stats.StatsScreen

@Composable
fun LottoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(onGenerateClick = { navController.navigate(Routes.GENERATE) })
        }
        composable(TopLevelDestination.SCAN.route) { ScanScreen() }
        composable(TopLevelDestination.STATS.route) { StatsScreen() }
        composable(TopLevelDestination.MY_TICKETS.route) { MyTicketsScreen() }
        composable(Routes.GENERATE) {
            GenerateScreen(onBack = { navController.popBackStack() })
        }
    }
}
