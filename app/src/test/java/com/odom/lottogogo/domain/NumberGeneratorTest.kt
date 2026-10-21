package com.odom.lottogogo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberGeneratorTest {

    private val generator = NumberGenerator()

    @Test
    fun generateGame_isSixSortedUniqueNumbersInRange_1000Times() {
        repeat(1_000) {
            val game = generator.generateGame()
            assertEquals(6, game.size)
            assertTrue(game.all { it in 1..45 })
            assertEquals(6, game.toSet().size)
            assertEquals(game.sorted(), game)
        }
    }

    @Test
    fun generate_returnsRequestedGameCount() {
        (1..5).forEach { count ->
            val games = generator.generate(count)
            assertEquals(count, games.size)
            assertTrue(games.all(LottoRules::isValidGame))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun generate_zeroGames_throws() {
        generator.generate(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun generate_sixGames_throws() {
        generator.generate(6)
    }
}
