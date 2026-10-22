package com.odom.lottogogo.ui.mytickets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.ComposeTestActivity
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.domain.RankCalculator
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MyTicketsContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    private val phone = WindowSizeClass(minWidthDp = 411, minHeightDp = 891)
    private val tablet = WindowSizeClass(minWidthDp = 1280, minHeightDp = 800)

    private val draw1246 = Draw(1246, LocalDate.of(2026, 10, 17), 1, 2, 3, 4, 5, 6, 7, 2_000_000_000L, 10)

    /** ViewModel 역할을 하는 상태 호스트: 추가 시 1246회는 즉시 등수 판정 */
    private fun setContent(windowSizeClass: WindowSizeClass, initial: List<MyTicket> = emptyList()) {
        composeRule.setContent {
            var tickets by remember { mutableStateOf(initial) }
            var selectedId by remember { mutableStateOf<Long?>(null) }
            val draws = mapOf(1246 to draw1246)
            val selected = tickets.firstOrNull { it.id == selectedId }?.let { TicketDetail(it, draws[it.round]) }
            LottoGogoTheme {
                MyTicketsContent(
                    state = MyTicketsUiState(
                        isLoading = false,
                        groups = tickets.groupBy { it.round }.map { (r, t) -> TicketGroup(r, t) },
                        selected = selected,
                        nextRound = 1247,
                    ),
                    windowSizeClass = windowSizeClass,
                    onSelect = { selectedId = it },
                    onAdd = { round, numbers ->
                        val rank = draws[round]?.let { RankCalculator.rank(numbers, it) }
                        tickets = tickets + MyTicket(tickets.size + 1L, round, MyTicket.encode(numbers), Source.MANUAL, 0L, rank)
                    },
                    onEdit = { _, _, _ -> },
                    onDelete = { t -> tickets = tickets - t },
                )
            }
        }
    }

    private val sample = listOf(
        MyTicket(1, 1247, "10,11,12,13,14,15", Source.GENERATED, 2L, null),
        MyTicket(2, 1246, "1,2,3,4,5,45", Source.QR, 1L, 3),
    )

    @Test
    fun list_groupsByRoundWithRankBadges() {
        setContent(phone, sample)

        composeRule.onNodeWithText("제 1247회").assertIsDisplayed()
        composeRule.onNodeWithText("제 1246회").assertIsDisplayed()
        composeRule.onNodeWithText("추첨 전").assertIsDisplayed()
        composeRule.onNodeWithText("3등").assertIsDisplayed()
    }

    @Test
    fun compact_tapOpensDetail_backReturnsToList() {
        setContent(phone, sample)

        composeRule.onNodeWithText("QR 스캔").performClick()
        composeRule.onNodeWithText("5개 일치").assertIsDisplayed()
        composeRule.onNodeWithText("자동 생성").assertDoesNotExist()   // 목록 숨김

        // 창 포커스가 없어도(잠금 화면 위) 동작하도록 디스패처로 뒤로 가기 전달
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText("자동 생성").assertIsDisplayed()
        composeRule.onNodeWithText("5개 일치").assertDoesNotExist()
    }

    @Test
    fun expanded_showsListAndDetailTogether() {
        setContent(tablet, sample)
        composeRule.onNodeWithText("왼쪽 목록에서 번호를 선택하세요").assertIsDisplayed()

        composeRule.onNodeWithText("QR 스캔").performClick()

        composeRule.onNodeWithText("5개 일치").assertIsDisplayed()
        composeRule.onNodeWithText("자동 생성").assertIsDisplayed()   // 목록 유지
    }

    @Test
    fun manualInput_drawnRound_showsRankImmediately() {
        setContent(phone)
        composeRule.onNodeWithText("저장된 번호가 없습니다").assertIsDisplayed()

        composeRule.onNodeWithText("번호 직접 입력").performClick()
        // 기본 회차 1247 → 1246으로 변경
        composeRule.onNodeWithText("1247").performTextReplacement("1246")
        listOf(1, 2, 3, 4, 40, 41).forEach {
            composeRule.onNodeWithContentDescription("번호 $it").performClick()
        }
        composeRule.onNodeWithText("저장").performClick()

        composeRule.onNodeWithText("제 1246회").assertIsDisplayed()
        composeRule.onNodeWithText("4등").assertIsDisplayed()
    }

    @Test
    fun delete_removesFromList() {
        setContent(phone, sample.take(1))

        composeRule.onNodeWithText("자동 생성").performClick()
        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onAllNodesWithText("삭제").assertCountEquals(2) // 버튼 + 확인 다이얼로그
        composeRule.onAllNodesWithText("삭제").onLast().performClick()

        composeRule.onNodeWithText("저장된 번호가 없습니다").assertIsDisplayed()
    }
}
