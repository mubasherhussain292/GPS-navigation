package com.example.gpsnavigation.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gpsnavigation.models.NavPointEntity

@Dao
interface NavPointDao {

    @Insert
    suspend fun insertPoint(point: NavPointEntity)

    @Query("SELECT * FROM nav_point WHERE sessionId = :sessionId ORDER BY timeMillis ASC")
    suspend fun getPoints(sessionId: Long): List<NavPointEntity>
}
