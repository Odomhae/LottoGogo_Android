package com.odom.lottogogo.data.repository

import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.testutil.FakeDrawDao
import com.odom.lottogogo.testutil.FakeTicketDao
import com.odom.lottogogo.testutil.testDraw
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TicketRepositoryTest {

    private val ticketDao = FakeTicketDao()
    private val drawDao = FakeDrawDao(listOf(testDraw(10, listOf(1, 2, 3, 4, 5, 6), bonus = 7)))
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)
    private val repository = TicketRepository(ticketDao, drawDao, clock)

    @Test
    fun save_drawnRound_ranksImmediately() = runTest {
        repository.save(
            round = 10,
            games = listOf(listOf(1, 2, 3, 4, 5, 6), listOf(6, 5, 4, 3, 2, 7), listOf(40, 41, 42, 43, 44, 45)),
            source = Source.GENERATED,
        )

        val saved = ticketDao.current.sortedBy { it.id }
        assertEquals(listOf(1, 2, 0), saved.map { it.rank })
        assertEquals("2,3,4,5,6,7", saved[1].numbers)
        assertEquals(1_000L, saved[0].createdAt)
        assertEquals(Source.GENERATED, saved[0].source)
    }

    @Test
    fun save_undrawnRound_isPending() = runTest {
        repository.save(round = 11, games = listOf(listOf(1, 2, 3, 4, 5, 6)), source = Source.MANUAL)

        val saved = ticketDao.current.single()
        assertNull(saved.rank)
        assertEquals(1, repository.pending(11).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun save_invalidGame_throws() = runTest {
        repository.save(round = 10, games = listOf(listOf(1, 1, 2, 3, 4, 5)), source = Source.MANUAL)
    }

    @Test
    fun edit_reRanks() = runTest {
        repository.save(round = 11, games = listOf(listOf(1, 2, 3, 40, 41, 42)), source = Source.MANUAL)
        val ticket = ticketDao.current.single()

        repository.edit(ticket, round = 10, numbers = listOf(1, 2, 3, 40, 41, 42))

        val edited = ticketDao.current.single()
        assertEquals(10, edited.round)
        assertEquals(5, edited.rank)
    }
}
