package com.renato.launcher.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.renato.launcher.data.database.collection.CollectionAppEntity
import com.renato.launcher.data.database.collection.CollectionDao
import com.renato.launcher.data.database.collection.CollectionEntity
import com.renato.launcher.data.database.favorite.FavoriteDao
import com.renato.launcher.data.database.favorite.FavoriteEntity
import com.renato.launcher.data.database.recent.RecentAppEntity
import com.renato.launcher.data.database.recent.RecentDao
import com.renato.launcher.data.database.recent.RecentSearchEntity

@Database(
    entities = [
        FavoriteEntity::class,
        RecentAppEntity::class,
        RecentSearchEntity::class,
        CollectionEntity::class,
        CollectionAppEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LauncherDatabase :
    RoomDatabase() {

    abstract fun favoriteDao():
        FavoriteDao

    abstract fun recentDao():
        RecentDao

    abstract fun collectionDao():
        CollectionDao

    companion object {

        @Volatile
        private var instance:
            LauncherDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(
                1,
                2
            ) {
                override fun migrate(
                    database:
                        SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recent_apps` (
                            `componentName` TEXT NOT NULL,
                            `packageName` TEXT NOT NULL,
                            `userSerial` INTEGER NOT NULL,
                            `lastOpenedAt` INTEGER NOT NULL,
                            PRIMARY KEY(
                                `componentName`,
                                `userSerial`
                            )
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recent_searches` (
                            `componentName` TEXT NOT NULL,
                            `packageName` TEXT NOT NULL,
                            `userSerial` INTEGER NOT NULL,
                            `lastSearchedAt` INTEGER NOT NULL,
                            PRIMARY KEY(
                                `componentName`,
                                `userSerial`
                            )
                        )
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(
                2,
                3
            ) {
                override fun migrate(
                    database:
                        SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `collections` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `position` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `collection_apps` (
                            `collectionId` INTEGER NOT NULL,
                            `componentName` TEXT NOT NULL,
                            `packageName` TEXT NOT NULL,
                            `userSerial` INTEGER NOT NULL,
                            `position` INTEGER NOT NULL,
                            PRIMARY KEY(
                                `collectionId`,
                                `componentName`,
                                `userSerial`
                            ),
                            FOREIGN KEY(
                                `collectionId`
                            )
                            REFERENCES `collections`(
                                `id`
                            )
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_collection_apps_collectionId`
                        ON `collection_apps`(
                            `collectionId`
                        )
                        """.trimIndent()
                    )
                }
            }

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
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3
                            )
                            .build()
                            .also {
                                instance = it
                            }
                }
        }
    }
}
