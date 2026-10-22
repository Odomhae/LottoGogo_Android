package com.odom.lottogogo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

/** Compose UI 테스트 호스트. 잠금 화면 위 표시 설정은 debug AndroidManifest에 있다. */
@AndroidEntryPoint
class ComposeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // MainActivity와 같은 edge-to-edge 환경에서 시스템 바 여백을 검증한다
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
    }
}
