package com.gondroid.quoteanime.domain.model

enum class NotificationFrequency(val intervalHours: Long, val label: String) {
    DAILY(24, "Diaria"),
    TWICE_DAILY(12, "2× día"),
    WEEKLY(168, "Semanal")
}
