package com.odom.lottogogo.ui.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.odom.lottogogo.R
import com.odom.lottogogo.domain.LottoRules
import com.odom.lottogogo.ui.components.GameRow
import com.odom.lottogogo.ui.components.gameLabel
import com.odom.lottogogo.ui.isCompactWidth
import com.odom.lottogogo.ui.theme.Dimens
import com.odom.lottogogo.ui.theme.LottoGogoTheme

@Composable
fun GenerateScreen(
    onBack: () -> Unit,
    viewModel: GenerateViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GenerateContent(
        state = state,
        windowSizeClass = windowSizeClass,
        onBack = onBack,
        onGameCountChange = viewModel::setGameCount,
        onGenerate = viewModel::generate,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateContent(
    state: GenerateUiState,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit,
    onGameCountChange: (Int) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generate_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (windowSizeClass.isCompactWidth) Dimens.SinglePaneMaxWidth else 960.dp)
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GenerateControls(state, onGameCountChange, onGenerate, onSave)
                GeneratedGames(
                    games = state.games,
                    columns = if (windowSizeClass.isCompactWidth) 1 else 2,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

@Composable
private fun GenerateControls(
    state: GenerateUiState,
    onGameCountChange: (Int) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.generate_target_round, state.targetRound),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(R.string.generate_game_count), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            (1..LottoRules.MAX_GAMES).forEach { count ->
                SegmentedButton(
                    selected = state.gameCount == count,
                    onClick = { onGameCountChange(count) },
                    shape = SegmentedButtonDefaults.itemShape(count - 1, LottoRules.MAX_GAMES),
                ) {
                    Text(stringResource(R.string.generate_games_label, count))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onGenerate, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_generate))
            }
            OutlinedButton(onClick = onSave, enabled = state.canSave, modifier = Modifier.weight(1f)) {
                Text(stringResource(if (state.isSaved) R.string.generate_saved else R.string.action_save_to_tickets))
            }
        }
    }
}

@Composable
private fun GeneratedGames(games: List<List<Int>>, columns: Int, modifier: Modifier = Modifier) {
    if (games.isEmpty()) {
        Text(
            text = stringResource(R.string.generate_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(games) { index, game ->
            Card {
                GameRow(label = gameLabel(index), numbers = game, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun GenerateContentPreview() {
    LottoGogoTheme {
        GenerateContent(
            state = GenerateUiState(
                gameCount = 3,
                games = listOf(listOf(3, 11, 17, 24, 38, 42), listOf(1, 9, 22, 30, 33, 45), listOf(5, 6, 19, 28, 36, 41)),
                targetRound = 1247,
            ),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            onBack = {},
            onGameCountChange = {},
            onGenerate = {},
            onSave = {},
        )
    }
}
