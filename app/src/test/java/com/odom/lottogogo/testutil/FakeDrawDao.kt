package com.odom.lottogogo.testutil

import com.odom.lottogogo.data.local.dao.DrawDao
import com.odom.lottogogo.data.local.dao.NumberCount
import com.odom.lottogogo.data.local.entity.Draw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** 메모리 기반 DrawDao. countByNumber도 SQL과 같은 규칙으로 계산한다. */
class FakeDrawDao(initial: List<Draw> = emptyList()) : DrawDao {
    private val draws = MutableStateFlow(initial.associateBy { it.round })

    override fun latest(): Flow<Draw?> = draws.map { it.values.maxByOrNull(Draw::round) }
    override fun byRound(round: Int): Flow<Draw?> = draws.map { it[round] }
    override fun rounds(): Flow<List<Int>> = draws.map { it.keys.sortedDescending() }
    override suspend fun maxRound(): Int? = draws.value.keys.maxOrNull()
    override suspend fun get(round: Int): Draw? = draws.value[round]

    override suspend fun insertAll(draws: List<Draw>) {
        this.draws.update { current -> current + draws.associateBy { it.round } }
    }

    override fun countByNumber(fromRound: Int, includeBonus: Boolean): Flow<List<NumberCount>> =
        draws.map { all ->
            all.values
                .filter { it.round > fromRound }
                .flatMap { if (includeBonus) it.numbers + it.bonus else it.numbers }
                .groupingBy { it }
                .eachCount()
                .toSortedMap()
                .map { (num, cnt) -> NumberCount(num, cnt) }
        }
}

fun testDraw(round: Int, numbers: List<Int> = listOf(1, 2, 3, 4, 5, 6), bonus: Int = 7) = Draw(
    round = round,
    date = LocalDate.of(2026, 1, 3).plusWeeks(round.toLong() - 1),
    n1 = numbers[0], n2 = numbers[1], n3 = numbers[2],
    n4 = numbers[3], n5 = numbers[4], n6 = numbers[5],
    bonus = bonus,
    firstPrize = 1_000_000_000L,
    firstWinners = 10,
)
