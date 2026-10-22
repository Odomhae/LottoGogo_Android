package com.odom.lottogogo.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 동기화 결과 시각 (epoch millis). 한 번도 시도하지 않았으면 null */
data class SyncStatus(
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
) {
    /** 마지막 시도가 실패했으면 오프라인(마지막 갱신 시각 표시) */
    val isOffline: Boolean
        get() = lastFailureAt != null && (lastSuccessAt == null || lastFailureAt > lastSuccessAt)
}

interface SyncStatusStore {
    val status: Flow<SyncStatus>
    suspend fun markSuccess(at: Long)
    suspend fun markFailure(at: Long)
}

@Singleton
class DataStoreSyncStatusStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SyncStatusStore {

    override val status: Flow<SyncStatus> = dataStore.data.map { prefs ->
        SyncStatus(lastSuccessAt = prefs[LAST_SUCCESS], lastFailureAt = prefs[LAST_FAILURE])
    }

    override suspend fun markSuccess(at: Long) {
        dataStore.edit { it[LAST_SUCCESS] = at }
    }

    override suspend fun markFailure(at: Long) {
        dataStore.edit { it[LAST_FAILURE] = at }
    }

    private companion object {
        val LAST_SUCCESS = longPreferencesKey("last_sync_success")
        val LAST_FAILURE = longPreferencesKey("last_sync_failure")
    }
}
