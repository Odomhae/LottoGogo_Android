package com.odom.lottogogo.data.repository

import com.odom.lottogogo.data.local.dao.DrawDao
import com.odom.lottogogo.data.local.dao.TicketDao
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.domain.LottoRules
import com.odom.lottogogo.domain.RankCalculator
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketRepository @Inject constructor(
    private val ticketDao: TicketDao,
    private val drawDao: DrawDao,
    private val clock: Clock,
) {
    fun all(): Flow<List<MyTicket>> = ticketDao.all()

    fun byId(id: Long): Flow<MyTicket?> = ticketDao.byId(id)

    suspend fun pending(round: Int): List<MyTicket> = ticketDao.pending(round)

    /** 게임마다 티켓 1개로 저장. 이미 추첨된 회차면 저장 즉시 등수를 판정한다. */
    suspend fun save(round: Int, games: List<List<Int>>, source: Source) {
        require(games.all(LottoRules::isValidGame)) { "invalid games: $games" }
        val now = clock.millis()
        val tickets = games.map { numbers ->
            MyTicket(
                round = round,
                numbers = MyTicket.encode(numbers),
                source = source,
                createdAt = now,
                rank = rankOf(round, numbers),
            )
        }
        ticketDao.insert(tickets)
    }

    /** 번호/회차 수정 후 등수를 다시 판정한다. */
    suspend fun edit(ticket: MyTicket, round: Int, numbers: List<Int>) {
        require(LottoRules.isValidGame(numbers)) { "invalid numbers: $numbers" }
        ticketDao.update(
            ticket.copy(round = round, numbers = MyTicket.encode(numbers), rank = rankOf(round, numbers))
        )
    }

    suspend fun update(ticket: MyTicket) = ticketDao.update(ticket)

    suspend fun delete(ticket: MyTicket) = ticketDao.delete(ticket)

    private suspend fun rankOf(round: Int, numbers: List<Int>): Int? =
        drawDao.get(round)?.let { RankCalculator.rank(numbers, it) }
}
