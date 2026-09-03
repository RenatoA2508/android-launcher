package com.renato.launcher.collections

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.collection.CollectionAppEntity
import com.renato.launcher.data.database.collection.CollectionDao
import com.renato.launcher.data.database.collection.CollectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

const val MAX_COLLECTIONS = 6

class CollectionRepository(
    context: Context,
    private val collectionDao: CollectionDao
) {

    private val userManager =
        context.getSystemService(
            UserManager::class.java
        )

    private val launcherApps =
        context.getSystemService(
            LauncherApps::class.java
        )

    val collections:
        Flow<List<CollectionEntity>> =
        collectionDao
            .observeCollections()

    val collectionApps:
        Flow<List<CollectionAppEntity>> =
        collectionDao
            .observeCollectionApps()

    suspend fun createCollection(
        name: String,
        apps: List<InstalledApp>
    ): Long {
        val normalizedName =
            normalizeName(
                name
            )

        return collectionDao
            .createCollection(
                name =
                    normalizedName,
                apps =
                    apps.toEntities(
                        collectionId =
                            0L
                    ),
                maxCollections =
                    MAX_COLLECTIONS
            )
    }

    suspend fun updateCollection(
        collectionId: Long,
        name: String,
        apps: List<InstalledApp>
    ) {
        val normalizedName =
            normalizeName(
                name
            )

        collectionDao
            .updateCollection(
                collectionId =
                    collectionId,
                name =
                    normalizedName,
                apps =
                    apps.toEntities(
                        collectionId =
                            collectionId
                    )
            )
    }

    suspend fun replaceCollectionApps(
        collectionId: Long,
        apps: List<InstalledApp>
    ) {
        collectionDao
            .replaceCollectionApps(
                collectionId =
                    collectionId,
                apps =
                    apps.toEntities(
                        collectionId =
                            collectionId
                    )
            )
    }

    suspend fun replaceCollectionOrder(
        collections: List<CollectionEntity>
    ) {
        collectionDao
            .replaceCollectionOrder(
                collections =
                    collections
            )
    }

    suspend fun deleteCollection(
        collectionId: Long
    ) {
        collectionDao
            .deleteCollection(
                collectionId
            )
    }

    suspend fun removePackage(
        packageName: String,
        user: UserHandle
    ) {
        collectionDao
            .deleteAppsByPackage(
                packageName =
                    packageName,
                userSerial =
                    getUserSerial(
                        user
                    )
            )
    }

    suspend fun swapCollections(
        first: CollectionEntity,
        second: CollectionEntity
    ) {
        if (
            first.id ==
            second.id
        ) {
            return
        }

        collectionDao
            .swapCollectionPositions(
                firstCollectionId =
                    first.id,
                firstPosition =
                    first.position,
                secondCollectionId =
                    second.id,
                secondPosition =
                    second.position
            )
    }

    /**
     * Critical Home path for Collections.
     *
     * Resolve only the launcher activities that are actually stored inside
     * collections instead of waiting for the complete installed-app catalog.
     * This mirrors the fast-path already used by Favorites on Home.
     */
    suspend fun resolveCollectionApps(
        savedApps: List<CollectionAppEntity>
    ): Map<Long, List<InstalledApp>> =
        withContext(
            Dispatchers.Default
        ) {
            val usersBySerial =
                userManager
                    .userProfiles
                    .associateBy {
                            user ->

                        getUserSerial(
                            user
                        )
                    }

            val resolvedAppsByKey =
                savedApps
                    .distinctBy {
                        appKey(
                            componentName =
                                it.componentName,
                            userSerial =
                                it.userSerial
                        )
                    }
                    .mapNotNull {
                            entry ->

                        val user =
                            usersBySerial[
                                entry.userSerial
                            ] ?: return@mapNotNull null

                        val activityInfo =
                            launcherApps
                                .getActivityList(
                                    entry.packageName,
                                    user
                                )
                                .firstOrNull {
                                        info ->

                                    info.componentName
                                        .flattenToString() ==
                                        entry.componentName
                                }
                                ?: return@mapNotNull null

                        val app =
                            InstalledApp(
                                label =
                                    activityInfo
                                        .label
                                        .toString(),
                                packageName =
                                    entry.packageName,
                                componentName =
                                    activityInfo
                                        .componentName,
                                user =
                                    user,
                                icon =
                                    activityInfo
                                        .getIcon(
                                            0
                                        )
                            )

                        appKey(
                            componentName =
                                entry.componentName,
                            userSerial =
                                entry.userSerial
                        ) to app
                    }
                    .toMap()

            savedApps
                .groupBy {
                    it.collectionId
                }
                .mapValues {
                        (_, collectionEntries) ->

                    collectionEntries
                        .sortedBy {
                            it.position
                        }
                        .mapNotNull {
                                entry ->

                            resolvedAppsByKey[
                                appKey(
                                    componentName =
                                        entry.componentName,
                                    userSerial =
                                        entry.userSerial
                                )
                            ]
                        }
                }
        }

    /**
     * Full-catalog resolver retained for collection-management screens that
     * already have the complete app catalog in memory.
     */
    fun resolveCollectionApps(
        savedApps: List<CollectionAppEntity>,
        installedApps: List<InstalledApp>
    ): Map<Long, List<InstalledApp>> {
        val installedAppsByKey =
            installedApps
                .associateBy {
                        app ->

                    appKey(
                        componentName =
                            app.componentName
                                .flattenToString(),
                        userSerial =
                            getUserSerial(
                                app.user
                            )
                    )
                }

        return savedApps
            .groupBy {
                it.collectionId
            }
            .mapValues {
                    (_, collectionEntries) ->

                collectionEntries
                    .sortedBy {
                        it.position
                    }
                    .mapNotNull {
                            entry ->

                        installedAppsByKey[
                            appKey(
                                componentName =
                                    entry.componentName,
                                userSerial =
                                    entry.userSerial
                            )
                        ]
                    }
            }
    }

    private fun List<InstalledApp>.toEntities(
        collectionId: Long
    ): List<CollectionAppEntity> {
        return distinctBy { app ->
            appKey(
                componentName =
                    app.componentName
                        .flattenToString(),
                userSerial =
                    getUserSerial(
                        app.user
                    )
            )
        }
            .mapIndexed {
                    index,
                    app ->

                CollectionAppEntity(
                    collectionId =
                        collectionId,
                    componentName =
                        app.componentName
                            .flattenToString(),
                    packageName =
                        app.packageName,
                    userSerial =
                        getUserSerial(
                            app.user
                        ),
                    position =
                        index
                )
            }
    }

    private fun normalizeName(
        name: String
    ): String {
        val normalizedName =
            name.trim()

        require(
            normalizedName.isNotEmpty()
        ) {
            "Collection name cannot be blank."
        }

        return normalizedName
    }

    private fun getUserSerial(
        user: UserHandle
    ): Long {
        return userManager
            .getSerialNumberForUser(
                user
            )
    }

    private fun appKey(
        componentName: String,
        userSerial: Long
    ): String {
        return "$userSerial:$componentName"
    }
}
