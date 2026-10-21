package com.odom.lottogogo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.odom.lottogogo.R

/**
 * 회차 선택: 이전/다음 버튼 + 회차 목록 드롭다운(다이얼로그).
 * 회차가 1,000개 이상이라 DropdownMenu(비지연 Column) 대신 LazyColumn 다이얼로그를 쓴다.
 */
@Composable
fun RoundSelector(
    state: HomeUiState,
    onSelectRound: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val draw = state.draw ?: return
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = { state.previousRound?.let(onSelectRound) },
            enabled = state.previousRound != null,
        ) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.action_previous_round))
        }
        OutlinedButton(onClick = { showPicker = true }) {
            Text(stringResource(R.string.round_title, draw.round))
            Icon(painterResource(R.drawable.ic_arrow_drop_down), stringResource(R.string.action_select_round))
        }
        IconButton(
            onClick = { state.nextRound?.let(onSelectRound) },
            enabled = state.nextRound != null,
        ) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.action_next_round))
        }
    }

    if (showPicker) {
        RoundPickerDialog(
            rounds = state.rounds,
            selectedRound = draw.round,
            onSelect = {
                onSelectRound(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun RoundPickerDialog(
    rounds: List<Int>,
    selectedRound: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = rounds.indexOf(selectedRound).coerceAtLeast(0),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_select_round)) },
        text = {
            LazyColumn(state = listState, modifier = Modifier.heightIn(max = 400.dp)) {
                items(rounds, key = { it }) { round ->
                    RoundListItem(round, selected = round == selectedRound, onClick = { onSelect(round) })
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun RoundListItem(
    round: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.round_title, round)) },
        modifier = modifier.selectable(selected = selected, role = Role.Button, onClick = onClick),
        colors = if (selected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        tonalElevation = 0.dp,
    )
}
