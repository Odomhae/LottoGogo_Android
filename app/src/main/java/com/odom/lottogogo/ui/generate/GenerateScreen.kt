package com.odom.lottogogo.ui.generate

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odom.lottogogo.R
import com.odom.lottogogo.ui.components.PlaceholderContent

// Step 7에서 구현
@Composable
fun GenerateScreen(onBack: () -> Unit) {
    PlaceholderContent(title = stringResource(R.string.generate_title)) {
        OutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.action_back))
        }
    }
}
