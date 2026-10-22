package com.odom.lottogogo.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.odom.lottogogo.R
import com.odom.lottogogo.ui.components.DrawNumbers
import com.odom.lottogogo.ui.components.GameRow
import com.odom.lottogogo.ui.components.RankBadge
import com.odom.lottogogo.ui.components.gameLabel

/** 스캔 결과 본문. BottomSheet(Compact)와 Dialog(Medium 이상)에서 함께 쓴다. */
@Composable
fun ScanResultContent(
    result: ScanResult,
    isSaved: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.round_title, result.round), style = MaterialTheme.typography.headlineSmall)
        val draw = result.draw
        if (draw == null) {
            Text(stringResource(R.string.scan_not_drawn), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.my_tickets_winning_numbers), style = MaterialTheme.typography.labelLarge)
            DrawNumbers(draw)
        }
        result.games.forEachIndexed { index, game ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GameRow(label = gameLabel(index), numbers = game.numbers, ballSize = 32.dp)
                RankBadge(game.rank)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_close))
            }
            Button(onClick = onSave, enabled = !isSaved, modifier = Modifier.weight(1f)) {
                Text(stringResource(if (isSaved) R.string.generate_saved else R.string.action_save_to_tickets))
            }
        }
    }
}
