package com.odom.lottogogo.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.odom.lottogogo.R

/** 하단 바 / 내비게이션 레일에 표시되는 최상위 탭 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME("home", R.string.nav_home, R.drawable.ic_home),
    SCAN("scan", R.string.nav_scan, R.drawable.ic_qr_scan),
    STATS("stats", R.string.nav_stats, R.drawable.ic_bar_chart),
    MY_TICKETS("my_tickets", R.string.nav_my_tickets, R.drawable.ic_ticket),
}

/** 탭에 속하지 않는 하위 화면 */
object Routes {
    const val GENERATE = "generate"
}
