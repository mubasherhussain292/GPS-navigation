package com.example.gpsnavigation.db


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val address: String,
    val lat: Double,
    val lon: Double,
    val dateAndTime: String,
    val favoriteCheck: Int = 1
): Parcelable

