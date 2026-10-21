package com.odom.lottogogo.data.repository

import com.odom.lottogogo.data.local.dao.DrawDao
import com.odom.lottogogo.data.local.dao.NumberCount
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.domain.LottoRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawRepository @Inject constructor(
    private val drawDao: DrawDao
) {
    fun latest(): Flow<Draw?> = drawDao.latest()

    fun byRound(round: Int): Flow<Draw?> = drawDao.byRound(round)

    fun rounds(): Flow<List<Int>> = drawDao.rounds()

    suspend fun get(round: Int): Draw? = drawDao.get(round)

    /**
     * 1~45 번호별 출현 횟수. 한 번도 나오지 않은 번호는 0으로 채운다.
     * @param lastN 최근 N회만 집계. null이면 전체.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun numberCounts(lastN: Int?, includeBonus: Boolean): Flow<List<NumberCount>> =
        drawDao.latest()
            .map { it?.round ?: 0 }
            .distinctUntilChanged()
            .flatMapLatest { maxRound ->
                val fromRound = if (lastN == null) 0 else (maxRound - lastN).coerceAtLeast(0)
                drawDao.countByNumber(fromRound, includeBonus)
            }
            .map(::fillAllNumbers)

    private fun fillAllNumbers(counts: List<NumberCount>): List<NumberCount> {
        val byNum = counts.associate { it.num to it.cnt }
        return LottoRules.NUMBER_RANGE.map { NumberCount(it, byNum[it] ?: 0) }
    }
}
