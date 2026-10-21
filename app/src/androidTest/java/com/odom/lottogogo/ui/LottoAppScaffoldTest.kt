package com.odom.lottogogo.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.ComposeTestActivity
import com.odom.lottogogo.ui.navigation.Routes
import com.odom.lottogogo.ui.navigation.TopLevelDestination
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LottoAppScaffoldTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    private lateinit var navController: NavHostController

    private val phone = WindowSizeClass(minWidthDp = 411, minHeightDp = 891)
    private val tablet = WindowSizeClass(minWidthDp = 1280, minHeightDp = 800)

    private fun setContent(windowSizeClass: WindowSizeClass) {
        composeRule.setContent {
            navController = rememberNavController()
            LottoGogoTheme {
                LottoAppScaffold(navController = navController, windowSizeClass = windowSizeClass)
            }
        }
    }

    // 시작 화면(홈)에 보이지 않는 탭 라벨로 위치를 비교한다
    private fun tabBounds(label: String) =
        composeRule.onNodeWithText(label).getUnclippedBoundsInRoot()

    @Test
    fun compact_showsBottomBar() {
        setContent(phone)

        val scan = tabBounds("QR 확인")
        val myTickets = tabBounds("내 번호")
        // 하단 바: 같은 행에 가로로 나열
        assertEquals(scan.top, myTickets.top)
        assertTrue(myTickets.left > scan.left)
    }

    @Test
    fun expanded_showsNavigationRail() {
        setContent(tablet)

        val scan = tabBounds("QR 확인")
        val myTickets = tabBounds("내 번호")
        // 레일: 같은 열에 세로로 나열
        assertEquals(scan.left, myTickets.left)
        assertTrue(myTickets.top > scan.top)
    }

    @Test
    fun navigationSuiteType_byWidth() {
        assertEquals(
            NavigationSuiteType.NavigationBar,
            navigationSuiteTypeFor(phone)
        )
        assertEquals(
            NavigationSuiteType.NavigationRail,
            navigationSuiteTypeFor(WindowSizeClass(minWidthDp = 600, minHeightDp = 800))
        )
        assertEquals(
            NavigationSuiteType.NavigationRail,
            navigationSuiteTypeFor(tablet)
        )
    }

    @Test
    fun clickingTabs_navigates() {
        setContent(phone)

        composeRule.onNodeWithText("QR 확인").performClick()
        composeRule.runOnIdle {
            assertEquals(TopLevelDestination.SCAN.route, navController.currentDestination?.route)
        }

        composeRule.onNodeWithText("내 번호").performClick()
        composeRule.runOnIdle {
            assertEquals(TopLevelDestination.MY_TICKETS.route, navController.currentDestination?.route)
        }
    }

    @Test
    fun home_opensGenerateAndBack() {
        setContent(phone)

        composeRule.onNodeWithText("번호 생성하기").performClick()
        composeRule.runOnIdle { assertEquals(Routes.GENERATE, navController.currentDestination?.route) }

        composeRule.onNodeWithText("뒤로 가기").performClick()
        composeRule.runOnIdle {
            assertEquals(TopLevelDestination.HOME.route, navController.currentDestination?.route)
        }
    }
}
