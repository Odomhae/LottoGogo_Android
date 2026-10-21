package com.odom.lottogogo.ui.home

import androidx.lifecycle.SavedStateHandle
import com.odom.lottogogo.data.repository.DrawRepository
import com.odom.lottogogo.testutil.FakeDrawDao
import com.odom.lottogogo.testutil.MainDispatcherRule
import com.odom.lottogogo.testutil.testDraw
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeDrawDao(listOf(testDraw(1), testDraw(2), testDraw(3)))

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        HomeViewModel(DrawRepository(dao), savedStateHandle)

    @Test
    fun initialState_showsLatestDraw() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.draw?.round)
        assertEquals(listOf(3, 2, 1), state.rounds)
        assertEquals(2, state.previousRound)
        assertNull(state.nextRound)
    }

    @Test
    fun selectRound_showsThatDraw() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vm.selectRound(1)

        val state = vm.uiState.value
        assertEquals(1, state.draw?.round)
        assertNull(state.previousRound)
        assertEquals(2, state.nextRound)
    }

    @Test
    fun selectedRound_restoredFromSavedState() = runTest {
        val vm = viewModel(SavedStateHandle(mapOf("selectedRound" to 2)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        assertEquals(2, vm.uiState.value.draw?.round)
    }

    @Test
    fun newDraw_updatesLatestWhenNothingSelected() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        dao.insertAll(listOf(testDraw(4)))

        assertEquals(4, vm.uiState.value.draw?.round)
        assertEquals(listOf(4, 3, 2, 1), vm.uiState.value.rounds)
    }

    @Test
    fun emptyDatabase_isEmptyState() = runTest {
        val vm = HomeViewModel(DrawRepository(FakeDrawDao()), SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        assertTrue(vm.uiState.value.isEmpty)
    }
}
