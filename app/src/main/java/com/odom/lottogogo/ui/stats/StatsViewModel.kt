package com.odom.lottogogo.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.lottogogo.data.local.dao.NumberCount
import com.odom.lottogogo.data.repository.DrawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 최근 N회 필터. lastN == null 이면 전체 */
enum class RecentFilter(val lastN: Int?) {
    ALL(null), LAST_100(100), LAST_50(50), LAST_10(10)
}

data class StatsUiState(
    val isLoading: Boolean = true,
    val counts: List<NumberCount> = emptyList(),   // 1~45 순서, 항상 45개
    val filter: RecentFilter = RecentFilter.ALL,
    val includeBonus: Boolean = false,
) {
    val maxCount: Int get() = counts.maxOfOrNull { it.cnt } ?: 0
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    drawRepository: DrawRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, RecentFilter.ALL)
    private val includeBonus = savedStateHandle.getStateFlow(KEY_INCLUDE_BONUS, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> =
        combine(filter, includeBonus, ::Pair)
            .flatMapLatest { (filter, bonus) ->
                drawRepository.numberCounts(filter.lastN, bonus).map { counts ->
                    StatsUiState(isLoading = false, counts = counts, filter = filter, includeBonus = bonus)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    fun setFilter(filter: RecentFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    fun setIncludeBonus(include: Boolean) {
        savedStateHandle[KEY_INCLUDE_BONUS] = include
    }

    private companion object {
        const val KEY_FILTER = "filter"
        const val KEY_INCLUDE_BONUS = "includeBonus"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
