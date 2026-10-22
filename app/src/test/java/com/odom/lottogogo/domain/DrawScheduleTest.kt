package com.odom.lottogogo.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class DrawScheduleTest {

    private fun kst(text: String) = LocalDateTime.parse(text).atZone(DrawSchedule.ZONE)

    @Test
    fun roundOn_knownDates() {
        assertEquals(1, DrawSchedule.roundOn(LocalDate.of(2002, 12, 7)))
        assertEquals(1150, DrawSchedule.roundOn(LocalDate.of(2024, 12, 14)))
        assertEquals(1247, DrawSchedule.roundOn(LocalDate.of(2026, 10, 24)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun roundOn_nonSaturday_throws() {
        DrawSchedule.roundOn(LocalDate.of(2026, 10, 22))
    }

    @Test
    fun nextRound_weekdayIsThisSaturday() {
        assertEquals(1247, DrawSchedule.nextRound(kst("2026-10-22T09:00")))   // 목
        assertEquals(1247, DrawSchedule.nextRound(kst("2026-10-18T00:00")))   // 일
    }

    @Test
    fun nextRound_saturdayDeadlineBoundary() {
        assertEquals(1247, DrawSchedule.nextRound(kst("2026-10-24T19:59:59")))
        assertEquals(1248, DrawSchedule.nextRound(kst("2026-10-24T20:00")))
        assertEquals(1248, DrawSchedule.nextRound(kst("2026-10-24T23:00")))
    }

    @Test
    fun nextRound_convertsToKst() {
        // UTC 토 11:00 = KST 토 20:00 → 다음 회차
        val utc = LocalDateTime.parse("2026-10-24T11:00").atZone(ZoneOffset.UTC)
        assertEquals(1248, DrawSchedule.nextRound(utc))
    }
}
