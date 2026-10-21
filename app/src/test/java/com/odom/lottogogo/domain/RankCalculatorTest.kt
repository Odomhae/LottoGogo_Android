package com.odom.lottogogo.domain

import com.odom.lottogogo.data.local.entity.Draw
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RankCalculatorTest {

    // 당첨: 1 2 3 4 5 6 + 보너스 7
    private val draw = Draw(
        round = 1, date = LocalDate.of(2026, 1, 3),
        n1 = 1, n2 = 2, n3 = 3, n4 = 4, n5 = 5, n6 = 6,
        bonus = 7, firstPrize = 0L, firstWinners = 0
    )

    private fun rank(vararg numbers: Int) = RankCalculator.rank(numbers.toList(), draw)

    @Test fun sixMatches_isFirst() = assertEquals(1, rank(1, 2, 3, 4, 5, 6))
    @Test fun sixMatches_unordered_isFirst() = assertEquals(1, rank(6, 5, 4, 3, 2, 1))
    @Test fun fiveMatchesPlusBonus_isSecond() = assertEquals(2, rank(1, 2, 3, 4, 5, 7))
    @Test fun fiveMatches_isThird() = assertEquals(3, rank(1, 2, 3, 4, 5, 45))
    @Test fun fourMatches_isFourth() = assertEquals(4, rank(1, 2, 3, 4, 44, 45))
    @Test fun fourMatchesPlusBonus_isStillFourth() = assertEquals(4, rank(1, 2, 3, 4, 7, 45))
    @Test fun threeMatches_isFifth() = assertEquals(5, rank(1, 2, 3, 43, 44, 45))
    @Test fun threeMatchesPlusBonus_isStillFifth() = assertEquals(5, rank(1, 2, 3, 7, 44, 45))
    @Test fun twoMatchesPlusBonus_isNoPrize() = assertEquals(0, rank(1, 2, 7, 43, 44, 45))
    @Test fun noMatches_isNoPrize() = assertEquals(0, rank(40, 41, 42, 43, 44, 45))

    @Test(expected = IllegalArgumentException::class)
    fun wrongSize_throws() {
        rank(1, 2, 3, 4, 5)
    }
}
