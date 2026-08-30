package com.renato.launcher

import android.app.role.RoleManager
import android.os.Bundle
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
import com.renato.launcher.favorites.FavoritePickerScreen
import com.renato.launcher.favorites.FavoriteRepository
import com.renato.launcher.home.HomeScreen
import com.renato.launcher.search.SearchScreen
import com.renato.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

                val coroutineScope =
                    rememberCoroutineScope()

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
                            LauncherScreen.HOME
                        )
                    }

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

                LaunchedEffect(
                    isHomeApp
                ) {
                    if (isHomeApp) {
                        installedAppsLoaded =
                            false

                        installedApps =
                            appRepository
                                .getInstalledApps()

                        installedAppsLoaded =
                            true
                    }
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

                val favoriteApps =
                    remember(
                        savedFavorites,
                        installedApps
                    ) {
                        favoriteRepository
                            .resolveFavorites(
                                savedFavorites =
                                    savedFavorites,
                                installedApps =
                                    installedApps
                            )
                    }

                if (isHomeApp) {

                    when (
                        currentScreen
                    ) {

                        LauncherScreen.HOME -> {

                            HomeScreen(
                                favoriteApps =
                                    favoriteApps,
                                favoritesLoaded =
                                    installedAppsLoaded &&
                                        savedFavoritesLoaded,
                                onAppClick =
                                    appRepository::launch,
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

                                    coroutineScope
                                        .launch {
                                            favoriteRepository
                                                .replaceFavorites(
                                                    selectedApps
                                                )

                                            currentScreen =
                                                LauncherScreen
                                                    .HOME
                                        }
                                }
                            )
                        }

                        LauncherScreen.SEARCH -> {

                            SearchScreen(
                                apps =
                                    installedApps,
                                onAppClick = {
                                        app ->

                                    /*
                                     * Reset launcher to Home
                                     * BEFORE launching the app.
                                     *
                                     * Therefore pressing Home
                                     * later returns to Home,
                                     * not the previous Search.
                                     */
                                    currentScreen =
                                        LauncherScreen
                                            .HOME

                                    appRepository
                                        .launch(
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
