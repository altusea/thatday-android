package com.github.altusea.thatday.data

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

    suspend fun eventsWithFutureReminder(): List<Event> = dao.getWithFutureReminder(now())
}
