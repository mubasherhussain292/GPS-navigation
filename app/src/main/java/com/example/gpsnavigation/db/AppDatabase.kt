package com.example.gpsnavigation.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.gpsappworktest.db.RecentDao
import com.example.gpsnavigation.models.NavPointEntity

@Database(
    entities = [
        Favorite::class,
        Recent::class,
        NavSessionEntity::class,
        NavPointEntity::class
    ],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun navSessionDao(): NavSessionDao
    abstract fun navPointDao(): NavPointDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS nav_session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startedAtMillis INTEGER NOT NULL,
                        endedAtMillis INTEGER,
                        routeType TEXT NOT NULL,
                        startName TEXT,
                        startLat REAL NOT NULL,
                        startLng REAL NOT NULL,
                        endName TEXT,
                        endLat REAL NOT NULL,
                        endLng REAL NOT NULL,
                        trackFilePath TEXT NOT NULL,
                        totalDistanceMeters REAL,
                        totalDurationSec INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "favorites_dbs"
                )
                    .addMigrations(MIGRATION_2_3)
                     .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

