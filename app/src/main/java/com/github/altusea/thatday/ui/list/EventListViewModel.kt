package com.github.altusea.thatday.ui.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.altusea.thatday.data.Event
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.data.backup.BackupManager
import com.github.altusea.thatday.time.Relative
import com.github.altusea.thatday.time.RelativeTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

data class EventGroup(
    val relative: Relative,
    val events: List<Event>,
)

data class EventListUiState(
    val loading: Boolean = true,
    val groups: List<EventGroup> = emptyList(),
) {
    val isEmpty: Boolean get() = !loading && groups.isEmpty()
}

/** One-shot result of a backup/restore action, resolved to text by the UI. */
sealed interface BackupMessage {
    data class Exported(val count: Int) : BackupMessage
    data object ExportFailed : BackupMessage
    data class Imported(val imported: Int, val skipped: Int) : BackupMessage
    data object ImportFailed : BackupMessage
}

class EventListViewModel(
    repository: EventRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _messages = MutableSharedFlow<BackupMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<BackupMessage> = _messages.asSharedFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching { backupManager.export(uri) }
                .fold(
                    onSuccess = { BackupMessage.Exported(it) },
                    onFailure = { BackupMessage.ExportFailed },
                )
            _messages.emit(message)
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching { backupManager.import(uri) }
                .fold(
                    onSuccess = { BackupMessage.Imported(imported = it.imported, skipped = it.skipped) },
                    onFailure = { BackupMessage.ImportFailed },
                )
            _messages.emit(message)
        }
    }

    val uiState: StateFlow<EventListUiState> =
        combine(repository.observeEvents(), nowTicker()) { events, now ->
            buildState(events, now, ZoneId.systemDefault())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EventListUiState(),
        )

    private fun buildState(events: List<Event>, now: Long, zone: ZoneId): EventListUiState {
        val grouped = LinkedHashMap<Relative, MutableList<Event>>()
        for (event in events) {
            val key = RelativeTime.relative(event.occurredAt, now, zone)
            grouped.getOrPut(key) { mutableListOf() }.add(event)
        }
        val groups = grouped
            .map { (relative, list) -> EventGroup(relative, list.sortedBy { it.occurredAt }) }
            // Nearest to now first; on a tie the future wins, so "3 天后" sits
            // above "3 天前" (DESIGN.md §3.1).
            .sortedWith(compareBy({ it.relative.approxDays }, { if (it.relative.isFutureSide()) 0 else 1 }))
        return EventListUiState(loading = false, groups = groups)
    }

    /** Re-emits every minute so relative labels never go stale (DESIGN.md §3.0 无障碍). */
    private fun nowTicker() = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }
}

private fun Relative.isFutureSide(): Boolean = when (this) {
    Relative.Today -> true
    is Relative.Away -> future
}
