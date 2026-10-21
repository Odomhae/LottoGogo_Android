package com.odom.lottogogo.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.odom.lottogogo.data.local.dao.DrawDao
import com.odom.lottogogo.data.local.dao.NumberCount
import com.odom.lottogogo.data.local.dao.TicketDao
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.repository.DrawRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LottoDatabaseTest {

    private lateinit var db: LottoDatabase
    private lateinit var drawDao: DrawDao
    private lateinit var ticketDao: TicketDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LottoDatabase::class.java).build()
        drawDao = db.drawDao()
        ticketDao = db.ticketDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draw(round: Int, numbers: List<Int>, bonus: Int) = Draw(
        round = round,
        date = LocalDate.of(2026, 1, 3).plusWeeks(round.toLong() - 1),
        n1 = numbers[0], n2 = numbers[1], n3 = numbers[2],
        n4 = numbers[3], n5 = numbers[4], n6 = numbers[5],
        bonus = bonus,
        firstPrize = 2_000_000_000L,
        firstWinners = 10
    )

    // 1회: 1~6 +7, 2회: 1,2,3,10,11,12 +45, 3회: 1,20~24 +2
    private val draws = listOf(
        draw(1, listOf(1, 2, 3, 4, 5, 6), bonus = 7),
        draw(2, listOf(1, 2, 3, 10, 11, 12), bonus = 45),
        draw(3, listOf(1, 20, 21, 22, 23, 24), bonus = 2),
    )

    private fun List<NumberCount>.toMap() = associate { it.num to it.cnt }

    @Test
    fun insertAndQuery_roundTripsConvertersAndOrdering() = runTest {
        drawDao.insertAll(draws)

        assertEquals(draws[2], drawDao.latest().first())
        assertEquals(draws[1], drawDao.get(2))
        assertEquals(listOf(3, 2, 1), drawDao.rounds().first())
        assertEquals(3, drawDao.maxRound())
        assertNull(drawDao.get(99))
    }

    @Test
    fun maxRound_isNullWhenEmpty() = runTest {
        assertNull(drawDao.maxRound())
    }

    @Test
    fun countByNumber_allRounds_excludingBonus() = runTest {
        drawDao.insertAll(draws)

        val counts = drawDao.countByNumber(fromRound = 0, includeBonus = false).first().toMap()

        assertEquals(3, counts[1])
        assertEquals(2, counts[2])
        assertEquals(2, counts[3])
        assertEquals(1, counts[24])
        assertNull(counts[7])
        assertNull(counts[45])
        assertEquals(18, counts.values.sum())
    }

    @Test
    fun countByNumber_allRounds_includingBonus() = runTest {
        drawDao.insertAll(draws)

        val counts = drawDao.countByNumber(fromRound = 0, includeBonus = true).first().toMap()

        assertEquals(3, counts[1])
        assertEquals(3, counts[2])
        assertEquals(1, counts[7])
        assertEquals(1, counts[45])
        assertEquals(21, counts.values.sum())
    }

    @Test
    fun countByNumber_recentN_filtersByRound() = runTest {
        drawDao.insertAll(draws)

        // 최근 1회 = round > 3 - 1
        val last1 = drawDao.countByNumber(fromRound = 2, includeBonus = false).first().toMap()
        assertEquals(mapOf(1 to 1, 20 to 1, 21 to 1, 22 to 1, 23 to 1, 24 to 1), last1)

        val last1Bonus = drawDao.countByNumber(fromRound = 2, includeBonus = true).first().toMap()
        assertEquals(1, last1Bonus[2])
        assertNull(last1Bonus[45])

        // 최근 2회 = round > 3 - 2
        val last2 = drawDao.countByNumber(fromRound = 1, includeBonus = true).first().toMap()
        assertEquals(2, last2[1])
        assertEquals(2, last2[2])
        assertEquals(1, last2[45])
        assertNull(last2[7])
        assertEquals(14, last2.values.sum())
    }

    @Test
    fun numberCounts_fillsAll45AndAppliesLastN() = runTest {
        drawDao.insertAll(draws)
        val repository = DrawRepository(drawDao)

        val all = repository.numberCounts(lastN = null, includeBonus = false).first()
        assertEquals((1..45).toList(), all.map { it.num })
        assertEquals(0, all.first { it.num == 45 }.cnt)
        assertEquals(3, all.first { it.num == 1 }.cnt)

        val last1 = repository.numberCounts(lastN = 1, includeBonus = false).first()
        assertEquals(45, last1.size)
        assertEquals(6, last1.sumOf { it.cnt })
        assertEquals(0, last1.first { it.num == 2 }.cnt)

        // N이 전체 회차보다 크면 전체와 같다
        val last100 = repository.numberCounts(lastN = 100, includeBonus = true).first()
        assertEquals(21, last100.sumOf { it.cnt })
    }

    @Test
    fun pending_returnsOnlyUndrawnTicketsOfRound() = runTest {
        val unranked = MyTicket(round = 3, numbers = "1,2,3,4,5,6", source = Source.QR, createdAt = 1L)
        ticketDao.insert(
            listOf(
                unranked,
                MyTicket(round = 3, numbers = "7,8,9,10,11,12", source = Source.GENERATED, createdAt = 2L, rank = 0),
                MyTicket(round = 4, numbers = "1,2,3,4,5,6", source = Source.MANUAL, createdAt = 3L),
            )
        )

        val pending = ticketDao.pending(3)

        assertEquals(1, pending.size)
        assertEquals(unranked.copy(id = pending.single().id), pending.single())
        assertEquals(Source.QR, pending.single().source)
        assertEquals(1, ticketDao.pending(4).size)
        assertEquals(0, ticketDao.pending(5).size)
    }

    @Test
    fun pending_excludesTicketAfterRankUpdate() = runTest {
        ticketDao.insert(listOf(MyTicket(round = 3, numbers = "1,2,3,4,5,6", source = Source.QR, createdAt = 1L)))
        val ticket = ticketDao.pending(3).single()

        ticketDao.update(ticket.copy(rank = 5))

        assertEquals(0, ticketDao.pending(3).size)
        assertEquals(5, ticketDao.byId(ticket.id).first()?.rank)
    }

    @Test
    fun all_ordersByRoundDescThenCreatedAtDesc() = runTest {
        ticketDao.insert(
            listOf(
                MyTicket(round = 1, numbers = "1,2,3,4,5,6", source = Source.MANUAL, createdAt = 10L),
                MyTicket(round = 2, numbers = "1,2,3,4,5,6", source = Source.MANUAL, createdAt = 1L),
                MyTicket(round = 2, numbers = "1,2,3,4,5,6", source = Source.MANUAL, createdAt = 5L),
            )
        )

        val all = ticketDao.all().first()

        assertEquals(listOf(2 to 5L, 2 to 1L, 1 to 10L), all.map { it.round to it.createdAt })

        ticketDao.delete(all.first())
        assertEquals(2, ticketDao.all().first().size)
    }
}
