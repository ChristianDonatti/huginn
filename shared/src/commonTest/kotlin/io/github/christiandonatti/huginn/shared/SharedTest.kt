package io.github.christiandonatti.huginn.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedTest {
    @Test
    fun minutesReadNaturally() {
        assertEquals("40 min", formatMinutes(40))
        assertEquals("1 h 35", formatMinutes(95))
        assertEquals("2 h", formatMinutes(120))
        assertEquals("1 h 05", formatMinutes(65))
        assertEquals("0 min", formatMinutes(-3))
    }

    @Test
    fun batteryLastsLongerOnLightSystems() {
        // 82% at 18%/h (GBA) is about 4 h 35; at 52%/h (GameCube) about 1 h 35.
        assertEquals(275, batteryMinutes(82, 18.0))
        assertEquals(95, batteryMinutes(82, 52.0))
        assertEquals(0, batteryMinutes(82, 0.0))
    }
}
