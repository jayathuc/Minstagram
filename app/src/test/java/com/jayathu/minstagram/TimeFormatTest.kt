package com.jayathu.minstagram

import com.jayathu.minstagram.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `seconds only`() {
        assertEquals("0s", formatDuration(0))
        assertEquals("59s", formatDuration(59))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("1m 0s", formatDuration(60))
        assertEquals("2m 5s", formatDuration(125))
        assertEquals("15m 0s", formatDuration(900))
    }
}
