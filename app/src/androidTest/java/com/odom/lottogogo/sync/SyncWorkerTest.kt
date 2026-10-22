package com.odom.lottogogo.sync

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.odom.lottogogo.data.local.LottoDatabase
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.data.remote.RemoteDrawDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: LottoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, LottoDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draw(round: Int, numbers: List<Int>, bonus: Int) = Draw(
        round, LocalDate.of(2026, 10, 24).plusWeeks(round - 1247L),
        numbers[0], numbers[1], numbers[2], numbers[3], numbers[4], numbers[5],
        bonus, 2_000_000_000L, 10,
    )

    private fun worker(remote: RemoteDrawDataSource): SyncWorker {
        val transaction = object : TransactionRunner {
            override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
        }
        val syncer = DrawSyncer(db.drawDao(), db.ticketDao(), remote, transaction)
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    SyncWorker(appContext, params, syncer)
            })
            .build()
    }

    @Test
    fun newRound_insertedAndPendingTicketRanked() = runTest {
        db.drawDao().insertAll(listOf(draw(1246, listOf(16, 22, 23, 24, 27, 28), 6)))
        db.ticketDao().insert(
            listOf(
                MyTicket(round = 1247, numbers = "1,2,3,4,40,41", source = Source.QR, createdAt = 0L),
                MyTicket(round = 1248, numbers = "1,2,3,4,5,6", source = Source.QR, createdAt = 0L),
            )
        )
        val remote = object : RemoteDrawDataSource {
            override suspend fun drawsAfter(afterRound: Int) =
                listOf(draw(1247, listOf(1, 2, 3, 4, 5, 6), 7)).filter { it.round > afterRound }
        }

        val result = worker(remote).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1247, db.drawDao().maxRound())
        val tickets = db.ticketDao().all().first().associateBy { it.round }
        assertEquals(4, tickets.getValue(1247).rank)
        assertNull(tickets.getValue(1248).rank)
    }

    @Test
    fun remoteFailure_retries() = runTest {
        val remote = object : RemoteDrawDataSource {
            override suspend fun drawsAfter(afterRound: Int): List<Draw> = throw IOException("offline")
        }

        val result = worker(remote).doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertNull(db.drawDao().maxRound())
    }
}
