package com.renato.launcher.data.database.favorite

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = [
        "componentName",
        "userSerial"
    ]
)
data class FavoriteEntity(
    val componentName: String,
    val packageName: String,
    val userSerial: Long,
    val position: Int
)
