package com.odom.lottogogo.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** 로또 6/45 추첨 일정. 1회(2002-12-07)부터 매주 토요일 추첨 */
object DrawSchedule {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    private val FIRST_DRAW_DATE: LocalDate = LocalDate.of(2002, 12, 7)
    private val SALES_DEADLINE: LocalTime = LocalTime.of(20, 0)

    /** 해당 토요일에 추첨하는 회차 */
    fun roundOn(drawDate: LocalDate): Int {
        require(drawDate.dayOfWeek == DayOfWeek.SATURDAY) { "not a draw date: $drawDate" }
        require(!drawDate.isBefore(FIRST_DRAW_DATE)) { "before first draw: $drawDate" }
        return (ChronoUnit.WEEKS.between(FIRST_DRAW_DATE, drawDate) + 1).toInt()
    }

    /** 지금 구매하면 참여하는 회차. 토요일 판매 마감(20:00) 이후는 다음 주 회차 */
    fun nextRound(now: ZonedDateTime): Int {
        val local = now.withZoneSameInstant(ZONE)
        val thisSaturday = local.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        val deadline = thisSaturday.atTime(SALES_DEADLINE).atZone(ZONE)
        val drawDate = if (local.isBefore(deadline)) thisSaturday else thisSaturday.plusWeeks(1)
        return roundOn(drawDate)
    }
}
