package com.odom.lottogogo.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.ui.navigation.LottoNavHost
import com.odom.lottogogo.ui.navigation.TopLevelDestination
import com.odom.lottogogo.ui.theme.LottoGogoTheme

/** Compact: 하단 내비게이션 바, Medium / Expanded: 왼쪽 내비게이션 레일 */
fun navigationSuiteTypeFor(windowSizeClass: WindowSizeClass): NavigationSuiteType =
    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

@Composable
fun LottoAppScaffold(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationSuiteScaffold(
        layoutType = navigationSuiteTypeFor(windowSizeClass),
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                    onClick = { navController.navigateToTab(destination) },
                    icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        },
    ) {
        LottoNavHost(navController = navController)
    }
}

private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@PreviewScreenSizes
@Composable
private fun LottoAppScaffoldPreview() {
    LottoGogoTheme {
        LottoAppScaffold()
    }
}
