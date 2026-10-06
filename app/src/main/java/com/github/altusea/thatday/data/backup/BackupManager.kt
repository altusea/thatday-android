package com.github.altusea.thatday.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.github.altusea.thatday.data.EventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads and writes backup files through the Storage Access Framework, so the app
 * needs neither an `INTERNET` nor a storage permission (DESIGN.md §5 / §12.9).
 */
class BackupManager(
    private val repository: EventRepository,
    private val contentResolver: ContentResolver,
    private val now: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /** Writes every event to [uri]; returns how many were written. */
    suspend fun export(uri: Uri): Int = withContext(ioDispatcher) {
        val events = repository.allEvents()
        val text = EventBackupCodec.encode(events, now())
        val stream = contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open $uri for writing")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        events.size
    }

    /** Merges the events in [uri] into the database without deleting anything. */
    suspend fun import(uri: Uri): ImportResult = withContext(ioDispatcher) {
        val text = contentResolver.openInputStream(uri)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IOException("Cannot open $uri for reading")
        val incoming = EventBackupCodec.decode(text)
        repository.importMissing(incoming)
    }

    companion object {
        const val MIME_TYPE = "application/json"

        fun suggestedFileName(epochMillis: Long, zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
            val date = java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
            return "thatday-backup-$date.json"
        }
    }
}
