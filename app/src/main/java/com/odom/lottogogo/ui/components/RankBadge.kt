package com.odom.lottogogo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odom.lottogogo.R
import com.odom.lottogogo.domain.RankCalculator

/** 등수 배지. rank: null = 추첨 전, 0 = 낙첨, 1~5 = 등수. 색과 함께 항상 텍스트로 표시한다. */
@Composable
fun RankBadge(rank: Int?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (text, container, content) = when (rank) {
        null -> Triple(stringResource(R.string.rank_pending), colors.tertiaryContainer, colors.onTertiaryContainer)
        RankCalculator.NO_PRIZE -> Triple(stringResource(R.string.rank_none), colors.surfaceVariant, colors.onSurfaceVariant)
        else -> Triple(stringResource(R.string.rank_n, rank), colors.primary, colors.onPrimary)
    }
    Badge(text, container, content, modifier)
}

@Composable
private fun Badge(text: String, container: Color, content: Color, modifier: Modifier) {
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
