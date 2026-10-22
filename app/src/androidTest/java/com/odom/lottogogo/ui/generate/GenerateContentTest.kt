package com.odom.lottogogo.ui.generate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.ComposeTestActivity
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenerateContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    private val phone = WindowSizeClass(minWidthDp = 411, minHeightDp = 891)
    private val tablet = WindowSizeClass(minWidthDp = 1280, minHeightDp = 800)

    private val state = GenerateUiState(
        gameCount = 4,
        games = listOf(
            listOf(1, 2, 3, 4, 5, 6),
            listOf(7, 8, 9, 10, 11, 12),
            listOf(13, 14, 15, 16, 17, 18),
            listOf(19, 20, 21, 22, 23, 24),
        ),
        targetRound = 1247,
    )

    private fun setContent(windowSizeClass: WindowSizeClass, state: GenerateUiState = this.state, onSave: () -> Unit = {}) {
        composeRule.setContent {
            LottoGogoTheme {
                GenerateContent(state, windowSizeClass, {}, {}, {}, onSave)
            }
        }
    }

    private fun label(text: String) = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot()

    @Test
    fun compact_listsGamesVertically() {
        setContent(phone)

        assertEquals(label("A").left, label("B").left)
        assertTrue(label("B").top > label("A").top)
        composeRule.onNodeWithText("제 1247회 대상").assertIsDisplayed()
    }

    @Test
    fun expanded_twoColumnGrid() {
        setContent(tablet)

        // A B / C D
        assertEquals(label("A").top, label("B").top)
        assertTrue(label("B").left > label("A").left)
        assertEquals(label("A").left, label("C").left)
    }

    @Test
    fun saveButton_invokesCallback() {
        var saves = 0
        setContent(phone, onSave = { saves++ })

        composeRule.onNodeWithText("보관함에 저장").performClick()
        assertEquals(1, saves)
    }

    @Test
    fun savedState_disablesSave() {
        setContent(phone, state = state.copy(isSaved = true))

        composeRule.onNodeWithText("저장됨").assertIsNotEnabled()
    }
}
