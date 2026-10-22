package com.odom.lottogogo.ui.mytickets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.repository.DrawRepository
import com.odom.lottogogo.data.repository.TicketRepository
import com.odom.lottogogo.domain.DrawSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

data class TicketGroup(val round: Int, val tickets: List<MyTicket>)

data class TicketDetail(val ticket: MyTicket, val draw: Draw?)

data class MyTicketsUiState(
    val isLoading: Boolean = true,
    val groups: List<TicketGroup> = emptyList(),
    val selected: TicketDetail? = null,
    val nextRound: Int = 0,
) {
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}

@HiltViewModel
class MyTicketsViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val drawRepository: DrawRepository,
    private val savedStateHandle: SavedStateHandle,
    clock: Clock,
) : ViewModel() {

    private val nextRound = DrawSchedule.nextRound(ZonedDateTime.now(clock))
    private val selectedId = savedStateHandle.getStateFlow<Long?>(KEY_SELECTED_ID, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedDetail: Flow<TicketDetail?> = selectedId.flatMapLatest { id ->
        if (id == null) flowOf(null) else ticketRepository.byId(id).flatMapLatest(::detailOf)
    }

    val uiState: StateFlow<MyTicketsUiState> =
        combine(ticketRepository.all(), selectedDetail) { tickets, detail ->
            MyTicketsUiState(
                isLoading = false,
                groups = tickets.groupBy { it.round }.map { (round, list) -> TicketGroup(round, list) },
                selected = detail,
                nextRound = nextRound,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            MyTicketsUiState(nextRound = nextRound),
        )

    fun select(id: Long?) {
        savedStateHandle[KEY_SELECTED_ID] = id
    }

    fun addManual(round: Int, numbers: List<Int>) {
        viewModelScope.launch { ticketRepository.save(round, listOf(numbers), Source.MANUAL) }
    }

    fun edit(ticket: MyTicket, round: Int, numbers: List<Int>) {
        viewModelScope.launch { ticketRepository.edit(ticket, round, numbers) }
    }

    fun delete(ticket: MyTicket) {
        if (selectedId.value == ticket.id) select(null)
        viewModelScope.launch { ticketRepository.delete(ticket) }
    }

    private fun detailOf(ticket: MyTicket?): Flow<TicketDetail?> =
        if (ticket == null) {
            flowOf(null)
        } else {
            drawRepository.byRound(ticket.round).map { draw -> TicketDetail(ticket, draw) }
        }

    private companion object {
        const val KEY_SELECTED_ID = "selectedTicketId"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
