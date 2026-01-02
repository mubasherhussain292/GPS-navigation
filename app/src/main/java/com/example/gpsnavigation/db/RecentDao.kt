package com.example.myapplication.gpsappworktest.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gpsnavigation.db.Recent

@Dao
interface RecentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: Recent): Long

    @Query("SELECT * FROM recent")
    suspend fun getAllRecent(): List<Recent>

    @Query("SELECT * FROM recent WHERE address = :address LIMIT 1")
    suspend fun getRecentByAddress(address: String): Recent?

    @Update
    suspend fun updateRecent(recent: Recent)

    @Query("UPDATE nav_session SET trackFilePath = :path WHERE id = :sessionId")
    suspend fun updateTrackPath(sessionId: Long, path: String)
}