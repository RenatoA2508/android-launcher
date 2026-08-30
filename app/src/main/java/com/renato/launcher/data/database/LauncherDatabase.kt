package com.renato.launcher.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.renato.launcher.data.database.favorite.FavoriteDao
import com.renato.launcher.data.database.favorite.FavoriteEntity

@Database(
    entities = [
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LauncherDatabase :
    RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {

        @Volatile
        private var instance: LauncherDatabase? = null

        fun getInstance(
            context: Context
        ): LauncherDatabase {

            return instance
                ?: synchronized(this) {

                    instance
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            LauncherDatabase::class.java,
                            "launcher.db"
                        )
                            .build()
                            .also {
                                instance = it
                            }
                }
        }
    }
}
