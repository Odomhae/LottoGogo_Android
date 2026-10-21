package com.odom.lottogogo.ui

import androidx.window.core.layout.WindowSizeClass

/** 폭 600dp 미만 (폰 세로) */
val WindowSizeClass.isCompactWidth: Boolean
    get() = !isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/** 폭 840dp 이상 (태블릿) */
val WindowSizeClass.isExpandedWidth: Boolean
    get() = isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
