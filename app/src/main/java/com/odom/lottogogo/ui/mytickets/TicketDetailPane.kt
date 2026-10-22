package com.odom.lottogogo.ui.mytickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.odom.lottogogo.R
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.ui.components.DrawNumbers
import com.odom.lottogogo.ui.components.LottoBall
import com.odom.lottogogo.ui.components.NumberPickerDialog
import com.odom.lottogogo.ui.components.RankBadge
import com.odom.lottogogo.ui.theme.Dimens

private const val UNMATCHED_ALPHA = 0.3f

@Composable
fun TicketDetailPane(
    detail: TicketDetail?,
    showBack: Boolean,
    onBack: () -> Unit,
    onEdit: (MyTicket, Int, List<Int>) -> Unit,
    onDelete: (MyTicket) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.SinglePaneMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                }
            }
            if (detail == null) {
                Text(stringResource(R.string.my_tickets_select_hint), style = MaterialTheme.typography.bodyLarge)
            } else {
                TicketDetailCard(detail, onEdit, onDelete)
            }
        }
    }
}

@Composable
private fun TicketDetailCard(
    detail: TicketDetail,
    onEdit: (MyTicket, Int, List<Int>) -> Unit,
    onDelete: (MyTicket) -> Unit,
) {
    val ticket = detail.ticket
    val draw = detail.draw
    val numbers = ticket.numberList
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.round_title, ticket.round), style = MaterialTheme.typography.headlineSmall)
                RankBadge(ticket.rank)
            }
            Text(stringResource(sourceLabel(ticket.source)), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                numbers.forEach { n ->
                    val matched = draw == null || n in draw.numbers || n == draw.bonus
                    LottoBall(number = n, modifier = Modifier.alpha(if (matched) 1f else UNMATCHED_ALPHA))
                }
            }
            if (draw == null) {
                Text(stringResource(R.string.my_tickets_not_drawn), style = MaterialTheme.typography.bodyMedium)
            } else {
                val matchCount = numbers.count { it in draw.numbers }
                val bonusMatched = draw.bonus in numbers
                Text(
                    text = if (bonusMatched) {
                        stringResource(R.string.my_tickets_match_with_bonus, matchCount)
                    } else {
                        stringResource(R.string.my_tickets_match, matchCount)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(stringResource(R.string.my_tickets_winning_numbers), style = MaterialTheme.typography.labelLarge)
                DrawNumbers(draw)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showEdit = true }) { Text(stringResource(R.string.action_edit)) }
                OutlinedButton(onClick = { showDeleteConfirm = true }) { Text(stringResource(R.string.action_delete)) }
            }
        }
    }

    if (showEdit) {
        NumberPickerDialog(
            title = stringResource(R.string.action_edit),
            initialRound = ticket.round,
            initialNumbers = numbers,
            onConfirm = { round, newNumbers ->
                onEdit(ticket, round, newNumbers)
                showEdit = false
            },
            onDismiss = { showEdit = false },
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            text = { Text(stringResource(R.string.my_tickets_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(ticket)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
