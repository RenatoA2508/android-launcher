package com.renato.launcher.data.database.collection

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "collection_apps",
    primaryKeys = [
        "collectionId",
        "componentName",
        "userSerial"
    ],
    foreignKeys = [
        ForeignKey(
            entity =
                CollectionEntity::class,
            parentColumns = [
                "id"
            ],
            childColumns = [
                "collectionId"
            ],
            onDelete =
                ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = [
                "collectionId"
            ]
        )
    ]
)
data class CollectionAppEntity(
    val collectionId: Long,
    val componentName: String,
    val packageName: String,
    val userSerial: Long,
    val position: Int
)
