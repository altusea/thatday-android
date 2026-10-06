package com.github.altusea.thatday.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The one and only entity of the app (DESIGN.md §5).
 *
 * [occurredAt] is epoch millis. For [TimeKind.ALL_DAY] it is normalised to the
 * local start of the selected day; the time part is ignored when displaying.
 */
@Entity(
    tableName = "events",
    indices = [Index("occurredAt")],
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val occurredAt: Long,
    val timeKind: TimeKind,
    val note: String? = null,
    /** Absolute epoch millis of the reminder; `null` means "do not remind". */
    val remindAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
