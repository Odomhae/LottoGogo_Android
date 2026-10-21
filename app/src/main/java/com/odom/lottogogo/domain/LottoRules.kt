package com.odom.lottogogo.domain

object LottoRules {
    const val MIN_NUMBER = 1
    const val MAX_NUMBER = 45
    const val NUMBERS_PER_GAME = 6
    const val MAX_GAMES = 5

    val NUMBER_RANGE = MIN_NUMBER..MAX_NUMBER

    fun isValidGame(numbers: List<Int>): Boolean =
        numbers.size == NUMBERS_PER_GAME &&
            numbers.all { it in NUMBER_RANGE } &&
            numbers.toSet().size == NUMBERS_PER_GAME
}
