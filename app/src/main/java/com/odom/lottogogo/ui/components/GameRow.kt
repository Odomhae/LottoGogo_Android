package com.odom.lottogogo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** "A  ● ● ● ● ● ●" 형태의 게임 한 줄 */
@Composable
fun GameRow(
    label: String,
    numbers: List<Int>,
    modifier: Modifier = Modifier,
    ballSize: Dp = 36.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(20.dp))
        numbers.forEach { LottoBall(number = it, size = ballSize) }
    }
}

/** 게임 순번 0 → "A" */
fun gameLabel(index: Int): String = ('A' + index).toString()
