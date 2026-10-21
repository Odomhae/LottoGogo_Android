package com.odom.lottogogo.ui.components

import com.odom.lottogogo.ui.theme.BallBlue
import com.odom.lottogogo.ui.theme.BallGray
import com.odom.lottogogo.ui.theme.BallGreen
import com.odom.lottogogo.ui.theme.BallRed
import com.odom.lottogogo.ui.theme.BallYellow
import org.junit.Assert.assertEquals
import org.junit.Test

class BallColorTest {
    @Test
    fun colorBySection() {
        assertEquals(BallYellow, ballColor(1))
        assertEquals(BallYellow, ballColor(10))
        assertEquals(BallBlue, ballColor(11))
        assertEquals(BallBlue, ballColor(20))
        assertEquals(BallRed, ballColor(21))
        assertEquals(BallRed, ballColor(30))
        assertEquals(BallGray, ballColor(31))
        assertEquals(BallGray, ballColor(40))
        assertEquals(BallGreen, ballColor(41))
        assertEquals(BallGreen, ballColor(45))
    }
}
