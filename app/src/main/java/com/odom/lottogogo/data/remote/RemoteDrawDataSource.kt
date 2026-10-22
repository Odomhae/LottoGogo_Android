package com.odom.lottogogo.data.remote

import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.domain.LottoRules
import java.time.LocalDate

/** 신규 회차 증분 조회용 원격 소스. 결과는 반드시 Room을 거쳐 UI에 도달한다. */
interface RemoteDrawDataSource {
    /** round > [afterRound] 인 회차를 오름차순으로 반환 */
    suspend fun drawsAfter(afterRound: Int): List<Draw>
}

/**
 * Firestore 문서(필드는 Draw와 동일, date는 "yyyy-MM-dd")를 Draw로 변환한다.
 * 형식이 맞지 않는 문서는 null (잘못된 데이터가 로컬 DB에 들어가지 않도록).
 */
fun Map<String, Any?>.toDrawOrNull(): Draw? = runCatching {
    val numbers = (1..6).map { int("n$it") }.sorted()
    val bonus = int("bonus")
    require(LottoRules.isValidGame(numbers) && bonus in LottoRules.NUMBER_RANGE && bonus !in numbers)
    Draw(
        round = int("round").also { require(it > 0) },
        date = LocalDate.parse(this["date"] as String),
        n1 = numbers[0], n2 = numbers[1], n3 = numbers[2],
        n4 = numbers[3], n5 = numbers[4], n6 = numbers[5],
        bonus = bonus,
        firstPrize = (this["firstPrize"] as Number).toLong(),
        firstWinners = int("firstWinners"),
    )
}.getOrNull()

private fun Map<String, Any?>.int(key: String): Int = (this[key] as Number).toInt()
