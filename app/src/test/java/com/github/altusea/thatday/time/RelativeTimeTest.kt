package com.github.altusea.thatday.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RelativeTimeTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `an event earlier today is still today`() {
        val now = at(2026, 11, 19, 14, 30)
        val event = at(2026, 11, 19, 8, 0)
        assertEquals(Relative.Today, RelativeTime.relative(event, now, zone))
    }

    @Test
    fun `an event later today is still today`() {
        val now = at(2026, 11, 19, 8, 0)
        val event = at(2026, 11, 19, 23, 59)
        assertEquals(Relative.Today, RelativeTime.relative(event, now, zone))
    }

    @Test
    fun `three days ahead is a future day bucket`() {
        val now = at(2026, 11, 19, 14, 30)
        val event = at(2026, 11, 22, 9, 0)
        assertEquals(
            Relative.Away(amount = 3, unit = RelativeUnit.DAY, future = true),
            RelativeTime.relative(event, now, zone),
        )
    }

    @Test
    fun `three days ago is a past day bucket`() {
        val now = at(2026, 11, 19, 14, 30)
        val event = at(2026, 11, 16, 9, 0)
        assertEquals(
            Relative.Away(amount = 3, unit = RelativeUnit.DAY, future = false),
            RelativeTime.relative(event, now, zone),
        )
    }

    @Test
    fun `fourteen days is expressed in weeks`() {
        val now = at(2026, 11, 19)
        val event = at(2026, 12, 3)
        assertEquals(
            Relative.Away(amount = 2, unit = RelativeUnit.WEEK, future = true),
            RelativeTime.relative(event, now, zone),
        )
    }

    @Test
    fun `two years is expressed in years`() {
        val now = at(2026, 11, 19)
        val event = at(2028, 11, 19)
        assertEquals(
            Relative.Away(amount = 2, unit = RelativeUnit.YEAR, future = true),
            RelativeTime.relative(event, now, zone),
        )
    }

    @Test
    fun `groups sort by absolute distance with today first`() {
        val now = at(2026, 11, 19, 14, 30)
        val today = RelativeTime.relative(at(2026, 11, 19, 10, 0), now, zone)
        val inThreeDays = RelativeTime.relative(at(2026, 11, 22), now, zone)
        assertEquals(0L, today.approxDays)
        assertEquals(3L, inThreeDays.approxDays)
        assertEquals(true, (inThreeDays as Relative.Away).future)
    }
}
