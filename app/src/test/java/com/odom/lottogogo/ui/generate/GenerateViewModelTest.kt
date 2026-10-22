package com.odom.lottogogo.ui.generate

import androidx.lifecycle.SavedStateHandle
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.repository.TicketRepository
import com.odom.lottogogo.domain.DrawSchedule
import com.odom.lottogogo.domain.LottoRules
import com.odom.lottogogo.domain.NumberGenerator
import com.odom.lottogogo.testutil.FakeDrawDao
import com.odom.lottogogo.testutil.FakeTicketDao
import com.odom.lottogogo.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime
import java.util.Random

class GenerateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // 2026-10-22(목) → 대상 회차 1247
    private val clock = Clock.fixed(
        LocalDateTime.parse("2026-10-22T09:00").atZone(DrawSchedule.ZONE).toInstant(),
        DrawSchedule.ZONE,
    )
    private val ticketDao = FakeTicketDao()
    private val repository = TicketRepository(ticketDao, FakeDrawDao(), clock)

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        GenerateViewModel(NumberGenerator(Random(42)), repository, handle, clock)

    @Test
    fun initialState() {
        val state = viewModel().uiState.value

        assertEquals(1247, state.targetRound)
        assertEquals(5, state.gameCount)
        assertTrue(state.games.isEmpty())
        assertFalse(state.canSave)
    }

    @Test
    fun generate_createsRequestedValidGames() {
        val vm = viewModel()

        vm.setGameCount(3)
        vm.generate()

        val state = vm.uiState.value
        assertEquals(3, state.games.size)
        assertTrue(state.games.all(LottoRules::isValidGame))
        assertTrue(state.canSave)
    }

    @Test
    fun setGameCount_clampsTo1to5() {
        val vm = viewModel()
        vm.setGameCount(9)
        assertEquals(5, vm.uiState.value.gameCount)
        vm.setGameCount(0)
        assertEquals(1, vm.uiState.value.gameCount)
    }

    @Test
    fun save_storesGamesOnceForTargetRound() = runTest {
        val vm = viewModel()
        vm.setGameCount(2)
        vm.generate()
        val games = vm.uiState.value.games

        vm.save()
        vm.save() // 중복 저장 방지

        val saved = ticketDao.current
        assertEquals(2, saved.size)
        assertTrue(saved.all { it.round == 1247 && it.source == Source.GENERATED && it.rank == null })
        assertEquals(games.map { it.joinToString(",") }.toSet(), saved.map { it.numbers }.toSet())
        assertTrue(vm.uiState.value.isSaved)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun regenerate_afterSave_allowsSavingAgain() {
        val vm = viewModel()
        vm.generate()
        vm.save()

        vm.generate()

        assertFalse(vm.uiState.value.isSaved)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun gamesRestoredFromSavedState() {
        val handle = SavedStateHandle(mapOf("games" to "1,2,3,4,5,6;7,8,9,10,11,12", "gameCount" to 2))

        val state = viewModel(handle).uiState.value

        assertEquals(listOf(listOf(1, 2, 3, 4, 5, 6), listOf(7, 8, 9, 10, 11, 12)), state.games)
        assertEquals(2, state.gameCount)
    }
}
