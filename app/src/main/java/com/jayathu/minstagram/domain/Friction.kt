package com.jayathu.minstagram.domain

// The pause grows with each session today, so the fourth doomscroll
// attempt waits longer than the first.
fun unlockDelaySeconds(baseSeconds: Int, sessionsToday: Int): Int =
    (baseSeconds + 2 * sessionsToday).coerceAtMost(15)
