package com.renato.launcher.data.database.collection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query(
        """
        SELECT *
        FROM collections
        ORDER BY position ASC
        """
    )
    fun observeCollections():
        Flow<List<CollectionEntity>>

    @Query(
        """
        SELECT *
        FROM collection_apps
        ORDER BY collectionId ASC, position ASC
        """
    )
    fun observeCollectionApps():
        Flow<List<CollectionAppEntity>>

    @Query(
        """
        SELECT *
        FROM collection_apps
        WHERE collectionId = :collectionId
        ORDER BY position ASC
        """
    )
    fun observeAppsForCollection(
        collectionId: Long
    ): Flow<List<CollectionAppEntity>>

    @Query(
        """
        SELECT COALESCE(MAX(position), -1) + 1
        FROM collections
        """
    )
    suspend fun nextCollectionPosition():
        Int

    @Query(
        """
        SELECT COUNT(*)
        FROM collections
        """
    )
    suspend fun collectionCount():
        Int

    @Insert(
        onConflict =
            OnConflictStrategy.ABORT
    )
    suspend fun insertCollection(
        collection: CollectionEntity
    ): Long

    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertCollectionApps(
        apps: List<CollectionAppEntity>
    )

    /**
     * Collection creation is one database transaction.
     *
     * Previously the collection row was committed first and its app rows were
     * written by a second transaction. A process death between those writes
     * could therefore leave an unintended empty collection. The max-count
     * check, position allocation, parent insert and child inserts now commit
     * together or not at all.
     */
    @Transaction
    suspend fun createCollection(
        name: String,
        apps: List<CollectionAppEntity>,
        maxCollections: Int
    ): Long {
        check(
            collectionCount() <
                maxCollections
        ) {
            "A maximum of $maxCollections collections is allowed."
        }

        val collectionId =
            insertCollection(
                CollectionEntity(
                    name = name,
                    position =
                        nextCollectionPosition()
                )
            )

        if (
            apps.isNotEmpty()
        ) {
            insertCollectionApps(
                apps.map { app ->
                    app.copy(
                        collectionId =
                            collectionId
                    )
                }
            )
        }

        return collectionId
    }

    @Query(
        """
        UPDATE collections
        SET name = :name
        WHERE id = :collectionId
        """
    )
    suspend fun renameCollection(
        collectionId: Long,
        name: String
    )

    @Query(
        """
        UPDATE collections
        SET position = :position
        WHERE id = :collectionId
        """
    )
    suspend fun updateCollectionPosition(
        collectionId: Long,
        position: Int
    )

    @Query(
        """
        DELETE FROM collection_apps
        WHERE collectionId = :collectionId
        """
    )
    suspend fun deleteAppsForCollection(
        collectionId: Long
    )

    @Query(
        """
        DELETE FROM collection_apps
        WHERE packageName = :packageName
          AND userSerial = :userSerial
        """
    )
    suspend fun deleteAppsByPackage(
        packageName: String,
        userSerial: Long
    )

    @Query(
        """
        DELETE FROM collections
        WHERE id = :collectionId
        """
    )
    suspend fun deleteCollection(
        collectionId: Long
    )

    @Transaction
    suspend fun replaceCollectionApps(
        collectionId: Long,
        apps: List<CollectionAppEntity>
    ) {
        deleteAppsForCollection(
            collectionId
        )

        if (
            apps.isNotEmpty()
        ) {
            insertCollectionApps(
                apps
            )
        }
    }

    @Transaction
    suspend fun updateCollection(
        collectionId: Long,
        name: String,
        apps: List<CollectionAppEntity>
    ) {
        renameCollection(
            collectionId =
                collectionId,
            name =
                name
        )

        deleteAppsForCollection(
            collectionId
        )

        if (
            apps.isNotEmpty()
        ) {
            insertCollectionApps(
                apps
            )
        }
    }

    @Transaction
    suspend fun replaceCollectionOrder(
        collections: List<CollectionEntity>
    ) {
        collections
            .forEachIndexed {
                    index,
                    collection ->

                updateCollectionPosition(
                    collectionId =
                        collection.id,
                    position =
                        index
                )
            }
    }

    @Transaction
    suspend fun swapCollectionPositions(
        firstCollectionId: Long,
        firstPosition: Int,
        secondCollectionId: Long,
        secondPosition: Int
    ) {
        updateCollectionPosition(
            collectionId =
                firstCollectionId,
            position =
                secondPosition
        )

        updateCollectionPosition(
            collectionId =
                secondCollectionId,
            position =
                firstPosition
        )
    }
}
