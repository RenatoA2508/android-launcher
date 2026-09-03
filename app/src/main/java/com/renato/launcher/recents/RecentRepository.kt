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
        recentDao.recordRecentLaunch(
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
        val userSerial =
            getUserSerial(
                app
            )

        val now =
            System.currentTimeMillis()

        recentDao.recordSearchLaunch(
            recentApp =
                RecentAppEntity(
                    componentName =
                        app.componentName
                            .flattenToString(),
                    packageName =
                        app.packageName,
                    userSerial =
                        userSerial,
                    lastOpenedAt =
                        now
                ),
            recentSearch =
                RecentSearchEntity(
                    componentName =
                        app.componentName
                            .flattenToString(),
                    packageName =
                        app.packageName,
                    userSerial =
                        userSerial,
                    lastSearchedAt =
                        now
                ),
            recentLimit =
                RECENT_SECTION_LIMIT
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
        recentLimit: Int = RECENT_SECTION_LIMIT,
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

        val savedRecentAppsByKey =
            savedRecentApps.associateBy { recent ->
                appKey(
                    componentName =
                        recent.componentName,
                    userSerial =
                        recent.userSerial
                )
            }

        val savedRecentSearchesByKey =
            savedRecentSearches.associateBy { recent ->
                appKey(
                    componentName =
                        recent.componentName,
                    userSerial =
                        recent.userSerial
                )
            }

        /*
         * RC1 recorded a typed-search launch in both history tables. Keep those
         * existing databases semantically correct during the RC1 -> RC2 update
         * by using the newest source timestamp as the winner:
         *
         * - search at the same/newer instant -> "Buscadas recientemente"
         * - a later generic use -> "Recientes"
         *
         * New RC2 writes also remove the losing search row when an app is
         * promoted, but this timestamp rule makes the upgrade correct without a
         * schema migration or destructive history rewrite.
         */
        val searchedApps =
            savedRecentSearches
                .filter { searched ->
                    val key =
                        appKey(
                            componentName =
                                searched.componentName,
                            userSerial =
                                searched.userSerial
                        )

                    val recent =
                        savedRecentAppsByKey[
                            key
                        ]

                    recent == null ||
                        recent.lastOpenedAt <=
                            searched.lastSearchedAt
                }
                .mapNotNull { searched ->
                    installedAppsByKey[
                        appKey(
                            componentName =
                                searched.componentName,
                            userSerial =
                                searched.userSerial
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

        val recentApps =
            savedRecentApps
                .filter { recent ->
                    val key =
                        appKey(
                            componentName =
                                recent.componentName,
                            userSerial =
                                recent.userSerial
                        )

                    val searched =
                        savedRecentSearchesByKey[
                            key
                        ]

                    searched == null ||
                        recent.lastOpenedAt >
                            searched.lastSearchedAt
                }
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
                    recentLimit
                )

        return RecentSections(
            recentApps =
                recentApps,
            searchedApps =
                searchedApps
        )
    }

    private companion object {
        const val RECENT_SECTION_LIMIT = 4
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
