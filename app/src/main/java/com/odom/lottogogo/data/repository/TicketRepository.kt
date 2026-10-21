package com.odom.lottogogo.data.repository

import com.odom.lottogogo.data.local.dao.TicketDao
import com.odom.lottogogo.data.local.entity.MyTicket
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketRepository @Inject constructor(
    private val ticketDao: TicketDao
) {
    fun all(): Flow<List<MyTicket>> = ticketDao.all()

    fun byId(id: Long): Flow<MyTicket?> = ticketDao.byId(id)

    suspend fun pending(round: Int): List<MyTicket> = ticketDao.pending(round)

    // 저장 시 등수 판정은 Step 7(RankCalculator 연동)에서 추가
    suspend fun save(tickets: List<MyTicket>) = ticketDao.insert(tickets)

    suspend fun update(ticket: MyTicket) = ticketDao.update(ticket)

    suspend fun delete(ticket: MyTicket) = ticketDao.delete(ticket)
}
