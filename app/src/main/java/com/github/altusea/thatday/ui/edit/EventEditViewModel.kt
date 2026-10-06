package com.github.altusea.thatday.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.altusea.thatday.data.Event
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.data.TimeKind
import com.github.altusea.thatday.time.EventTimes
import com.github.altusea.thatday.time.RemindTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class EventEditUiState(
    val loading: Boolean = true,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val timeKind: TimeKind = TimeKind.ALL_DAY,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
    val note: String = "",
    val remind: Boolean = false,
    val aheadDays: Int = 1,
    /** Whether a reminder placed before this event would still be in the future. */
    val reminderAvailable: Boolean = false,
    val isDirty: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && !saved

    companion object {
        const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTE = 0
    }
}

class EventEditViewModel(
    private val repository: EventRepository,
    private val eventId: Long?,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(EventEditUiState())
    val state: StateFlow<EventEditUiState> = _state.asStateFlow()

    /** Snapshot used for dirty-checking; `null` means "new, still empty". */
    private var original: Event? = null

    init {
        if (eventId == null) {
            _state.update {
                it.copy(loading = false, date = LocalDate.now(zone)).recompute()
            }
        } else {
            viewModelScope.launch {
                val event = repository.getEvent(eventId)
                original = event
                _state.value = if (event == null) {
                    EventEditUiState(loading = false, date = LocalDate.now(zone)).recompute()
                } else {
                    EventEditUiState(
                        loading = false,
                        title = event.title,
                        date = EventTimes.dateOf(event.occurredAt, zone),
                        timeKind = event.timeKind,
                        hour = EventTimes.hourOf(event.occurredAt, zone),
                        minute = EventTimes.minuteOf(event.occurredAt, zone),
                        note = event.note.orEmpty(),
                        remind = event.remindAt != null,
                    ).recompute()
                }
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value).recompute() }

    fun onDateChange(value: LocalDate) = _state.update { it.copy(date = value).recompute() }

    fun onTimeKindChange(value: TimeKind) = _state.update { it.copy(timeKind = value).recompute() }

    fun onTimeChange(hour: Int, minute: Int) =
        _state.update { it.copy(hour = hour, minute = minute).recompute() }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value).recompute() }

    fun onRemindChange(value: Boolean) = _state.update { it.copy(remind = value).recompute() }

    fun onAheadDaysChange(value: Int) =
        _state.update { it.copy(aheadDays = value.coerceAtLeast(0)).recompute() }

    fun save() {
        val snapshot = _state.value
        if (!snapshot.canSave) return
        viewModelScope.launch {
            val occurredAt = EventTimes.combine(
                date = snapshot.date,
                timeKind = snapshot.timeKind,
                hour = snapshot.hour,
                minute = snapshot.minute,
                zone = zone,
            )
            val remindAt = if (snapshot.remind) {
                RemindTime.compute(occurredAt, snapshot.timeKind, snapshot.aheadDays, zone)
                    .takeIf { it > now() }
            } else {
                null
            }
            val note = snapshot.note.trim().ifBlank { null }
            val title = snapshot.title.trim()

            if (eventId == null) {
                repository.addEvent(title, occurredAt, snapshot.timeKind, note, remindAt)
            } else {
                repository.updateEvent(eventId, title, occurredAt, snapshot.timeKind, note, remindAt)
            }
            _state.update { it.copy(saved = true) }
        }
    }

    private fun EventEditUiState.recompute(): EventEditUiState {
        val occurredAt = EventTimes.combine(date, timeKind, hour, minute, zone)
        val reminderAvailable = RemindTime.compute(occurredAt, timeKind, aheadDays, zone) > now()
        val remind = remind && reminderAvailable
        val dirty = if (original == null) {
            title.isNotBlank() || note.isNotBlank() || timeKind != TimeKind.ALL_DAY
        } else {
            val base = original!!
            title != base.title ||
                note != base.note.orEmpty() ||
                timeKind != base.timeKind ||
                date != EventTimes.dateOf(base.occurredAt, zone) ||
                (timeKind == TimeKind.TIMED &&
                    (hour != EventTimes.hourOf(base.occurredAt, zone) ||
                        minute != EventTimes.minuteOf(base.occurredAt, zone)))
        }
        return copy(reminderAvailable = reminderAvailable, remind = remind, isDirty = dirty)
    }
}
