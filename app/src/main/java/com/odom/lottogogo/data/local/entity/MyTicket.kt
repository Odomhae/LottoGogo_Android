package com.odom.lottogogo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Source { QR, GENERATED, MANUAL }

@Entity(indices = [Index("round")])
data class MyTicket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val round: Int,
    val numbers: String,      // "3,11,17,24,38,42" (오름차순)
    val source: Source,
    val createdAt: Long,
    val rank: Int? = null     // null = 추첨 전, 0 = 낙첨, 1~5 = 등수
) {
    val numberList: List<Int> get() = numbers.split(',').map { it.trim().toInt() }

    companion object {
        fun encode(numbers: List<Int>): String = numbers.sorted().joinToString(",")
    }
}
