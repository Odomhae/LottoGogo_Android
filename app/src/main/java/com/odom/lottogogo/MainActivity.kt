package com.odom.lottogogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.odom.lottogogo.ui.LottoAppScaffold
import com.odom.lottogogo.ui.theme.LottoGogoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LottoGogoTheme {
                LottoAppScaffold()
            }
        }
    }
}
