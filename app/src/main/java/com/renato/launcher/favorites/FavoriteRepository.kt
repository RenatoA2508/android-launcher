package com.renato.launcher.favorites

import android.content.Context
import android.os.UserManager
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.favorite.FavoriteDao
import com.renato.launcher.data.database.favorite.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoriteRepository(
    context: Context,
    private val favoriteDao: FavoriteDao
) {

    private val userManager =
        context.getSystemService(UserManager::class.java)

    val favorites: Flow<List<FavoriteEntity>> =
        favoriteDao.observeFavorites()

    suspend fun replaceFavorites(
        apps: List<InstalledApp>
    ) {
        val entities =
            apps.mapIndexed { index, app ->
                FavoriteEntity(
                    componentName =
                        app.componentName
                            .flattenToString(),
                    packageName =
                        app.packageName,
                    userSerial =
                        getUserSerial(app),
                    position =
                        index
                )
            }

        favoriteDao.replaceAll(
            entities
        )
    }

    fun resolveFavorites(
        savedFavorites: List<FavoriteEntity>,
        installedApps: List<InstalledApp>
    ): List<InstalledApp> {

        val installedAppsByKey =
            installedApps.associateBy { app ->
                appKey(
                    componentName =
                        app.componentName
                            .flattenToString(),
                    userSerial =
                        getUserSerial(app)
                )
            }

        return savedFavorites
            .sortedBy {
                it.position
            }
            .mapNotNull { favorite ->
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

    private fun getUserSerial(
        app: InstalledApp
    ): Long {
        return userManager
            .getSerialNumberForUser(
                app.user
            )
    }

    private fun appKey(
        componentName: String,
        userSerial: Long
    ): String {
        return "$userSerial:$componentName"
    }
}
