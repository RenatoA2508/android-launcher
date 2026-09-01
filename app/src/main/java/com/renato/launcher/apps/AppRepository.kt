package com.renato.launcher.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import com.renato.launcher.core.model.InstalledApp
import java.text.Collator
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context
) {

    data class AppChange(
        val type: Type,
        val packageNames: List<String>,
        val user: UserHandle
    ) {
        enum class Type {
            ADDED,
            REMOVED,
            CHANGED,
            AVAILABLE,
            UNAVAILABLE
        }
    }

    private val launcherApps =
        context.getSystemService(
            LauncherApps::class.java
        )

    /**
     * Package events from the system launcher service.
     *
     * This lets Home react to installs, removals and package changes
     * instead of waiting until the launcher process is recreated.
     */
    val appChanges: Flow<AppChange> =
        callbackFlow {
            val callback =
                object : LauncherApps.Callback() {
                    override fun onPackageRemoved(
                        packageName: String,
                        user: UserHandle
                    ) {
                        trySend(
                            AppChange(
                                type =
                                    AppChange.Type.REMOVED,
                                packageNames =
                                    listOf(
                                        packageName
                                    ),
                                user =
                                    user
                            )
                        )
                    }

                    override fun onPackageAdded(
                        packageName: String,
                        user: UserHandle
                    ) {
                        trySend(
                            AppChange(
                                type =
                                    AppChange.Type.ADDED,
                                packageNames =
                                    listOf(
                                        packageName
                                    ),
                                user =
                                    user
                            )
                        )
                    }

                    override fun onPackageChanged(
                        packageName: String,
                        user: UserHandle
                    ) {
                        trySend(
                            AppChange(
                                type =
                                    AppChange.Type.CHANGED,
                                packageNames =
                                    listOf(
                                        packageName
                                    ),
                                user =
                                    user
                            )
                        )
                    }

                    override fun onPackagesAvailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean
                    ) {
                        trySend(
                            AppChange(
                                type =
                                    AppChange.Type.AVAILABLE,
                                packageNames =
                                    packageNames.toList(),
                                user =
                                    user
                            )
                        )
                    }

                    override fun onPackagesUnavailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean
                    ) {
                        trySend(
                            AppChange(
                                type =
                                    AppChange.Type.UNAVAILABLE,
                                packageNames =
                                    packageNames.toList(),
                                user =
                                    user
                            )
                        )
                    }
                }

            launcherApps.registerCallback(
                callback
            )

            awaitClose {
                launcherApps.unregisterCallback(
                    callback
                )
            }
        }

    /**
     * Full app discovery is intentionally performed off the main thread.
     *
     * Loading every launcher activity also loads each icon. Doing that on
     * Compose's main coroutine can delay the first Home frame after Android
     * recreates the launcher process.
     */
    suspend fun getInstalledApps(): List<InstalledApp> =
        withContext(
            Dispatchers.IO
        ) {
            loadInstalledApps()
        }

    fun launch(
        app: InstalledApp
    ) {
        launcherApps.startMainActivity(
            app.componentName,
            app.user,
            null,
            null
        )
    }

    private fun loadInstalledApps(): List<InstalledApp> {
        val appNameCollator =
            Collator
                .getInstance(
                    Locale.getDefault()
                )
                .apply {
                    strength =
                        Collator.PRIMARY
                }

        return launcherApps.profiles
            .flatMap { user ->
                launcherApps
                    .getActivityList(
                        null,
                        user
                    )
                    .asSequence()
                    .filter {
                            activityInfo ->

                        activityInfo
                            .applicationInfo
                            .packageName !=
                            context.packageName
                    }
                    .map {
                            activityInfo ->

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
                    .toList()
            }
            .sortedWith {
                    first,
                    second ->

                appNameCollator.compare(
                    normalizeLabelForSorting(
                        first.label
                    ),
                    normalizeLabelForSorting(
                        second.label
                    )
                )
            }
    }

    private fun normalizeLabelForSorting(
        label: String
    ): String {
        return Normalizer
            .normalize(
                label,
                Normalizer.Form.NFKC
            )
            .replace(
                Regex(
                    "\\p{Cf}"
                ),
                ""
            )
            .trim()
    }
}

