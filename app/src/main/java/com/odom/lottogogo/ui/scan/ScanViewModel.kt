package com.odom.lottogogo.ui.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.repository.DrawRepository
import com.odom.lottogogo.data.repository.TicketRepository
import com.odom.lottogogo.domain.ParsedTicket
import com.odom.lottogogo.domain.QrParser
import com.odom.lottogogo.domain.RankCalculator
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
import javax.inject.Inject

/** rank: null = 추첨 전 */
data class GameResult(val numbers: List<Int>, val rank: Int?)

data class ScanResult(val round: Int, val games: List<GameResult>, val draw: Draw?)

data class ScanUiState(
    val result: ScanResult? = null,
    val isInvalidQr: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val drawRepository: DrawRepository,
    private val ticketRepository: TicketRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // 원본 QR 문자열만 저장하고 결과는 다시 계산한다 (회전/프로세스 재생성 대응)
    private val qr = savedStateHandle.getStateFlow<String?>(KEY_QR, null)
    private val isSaved = savedStateHandle.getStateFlow(KEY_SAVED, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScanUiState> =
        combine(qr.flatMapLatest(::resultOf), isSaved) { state, saved -> state.copy(isSaved = saved) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ScanUiState())

    /** @param contents 스캔 결과. null이면 사용자가 취소한 것 */
    fun onScanned(contents: String?) {
        if (contents == null) return
        savedStateHandle[KEY_SAVED] = false
        savedStateHandle[KEY_QR] = contents
    }

    fun dismiss() {
        savedStateHandle[KEY_QR] = null
    }

    fun save() {
        val result = uiState.value.result ?: return
        if (isSaved.value) return
        savedStateHandle[KEY_SAVED] = true
        viewModelScope.launch {
            ticketRepository.save(result.round, result.games.map { it.numbers }, Source.QR)
        }
    }

    private fun resultOf(text: String?): Flow<ScanUiState> {
        if (text == null) return flowOf(ScanUiState())
        val ticket = QrParser.parse(text).getOrNull() ?: return flowOf(ScanUiState(isInvalidQr = true))
        return drawRepository.byRound(ticket.round).map { draw -> ScanUiState(result = evaluate(ticket, draw)) }
    }

    private companion object {
        const val KEY_QR = "qr"
        const val KEY_SAVED = "saved"
        const val STOP_TIMEOUT_MS = 5_000L

        fun evaluate(ticket: ParsedTicket, draw: Draw?) = ScanResult(
            round = ticket.round,
            games = ticket.games.map { GameResult(it, draw?.let { d -> RankCalculator.rank(it, d) }) },
            draw = draw,
        )
    }
}
