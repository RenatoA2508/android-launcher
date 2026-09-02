package com.renato.launcher.data.database.recent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {

    @Query(
        """
        SELECT *
        FROM recent_apps
        ORDER BY lastOpenedAt DESC
        """
    )
    fun observeRecentApps():
        Flow<List<RecentAppEntity>>

    @Query(
        """
        SELECT *
        FROM recent_searches
        ORDER BY lastSearchedAt DESC
        """
    )
    fun observeRecentSearches():
        Flow<List<RecentSearchEntity>>

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertRecentApp(
        recentApp: RecentAppEntity
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertRecentSearch(
        recentSearch: RecentSearchEntity
    )

    @Query(
        """
        DELETE FROM recent_apps
        WHERE packageName = :packageName
          AND userSerial = :userSerial
        """
    )
    suspend fun deleteRecentAppsByPackage(
        packageName: String,
        userSerial: Long
    )

    @Query(
        """
        DELETE FROM recent_searches
        WHERE packageName = :packageName
          AND userSerial = :userSerial
        """
    )
    suspend fun deleteRecentSearchesByPackage(
        packageName: String,
        userSerial: Long
    )

    @Transaction
    suspend fun deletePackageHistory(
        packageName: String,
        userSerial: Long
    ) {
        deleteRecentAppsByPackage(
            packageName =
                packageName,
            userSerial =
                userSerial
        )

        deleteRecentSearchesByPackage(
            packageName =
                packageName,
            userSerial =
                userSerial
        )
    }
}
