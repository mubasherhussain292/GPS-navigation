package com.example.gpsnavigation.db


import androidx.room.*

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite): Long

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<Favorite>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE address = :address)")
    suspend fun isFavorite(address: String): Boolean

    @Query("DELETE FROM favorites WHERE address = :address")
    suspend fun deleteFavoriteByAddress(address: String): Int
}
