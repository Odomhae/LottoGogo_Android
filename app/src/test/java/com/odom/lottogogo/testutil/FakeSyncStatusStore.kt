package com.odom.lottogogo.testutil

import com.odom.lottogogo.data.local.SyncStatus
import com.odom.lottogogo.data.local.SyncStatusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSyncStatusStore(initial: SyncStatus = SyncStatus()) : SyncStatusStore {
    override val status = MutableStateFlow(initial)
    override suspend fun markSuccess(at: Long) = status.update { it.copy(lastSuccessAt = at) }
    override suspend fun markFailure(at: Long) = status.update { it.copy(lastFailureAt = at) }
}
