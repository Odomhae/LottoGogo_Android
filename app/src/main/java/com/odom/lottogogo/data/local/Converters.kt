package com.odom.lottogogo.data.local

import androidx.room.TypeConverter
import com.odom.lottogogo.data.local.entity.Source
import java.time.LocalDate

class Converters {
    // ISO-8601 "yyyy-MM-dd" — Firestore, assets/lotto.db와 같은 형식
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromSource(source: Source): String = source.name

    @TypeConverter
    fun toSource(value: String): Source = Source.valueOf(value)
}
