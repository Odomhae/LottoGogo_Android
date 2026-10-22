package com.odom.lottogogo.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.odom.lottogogo.R
import com.odom.lottogogo.data.local.SyncStatus
import com.odom.lottogogo.domain.DrawSchedule
import java.time.Instant
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

fun formatSyncTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(DrawSchedule.ZONE).format(timeFormatter)

/** 마지막 갱신 시각. 마지막 동기화가 실패했으면 오프라인 문구를 강조색으로 표시 */
@Composable
fun SyncStatusLine(status: SyncStatus, modifier: Modifier = Modifier) {
    val lastSuccess = status.lastSuccessAt?.let(::formatSyncTime)
    val text = when {
        status.isOffline && lastSuccess != null -> stringResource(R.string.sync_offline, lastSuccess)
        status.isOffline -> stringResource(R.string.sync_offline_never)
        lastSuccess != null -> stringResource(R.string.sync_last_updated, lastSuccess)
        else -> stringResource(R.string.sync_never)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (status.isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
