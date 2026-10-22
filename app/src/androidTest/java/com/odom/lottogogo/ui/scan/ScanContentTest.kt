package com.odom.lottogogo.ui.scan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class ScanContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    private val phone = WindowSizeClass(minWidthDp = 411, minHeightDp = 891)
    private val tablet = WindowSizeClass(minWidthDp = 1280, minHeightDp = 800)

    private val draw = Draw(1100, LocalDate.of(2024, 1, 6), 1, 2, 3, 4, 5, 6, 7, 2_000_000_000L, 10)
    private val drawnResult = ScanResult(
        round = 1100,
        games = listOf(
            GameResult(listOf(1, 2, 3, 4, 5, 6), 1),
            GameResult(listOf(40, 41, 42, 43, 44, 45), 0),
        ),
        draw = draw,
    )

    private fun setContent(
        state: ScanUiState,
        windowSizeClass: WindowSizeClass = phone,
        permissionDenied: Boolean = false,
        onScan: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onSave: () -> Unit = {},
    ) {
        composeRule.setContent {
            LottoGogoTheme {
                ScanContent(state, permissionDenied, windowSizeClass, onScan, onOpenSettings, onSave, {})
            }
        }
    }

    @Test
    fun idle_scanButtonInvokesCallback() {
        var scans = 0
        setContent(ScanUiState(), onScan = { scans++ })

        composeRule.onNodeWithText("QR 스캔하기").performClick()

        assertEquals(1, scans)
    }

    @Test
    fun permissionDenied_showsGuideAndSettingsButton() {
        var opened = 0
        setContent(ScanUiState(), permissionDenied = true, onOpenSettings = { opened++ })

        composeRule.onNodeWithText("QR 스캔을 위해 카메라 권한이 필요합니다. 설정에서 권한을 허용해 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("설정 열기").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun compact_resultInBottomSheet_withRankPerGame() {
        setContent(ScanUiState(result = drawnResult), phone)

        composeRule.onNodeWithTag(SCAN_RESULT_SHEET_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("제 1100회").assertIsDisplayed()
        composeRule.onNodeWithText("1등").assertIsDisplayed()
        composeRule.onNodeWithText("낙첨").assertIsDisplayed()
    }

    @Test
    fun expanded_resultInDialog() {
        setContent(ScanUiState(result = drawnResult), tablet)

        composeRule.onNodeWithTag(SCAN_RESULT_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SCAN_RESULT_SHEET_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("1등").assertIsDisplayed()
    }

    @Test
    fun undrawnRound_showsPending() {
        val pending = ScanResult(1300, listOf(GameResult(listOf(1, 2, 3, 4, 5, 6), null)), draw = null)
        setContent(ScanUiState(result = pending))

        composeRule.onNodeWithText("아직 추첨 전인 회차입니다").assertIsDisplayed()
        composeRule.onNodeWithText("추첨 전").assertIsDisplayed()
    }

    @Test
    fun save_invokesCallback() {
        var saves = 0
        setContent(ScanUiState(result = drawnResult), onSave = { saves++ })
        composeRule.onNodeWithText("보관함에 저장").performClick()
        assertEquals(1, saves)
    }

    @Test
    fun savedState_disablesSaveButton() {
        setContent(ScanUiState(result = drawnResult, isSaved = true))
        composeRule.onNodeWithText("저장됨").assertIsNotEnabled()
    }

    @Test
    fun invalidQr_showsError() {
        setContent(ScanUiState(isInvalidQr = true))
        composeRule.onNodeWithText("로또 용지 QR 코드가 아닙니다").assertIsDisplayed()
    }
}
