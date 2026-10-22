package com.odom.lottogogo.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.odom.lottogogo.data.local.SyncStatusStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.time.Clock

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncer: DrawSyncer,
    private val syncStatus: SyncStatusStore,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val result = syncer.sync()
        Log.i(TAG, "synced rounds=${result.newRounds} won=${result.wonTickets.size}")
        syncStatus.markSuccess(clock.millis())
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크/Firestore 오류: WorkManager 지수 백오프로 재시도
        Log.w(TAG, "sync failed (attempt ${runAttemptCount + 1})", e)
        syncStatus.markFailure(clock.millis())
        Result.retry()
    }

    private companion object {
        const val TAG = "SyncWorker"
    }
}
