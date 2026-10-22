package com.odom.lottogogo.ui.stats

import androidx.lifecycle.SavedStateHandle
import com.odom.lottogogo.data.repository.DrawRepository
import com.odom.lottogogo.testutil.FakeDrawDao
import com.odom.lottogogo.testutil.MainDispatcherRule
import com.odom.lottogogo.testutil.testDraw
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // 1회: 1~6 +7, 2회: 1,2,3,10,11,12 +45, 3회: 1,20~24 +2
    private val dao = FakeDrawDao(
        listOf(
            testDraw(1, listOf(1, 2, 3, 4, 5, 6), bonus = 7),
            testDraw(2, listOf(1, 2, 3, 10, 11, 12), bonus = 45),
            testDraw(3, listOf(1, 20, 21, 22, 23, 24), bonus = 2),
        )
    )

    private fun TestScope.subscribedViewModel(handle: SavedStateHandle = SavedStateHandle()): StatsViewModel {
        val vm = StatsViewModel(DrawRepository(dao), handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        return vm
    }

    private fun StatsUiState.countOf(number: Int) = counts.first { it.num == number }.cnt

    @Test
    fun default_allRoundsWithoutBonus() = runTest {
        val state = subscribedViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(45, state.counts.size)
        assertEquals(RecentFilter.ALL, state.filter)
        assertFalse(state.includeBonus)
        assertEquals(3, state.countOf(1))
        assertEquals(2, state.countOf(2))
        assertEquals(0, state.countOf(45))
        assertEquals(3, state.maxCount)
    }

    @Test
    fun includeBonus_addsBonusNumbers() = runTest {
        val vm = subscribedViewModel()

        vm.setIncludeBonus(true)

        val state = vm.uiState.value
        assertTrue(state.includeBonus)
        assertEquals(3, state.countOf(2))
        assertEquals(1, state.countOf(45))
        assertEquals(21, state.counts.sumOf { it.cnt })
    }

    @Test
    fun recentFilter_limitsRounds() = runTest {
        val vm = subscribedViewModel()

        vm.setFilter(RecentFilter.LAST_10)
        assertEquals(18, vm.uiState.value.counts.sumOf { it.cnt }) // 10 > 3회 → 전체

        dao.insertAll((4..13).map { testDraw(it, listOf(40, 41, 42, 43, 44, 45), bonus = 1) })

        // 최근 10회 = 4~13회
        val state = vm.uiState.value
        assertEquals(RecentFilter.LAST_10, state.filter)
        assertEquals(10, state.countOf(45))
        assertEquals(0, state.countOf(1))
        assertEquals(60, state.counts.sumOf { it.cnt })
    }

    @Test
    fun filterAndBonus_restoredFromSavedState() = runTest {
        val handle = SavedStateHandle(mapOf("filter" to RecentFilter.LAST_50, "includeBonus" to true))

        val state = subscribedViewModel(handle).uiState.value

        assertEquals(RecentFilter.LAST_50, state.filter)
        assertTrue(state.includeBonus)
    }

    @Test
    fun heatLevel_minToMaxRange() {
        // 전체 회차처럼 150~190에 몰려 있어도 최소=0, 최대=1로 펼친다
        assertEquals(0f, heatLevel(150, 150, 190))
        assertEquals(0.5f, heatLevel(170, 150, 190))
        assertEquals(1f, heatLevel(190, 150, 190))
        assertEquals(1f, heatLevel(5, 5, 5))       // 모두 같으면 최대 농도
        assertEquals(0f, heatLevel(0, 0, 0))       // 데이터 없음
    }

    @Test
    fun heatLevel_isRatioToMax() {
        assertEquals(0f, heatLevel(0, 0))
        assertEquals(0f, heatLevel(0, 10))
        assertEquals(0.5f, heatLevel(5, 10))
        assertEquals(1f, heatLevel(10, 10))
    }
}
