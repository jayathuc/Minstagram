package com.jayathu.minstagram.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY endedAtMs DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAtMs DESC")
    fun all(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY endedAtMs DESC LIMIT 1")
    fun latest(): Flow<SessionEntity?>

    @Query("SELECT COUNT(*) FROM sessions WHERE startedAtMs >= :sinceMs")
    fun countSince(sinceMs: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(actualSeconds), 0) FROM sessions WHERE startedAtMs >= :sinceMs")
    fun totalSecondsSince(sinceMs: Long): Flow<Int>
}
