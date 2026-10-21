package com.odom.lottogogo.domain

import java.security.SecureRandom
import java.util.Random
import javax.inject.Inject

class NumberGenerator(private val random: Random) {

    @Inject
    constructor() : this(SecureRandom())

    fun generateGame(): List<Int> =
        LottoRules.NUMBER_RANGE.shuffled(random).take(LottoRules.NUMBERS_PER_GAME).sorted()

    fun generate(games: Int): List<List<Int>> {
        require(games in 1..LottoRules.MAX_GAMES) { "games must be 1..${LottoRules.MAX_GAMES}: $games" }
        return List(games) { generateGame() }
    }
}
