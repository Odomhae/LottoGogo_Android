package com.odom.lottogogo.domain

data class ParsedTicket(val round: Int, val games: List<List<Int>>)

/**
 * 동행복권 로또 용지 QR 파서.
 *
 * 형식: `...?v=` + 회차 4자리 + (구분 문자 1자리 + 번호 12자리) × 게임 수 + [추가 식별 값]
 * 예: `https://m.dhlottery.co.kr/?v=1100q041014193538m051222233739n000000000000...1234567890`
 * - 구분 문자는 게임 종류(자동/수동/반자동 등). 번호가 모두 0인 칸은 빈 게임으로 보고 건너뛴다.
 * - 실제 형식은 실물 용지로 확인한다 (src/test/resources/qr_samples.txt).
 */
object QrParser {
    private const val ROUND_LENGTH = 4
    private const val GAME_DIGITS = LottoRules.NUMBERS_PER_GAME * 2

    private val V_PARAM = Regex("""[?&]v=([^&#]+)""")
    private val PAYLOAD = Regex("""^(\d{$ROUND_LENGTH})((?:[A-Za-z]\d{$GAME_DIGITS})+)""")
    private val GAME = Regex("""[A-Za-z](\d{$GAME_DIGITS})""")
    private val EMPTY_GAME = "0".repeat(GAME_DIGITS)

    fun parse(qr: String): Result<ParsedTicket> = runCatching {
        val value = V_PARAM.find(qr.trim())?.groupValues?.get(1)
            ?: throw IllegalArgumentException("v parameter not found")
        val match = PAYLOAD.find(value)
            ?: throw IllegalArgumentException("invalid payload: $value")

        val round = match.groupValues[1].toInt()
        require(round > 0) { "invalid round: $round" }

        val games = GAME.findAll(match.groupValues[2])
            .map { it.groupValues[1] }
            .filter { it != EMPTY_GAME }
            .map(::parseGame)
            .toList()
        require(games.size in 1..LottoRules.MAX_GAMES) { "invalid game count: ${games.size}" }

        ParsedTicket(round, games)
    }

    private fun parseGame(digits: String): List<Int> {
        val numbers = digits.chunked(2).map { it.toInt() }
        require(LottoRules.isValidGame(numbers)) { "invalid game: $digits" }
        return numbers.sorted()
    }
}
