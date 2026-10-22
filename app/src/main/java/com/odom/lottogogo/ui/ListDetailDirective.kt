package com.odom.lottogogo.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/** Compact: 단일 패널, Medium 이상: 목록 + 상세 2단 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun listDetailDirective(windowSizeClass: WindowSizeClass): PaneScaffoldDirective =
    if (windowSizeClass.isCompactWidth) {
        PaneScaffoldDirective.Default
    } else {
        PaneScaffoldDirective.Default.copy(
            maxHorizontalPartitions = 2,
            horizontalPartitionSpacerSize = 24.dp,
            defaultPanePreferredWidth = 320.dp,
        )
    }
