package com.odom.lottogogo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/** 0.0(최소) ~ 1.0(최대) 색 농도. 최대값이 0이면 0 */
fun heatLevel(count: Int, maxCount: Int): Float =
    if (maxCount <= 0) 0f else (count.toFloat() / maxCount).coerceIn(0f, 1f)

fun numberCellTag(number: Int) = "number_cell_$number"

/** 번호 그리드. 셀 배경 농도 + 숫자/횟수 텍스트로 출현 횟수를 표시한다. */
@Composable
fun NumberGrid(
    counts: List<NumberCount>,
    maxCount: Int,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        counts.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { NumberCell(it, maxCount, Modifier.weight(1f)) }
                // 마지막 행이 덜 찼을 때 셀 크기 유지
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NumberCell(count: NumberCount, maxCount: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background = lerp(colors.surfaceContainerHighest, colors.primary, heatLevel(count.cnt, maxCount) * 0.85f)
    val content = lerp(colors.onSurface, colors.onPrimary, if (heatLevel(count.cnt, maxCount) > 0.5f) 1f else 0f)
    val description = stringResource(R.string.stats_cell_description, count.num, count.cnt)

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(background, RoundedCornerShape(8.dp))
            .testTag(numberCellTag(count.num))
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.num.toString(), color = content, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(count.cnt.toString(), color = content, fontSize = 10.sp)
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
