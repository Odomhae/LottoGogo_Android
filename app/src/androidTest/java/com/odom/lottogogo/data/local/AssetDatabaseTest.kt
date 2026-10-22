package com.odom.lottogogo.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.odom.lottogogo.domain.DrawSchedule
import com.odom.lottogogo.domain.LottoRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** assets/lotto.db가 Room 스키마와 일치하고 전 회차를 담고 있는지 확인한다 (새로 설치한 상태와 동일). */
@RunWith(AndroidJUnit4::class)
class AssetDatabaseTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: LottoDatabase

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
        // 스키마가 다르면 build 후 첫 쿼리에서 Room이 예외를 던진다
        db = Room.databaseBuilder(context, LottoDatabase::class.java, TEST_DB)
            .createFromAsset(LottoDatabase.ASSET)
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun containsAllRoundsWithoutGaps() = runTest {
        val maxRound = requireNotNull(db.drawDao().maxRound())

        assertTrue("max round $maxRound", maxRound >= MIN_EXPECTED_ROUND)
        assertEquals((maxRound downTo 1).toList(), db.drawDao().rounds().first())
    }

    @Test
    fun firstRoundMatchesKnownResult() = runTest {
        val draw = requireNotNull(db.drawDao().get(1))

        assertEquals(LocalDate.of(2002, 12, 7), draw.date)
        assertEquals(listOf(10, 23, 29, 33, 37, 40), draw.numbers)
        assertEquals(16, draw.bonus)
    }

    @Test
    fun everyDrawIsValidAndOnSchedule() = runTest {
        val maxRound = requireNotNull(db.drawDao().maxRound())
        (1..maxRound).forEach { round ->
            val draw = requireNotNull(db.drawDao().get(round))
            assertTrue("round $round numbers", LottoRules.isValidGame(draw.numbers))
            assertEquals("round $round sorted", draw.numbers.sorted(), draw.numbers)
            assertTrue("round $round bonus", draw.bonus in LottoRules.NUMBER_RANGE && draw.bonus !in draw.numbers)
            assertEquals("round $round date", round, DrawSchedule.roundOn(draw.date))
        }
    }

    @Test
    fun ticketTableIsEmpty() = runTest {
        assertTrue(db.ticketDao().all().first().isEmpty())
    }

    private companion object {
        const val TEST_DB = "asset-test.db"
        const val MIN_EXPECTED_ROUND = 1246   // 2026-10-17 추첨분까지 내장
    }
}
