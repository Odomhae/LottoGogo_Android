package com.odom.lottogogo.ui.mytickets

import androidx.lifecycle.SavedStateHandle
import com.odom.lottogogo.data.repository.DrawRepository
import com.odom.lottogogo.data.repository.TicketRepository
import com.odom.lottogogo.domain.DrawSchedule
import com.odom.lottogogo.testutil.FakeDrawDao
import com.odom.lottogogo.testutil.FakeTicketDao
import com.odom.lottogogo.testutil.MainDispatcherRule
import com.odom.lottogogo.testutil.testDraw
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime

class MyTicketsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(
        LocalDateTime.parse("2026-10-22T09:00").atZone(DrawSchedule.ZONE).toInstant(),
        DrawSchedule.ZONE,
    )
    // 1246회 추첨 완료: 1~6 + 7
    private val drawDao = FakeDrawDao(listOf(testDraw(1246, listOf(1, 2, 3, 4, 5, 6), bonus = 7)))
    private val ticketDao = FakeTicketDao()
    private val ticketRepository = TicketRepository(ticketDao, drawDao, clock)

    private fun TestScope.subscribedViewModel(): MyTicketsViewModel {
        val vm = MyTicketsViewModel(ticketRepository, DrawRepository(drawDao), SavedStateHandle(), clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        return vm
    }

    @Test
    fun emptyInitially_withNextRound() = runTest {
        val state = subscribedViewModel().uiState.value

        assertTrue(state.isEmpty)
        assertEquals(1247, state.nextRound)
    }

    @Test
    fun addManual_drawnRound_showsRankImmediately() = runTest {
        val vm = subscribedViewModel()

        vm.addManual(1246, listOf(1, 2, 3, 4, 5, 45))   // 5개 일치 → 3등
        vm.addManual(1247, listOf(1, 2, 3, 4, 5, 6))    // 미추첨

        val groups = vm.uiState.value.groups
        assertEquals(listOf(1247, 1246), groups.map { it.round })
        assertNull(groups[0].tickets.single().rank)
        assertEquals(3, groups[1].tickets.single().rank)
    }

    @Test
    fun select_providesDetailWithDraw() = runTest {
        val vm = subscribedViewModel()
        vm.addManual(1246, listOf(1, 2, 3, 40, 41, 42))
        val ticket = vm.uiState.value.groups.single().tickets.single()

        vm.select(ticket.id)

        val detail = vm.uiState.value.selected
        assertEquals(ticket.id, detail?.ticket?.id)
        assertEquals(1246, detail?.draw?.round)
    }

    @Test
    fun edit_updatesRankAndDetail() = runTest {
        val vm = subscribedViewModel()
        vm.addManual(1247, listOf(1, 2, 3, 40, 41, 42))
        val ticket = vm.uiState.value.groups.single().tickets.single()
        vm.select(ticket.id)

        vm.edit(ticket, 1246, listOf(1, 2, 3, 4, 41, 42))

        val selected = vm.uiState.value.selected
        assertEquals(4, selected?.ticket?.rank)
        assertEquals(1246, selected?.draw?.round)
    }

    @Test
    fun delete_removesTicketAndClearsSelection() = runTest {
        val vm = subscribedViewModel()
        vm.addManual(1247, listOf(1, 2, 3, 4, 5, 6))
        val ticket = vm.uiState.value.groups.single().tickets.single()
        vm.select(ticket.id)

        vm.delete(ticket)

        assertTrue(vm.uiState.value.isEmpty)
        assertNull(vm.uiState.value.selected)
    }
}
