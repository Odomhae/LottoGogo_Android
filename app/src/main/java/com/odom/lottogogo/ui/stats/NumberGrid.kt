package com.odom.lottogogo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.lottogogo.R
import com.odom.lottogogo.data.local.dao.NumberCount
import com.odom.lottogogo.ui.components.LottoBall
import com.odom.lottogogo.ui.components.ballColor

/** 0 ~ 최대값 대비 비율 (막대 차트 길이). 최대값이 0이면 0 */
fun heatLevel(count: Int, maxCount: Int): Float =
    if (maxCount <= 0) 0f else (count.toFloat() / maxCount).coerceIn(0f, 1f)

/**
 * 최소 ~ 최대 사이 위치 (그리드 색 농도). 전체 회차처럼 횟수가 좁은 범위에 몰려 있어도 차이가 보이도록
 * 0이 아니라 최소 횟수를 기준으로 한다. 모든 값이 같으면 1(값이 있을 때) 또는 0.
 */
fun heatLevel(count: Int, minCount: Int, maxCount: Int): Float = when {
    maxCount <= 0 -> 0f
    maxCount == minCount -> 1f
    else -> ((count - minCount).toFloat() / (maxCount - minCount)).coerceIn(0f, 1f)
}

fun numberCellTag(number: Int) = "number_cell_$number"

/** 번호 그리드. 셀 배경 농도 + 숫자/횟수 텍스트로 출현 횟수를 표시한다. */
@Composable
fun NumberGrid(
    counts: List<NumberCount>,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    val minCount = counts.minOfOrNull { it.cnt } ?: 0
    val maxCount = counts.maxOfOrNull { it.cnt } ?: 0
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        counts.chunked(columns).forEach { row ->
            // 한 행의 셀 높이를 가장 큰 셀에 맞춘다 (글자 크기 확대 시에도 번호/횟수 두 줄 표시)
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { NumberCell(it, heatLevel(it.cnt, minCount, maxCount), Modifier.weight(1f).fillMaxHeight()) }
                // 마지막 행이 덜 찼을 때 셀 크기 유지
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NumberCell(count: NumberCount, heat: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background = lerp(colors.surfaceContainerHighest, colors.primary, heat * 0.85f)
    val content = if (heat > 0.5f) colors.onPrimary else colors.onSurface
    val description = stringResource(R.string.stats_cell_description, count.num, count.cnt)

    Column(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(background, RoundedCornerShape(8.dp))
            .testTag(numberCellTag(count.num))
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.num.toString(), color = content, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        Text(count.cnt.toString(), color = content, fontSize = 11.sp, maxLines = 1)
    }
}

/** Expanded 오른쪽 막대 차트 (번호 순) */
@Composable
fun NumberBarChart(
    counts: List<NumberCount>,
    maxCount: Int,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(counts, key = { it.num }) { count ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                LottoBall(number = count.num, size = 24.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(heatLevel(count.cnt, maxCount))
                            .background(ballColor(count.num), RoundedCornerShape(4.dp)),
                    )
                }
                Text(
                    text = count.cnt.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(32.dp),
                )
            }
        }
    }
}
