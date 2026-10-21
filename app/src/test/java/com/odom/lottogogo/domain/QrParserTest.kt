package com.odom.lottogogo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrParserTest {

    private data class Sample(val qr: String, val round: Int, val games: List<List<Int>>)

    private fun loadSamples(): List<Sample> {
        val text = requireNotNull(javaClass.classLoader?.getResource("qr_samples.txt")).readText()
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                val (qr, round, games) = line.split("|").map { it.trim() }
                Sample(
                    qr = qr,
                    round = round.toInt(),
                    games = games.split(";").map { g -> g.split(",").map { it.trim().toInt() } }
                )
            }
    }

    @Test
    fun parsesAllSamples() {
        val samples = loadSamples()
        assertTrue(samples.isNotEmpty())
        samples.forEach { sample ->
            val parsed = QrParser.parse(sample.qr).getOrThrow()
            assertEquals(sample.qr, ParsedTicket(sample.round, sample.games), parsed)
        }
    }

    @Test
    fun parsesGameCountsOneToFive() {
        val game = "q010203040506"
        (1..5).forEach { count ->
            val parsed = QrParser.parse("https://m.dhlottery.co.kr/?v=1100${game.repeat(count)}").getOrThrow()
            assertEquals(count, parsed.games.size)
        }
    }

    @Test
    fun sortsNumbersWithinGame() {
        val parsed = QrParser.parse("https://m.dhlottery.co.kr/?v=1100m450102030405").getOrThrow()
        assertEquals(listOf(listOf(1, 2, 3, 4, 5, 45)), parsed.games)
    }

    @Test
    fun ignoresTrailingIdentifierAndOtherParams() {
        val parsed = QrParser.parse("https://m.dhlottery.co.kr/qr.do?method=winQr&v=1100q0102030405069876543210&x=1")
            .getOrThrow()
        assertEquals(ParsedTicket(1100, listOf(listOf(1, 2, 3, 4, 5, 6))), parsed)
    }

    @Test
    fun failsOnInvalidInput() {
        val invalid = listOf(
            "",
            "hello world",
            "https://m.dhlottery.co.kr/?x=1100q010203040506",            // v 없음
            "https://m.dhlottery.co.kr/?v=110q010203040506",              // 회차 3자리
            "https://m.dhlottery.co.kr/?v=0000q010203040506",             // 회차 0
            "https://m.dhlottery.co.kr/?v=1100",                          // 게임 없음
            "https://m.dhlottery.co.kr/?v=1100n000000000000",             // 빈 게임만
            "https://m.dhlottery.co.kr/?v=1100q01020304050",              // 번호 11자리
            "https://m.dhlottery.co.kr/?v=1100q010203040546",             // 46
            "https://m.dhlottery.co.kr/?v=1100q000203040506",             // 0
            "https://m.dhlottery.co.kr/?v=1100q010103040506",             // 중복
            "https://m.dhlottery.co.kr/?v=1100" + "q010203040506".repeat(6), // 6게임
        )
        invalid.forEach { qr ->
            assertTrue("should fail: $qr", QrParser.parse(qr).isFailure)
        }
    }
}
