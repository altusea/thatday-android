package com.github.altusea.thatday.data

import com.github.altusea.thatday.data.backup.BackupMerge
import com.github.altusea.thatday.data.backup.ImportResult
import kotlinx.coroutines.flow.Flow

/**
 * Single write entry point for events (DESIGN.md §6).
 *
 * Reminder (re)scheduling will be wired in here in M3 so that no caller can
 * write an event without also keeping the alarm in sync.
 */
class EventRepository(
    private val dao: EventDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun observeEvents(): Flow<List<Event>> = dao.observeAll()

    fun observeEvent(id: Long): Flow<Event?> = dao.observeById(id)

    suspend fun getEvent(id: Long): Event? = dao.getById(id)

    suspend fun addEvent(
        title: String,
        occurredAt: Long,
        timeKind: TimeKind,
        note: String?,
        remindAt: Long?,
    ): Long {
        val timestamp = now()
        return dao.insert(
            Event(
                title = title,
                occurredAt = occurredAt,
                timeKind = timeKind,
                note = note,
                remindAt = remindAt,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    suspend fun updateEvent(
        id: Long,
        title: String,
        occurredAt: Long,
        timeKind: TimeKind,
        note: String?,
        remindAt: Long?,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                title = title,
                occurredAt = occurredAt,
                timeKind = timeKind,
                note = note,
                remindAt = remindAt,
                updatedAt = now(),
            ),
        )
    }

    suspend fun deleteEvent(id: Long) = dao.deleteById(id)

    /** All events, for backup export. */
    suspend fun allEvents(): List<Event> = dao.getAll()

    /** Merge restored events in without deleting or overwriting anything. */
    suspend fun importMissing(incoming: List<Event>): ImportResult {
        val merge = BackupMerge.splitNew(existing = dao.getAll(), incoming = incoming)
        if (merge.newEvents.isNotEmpty()) {
            val timestamp = now()
            val toInsert = merge.newEvents.map { event ->
                event.copy(
                    id = 0,
                    createdAt = event.createdAt.takeIf { it > 0 } ?: timestamp,
                    updatedAt = event.updatedAt.takeIf { it > 0 } ?: timestamp,
                )
            }
            dao.insertAll(toInsert)
        }
        return ImportResult(imported = merge.newEvents.size, skipped = merge.skipped)
    }

    suspend fun eventsWithFutureReminder(): List<Event> = dao.getWithFutureReminder(now())
}
