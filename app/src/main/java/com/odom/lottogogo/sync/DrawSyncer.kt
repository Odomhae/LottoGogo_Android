package com.odom.lottogogo.sync

import com.odom.lottogogo.data.local.dao.DrawDao
import com.odom.lottogogo.data.local.dao.TicketDao
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.remote.RemoteDrawDataSource
import com.odom.lottogogo.domain.RankCalculator
import javax.inject.Inject

/** 여러 DB 작업을 하나의 트랜잭션으로 묶는다 (테스트에서는 그대로 실행) */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

data class SyncResult(
    val newRounds: List<Int>,
    val wonTickets: List<MyTicket>,   // 이번 동기화로 1~5등이 확정된 티켓 (2차 F-07 알림용)
)

/**
 * 1. 로컬 최대 회차 확인 → 2. 원격에서 이후 회차 조회 → 3. 트랜잭션으로 insert
 * → 4. 새 회차의 추첨 전 티켓 등수 판정
 */
class DrawSyncer @Inject constructor(
    private val drawDao: DrawDao,
    private val ticketDao: TicketDao,
    private val remote: RemoteDrawDataSource,
    private val transaction: TransactionRunner,
) {
    suspend fun sync(): SyncResult {
        val maxRound = drawDao.maxRound() ?: 0
        val draws = remote.drawsAfter(maxRound).filter { it.round > maxRound }
        if (draws.isEmpty()) return SyncResult(emptyList(), emptyList())

        return transaction.run {
            drawDao.insertAll(draws)
            val ranked = draws.flatMap { draw ->
                ticketDao.pending(draw.round).map { ticket ->
                    ticket.copy(rank = RankCalculator.rank(ticket.numberList, draw)).also { ticketDao.update(it) }
                }
            }
            SyncResult(
                newRounds = draws.map { it.round },
                wonTickets = ranked.filter { it.rank != RankCalculator.NO_PRIZE },
            )
        }
    }
}
