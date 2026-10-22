package com.odom.lottogogo.testutil

import com.odom.lottogogo.data.local.dao.TicketDao
import com.odom.lottogogo.data.local.entity.MyTicket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** 메모리 기반 TicketDao. 정렬/필터 규칙은 SQL과 동일하다. */
class FakeTicketDao : TicketDao {
    private val tickets = MutableStateFlow<Map<Long, MyTicket>>(emptyMap())
    private var nextId = 1L

    val current: List<MyTicket> get() = tickets.value.values.toList()

    override fun all(): Flow<List<MyTicket>> = tickets.map { map ->
        map.values.sortedWith(compareByDescending<MyTicket> { it.round }.thenByDescending { it.createdAt })
    }

    override fun byId(id: Long): Flow<MyTicket?> = tickets.map { it[id] }

    override suspend fun pending(round: Int): List<MyTicket> =
        tickets.value.values.filter { it.round == round && it.rank == null }

    override suspend fun insert(tickets: List<MyTicket>) {
        this.tickets.update { current -> current + tickets.map { t -> t.copy(id = nextId++) }.associateBy { it.id } }
    }

    override suspend fun update(ticket: MyTicket) {
        tickets.update { it + (ticket.id to ticket) }
    }

    override suspend fun delete(ticket: MyTicket) {
        tickets.update { it - ticket.id }
    }
}
