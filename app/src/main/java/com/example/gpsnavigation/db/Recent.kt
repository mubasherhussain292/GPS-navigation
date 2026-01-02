package com.example.gpsnavigation.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "recent")
@Parcelize
data class Recent(
    @PrimaryKey val address: String,  // unique key
    val lat: Double,
    val lon: Double,
    val dateAndTime: String
) : Parcelable

