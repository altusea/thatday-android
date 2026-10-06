package com.github.altusea.thatday.di

import android.content.Context
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.data.ThatDayDb

/**
 * Minimal manual DI. The app has a single screen graph and one repository, so a
 * full DI framework would be more ceremony than value.
 */
object ServiceLocator {

    @Volatile
    private var repository: EventRepository? = null

    fun repository(context: Context): EventRepository =
        repository ?: synchronized(this) {
            repository ?: EventRepository(ThatDayDb.build(context).eventDao()).also {
                repository = it
            }
        }
}
