package com.odom.lottogogo.ui.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.ComposeTestActivity
import com.odom.lottogogo.data.local.dao.NumberCount
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    private val phone = WindowSizeClass(minWidthDp = 411, minHeightDp = 891)
    private val tablet = WindowSizeClass(minWidthDp = 1280, minHeightDp = 800)

    /** 필터/보너스 값에 따라 횟수가 달라지는 가짜 데이터 */
    private fun countsFor(filter: RecentFilter, bonus: Boolean) =
        (1..45).map { NumberCount(it, (filter.lastN ?: 200) / 10 + if (bonus) 1 else 0) }

    private fun setContent(windowSizeClass: WindowSizeClass) {
        composeRule.setContent {
            var filter by remember { mutableStateOf(RecentFilter.ALL) }
            var bonus by remember { mutableStateOf(false) }
            LottoGogoTheme {
                StatsContent(
                    state = StatsUiState(false, countsFor(filter, bonus), filter, bonus),
                    windowSizeClass = windowSizeClass,
                    onFilterChange = { filter = it },
                    onIncludeBonusChange = { bonus = it },
                )
            }
        }
    }

    private fun cell(number: Int) = composeRule.onNodeWithTag(numberCellTag(number)).getUnclippedBoundsInRoot()

    @Test
    fun compact_grid9x5() {
        setContent(phone)

        // 9번은 1행 마지막, 10번은 2행 첫 칸
        assertEquals(cell(1).top, cell(9).top)
        assertEquals(cell(1).left, cell(10).left)
        assertTrue(cell(10).top > cell(1).top)
        assertEquals(cell(1).left, cell(37).left) // 5행 첫 칸
    }

    @Test
    fun expanded_grid15x3() {
        setContent(tablet)

        assertEquals(cell(1).top, cell(15).top)
        assertEquals(cell(1).left, cell(16).left)
        assertEquals(cell(1).left, cell(31).left) // 3행 첫 칸
        assertTrue(cell(31).top > cell(16).top)
    }

    @Test
    fun filterAndBonus_updateImmediately() {
        setContent(phone)
        composeRule.onNodeWithContentDescription("번호 7, 20회").assertIsDisplayed()

        composeRule.onNodeWithText("10회").performClick()
        composeRule.onNodeWithText("10회").assertIsSelected()
        composeRule.onNodeWithContentDescription("번호 7, 1회").assertIsDisplayed()

        composeRule.onNodeWithText("보너스 번호 포함").performClick()
        composeRule.onNodeWithContentDescription("번호 7, 2회").assertIsDisplayed()
    }

    @Test
    fun showsReferenceNotice() {
        setContent(phone)
        composeRule.onNodeWithText("과거 데이터 기반 참고용 통계입니다.").assertIsDisplayed()
    }
}
