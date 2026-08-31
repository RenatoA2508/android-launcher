package com.renato.launcher.data.database.recent

import androidx.room.Entity

@Entity(
    tableName = "recent_apps",
    primaryKeys = [
        "componentName",
        "userSerial"
    ]
)
data class RecentAppEntity(
    val componentName: String,
    val packageName: String,
    val userSerial: Long,
    val lastOpenedAt: Long
)
