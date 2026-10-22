package com.odom.lottogogo.ui.mytickets

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.R
import com.odom.lottogogo.data.local.entity.MyTicket
import com.odom.lottogogo.data.local.entity.Source
import com.odom.lottogogo.ui.components.LottoBall
import com.odom.lottogogo.ui.components.NumberPickerDialog
import com.odom.lottogogo.ui.components.RankBadge
import com.odom.lottogogo.ui.listDetailDirective
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import kotlinx.coroutines.launch

@Composable
fun MyTicketsScreen(
    viewModel: MyTicketsViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyTicketsContent(
        state = state,
        windowSizeClass = windowSizeClass,
        onSelect = viewModel::select,
        onAdd = viewModel::addManual,
        onEdit = viewModel::edit,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MyTicketsContent(
    state: MyTicketsUiState,
    windowSizeClass: WindowSizeClass,
    onSelect: (Long?) -> Unit,
    onAdd: (Int, List<Int>) -> Unit,
    onEdit: (MyTicket, Int, List<Int>) -> Unit,
    onDelete: (MyTicket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>(
        scaffoldDirective = listDetailDirective(windowSizeClass),
    )
    val scope = rememberCoroutineScope()
    val goBack: () -> Unit = { scope.launch { navigator.navigateBack() } }

    // Compact 상세 → 목록. 2단에서는 되돌릴 화면 변화가 없어 목록을 닫지 않는다
    BackHandler(enabled = navigator.canNavigateBack()) { goBack() }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                TicketListPane(
                    state = state,
                    selectedId = state.selected?.ticket?.id,
                    onTicketClick = { ticket ->
                        onSelect(ticket.id)
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, ticket.id) }
                    },
                    onAdd = onAdd,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                TicketDetailPane(
                    detail = state.selected,
                    showBack = navigator.canNavigateBack(),
                    onBack = goBack,
                    onEdit = onEdit,
                    onDelete = { ticket ->
                        onDelete(ticket)
                        if (navigator.canNavigateBack()) goBack()
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TicketListPane(
    state: MyTicketsUiState,
    selectedId: Long?,
    onTicketClick: (MyTicket) -> Unit,
    onAdd: (Int, List<Int>) -> Unit,
) {
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (state.isEmpty) {
            Text(
                text = stringResource(R.string.my_tickets_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                state.groups.forEach { group ->
                    stickyHeader(key = "header_${group.round}") {
                        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.round_title, group.round),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    items(group.tickets, key = { it.id }) { ticket ->
                        TicketListItem(ticket, selected = ticket.id == selectedId, onClick = { onTicketClick(ticket) })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Text(stringResource(R.string.action_manual_input))
        }
    }

    if (showAdd) {
        NumberPickerDialog(
            title = stringResource(R.string.action_manual_input),
            initialRound = state.nextRound,
            initialNumbers = emptyList(),
            onConfirm = { round, numbers ->
                onAdd(round, numbers)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun TicketListItem(ticket: MyTicket, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ticket.numberList.forEach { LottoBall(number = it, size = 28.dp) }
            }
        },
        supportingContent = { Text(stringResource(sourceLabel(ticket.source))) },
        trailingContent = { RankBadge(ticket.rank) },
        colors = if (selected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
    )
}

@StringRes
fun sourceLabel(source: Source): Int = when (source) {
    Source.QR -> R.string.source_qr
    Source.GENERATED -> R.string.source_generated
    Source.MANUAL -> R.string.source_manual
}

@PreviewScreenSizes
@Composable
private fun MyTicketsContentPreview() {
    val tickets = listOf(
        MyTicket(1, 1247, "3,11,17,24,38,42", Source.GENERATED, 0L, null),
        MyTicket(2, 1246, "1,2,3,4,5,6", Source.QR, 0L, 5),
        MyTicket(3, 1246, "7,8,9,10,11,12", Source.MANUAL, 0L, 0),
    )
    LottoGogoTheme {
        MyTicketsContent(
            state = MyTicketsUiState(
                isLoading = false,
                groups = tickets.groupBy { it.round }.map { (r, t) -> TicketGroup(r, t) },
                nextRound = 1247,
            ),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            onSelect = {},
            onAdd = { _, _ -> },
            onEdit = { _, _, _ -> },
            onDelete = {},
        )
    }
}
