package com.example.cas

import com.example.cas.ui.pinInside
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Axis labels are pinned inside the canvas. While the keypad opens or closes the canvas can be
 * a few pixels tall, so the maximum falls below the minimum; coerceIn throws on that, which
 * crashed the 2D and complex graphs when Enter collapsed the keypad.
 */
class PinInsideTest {
    @Test fun ordinaryRange() = assertEquals(50f, pinInside(50f, 2f, 100f))
    @Test fun aboveTheTop() = assertEquals(100f, pinInside(300f, 2f, 100f))
    @Test fun belowTheBottom() = assertEquals(2f, pinInside(-20f, 2f, 100f))
    @Test fun emptyRangeFallsBackToTheMinimum() = assertEquals(5.25f, pinInside(10f, 5.25f, -42f))
    @Test fun notANumber() = assertEquals(2f, pinInside(Float.NaN, 2f, 100f))
    @Test fun infinite() = assertEquals(2f, pinInside(Float.POSITIVE_INFINITY, 2f, 100f))
    @Test fun integerEmptyRange() = assertEquals(8, pinInside(40, 8, -30))
    @Test fun integerOrdinary() = assertEquals(40, pinInside(40, 8, 100))
}
