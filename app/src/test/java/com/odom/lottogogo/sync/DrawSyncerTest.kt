package com.odom.lottogogo.sync

import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.remote.RemoteDrawDataSource
import com.odom.lottogogo.testutil.FakeDrawDao
import com.odom.lottogogo.testutil.FakeTicketDao
import com.odom.lottogogo.testutil.testDraw
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawSyncerTest {

    private class FakeRemote(var draws: List<Draw> = emptyList()) : RemoteDrawDataSource {
        val requestedAfter = mutableListOf<Int>()
        override suspend fun drawsAfter(afterRound: Int): List<Draw> {
            requestedAfter += afterRound
            return draws.filter { it.round > afterRound }
        }
    }

    private val drawDao = FakeDrawDao(listOf(testDraw(1), testDraw(2)))
    private val ticketDao = FakeTicketDao()
    private val remote = FakeRemote()
    private var transactions = 0
    private val countingTransaction = object : TransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            transactions++
            return block()
        }
    }
    private val syncer = DrawSyncer(drawDao, ticketDao, remote, countingTransaction)

    private fun ticket(round: Int, numbers: String) =
        MyTicket(round = round, numbers = numbers, source = Source.MANUAL, createdAt = 0L)

    @Test
    fun noNewRounds_doesNothing() = runTest {
        val result = syncer.sync()

        assertEquals(listOf(2), remote.requestedAfter)
        assertTrue(result.newRounds.isEmpty())
        assertEquals(0, transactions)
    }

    @Test
    fun newRounds_insertedInTransaction_andPendingTicketsRanked() = runTest {
        remote.draws = listOf(
            testDraw(3, listOf(1, 2, 3, 4, 5, 6), bonus = 7),
            testDraw(4, listOf(10, 11, 12, 13, 14, 15), bonus = 16),
        )
        ticketDao.insert(
            listOf(
                ticket(3, "1,2,3,4,5,7"),        // 2등
                ticket(3, "40,41,42,43,44,45"),  // 낙첨
                ticket(4, "10,11,12,20,21,22"),  // 5등
                ticket(5, "1,2,3,4,5,6"),        // 아직 추첨 전
            )
        )

        val result = syncer.sync()

        assertEquals(listOf(3, 4), result.newRounds)
        assertEquals(1, transactions)
        assertEquals(4, drawDao.maxRound())
        val ranks = ticketDao.current.sortedBy { it.id }.map { it.rank }
        assertEquals(listOf(2, 0, 5, null), ranks)
        assertEquals(setOf(2, 5), result.wonTickets.map { it.rank }.toSet())
    }

    @Test
    fun alreadyRankedTickets_untouched() = runTest {
        remote.draws = listOf(testDraw(3, listOf(1, 2, 3, 4, 5, 6), bonus = 7))
        ticketDao.insert(listOf(ticket(3, "1,2,3,4,5,6").copy(rank = 0)))   // 수동으로 들어간 값

        syncer.sync()

        assertEquals(0, ticketDao.current.single().rank)
    }

    @Test
    fun secondSync_requestsFromNewMax() = runTest {
        remote.draws = listOf(testDraw(3))
        syncer.sync()
        syncer.sync()

        assertEquals(listOf(2, 3), remote.requestedAfter)
    }

    @Test(expected = IllegalStateException::class)
    fun remoteFailure_propagates() = runTest {
        val failing = DrawSyncer(drawDao, ticketDao, object : RemoteDrawDataSource {
            override suspend fun drawsAfter(afterRound: Int): List<Draw> = error("offline")
        }, countingTransaction)
        failing.sync()
    }
}
