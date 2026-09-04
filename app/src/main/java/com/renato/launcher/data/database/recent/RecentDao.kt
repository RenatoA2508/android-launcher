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
        SELECT EXISTS(
            SELECT 1
            FROM (
                SELECT componentName, userSerial
                FROM recent_apps
                ORDER BY lastOpenedAt DESC
                LIMIT :recentLimit
            ) AS visible_recent_apps
            WHERE componentName = :componentName
              AND userSerial = :userSerial
        )
        """
    )
    suspend fun isInVisibleRecentWindow(
        componentName: String,
        userSerial: Long,
        recentLimit: Int
    ): Boolean

    @Query(
        """
        DELETE FROM recent_searches
        WHERE componentName = :componentName
          AND userSerial = :userSerial
        """
    )
    suspend fun deleteRecentSearch(
        componentName: String,
        userSerial: Long
    )

    /**
     * A normal app launch is stronger than search history.
     *
     * Promote the app to Recents and remove any stale "recently searched"
     * entry in one Room transaction so Search never exposes the same app in
     * both sections after a real subsequent use.
     */
    @Transaction
    suspend fun recordRecentLaunch(
        recentApp: RecentAppEntity
    ) {
        upsertRecentApp(
            recentApp
        )

        deleteRecentSearch(
            componentName =
                recentApp.componentName,
            userSerial =
                recentApp.userSerial
        )
    }

    /**
     * A first launch directly from a typed search belongs to
     * "Buscadas recientemente".
     *
     * If the app is still inside the currently visible Recents window, a typed
     * search should not demote it: refresh Recents and keep search history
     * absent. If it has already fallen out of that visible window, searching it
     * again is a new search event and it belongs in "Buscadas recientemente".
     */
    @Transaction
    suspend fun recordSearchLaunch(
        recentApp: RecentAppEntity,
        recentSearch: RecentSearchEntity,
        recentLimit: Int
    ) {
        if (
            isInVisibleRecentWindow(
                componentName =
                    recentApp.componentName,
                userSerial =
                    recentApp.userSerial,
                recentLimit =
                    recentLimit
            )
        ) {
            upsertRecentApp(
                recentApp
            )

            deleteRecentSearch(
                componentName =
                    recentApp.componentName,
                userSerial =
                    recentApp.userSerial
            )
        } else {
            upsertRecentSearch(
                recentSearch
            )
        }
    }

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
