package com.github.altusea.thatday.time

import com.github.altusea.thatday.data.TimeKind
import java.time.Instant
import java.time.ZoneId

/**
 * Pure reminder-time computation (DESIGN.md §4.2).
 *
 * - timed event  -> one hour before it happens
 * - all-day      -> 09:00 on the event day
 * - then minus [aheadDays] whole days
 */
object RemindTime {

    fun compute(
        occurredAt: Long,
        timeKind: TimeKind,
        aheadDays: Int,
        zone: ZoneId,
    ): Long {
        val base = when (timeKind) {
            TimeKind.TIMED -> occurredAt - ONE_HOUR_MILLIS
            TimeKind.ALL_DAY -> Instant.ofEpochMilli(occurredAt)
                .atZone(zone)
                .toLocalDate()
                .atTime(ALL_DAY_REMIND_HOUR, 0)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }
        if (aheadDays <= 0) return base
        return Instant.ofEpochMilli(base)
            .atZone(zone)
            .minusDays(aheadDays.toLong())
            .toInstant()
            .toEpochMilli()
    }

    private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
    private const val ALL_DAY_REMIND_HOUR = 9
}
