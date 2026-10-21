package com.odom.lottogogo.ui.components

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun date(date: LocalDate): String = date.format(dateFormatter)

    /** 1234567 → "1,234,567" */
    fun amount(value: Long): String = NumberFormat.getNumberInstance(Locale.KOREA).format(value)
}
