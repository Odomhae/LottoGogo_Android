package com.odom.lottogogo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.odom.lottogogo.R
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import java.time.LocalDate

@Composable
fun DrawCard(draw: Draw, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.round_title, draw.round),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.draw_date, Formatters.date(draw.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DrawNumbers(draw)
            HorizontalDivider()
            FirstPrizeInfo(draw)
        }
    }
}

@Composable
fun DrawNumbers(draw: Draw, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        draw.numbers.forEach { LottoBall(number = it) }
        Text(text = "+", style = MaterialTheme.typography.titleLarge)
        LottoBall(number = draw.bonus, isBonus = true)
    }
}

@Composable
private fun FirstPrizeInfo(draw: Draw) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (draw.firstWinners > 0) {
            Text(
                text = stringResource(R.string.first_prize_amount, Formatters.amount(draw.firstPrize)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.first_prize_winners, draw.firstWinners),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.first_prize_none),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

internal val SampleDraw = Draw(
    round = 1195,
    date = LocalDate.of(2025, 10, 25),
    n1 = 3, n2 = 15, n3 = 27, n4 = 33, n5 = 34, n6 = 36,
    bonus = 37,
    firstPrize = 2_543_256_870L,
    firstWinners = 11,
)

@Preview
@Composable
private fun DrawCardPreview() {
    LottoGogoTheme {
        DrawCard(SampleDraw)
    }
}
