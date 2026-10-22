package com.odom.lottogogo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    if (windowSizeClass.isCompactWidth) {
        NavigationSuiteType.NavigationBar
    } else {
        NavigationSuiteType.NavigationRail
    }

@Composable
fun LottoAppScaffold(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val layoutType = navigationSuiteTypeFor(windowSizeClass)

    NavigationSuiteScaffold(
        layoutType = layoutType,
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
        // 상태바와 겹치지 않게 콘텐츠에 시스템 바 여백 적용 (하위 Scaffold에는 소비된 것으로 전달)
        LottoNavHost(
            navController = navController,
            modifier = Modifier.windowInsetsPadding(contentInsets(layoutType)),
        )
    }
}

/**
 * 하단 바 모드: 아래쪽 시스템 바는 NavigationBar가 처리하므로 위/옆만.
 * 레일 모드: 레일이 시작 쪽을 처리하므로 위/끝/아래.
 */
@Composable
private fun contentInsets(layoutType: NavigationSuiteType): WindowInsets {
    val sides = if (layoutType == NavigationSuiteType.NavigationBar) {
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    } else {
        WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom
    }
    return WindowInsets.safeDrawing.only(sides)
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
