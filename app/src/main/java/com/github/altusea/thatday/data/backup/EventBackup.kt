package com.github.altusea.thatday.data.backup

import com.github.altusea.thatday.data.Event
import com.github.altusea.thatday.data.TimeKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Versioned, self-describing backup envelope (DESIGN.md §8 M5).
 *
 * Events are exported without their database id: ids are local and must be
 * reassigned on import.
 */
@Serializable
data class BackupFile(
    val app: String = EventBackupCodec.APP_ID,
    val version: Int = EventBackupCodec.FORMAT_VERSION,
    val exportedAt: Long,
    val events: List<EventDto>,
)

@Serializable
data class EventDto(
    val title: String,
    val occurredAt: Long,
    val timeKind: String,
    val note: String? = null,
    val remindAt: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Pure JSON codec — no Android or database dependencies, so it is unit-testable. */
object EventBackupCodec {

    const val APP_ID = "thatday"
    const val FORMAT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(events: List<Event>, exportedAt: Long): String {
        val file = BackupFile(
            exportedAt = exportedAt,
            events = events.map { it.toDto() },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** @throws BackupFormatException if [text] is not a usable thatday backup. */
    fun decode(text: String): List<Event> {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (t: Throwable) {
            throw BackupFormatException("Not a valid backup file", t)
        }
        if (file.app != APP_ID) {
            throw BackupFormatException("Not a thatday backup (app=${file.app})")
        }
        if (file.version > FORMAT_VERSION) {
            throw BackupFormatException("Backup version ${file.version} is newer than $FORMAT_VERSION")
        }
        return file.events.map { it.toEvent() }
    }

    private fun Event.toDto() = EventDto(
        title = title,
        occurredAt = occurredAt,
        timeKind = timeKind.name,
        note = note,
        remindAt = remindAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun EventDto.toEvent(): Event = Event(
        title = title,
        occurredAt = occurredAt,
        timeKind = runCatching { TimeKind.valueOf(timeKind) }.getOrDefault(TimeKind.ALL_DAY),
        note = note,
        remindAt = remindAt,
        createdAt = createdAt ?: 0L,
        updatedAt = updatedAt ?: 0L,
    )
}
