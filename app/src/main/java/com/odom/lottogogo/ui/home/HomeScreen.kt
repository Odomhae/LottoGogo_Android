package com.odom.lottogogo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.R
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.ui.components.DrawCard
import com.odom.lottogogo.ui.components.SampleDraw
import com.odom.lottogogo.ui.isCompactWidth
import com.odom.lottogogo.ui.theme.Dimens
import com.odom.lottogogo.ui.theme.LottoGogoTheme

@Composable
fun HomeScreen(
    onGenerateClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        windowSizeClass = windowSizeClass,
        onSelectRound = viewModel::selectRound,
        onGenerateClick = onGenerateClick,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    windowSizeClass: WindowSizeClass,
    onSelectRound: (Int) -> Unit,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredBox(modifier) { CircularProgressIndicator() }
        state.isEmpty -> CenteredBox(modifier) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onGenerateClick) { Text(stringResource(R.string.action_open_generate)) }
            }
        }
        windowSizeClass.isCompactWidth -> HomeSinglePane(state, onSelectRound, onGenerateClick, modifier)
        else -> HomeListDetail(state, onSelectRound, onGenerateClick, modifier)
    }
}

@Composable
private fun HomeSinglePane(
    state: HomeUiState,
    onSelectRound: (Int) -> Unit,
    onGenerateClick: () -> Unit,
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
            SyncStatusLine(state.syncStatus)
            RoundSelector(state = state, onSelectRound = onSelectRound)
            DrawDetail(draw = requireNotNull(state.draw), onGenerateClick = onGenerateClick)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun HomeListDetail(
    state: HomeUiState,
    onSelectRound: (Int) -> Unit,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Medium 이상에서는 항상 목록 + 상세 두 패널을 함께 표시한다
    val directive = PaneScaffoldDirective.Default.copy(
        maxHorizontalPartitions = 2,
        horizontalPartitionSpacerSize = 24.dp,
        defaultPanePreferredWidth = 280.dp,
    )
    val value = calculateThreePaneScaffoldValue(
        maxHorizontalPartitions = directive.maxHorizontalPartitions,
        adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
        currentDestination = ThreePaneScaffoldDestinationItem<Nothing>(ListDetailPaneScaffoldRole.Detail),
    )
    val draw = requireNotNull(state.draw)

    ListDetailPaneScaffold(
        directive = directive,
        value = value,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                LazyColumn {
                    items(state.rounds, key = { it }) { round ->
                        RoundListItem(round, selected = round == draw.round, onClick = { onSelectRound(round) })
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = Dimens.SinglePaneMaxWidth)
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SyncStatusLine(state.syncStatus)
                        DrawDetail(draw = draw, onGenerateClick = onGenerateClick)
                    }
                }
            }
        },
    )
}

@Composable
private fun DrawDetail(draw: Draw, onGenerateClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DrawCard(draw = draw)
        Button(onClick = onGenerateClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_open_generate))
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize().padding(Dimens.ScreenPadding), contentAlignment = Alignment.Center) {
        content()
    }
}

@PreviewScreenSizes
@Composable
private fun HomeContentPreview() {
    LottoGogoTheme {
        HomeContent(
            state = HomeUiState(isLoading = false, rounds = (1195 downTo 1).toList(), draw = SampleDraw),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            onSelectRound = {},
            onGenerateClick = {},
        )
    }
}
