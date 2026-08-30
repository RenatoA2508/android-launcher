package com.renato.launcher.data.database.favorite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query(
        """
        SELECT *
        FROM favorites
        ORDER BY position ASC
        """
    )
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertAll(
        favorites: List<FavoriteEntity>
    )

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(
        favorites: List<FavoriteEntity>
    ) {
        deleteAll()

        if (favorites.isNotEmpty()) {
            insertAll(favorites)
        }
    }
}
