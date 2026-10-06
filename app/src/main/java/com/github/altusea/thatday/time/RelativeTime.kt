package com.github.altusea.thatday.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** The calendar unit a relative distance is expressed in. */
enum class RelativeUnit { DAY, WEEK, MONTH, YEAR }

/**
 * How far an event is from "now", expressed in whole calendar units
 * (DESIGN.md §2.3). This is *derived* — nothing about it is stored.
 *
 * A [Relative] doubles as the grouping key and the sort key for the list, and
 * the UI turns it into a localised label via string resources.
 */
sealed interface Relative {

    /** Same local calendar day as now. */
    data object Today : Relative

    data class Away(
        val amount: Long,
        val unit: RelativeUnit,
        val future: Boolean,
    ) : Relative

    /** Rough ordering distance in days; only used to sort groups. */
    val approxDays: Long
        get() = when (this) {
            Today -> 0L
            is Away -> when (unit) {
                RelativeUnit.DAY -> amount
                RelativeUnit.WEEK -> amount * 7
                RelativeUnit.MONTH -> amount * 30
                RelativeUnit.YEAR -> amount * 365
            }
        }
}

object RelativeTime {

    /**
     * Classify [occurredAt] relative to [now]. Both are epoch millis; the
     * comparison is done on calendar days in [zone], so an event later today is
     * still "today".
     */
    fun relative(occurredAt: Long, now: Long, zone: ZoneId): Relative {
        val eventDate = EventTimes.dateOf(occurredAt, zone)
        val nowDate = EventTimes.dateOf(now, zone)
        val days = ChronoUnit.DAYS.between(nowDate, eventDate)
        if (days == 0L) return Relative.Today

        val future = days > 0
        val magnitude = abs(days)
        val (amount, unit) = when {
            magnitude < 7 -> magnitude to RelativeUnit.DAY
            magnitude < 30 -> (magnitude / 7) to RelativeUnit.WEEK
            magnitude < 365 -> (magnitude / 30) to RelativeUnit.MONTH
            else -> (magnitude / 365) to RelativeUnit.YEAR
        }
        return Relative.Away(amount = amount, unit = unit, future = future)
    }

    /** Locale-aware wall-clock label, e.g. `2026年11月19日` or `Nov 19, 2026 2:30 PM`. */
    fun absoluteLabel(
        occurredAt: Long,
        allDay: Boolean,
        zone: ZoneId,
        locale: Locale,
    ): String {
        val dateTime = Instant.ofEpochMilli(occurredAt).atZone(zone)
        val date = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(locale)
            .format(dateTime)
        if (allDay) return date
        val time = DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .format(dateTime)
        return "$date $time"
    }
}
