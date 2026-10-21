package com.odom.lottogogo.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.repository.DrawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val rounds: List<Int> = emptyList(),   // 내림차순
    val draw: Draw? = null,                // 선택한 회차 (선택 전에는 최신 회차)
) {
    val isEmpty: Boolean get() = !isLoading && draw == null
    val previousRound: Int? get() = draw?.let { d -> rounds.firstOrNull { it < d.round } }
    val nextRound: Int? get() = draw?.let { d -> rounds.lastOrNull { it > d.round } }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    drawRepository: DrawRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // 회전/폴더블 전환 후에도 선택 회차 유지. null = 최신 회차
    private val selectedRound = savedStateHandle.getStateFlow<Int?>(KEY_SELECTED_ROUND, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedDraw = selectedRound.flatMapLatest { round ->
        if (round == null) drawRepository.latest() else drawRepository.byRound(round)
    }

    val uiState: StateFlow<HomeUiState> =
        combine(drawRepository.rounds(), selectedDraw) { rounds, draw ->
            HomeUiState(isLoading = false, rounds = rounds, draw = draw)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun selectRound(round: Int) {
        savedStateHandle[KEY_SELECTED_ROUND] = round
    }

    private companion object {
        const val KEY_SELECTED_ROUND = "selectedRound"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
