package com.odom.lottogogo.ui.home

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odom.lottogogo.R
import com.odom.lottogogo.ui.components.PlaceholderContent

// Step 5에서 HomeViewModel + HomeContent로 교체
@Composable
fun HomeScreen(onGenerateClick: () -> Unit) {
    PlaceholderContent(title = stringResource(R.string.nav_home)) {
        Button(onClick = onGenerateClick) {
            Text(stringResource(R.string.action_open_generate))
        }
    }
}
