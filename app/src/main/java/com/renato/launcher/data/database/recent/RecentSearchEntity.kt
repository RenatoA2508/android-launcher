package com.renato.launcher.data.database.recent

import androidx.room.Entity

@Entity(
    tableName = "recent_searches",
    primaryKeys = [
        "componentName",
        "userSerial"
    ]
)
data class RecentSearchEntity(
    val componentName: String,
    val packageName: String,
    val userSerial: Long,
    val lastSearchedAt: Long
)
