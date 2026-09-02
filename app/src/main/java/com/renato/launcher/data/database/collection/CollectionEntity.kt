package com.renato.launcher.data.database.collection

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections"
)
data class CollectionEntity(
    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0L,
    val name: String,
    val position: Int
)
