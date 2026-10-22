package com.odom.lottogogo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.odom.lottogogo.ComposeTestActivity
import com.odom.lottogogo.ui.stats.numberCellTag
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** edge-to-edge에서 각 탭의 첫 콘텐츠가 상태바 아래에서 시작하는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class SystemBarInsetsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    private var statusBarHeight: Dp = 0.dp

    private fun setContent() {
        composeRule.setContent {
            val density = LocalDensity.current
            statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
            LottoGogoTheme { LottoAppScaffold() }
        }
    }

    private fun assertBelowStatusBar(node: SemanticsNodeInteraction, name: String) {
        val top = node.getUnclippedBoundsInRoot().top
        assertTrue("$name top=$top statusBar=$statusBarHeight", top >= statusBarHeight)
    }

    // 홈 최상단: 마지막 갱신/내장 데이터 안내 문구
    private val syncStatusText = SemanticsMatcher("sync status line") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { "갱신" in it.text || "내장" in it.text }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun tabsStartBelowStatusBar() {
        setContent()
        composeRule.waitForIdle()
        assertTrue("status bar inset should exist", statusBarHeight > 0.dp)

        assertBelowStatusBar(composeRule.onAllNodes(syncStatusText).onFirst(), "home sync status")
        screenshot("home")

        composeRule.onAllNodesWithText("통계").onFirst().performClick()
        composeRule.waitForIdle()
        assertBelowStatusBar(composeRule.onNodeWithTag(numberCellTag(1)), "stats cell 1")
        screenshot("stats")

        composeRule.onAllNodesWithText("QR 확인").onFirst().performClick()
        composeRule.waitForIdle()
        screenshot("scan")

        composeRule.onAllNodesWithText("내 번호").onFirst().performClick()
        composeRule.waitForIdle()
        screenshot("my_tickets")
    }
}
