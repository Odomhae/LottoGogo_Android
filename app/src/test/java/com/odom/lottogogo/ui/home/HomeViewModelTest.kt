package com.odom.lottogogo.ui.home

import androidx.lifecycle.SavedStateHandle
import com.odom.lottogogo.data.local.SyncStatus
import com.odom.lottogogo.data.repository.DrawRepository
import com.odom.lottogogo.testutil.FakeDrawDao
import com.odom.lottogogo.testutil.FakeSyncStatusStore
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
    private val syncStatus = FakeSyncStatusStore()

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        HomeViewModel(DrawRepository(dao), syncStatus, savedStateHandle)

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
        val vm = HomeViewModel(DrawRepository(FakeDrawDao()), FakeSyncStatusStore(), SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun syncStatus_propagatesAndDetectsOffline() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        syncStatus.markSuccess(100L)
        assertEquals(SyncStatus(lastSuccessAt = 100L), vm.uiState.value.syncStatus)
        assertFalse(vm.uiState.value.syncStatus.isOffline)

        syncStatus.markFailure(200L)
        assertTrue(vm.uiState.value.syncStatus.isOffline)

        syncStatus.markSuccess(300L)
        assertFalse(vm.uiState.value.syncStatus.isOffline)
    }

    @Test
    fun syncStatus_failureWithoutSuccess_isOffline() {
        assertTrue(SyncStatus(lastFailureAt = 1L).isOffline)
        assertFalse(SyncStatus().isOffline)
    }
}
