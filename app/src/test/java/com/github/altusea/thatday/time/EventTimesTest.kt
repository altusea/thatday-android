package com.github.altusea.thatday.time

import com.github.altusea.thatday.data.TimeKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class EventTimesTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `all-day events are normalised to local start of day`() {
        val date = LocalDate.of(2026, 11, 19)
        val expected = LocalDateTime.of(2026, 11, 19, 0, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, EventTimes.combine(date, TimeKind.ALL_DAY, 14, 30, zone))
    }

    @Test
    fun `timed events keep the wall-clock time`() {
        val date = LocalDate.of(2026, 11, 19)
        val expected = LocalDateTime.of(2026, 11, 19, 14, 30).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, EventTimes.combine(date, TimeKind.TIMED, 14, 30, zone))
    }

    @Test
    fun `date round-trips through the picker representation`() {
        val date = LocalDate.of(2026, 11, 19)
        assertEquals(date, EventTimes.pickerMillisToLocalDate(EventTimes.localDateToPickerMillis(date)))
    }

    @Test
    fun `date is recovered from stored millis`() {
        val millis = EventTimes.combine(LocalDate.of(2026, 11, 19), TimeKind.ALL_DAY, 0, 0, zone)
        assertEquals(LocalDate.of(2026, 11, 19), EventTimes.dateOf(millis, zone))
    }
}
