package com.github.altusea.thatday.data.backup

import com.github.altusea.thatday.data.Event

/** Outcome of importing a backup. */
data class ImportResult(
    val imported: Int,
    val skipped: Int,
)

/**
 * Restore policy: never destructive. Events already present are skipped, so
 * importing the same backup twice is idempotent and importing onto a populated
 * device cannot duplicate or overwrite anything.
 *
 * Two events are considered the same when title, time and note all match.
 */
object BackupMerge {

    data class Split(
        val newEvents: List<Event>,
        val skipped: Int,
    )

    fun splitNew(existing: List<Event>, incoming: List<Event>): Split {
        val seen = existing.mapTo(HashSet()) { it.identity() }
        val newEvents = ArrayList<Event>()
        var skipped = 0
        for (event in incoming) {
            if (seen.add(event.identity())) {
                newEvents += event
            } else {
                skipped++
            }
        }
        return Split(newEvents = newEvents, skipped = skipped)
    }

    private fun Event.identity(): String =
        listOf(title, occurredAt.toString(), timeKind.name, note.orEmpty()).joinToString("\u0000")
}
