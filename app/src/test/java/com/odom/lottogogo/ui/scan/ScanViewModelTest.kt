package com.odom.lottogogo.ui.scan

import androidx.lifecycle.SavedStateHandle
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.repository.DrawRepository
import com.odom.lottogogo.data.repository.TicketRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // 1100회: 1~6 + 7
    private val drawDao = FakeDrawDao(listOf(testDraw(1100, listOf(1, 2, 3, 4, 5, 6), bonus = 7)))
    private val ticketDao = FakeTicketDao()

    private fun TestScope.subscribedViewModel(handle: SavedStateHandle = SavedStateHandle()): ScanViewModel {
        val vm = ScanViewModel(
            DrawRepository(drawDao),
            TicketRepository(ticketDao, drawDao, Clock.systemUTC()),
            handle,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        return vm
    }

    // A: 1등, B: 2등(5+보너스), C: 낙첨
    private val drawnQr = "https://m.dhlottery.co.kr/?v=1100q010203040506q020304050607m404142434445n0000000000001234567890"
    private val undrawnQr = "https://m.dhlottery.co.kr/?v=1300q010203040506"

    @Test
    fun drawnRound_ranksEachGame() = runTest {
        val vm = subscribedViewModel()

        vm.onScanned(drawnQr)

        val result = vm.uiState.value.result!!
        assertEquals(1100, result.round)
        assertEquals(listOf(1, 2, 0), result.games.map { it.rank })
        assertEquals(1100, result.draw?.round)
    }

    @Test
    fun undrawnRound_isPending_thenRankedWhenDrawArrives() = runTest {
        val vm = subscribedViewModel()

        vm.onScanned(undrawnQr)
        assertNull(vm.uiState.value.result!!.draw)
        assertNull(vm.uiState.value.result!!.games.single().rank)

        drawDao.insertAll(listOf(testDraw(1300, listOf(1, 2, 3, 4, 5, 6), bonus = 7)))

        assertEquals(1, vm.uiState.value.result!!.games.single().rank)
    }

    @Test
    fun invalidQr_setsError() = runTest {
        val vm = subscribedViewModel()

        vm.onScanned("https://example.com")

        assertTrue(vm.uiState.value.isInvalidQr)
        assertNull(vm.uiState.value.result)
    }

    @Test
    fun cancelledScan_keepsState() = runTest {
        val vm = subscribedViewModel()
        vm.onScanned(drawnQr)

        vm.onScanned(null)

        assertEquals(1100, vm.uiState.value.result?.round)
    }

    @Test
    fun save_storesAllGamesOnceAsQr() = runTest {
        val vm = subscribedViewModel()
        vm.onScanned(drawnQr)

        vm.save()
        vm.save()

        val saved = ticketDao.current
        assertEquals(3, saved.size)
        assertTrue(saved.all { it.source == Source.QR && it.round == 1100 })
        assertEquals(setOf(1, 2, 0), saved.map { it.rank }.toSet())
        assertTrue(vm.uiState.value.isSaved)
    }

    @Test
    fun dismiss_clearsResult_andNewScanResetsSaved() = runTest {
        val vm = subscribedViewModel()
        vm.onScanned(drawnQr)
        vm.save()

        vm.dismiss()
        assertNull(vm.uiState.value.result)

        vm.onScanned(undrawnQr)
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun resultRestoredFromSavedState() = runTest {
        val vm = subscribedViewModel(SavedStateHandle(mapOf("qr" to drawnQr)))

        assertEquals(1100, vm.uiState.value.result?.round)
    }
}
