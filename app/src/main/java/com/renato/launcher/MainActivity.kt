package com.renato.launcher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.renato.launcher.apps.AppRepository
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.LauncherDatabase
import com.renato.launcher.data.database.favorite.FavoriteEntity
import com.renato.launcher.data.database.recent.RecentAppEntity
import com.renato.launcher.data.database.recent.RecentSearchEntity
import com.renato.launcher.favorites.FavoritePickerScreen
import com.renato.launcher.favorites.FavoriteRepository
import com.renato.launcher.home.HomeScreen
import com.renato.launcher.recents.RecentRepository
import com.renato.launcher.search.SearchScreen
import com.renato.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

private enum class LauncherScreen {
    HOME,
    FAVORITES,
    SEARCH
}

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        window.addFlags(
            WindowManager
                .LayoutParams
                .FLAG_SHOW_WALLPAPER
        )

        window
            .setBackgroundDrawableResource(
                android.R.color.transparent
            )

        setContent {
            LauncherTheme {
                val roleManager =
                    getSystemService(
                        RoleManager::class.java
                    )

                val appRepository =
                    remember {
                        AppRepository(
                            applicationContext
                        )
                    }

                val database =
                    remember {
                        LauncherDatabase
                            .getInstance(
                                applicationContext
                            )
                    }

                val favoriteRepository =
                    remember {
                        FavoriteRepository(
                            context =
                                applicationContext,
                            favoriteDao =
                                database
                                    .favoriteDao()
                        )
                    }

                val recentRepository =
                    remember {
                        RecentRepository(
                            context =
                                applicationContext,
                            recentDao =
                                database
                                    .recentDao()
                        )
                    }

                val coroutineScope =
                    rememberCoroutineScope()

                val catalogRefreshMutex =
                    remember {
                        Mutex()
                    }

                var isHomeApp by
                    remember {
                        mutableStateOf(
                            roleManager
                                .isRoleHeld(
                                    RoleManager
                                        .ROLE_HOME
                                )
                        )
                    }

                var currentScreen by
                    remember {
                        mutableStateOf(
                            LauncherScreen
                                .HOME
                        )
                    }

                /*
                 * Full catalog used by Search and Favorite Picker.
                 *
                 * It is deliberately independent from Home favorites.
                 */
                var installedApps by
                    remember {
                        mutableStateOf(
                            emptyList<
                                InstalledApp
                            >()
                        )
                    }

                var installedAppsLoaded by
                    remember {
                        mutableStateOf(
                            false
                        )
                    }

                var savedFavorites by
                    remember {
                        mutableStateOf(
                            emptyList<
                                FavoriteEntity
                            >()
                        )
                    }

                var savedFavoritesLoaded by
                    remember {
                        mutableStateOf(
                            false
                        )
                    }

                /*
                 * Home has its own resolved list so it never needs to wait
                 * for the complete installed-app catalog.
                 */
                var favoriteApps by
                    remember {
                        mutableStateOf(
                            emptyList<
                                InstalledApp
                            >()
                        )
                    }

                var favoriteAppsLoaded by
                    remember {
                        mutableStateOf(
                            false
                        )
                    }

                /*
                 * Package changes can update labels/icons without changing
                 * the Room favorites rows. Incrementing this value forces a
                 * small direct re-resolution of only the Home favorites.
                 */
                var favoriteRefreshRevision by
                    remember {
                        mutableStateOf(
                            0
                        )
                    }

                var savedRecentApps by
                    remember {
                        mutableStateOf(
                            emptyList<
                                RecentAppEntity
                            >()
                        )
                    }

                var savedRecentSearches by
                    remember {
                        mutableStateOf(
                            emptyList<
                                RecentSearchEntity
                            >()
                        )
                    }

                val homeRoleLauncher =
                    rememberLauncherForActivityResult(
                        contract =
                            ActivityResultContracts
                                .StartActivityForResult()
                    ) {
                        isHomeApp =
                            roleManager
                                .isRoleHeld(
                                    RoleManager
                                        .ROLE_HOME
                                )
                    }

                suspend fun refreshInstalledApps() {
                    catalogRefreshMutex
                        .withLock {
                            installedApps =
                                appRepository
                                    .getInstalledApps()

                            installedAppsLoaded =
                                true
                        }
                }

                /*
                 * Listen for package changes for the whole lifetime of the
                 * active launcher.
                 *
                 * The callback starts before the initial full catalog load,
                 * while Mutex serializes catalog refreshes so an older scan
                 * cannot win over a newer package event.
                 */
                LaunchedEffect(
                    isHomeApp,
                    appRepository,
                    favoriteRepository
                ) {
                    if (
                        !isHomeApp
                    ) {
                        return@LaunchedEffect
                    }

                    launch {
                        appRepository
                            .appChanges
                            .collect {
                                    change ->

                                if (
                                    change.type ==
                                    AppRepository
                                        .AppChange
                                        .Type
                                        .REMOVED
                                ) {
                                    /*
                                     * Optimistic Home update immediately
                                     * after Android confirms the uninstall.
                                     */
                                    favoriteApps =
                                        favoriteApps
                                            .filterNot {
                                                    app ->

                                                app.user ==
                                                    change.user &&
                                                app.packageName in
                                                    change.packageNames
                                            }

                                    change.packageNames
                                        .forEach {
                                                packageName ->

                                            favoriteRepository
                                                .removePackage(
                                                    packageName =
                                                        packageName,
                                                    user =
                                                        change.user
                                                )
                                        }
                                }

                                favoriteRefreshRevision +=
                                    1

                                refreshInstalledApps()
                            }
                    }

                    /*
                     * Give the callback collector a chance to register first,
                     * then start the full app scan off the main thread.
                     */
                    yield()

                    refreshInstalledApps()
                }

                LaunchedEffect(
                    favoriteRepository
                ) {
                    favoriteRepository
                        .favorites
                        .collect {
                                favorites ->

                            savedFavorites =
                                favorites

                            savedFavoritesLoaded =
                                true
                        }
                }

                /*
                 * Critical Home path:
                 *
                 * Room favorites -> resolve only those launcher activities.
                 *
                 * This can finish long before getInstalledApps() has scanned
                 * every package and loaded every icon.
                 */
                LaunchedEffect(
                    savedFavorites,
                    savedFavoritesLoaded,
                    favoriteRefreshRevision,
                    favoriteRepository
                ) {
                    if (
                        !savedFavoritesLoaded
                    ) {
                        return@LaunchedEffect
                    }

                    favoriteApps =
                        favoriteRepository
                            .resolveFavorites(
                                savedFavorites =
                                    savedFavorites
                            )

                    favoriteAppsLoaded =
                        true
                }

                LaunchedEffect(
                    recentRepository
                ) {
                    launch {
                        recentRepository
                            .recentApps
                            .collect {
                                    recentApps ->

                                savedRecentApps =
                                    recentApps
                            }
                    }

                    launch {
                        recentRepository
                            .recentSearches
                            .collect {
                                    recentSearches ->

                                savedRecentSearches =
                                    recentSearches
                            }
                    }
                }

                val recentSections =
                    remember(
                        savedRecentApps,
                        savedRecentSearches,
                        installedApps
                    ) {
                        recentRepository
                            .buildSections(
                                savedRecentApps =
                                    savedRecentApps,
                                savedRecentSearches =
                                    savedRecentSearches,
                                installedApps =
                                    installedApps
                            )
                    }

                if (
                    isHomeApp
                ) {
                    when (
                        currentScreen
                    ) {
                        LauncherScreen.HOME -> {
                            HomeScreen(
                                favoriteApps =
                                    favoriteApps,
                                favoritesLoaded =
                                    favoriteAppsLoaded,
                                onAppClick = {
                                        app ->

                                    appRepository
                                        .launch(
                                            app
                                        )

                                    coroutineScope
                                        .launch {
                                            recentRepository
                                                .recordLaunch(
                                                    app
                                                )
                                        }
                                },
                                onChooseFavorites = {
                                    currentScreen =
                                        LauncherScreen
                                            .FAVORITES
                                },
                                onEditFavorites = {
                                    currentScreen =
                                        LauncherScreen
                                            .FAVORITES
                                },
                                onOpenSearch = {
                                    currentScreen =
                                        LauncherScreen
                                            .SEARCH
                                },
                                onAppInfo = {
                                        app ->

                                    openAppInfo(
                                        app
                                    )
                                },
                                onRemoveFavorite = {
                                        app ->

                                    val remainingFavorites =
                                        favoriteApps
                                            .filterNot {
                                                    favorite ->

                                                isSameApp(
                                                    first =
                                                        favorite,
                                                    second =
                                                        app
                                                )
                                            }

                                    /*
                                     * Update Home immediately; Room then
                                     * becomes the persistent source of truth.
                                     */
                                    favoriteApps =
                                        remainingFavorites

                                    coroutineScope
                                        .launch {
                                            favoriteRepository
                                                .replaceFavorites(
                                                    remainingFavorites
                                                )
                                        }
                                },
                                onUninstallApp = {
                                        app ->

                                    requestAppUninstall(
                                        app
                                    )
                                }
                            )
                        }

                        LauncherScreen.FAVORITES -> {
                            FavoritePickerScreen(
                                apps =
                                    installedApps,
                                initialSelection =
                                    favoriteApps,
                                onCancel = {
                                    currentScreen =
                                        LauncherScreen
                                            .HOME
                                },
                                onSave = {
                                        selectedApps ->

                                    /*
                                     * Home can show the selected order on the
                                     * very next frame instead of waiting for
                                     * the Room Flow round trip.
                                     */
                                    favoriteApps =
                                        selectedApps

                                    favoriteAppsLoaded =
                                        true

                                    currentScreen =
                                        LauncherScreen
                                            .HOME

                                    coroutineScope
                                        .launch {
                                            favoriteRepository
                                                .replaceFavorites(
                                                    selectedApps
                                                )
                                        }
                                }
                            )
                        }

                        LauncherScreen.SEARCH -> {
                            SearchScreen(
                                apps =
                                    installedApps,
                                recentApps =
                                    recentSections
                                        .recentApps,
                                recentSearchApps =
                                    recentSections
                                        .searchedApps,
                                favoriteApps =
                                    favoriteApps,
                                onAppClick = {
                                        app,
                                        recordAsSearch ->

                                    currentScreen =
                                        LauncherScreen
                                            .HOME

                                    appRepository
                                        .launch(
                                            app
                                        )

                                    coroutineScope
                                        .launch {
                                            recentRepository
                                                .recordLaunch(
                                                    app
                                                )

                                            if (
                                                recordAsSearch
                                            ) {
                                                recentRepository
                                                    .recordSearchLaunch(
                                                        app
                                                    )
                                            }
                                        }
                                },
                                onAppInfo = {
                                        app ->

                                    openAppInfo(
                                        app
                                    )
                                },
                                onRemoveFavorite = {
                                        app ->

                                    val remainingFavorites =
                                        favoriteApps
                                            .filterNot {
                                                    favorite ->

                                                isSameApp(
                                                    first =
                                                        favorite,
                                                    second =
                                                        app
                                                )
                                            }

                                    favoriteApps =
                                        remainingFavorites

                                    coroutineScope
                                        .launch {
                                            favoriteRepository
                                                .replaceFavorites(
                                                    remainingFavorites
                                                )
                                        }
                                },
                                onUninstallApp = {
                                        app ->

                                    requestAppUninstall(
                                        app
                                    )
                                },
                                onBack = {
                                    currentScreen =
                                        LauncherScreen
                                            .HOME
                                }
                            )
                        }
                    }
                } else {
                    DefaultLauncherSetupScreen(
                        onSetDefaultLauncher = {
                            val intent =
                                roleManager
                                    .createRequestRoleIntent(
                                        RoleManager
                                            .ROLE_HOME
                                    )

                            homeRoleLauncher
                                .launch(
                                    intent
                                )
                        }
                    )
                }
            }
        }
    }

    private fun openAppInfo(
        app: InstalledApp
    ) {
        val intent =
            Intent(
                Settings
                    .ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data =
                    Uri.parse(
                        "package:${app.packageName}"
                    )
            }

        startActivity(
            intent
        )
    }

    /**
     * Opens Android's official uninstall confirmation UI.
     *
     * REQUEST_DELETE_PACKAGES is declared in the manifest. Android still owns
     * the confirmation dialog; the launcher never silently removes an app.
     */
    private fun requestAppUninstall(
        app: InstalledApp
    ) {
        val packageUri =
            Uri.parse(
                "package:${app.packageName}"
            )

        val uninstallIntent =
            Intent(
                Intent.ACTION_UNINSTALL_PACKAGE,
                packageUri
            )

        try {
            startActivity(
                uninstallIntent
            )
        } catch (
            _: ActivityNotFoundException
        ) {
            startActivity(
                Intent(
                    Intent.ACTION_DELETE,
                    packageUri
                )
            )
        }
    }
}

@Composable
private fun DefaultLauncherSetupScreen(
    onSetDefaultLauncher:
        () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text =
                "Launcher is not the default Home app",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Button(
            onClick =
                onSetDefaultLauncher
        ) {
            Text(
                text =
                    "Set as default launcher"
            )
        }
    }
}

private fun isSameApp(
    first: InstalledApp,
    second: InstalledApp
): Boolean {
    return first.componentName ==
        second.componentName &&
        first.user ==
            second.user
}

