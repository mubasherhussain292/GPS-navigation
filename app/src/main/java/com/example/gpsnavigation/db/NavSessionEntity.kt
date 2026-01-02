package com.example.gpsnavigation.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nav_session")
data class NavSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val routeType: String,
    val startName: String?,
    val startLat: Double,
    val startLng: Double,
    val endName: String?,
    val endLat: Double,
    val endLng: Double,
    val trackFilePath: String,
    val totalDistanceMeters: Double? = null,
    val totalDurationSec: Int? = null
)
