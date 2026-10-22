package com.odom.lottogogo.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DrawMappingTest {

    // Firestore는 정수를 Long으로 돌려준다
    private val doc: Map<String, Any?> = mapOf(
        "round" to 1247L, "date" to "2026-10-24",
        "n1" to 40L, "n2" to 3L, "n3" to 11L, "n4" to 17L, "n5" to 24L, "n6" to 38L,
        "bonus" to 7L, "firstPrize" to 2_543_256_870L, "firstWinners" to 11L,
    )

    @Test
    fun mapsAndSortsNumbers() {
        val draw = doc.toDrawOrNull()!!

        assertEquals(1247, draw.round)
        assertEquals(LocalDate.of(2026, 10, 24), draw.date)
        assertEquals(listOf(3, 11, 17, 24, 38, 40), draw.numbers)
        assertEquals(7, draw.bonus)
        assertEquals(2_543_256_870L, draw.firstPrize)
        assertEquals(11, draw.firstWinners)
    }

    @Test
    fun invalidDocuments_areNull() {
        assertNull((doc - "n6").toDrawOrNull())
        assertNull((doc + ("n6" to 3L)).toDrawOrNull())          // 중복
        assertNull((doc + ("bonus" to 40L)).toDrawOrNull())      // 보너스 중복
        assertNull((doc + ("n1" to 46L)).toDrawOrNull())         // 범위
        assertNull((doc + ("date" to "2026/10/24")).toDrawOrNull())
        assertNull((doc + ("round" to 0L)).toDrawOrNull())
    }
}
