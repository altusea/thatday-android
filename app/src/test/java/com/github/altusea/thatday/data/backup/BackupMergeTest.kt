package com.github.altusea.thatday.data.backup

import com.github.altusea.thatday.data.Event
import com.github.altusea.thatday.data.TimeKind
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMergeTest {

    private fun event(title: String, occurredAt: Long, note: String? = null) = Event(
        title = title,
        occurredAt = occurredAt,
        timeKind = TimeKind.ALL_DAY,
        note = note,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `events already present are skipped`() {
        val existing = listOf(event("A", 100L))
        val incoming = listOf(event("A", 100L), event("B", 200L))

        val split = BackupMerge.splitNew(existing, incoming)

        assertEquals(listOf("B"), split.newEvents.map { it.title })
        assertEquals(1, split.skipped)
    }

    @Test
    fun `duplicates inside the backup itself are collapsed`() {
        val incoming = listOf(event("A", 100L), event("A", 100L), event("A", 100L))

        val split = BackupMerge.splitNew(emptyList(), incoming)

        assertEquals(1, split.newEvents.size)
        assertEquals(2, split.skipped)
    }

    @Test
    fun `same title at a different time is a different event`() {
        val split = BackupMerge.splitNew(
            existing = listOf(event("A", 100L)),
            incoming = listOf(event("A", 200L)),
        )
        assertEquals(1, split.newEvents.size)
        assertEquals(0, split.skipped)
    }

    @Test
    fun `note is part of the identity`() {
        val split = BackupMerge.splitNew(
            existing = listOf(event("A", 100L, note = "x")),
            incoming = listOf(event("A", 100L, note = "y")),
        )
        assertEquals(1, split.newEvents.size)
    }
}
