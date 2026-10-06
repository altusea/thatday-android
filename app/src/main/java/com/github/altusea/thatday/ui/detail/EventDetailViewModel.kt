package com.github.altusea.thatday.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.altusea.thatday.data.Event
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.time.Relative
import com.github.altusea.thatday.time.RelativeTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

data class EventDetailUiState(
    val loading: Boolean = true,
    val event: Event? = null,
    val relative: Relative? = null,
)

class EventDetailViewModel(
    private val repository: EventRepository,
    private val eventId: Long,
) : ViewModel() {

    val uiState: StateFlow<EventDetailUiState> =
        combine(repository.observeEvent(eventId), nowTicker()) { event, now ->
            EventDetailUiState(
                loading = false,
                event = event,
                relative = event?.let { RelativeTime.relative(it.occurredAt, now, ZoneId.systemDefault()) },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EventDetailUiState(),
        )

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            onDeleted()
        }
    }

    private fun nowTicker() = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }
}
