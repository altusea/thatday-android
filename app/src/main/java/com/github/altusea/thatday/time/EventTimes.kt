package com.github.altusea.thatday.time

import com.github.altusea.thatday.data.TimeKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Pure helpers for moving between the stored epoch millis and the calendar
 * concepts the UI works with. No Android dependencies, so they are unit-testable.
 */
object EventTimes {

    /** Local start of day for [date] in [zone], as epoch millis. */
    fun startOfDay(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** The local calendar date an all-day / timed event falls on. */
    fun dateOf(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun hourOf(epochMillis: Long, zone: ZoneId): Int =
        Instant.ofEpochMilli(epochMillis).atZone(zone).hour

    fun minuteOf(epochMillis: Long, zone: ZoneId): Int =
        Instant.ofEpochMilli(epochMillis).atZone(zone).minute

    /**
     * Build the stored value from a date and (for timed events) a wall-clock time.
     */
    fun combine(date: LocalDate, timeKind: TimeKind, hour: Int, minute: Int, zone: ZoneId): Long {
        val localTime = if (timeKind == TimeKind.TIMED) {
            date.atTime(hour, minute)
        } else {
            date.atStartOfDay()
        }
        return localTime.atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * `DatePicker` speaks in UTC-midnight millis, while the app stores
     * zone-local midnight. These two convert between them without dropping a day.
     */
    fun localDateToPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun pickerMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}
