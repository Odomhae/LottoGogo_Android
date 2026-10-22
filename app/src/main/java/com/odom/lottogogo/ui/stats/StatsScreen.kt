package com.odom.lottogogo.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.R
import com.odom.lottogogo.data.local.dao.NumberCount
import com.odom.lottogogo.ui.isExpandedWidth
import com.odom.lottogogo.ui.theme.Dimens
import com.odom.lottogogo.ui.theme.LottoGogoTheme

private const val COMPACT_COLUMNS = 9   // 9 × 5
private const val EXPANDED_COLUMNS = 15 // 15 × 3

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(
        state = state,
        windowSizeClass = windowSizeClass,
        onFilterChange = viewModel::setFilter,
        onIncludeBonusChange = viewModel::setIncludeBonus,
    )
}

@Composable
fun StatsContent(
    state: StatsUiState,
    windowSizeClass: WindowSizeClass,
    onFilterChange: (RecentFilter) -> Unit,
    onIncludeBonusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (state.maxCount == 0) {
        Box(modifier.fillMaxSize().padding(Dimens.ScreenPadding), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    if (windowSizeClass.isExpandedWidth) {
        Row(
            modifier = modifier.fillMaxSize().padding(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier.weight(2f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NumberGrid(state.counts, columns = EXPANDED_COLUMNS)
                StatsFilters(state, onFilterChange, onIncludeBonusChange)
                ReferenceNotice()
            }
            NumberBarChart(state.counts, state.maxCount, modifier = Modifier.weight(1f))
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = Dimens.SinglePaneMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NumberGrid(state.counts, columns = COMPACT_COLUMNS)
                StatsFilters(state, onFilterChange, onIncludeBonusChange)
                ReferenceNotice()
            }
        }
    }
}

@Composable
private fun StatsFilters(
    state: StatsUiState,
    onFilterChange: (RecentFilter) -> Unit,
    onIncludeBonusChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RecentFilter.entries.forEachIndexed { index, filter ->
                SegmentedButton(
                    selected = state.filter == filter,
                    onClick = { onFilterChange(filter) },
                    shape = SegmentedButtonDefaults.itemShape(index, RecentFilter.entries.size),
                ) {
                    Text(filterLabel(filter), maxLines = 1)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = state.includeBonus,
                    role = Role.Switch,
                    onValueChange = onIncludeBonusChange,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.stats_include_bonus), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.includeBonus, onCheckedChange = null)
        }
    }
}

@Composable
private fun filterLabel(filter: RecentFilter): String = when (filter.lastN) {
    null -> stringResource(R.string.stats_filter_all)
    else -> stringResource(R.string.stats_filter_recent, filter.lastN)
}

@Composable
private fun ReferenceNotice() {
    Text(
        text = stringResource(R.string.stats_reference_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewScreenSizes
@Composable
private fun StatsContentPreview() {
    LottoGogoTheme {
        StatsContent(
            state = StatsUiState(
                isLoading = false,
                counts = (1..45).map { NumberCount(it, 120 + (it * 37) % 60) },
            ),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            onFilterChange = {},
            onIncludeBonusChange = {},
        )
    }
}
