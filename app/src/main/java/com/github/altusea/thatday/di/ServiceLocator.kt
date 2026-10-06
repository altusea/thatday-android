package com.github.altusea.thatday.di

import android.content.Context
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.data.ThatDayDb
import com.github.altusea.thatday.data.backup.BackupManager

/**
 * Minimal manual DI. The app has a single screen graph and one repository, so a
 * full DI framework would be more ceremony than value.
 */
object ServiceLocator {

    @Volatile
    private var repository: EventRepository? = null

    @Volatile
    private var backupManager: BackupManager? = null

    fun repository(context: Context): EventRepository =
        repository ?: synchronized(this) {
            repository ?: EventRepository(ThatDayDb.build(context).eventDao()).also {
                repository = it
            }
        }

    fun backupManager(context: Context): BackupManager =
        backupManager ?: synchronized(this) {
            val appContext = context.applicationContext
            backupManager ?: BackupManager(
                repository = repository(appContext),
                contentResolver = appContext.contentResolver,
            ).also { backupManager = it }
        }
}
