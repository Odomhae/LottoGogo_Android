package com.odom.lottogogo.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.ComposeTestActivity
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HomeContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    private val phone = WindowSizeClass(minWidthDp = 411, minHeightDp = 891)
    private val tablet = WindowSizeClass(minWidthDp = 1280, minHeightDp = 800)

    private val draws = (1..3).associateWith { round ->
        Draw(
            round = round,
            date = LocalDate.of(2026, 1, 3).plusWeeks(round.toLong() - 1),
            n1 = round, n2 = 11, n3 = 21, n4 = 31, n5 = 41, n6 = 45,
            bonus = 20,
            firstPrize = 2_000_000_000L,
            firstWinners = if (round == 2) 0 else 12,
        )
    }

    /** ViewModel 없이 선택 회차를 상태로 들고 있는 테스트 호스트 */
    private fun setContent(windowSizeClass: WindowSizeClass) {
        composeRule.setContent {
            var selected by rememberSaveable { mutableStateOf(3) }
            LottoGogoTheme {
                HomeContent(
                    state = HomeUiState(isLoading = false, rounds = listOf(3, 2, 1), draw = draws[selected]),
                    windowSizeClass = windowSizeClass,
                    onSelectRound = { selected = it },
                    onGenerateClick = {},
                )
            }
        }
    }

    @Test
    fun compact_showsLatestDrawCard() {
        setContent(phone)

        composeRule.onNodeWithText("2026.01.17 추첨").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("번호 3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("번호 45").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("보너스 번호 20").assertIsDisplayed()
        composeRule.onNodeWithText("1등 당첨금 2,000,000,000원").assertIsDisplayed()
        composeRule.onNodeWithText("1등 당첨자 12명").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("다음 회차").assertIsNotEnabled()
    }

    @Test
    fun compact_previousButtonChangesRound() {
        setContent(phone)

        composeRule.onNodeWithContentDescription("이전 회차").performClick()

        composeRule.onNodeWithText("2026.01.10 추첨").assertIsDisplayed()
        composeRule.onNodeWithText("1등 당첨자 없음").assertIsDisplayed()
    }

    @Test
    fun compact_pickerDialogSelectsRound() {
        setContent(phone)

        composeRule.onNodeWithContentDescription("회차 선택").performClick()
        composeRule.onNodeWithText("제 1회").performClick()

        composeRule.onNodeWithText("2026.01.03 추첨").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("이전 회차").assertIsNotEnabled()
    }

    @Test
    fun expanded_showsListAndDetailSideBySide() {
        setContent(tablet)

        val listItem = composeRule.onNodeWithText("제 1회").assertIsDisplayed()
        // 제 3회: 목록 항목 + 상세 카드 제목
        composeRule.onAllNodesWithText("제 3회").assertCountEquals(2)
        composeRule.onNodeWithText("2026.01.17 추첨").assertIsDisplayed()

        listItem.performClick()

        composeRule.onNodeWithText("2026.01.03 추첨").assertIsDisplayed()
        composeRule.onAllNodesWithText("제 1회").assertCountEquals(2)
    }

    @Test
    fun emptyState_showsMessageAndGenerateButton() {
        var generateClicks = 0
        composeRule.setContent {
            LottoGogoTheme {
                HomeContent(
                    state = HomeUiState(isLoading = false),
                    windowSizeClass = phone,
                    onSelectRound = {},
                    onGenerateClick = { generateClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("당첨번호 데이터가 없습니다").assertIsDisplayed()
        composeRule.onNodeWithText("번호 생성하기").performClick()
        assertEquals(1, generateClicks)
    }
}
