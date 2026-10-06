package com.github.altusea.thatday.data.backup

import com.github.altusea.thatday.data.Event
import com.github.altusea.thatday.data.TimeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EventBackupCodecTest {

    private fun event(
        title: String,
        occurredAt: Long,
        kind: TimeKind = TimeKind.ALL_DAY,
        note: String? = null,
        remindAt: Long? = null,
    ) = Event(
        title = title,
        occurredAt = occurredAt,
        timeKind = kind,
        note = note,
        remindAt = remindAt,
        createdAt = 1_000L,
        updatedAt = 2_000L,
    )

    @Test
    fun `round trip preserves every field`() {
        val events = listOf(
            event("A", 100L, TimeKind.ALL_DAY, note = "n", remindAt = 50L),
            event("B", 200L, TimeKind.TIMED),
        )

        val decoded = EventBackupCodec.decode(EventBackupCodec.encode(events, exportedAt = 999L))

        assertEquals(2, decoded.size)
        assertEquals("A", decoded[0].title)
        assertEquals(100L, decoded[0].occurredAt)
        assertEquals(TimeKind.ALL_DAY, decoded[0].timeKind)
        assertEquals("n", decoded[0].note)
        assertEquals(50L, decoded[0].remindAt)
        assertEquals("B", decoded[1].title)
        assertEquals(TimeKind.TIMED, decoded[1].timeKind)
    }

    @Test
    fun `ids are not carried across a round trip`() {
        val decoded = EventBackupCodec.decode(
            EventBackupCodec.encode(listOf(event("A", 1L).copy(id = 42L)), exportedAt = 1L),
        )
        assertEquals(0L, decoded.single().id)
    }

    @Test
    fun `decoding a foreign file fails`() {
        val text = """{"app":"other","version":1,"exportedAt":1,"events":[]}"""
        assertBackupFormatException(text)
    }

    @Test
    fun `decoding a newer format version fails`() {
        val text = """{"app":"thatday","version":99,"exportedAt":1,"events":[]}"""
        assertBackupFormatException(text)
    }

    @Test
    fun `decoding malformed json fails`() {
        assertBackupFormatException("this is not json")
    }

    @Test
    fun `unknown keys are ignored for forward compatibility`() {
        val text = """{"app":"thatday","version":1,"exportedAt":1,"events":[],"somethingNew":true}"""
        assertEquals(0, EventBackupCodec.decode(text).size)
    }

    private fun assertBackupFormatException(text: String) {
        try {
            EventBackupCodec.decode(text)
            fail("Expected BackupFormatException")
        } catch (expected: BackupFormatException) {
            assertTrue(expected.message?.isNotBlank() == true)
        }
    }
}
