package com.odom.lottogogo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.odom.lottogogo.R
import com.odom.lottogogo.ui.theme.BallBlue
import com.odom.lottogogo.ui.theme.BallGray
import com.odom.lottogogo.ui.theme.BallGreen
import com.odom.lottogogo.ui.theme.BallRed
import com.odom.lottogogo.ui.theme.BallText
import com.odom.lottogogo.ui.theme.BallYellow
import com.odom.lottogogo.ui.theme.Dimens
import com.odom.lottogogo.ui.theme.LottoGogoTheme

fun ballColor(number: Int): Color = when (number) {
    in 1..10 -> BallYellow
    in 11..20 -> BallBlue
    in 21..30 -> BallRed
    in 31..40 -> BallGray
    else -> BallGreen
}

/** 번호 공. 색상과 함께 숫자를 항상 표시해 색만으로 정보를 구분하지 않는다. */
@Composable
fun LottoBall(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.BallSize,
    isBonus: Boolean = false,
) {
    val description = if (isBonus) {
        stringResource(R.string.ball_bonus_description, number)
    } else {
        stringResource(R.string.ball_description, number)
    }
    Box(
        modifier = modifier
            .size(size)
            .background(ballColor(number), CircleShape)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = BallText,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
private fun LottoBallPreview() {
    LottoGogoTheme {
        LottoBall(number = 7)
    }
}
