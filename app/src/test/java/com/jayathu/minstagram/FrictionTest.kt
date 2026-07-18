package com.jayathu.minstagram

import com.jayathu.minstagram.domain.unlockDelaySeconds
import org.junit.Assert.assertEquals
import org.junit.Test

class FrictionTest {

    @Test
    fun `first session uses the base delay`() {
        assertEquals(5, unlockDelaySeconds(5, 0))
        assertEquals(3, unlockDelaySeconds(3, 0))
    }

    @Test
    fun `delay grows two seconds per session today`() {
        assertEquals(7, unlockDelaySeconds(5, 1))
        assertEquals(11, unlockDelaySeconds(5, 3))
    }

    @Test
    fun `delay caps at fifteen seconds`() {
        assertEquals(15, unlockDelaySeconds(5, 5))
        assertEquals(15, unlockDelaySeconds(10, 4))
        assertEquals(15, unlockDelaySeconds(15, 0))
    }
}
