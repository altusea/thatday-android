package com.github.altusea.thatday.time

import com.github.altusea.thatday.data.TimeKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RemindTimeTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `timed event reminds one hour before`() {
        val occurredAt = millisAt(2026, 11, 19, 14, 30)
        val expected = millisAt(2026, 11, 19, 13, 30)
        assertEquals(expected, RemindTime.compute(occurredAt, TimeKind.TIMED, aheadDays = 0, zone = zone))
    }

    @Test
    fun `all-day event reminds at nine in the morning`() {
        val occurredAt = millisAt(2026, 11, 19, 0, 0)
        val expected = millisAt(2026, 11, 19, 9, 0)
        assertEquals(expected, RemindTime.compute(occurredAt, TimeKind.ALL_DAY, aheadDays = 0, zone = zone))
    }

    @Test
    fun `ahead days shifts the reminder back whole days`() {
        val occurredAt = millisAt(2026, 11, 19, 14, 30)
        val expected = millisAt(2026, 11, 17, 13, 30)
        assertEquals(expected, RemindTime.compute(occurredAt, TimeKind.TIMED, aheadDays = 2, zone = zone))
    }

    @Test
    fun `all-day event is built from its local date`() {
        val occurredAt = EventTimes.combine(LocalDate.of(2026, 11, 19), TimeKind.ALL_DAY, 0, 0, zone)
        assertEquals(
            millisAt(2026, 11, 19, 9, 0),
            RemindTime.compute(occurredAt, TimeKind.ALL_DAY, aheadDays = 0, zone = zone),
        )
    }
}
