package com.renato.launcher.favorites

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.favorite.FavoriteDao
import com.renato.launcher.data.database.favorite.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FavoriteRepository(
    context: Context,
    private val favoriteDao: FavoriteDao
) {

    private val launcherApps =
        context.getSystemService(
            LauncherApps::class.java
        )

    private val userManager =
        context.getSystemService(
            UserManager::class.java
        )

    val favorites: Flow<List<FavoriteEntity>> =
        favoriteDao.observeFavorites()

    suspend fun replaceFavorites(
        apps: List<InstalledApp>
    ) {
        val entities =
            apps.mapIndexed {
                    index,
                    app ->

                FavoriteEntity(
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

        favoriteDao.replaceAll(
            entities
        )
    }

    /**
     * Resolves only the apps that are actually pinned to Home.
     *
     * This is intentionally independent from the full installed-app catalog.
     * After the launcher process is recreated, Home therefore does not have
     * to wait for every app and every icon to be discovered before favorites
     * can appear.
     */
    suspend fun resolveFavorites(
        savedFavorites: List<FavoriteEntity>
    ): List<InstalledApp> =
        withContext(
            Dispatchers.IO
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

            savedFavorites
                .sortedBy {
                    it.position
                }
                .mapNotNull {
                        favorite ->

                    val user =
                        usersBySerial[
                            favorite.userSerial
                        ] ?: return@mapNotNull null

                    val componentName =
                        ComponentName
                            .unflattenFromString(
                                favorite.componentName
                            ) ?: return@mapNotNull null

                    val activityInfo =
                        launcherApps
                            .getActivityList(
                                favorite.packageName,
                                user
                            )
                            .firstOrNull {
                                    activity ->

                                activity.componentName ==
                                    componentName
                            } ?: return@mapNotNull null

                    InstalledApp(
                        label =
                            activityInfo
                                .label
                                .toString(),
                        packageName =
                            activityInfo
                                .applicationInfo
                                .packageName,
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
                }
        }

    /**
     * Kept for callers that already have the complete app catalog available.
     */
    fun resolveFavorites(
        savedFavorites: List<FavoriteEntity>,
        installedApps: List<InstalledApp>
    ): List<InstalledApp> {
        val installedAppsByKey =
            installedApps.associateBy {
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

        return savedFavorites
            .sortedBy {
                it.position
            }
            .mapNotNull {
                    favorite ->

                installedAppsByKey[
                    appKey(
                        componentName =
                            favorite.componentName,
                        userSerial =
                            favorite.userSerial
                    )
                ]
            }
    }

    /**
     * A confirmed package removal should also remove its Home record.
     * Otherwise reinstalling the same package could unexpectedly restore an
     * old favorite later.
     */
    suspend fun removePackage(
        packageName: String,
        user: UserHandle
    ) {
        favoriteDao.deleteByPackage(
            packageName =
                packageName,
            userSerial =
                getUserSerial(
                    user
                )
        )
    }

    private fun getUserSerial(
        app: InstalledApp
    ): Long {
        return getUserSerial(
            app.user
        )
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

