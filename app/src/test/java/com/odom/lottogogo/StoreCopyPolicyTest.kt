package com.odom.lottogogo

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 스토어 정책: 당첨 확률 상승/예측/적중 표현 금지, 통계 화면 참고용 문구 필수 */
class StoreCopyPolicyTest {

    private val strings = File("src/main/res/values/strings.xml").readText()

    @Test
    fun noForbiddenExpressions() {
        listOf("확률", "예측", "적중", "구매하기", "보장").forEach { word ->
            assertTrue("forbidden expression: $word", word !in strings)
        }
    }

    @Test
    fun statsReferenceNoticePresent() {
        assertTrue("과거 데이터 기반 참고용" in strings)
    }
}
