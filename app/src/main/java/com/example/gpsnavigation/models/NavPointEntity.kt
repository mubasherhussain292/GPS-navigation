package com.example.myapplication.gpsappworktest.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.gpsappworktest.db.NavSessionEntity

@Entity(
    tableName = "nav_point",
    indices = [Index("sessionId"), Index("timeMillis")],
    foreignKeys = [
        ForeignKey(
            entity = NavSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NavPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timeMillis: Long,
    val lat: Double,
    val lng: Double
)

