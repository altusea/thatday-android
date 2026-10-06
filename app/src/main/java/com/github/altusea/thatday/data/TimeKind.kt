package com.github.altusea.thatday.data

/**
 * Whether an event is anchored to a whole day or to a specific time of day.
 *
 * Past/future is deliberately *not* modelled here: it is derived from
 * [Event.occurredAt] and the current time (see DESIGN.md §1.1 / §2.3).
 */
enum class TimeKind {
    ALL_DAY,
    TIMED,
}
