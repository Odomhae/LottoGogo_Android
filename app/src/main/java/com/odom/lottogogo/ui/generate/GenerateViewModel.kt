package com.odom.lottogogo.ui.generate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.repository.TicketRepository
import com.odom.lottogogo.domain.DrawSchedule
import com.odom.lottogogo.domain.LottoRules
import com.odom.lottogogo.domain.NumberGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

data class GenerateUiState(
    val gameCount: Int = DEFAULT_GAME_COUNT,
    val games: List<List<Int>> = emptyList(),
    val targetRound: Int = 0,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = games.isNotEmpty() && !isSaved

    companion object {
        const val DEFAULT_GAME_COUNT = 5
    }
}

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val generator: NumberGenerator,
    private val ticketRepository: TicketRepository,
    private val savedStateHandle: SavedStateHandle,
    clock: Clock,
) : ViewModel() {

    private val targetRound = DrawSchedule.nextRound(ZonedDateTime.now(clock))
    private val gameCount = savedStateHandle.getStateFlow(KEY_GAME_COUNT, GenerateUiState.DEFAULT_GAME_COUNT)
    // List<List<Int>>는 Bundle에 넣을 수 없어 "1,2,3,4,5,6;..." 문자열로 저장
    private val encodedGames = savedStateHandle.getStateFlow(KEY_GAMES, "")
    private val isSaved = savedStateHandle.getStateFlow(KEY_SAVED, false)

    val uiState: StateFlow<GenerateUiState> =
        combine(gameCount, encodedGames, isSaved) { count, games, saved ->
            GenerateUiState(count, decode(games), targetRound, saved)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GenerateUiState(gameCount.value, decode(encodedGames.value), targetRound, isSaved.value),
        )

    fun setGameCount(count: Int) {
        savedStateHandle[KEY_GAME_COUNT] = count.coerceIn(1, LottoRules.MAX_GAMES)
    }

    fun generate() {
        savedStateHandle[KEY_GAMES] = encode(generator.generate(gameCount.value))
        setSaved(false)
    }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        setSaved(true)
        viewModelScope.launch {
            ticketRepository.save(state.targetRound, state.games, Source.GENERATED)
        }
    }

    private fun setSaved(saved: Boolean) {
        savedStateHandle[KEY_SAVED] = saved
    }

    private companion object {
        const val KEY_GAME_COUNT = "gameCount"
        const val KEY_GAMES = "games"
        const val KEY_SAVED = "saved"

        fun encode(games: List<List<Int>>): String = games.joinToString(";") { it.joinToString(",") }

        fun decode(value: String): List<List<Int>> =
            if (value.isEmpty()) emptyList() else value.split(";").map { g -> g.split(",").map(String::toInt) }
    }
}
