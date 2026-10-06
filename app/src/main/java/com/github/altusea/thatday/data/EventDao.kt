package com.github.altusea.thatday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY occurredAt ASC")
    fun observeAll(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeById(id: Long): Flow<Event?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): Event?

    @Insert
    suspend fun insert(event: Event): Long

    @Update
    suspend fun update(event: Event)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM events WHERE remindAt IS NOT NULL AND remindAt > :now ORDER BY remindAt ASC")
    suspend fun getWithFutureReminder(now: Long): List<Event>
}
