package com.renato.launcher.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/**
 * Cross-feature cleanup for a package that Android has confirmed as removed.
 *
 * Favorites, collection membership and both history tables belong to the same
 * launcher database. Keeping the deletes in one Room transaction guarantees
 * that an abrupt process death cannot leave only part of the package removed
 * from persistent launcher state.
 */
@Dao
interface LauncherCleanupDao {

    @Query(
        """
        DELETE FROM favorites
        WHERE packageName = :packageName
          AND userSerial = :userSerial
        """
    )
    suspend fun deleteFavoriteReferences(
        packageName: String,
        userSerial: Long
    )

    @Query(
        """
        DELETE FROM collection_apps
        WHERE packageName = :packageName
          AND userSerial = :userSerial
        """
    )
    suspend fun deleteCollectionReferences(
        packageName: String,
        userSerial: Long
    )

    @Query(
        """
        DELETE FROM recent_apps
        WHERE packageName = :packageName
          AND userSerial = :userSerial
        """
    )
    suspend fun deleteRecentReferences(
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
    suspend fun deleteRecentSearchReferences(
        packageName: String,
        userSerial: Long
    )

    @Transaction
    suspend fun deletePackageReferences(
        packageName: String,
        userSerial: Long
    ) {
        deleteFavoriteReferences(
            packageName = packageName,
            userSerial = userSerial
        )

        deleteCollectionReferences(
            packageName = packageName,
            userSerial = userSerial
        )

        deleteRecentReferences(
            packageName = packageName,
            userSerial = userSerial
        )

        deleteRecentSearchReferences(
            packageName = packageName,
            userSerial = userSerial
        )
    }
}
