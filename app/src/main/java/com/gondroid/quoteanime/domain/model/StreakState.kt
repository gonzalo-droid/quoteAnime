package com.gondroid.quoteanime.domain.model

import java.time.LocalDate

/**
 * Always derived from the completion dates — never persisted, so it cannot drift
 * when the device time zone changes or when past days are marked retroactively.
 */
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val lastCompletedDate: LocalDate? = null,
    val completedToday: Boolean = false
)
