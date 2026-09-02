package com.renato.launcher.recents

import android.content.Context
import android.os.UserManager
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.recent.RecentAppEntity
import com.renato.launcher.data.database.recent.RecentDao
import com.renato.launcher.data.database.recent.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

data class RecentSections(
    val recentApps: List<InstalledApp>,
    val searchedApps: List<InstalledApp>
)

class RecentRepository(
    context: Context,
    private val recentDao: RecentDao
) {

    private val userManager =
        context.getSystemService(
            UserManager::class.java
        )

    val recentApps:
        Flow<List<RecentAppEntity>> =
        recentDao.observeRecentApps()

    val recentSearches:
        Flow<List<RecentSearchEntity>> =
        recentDao.observeRecentSearches()

    suspend fun recordLaunch(
        app: InstalledApp
    ) {
        recentDao.upsertRecentApp(
            RecentAppEntity(
                componentName =
                    app.componentName
                        .flattenToString(),
                packageName =
                    app.packageName,
                userSerial =
                    getUserSerial(app),
                lastOpenedAt =
                    System.currentTimeMillis()
            )
        )
    }

    suspend fun recordSearchLaunch(
        app: InstalledApp
    ) {
        recentDao.upsertRecentSearch(
            RecentSearchEntity(
                componentName =
                    app.componentName
                        .flattenToString(),
                packageName =
                    app.packageName,
                userSerial =
                    getUserSerial(app),
                lastSearchedAt =
                    System.currentTimeMillis()
            )
        )
    }

    /**
     * A confirmed uninstall must remove launcher history as well.
     *
     * Otherwise reinstalling the same package/component later could resurrect
     * an old Recents or "Buscadas recientemente" entry.
     */
    suspend fun removePackage(
        packageName: String,
        user: android.os.UserHandle
    ) {
        recentDao.deletePackageHistory(
            packageName =
                packageName,
            userSerial =
                userManager
                    .getSerialNumberForUser(
                        user
                    )
        )
    }

    fun buildSections(
        savedRecentApps:
            List<RecentAppEntity>,
        savedRecentSearches:
            List<RecentSearchEntity>,
        installedApps:
            List<InstalledApp>,
        recentLimit: Int = 4,
        searchedLimit: Int = 4
    ): RecentSections {

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

        /*
         * First resolve Search-specific history.
         *
         * These applications get priority in the
         * "Buscadas recientemente" section.
         */
        val searchedApps =
            savedRecentSearches
                .mapNotNull { recent ->
                    installedAppsByKey[
                        appKey(
                            componentName =
                                recent.componentName,
                            userSerial =
                                recent.userSerial
                        )
                    ]
                }
                .distinctBy { app ->
                    appKey(
                        componentName =
                            app.componentName
                                .flattenToString(),
                        userSerial =
                            getUserSerial(app)
                    )
                }
                .take(
                    searchedLimit
                )

        val searchedKeys =
            searchedApps
                .map { app ->
                    appKey(
                        componentName =
                            app.componentName
                                .flattenToString(),
                        userSerial =
                            getUserSerial(app)
                    )
                }
                .toSet()

        /*
         * The generic Recents section excludes
         * applications already shown under
         * "Buscadas recientemente".
         *
         * Therefore Search never shows the same
         * application twice.
         */
        val recentApps =
            savedRecentApps
                .mapNotNull { recent ->
                    installedAppsByKey[
                        appKey(
                            componentName =
                                recent.componentName,
                            userSerial =
                                recent.userSerial
                        )
                    ]
                }
                .filterNot { app ->
                    appKey(
                        componentName =
                            app.componentName
                                .flattenToString(),
                        userSerial =
                            getUserSerial(app)
                    ) in searchedKeys
                }
                .distinctBy { app ->
                    appKey(
                        componentName =
                            app.componentName
                                .flattenToString(),
                        userSerial =
                            getUserSerial(app)
                    )
                }
                .take(
                    recentLimit
                )

        return RecentSections(
            recentApps =
                recentApps,
            searchedApps =
                searchedApps
        )
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
