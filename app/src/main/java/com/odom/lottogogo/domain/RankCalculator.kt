package com.odom.lottogogo.domain

import com.odom.lottogogo.data.local.entity.Draw

object RankCalculator {
    const val NO_PRIZE = 0

    /** @return 1~5 = 등수, 0 = 낙첨 */
    fun rank(ticket: List<Int>, draw: Draw): Int = rank(ticket, draw.numbers, draw.bonus)

    fun rank(ticket: List<Int>, winning: List<Int>, bonus: Int): Int {
        require(ticket.size == LottoRules.NUMBERS_PER_GAME) { "ticket must have 6 numbers: $ticket" }
        val matched = ticket.count { it in winning }
        return when {
            matched == 6 -> 1
            matched == 5 && bonus in ticket -> 2
            matched == 5 -> 3
            matched == 4 -> 4
            matched == 3 -> 5
            else -> NO_PRIZE
        }
    }
}
