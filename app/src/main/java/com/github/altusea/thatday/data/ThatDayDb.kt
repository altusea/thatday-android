package com.github.altusea.thatday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Event::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ThatDayDb : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        private const val DB_NAME = "thatday.db"

        fun build(context: Context): ThatDayDb =
            Room.databaseBuilder(context.applicationContext, ThatDayDb::class.java, DB_NAME).build()
    }
}
