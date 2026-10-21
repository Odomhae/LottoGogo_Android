package com.odom.lottogogo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class Draw(
    @PrimaryKey val round: Int,
    val date: LocalDate,
    val n1: Int, val n2: Int, val n3: Int,
    val n4: Int, val n5: Int, val n6: Int,   // 오름차순 저장
    val bonus: Int,
    val firstPrize: Long,                     // 1등 1인당 당첨금(원)
    val firstWinners: Int
) {
    val numbers: List<Int> get() = listOf(n1, n2, n3, n4, n5, n6)
}
