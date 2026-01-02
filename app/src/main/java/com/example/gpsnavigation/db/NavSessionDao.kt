package com.example.gpsnavigation.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NavSessionDao {

    @Insert
    suspend fun insertSession(session: NavSessionEntity): Long

    @Query("UPDATE nav_session SET endedAtMillis = :endedAt WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endedAt: Long)

    @Query("SELECT * FROM nav_session WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: Long): NavSessionEntity?

    @Query("SELECT * FROM nav_session ORDER BY startedAtMillis DESC")
    suspend fun getAllSessions(): List<NavSessionEntity>
}
