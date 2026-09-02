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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.renato.launcher.allapps.AllAppsScreen
import com.renato.launcher.apps.AppRepository
import com.renato.launcher.collections.CollectionEditorScreen
import com.renato.launcher.collections.CollectionManagerScreen
import com.renato.launcher.collections.CollectionRepository
import com.renato.launcher.collections.MAX_COLLECTIONS
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.LauncherMutationQueue
import com.renato.launcher.data.database.LauncherDatabase
import com.renato.launcher.data.database.collection.CollectionAppEntity
import com.renato.launcher.data.database.collection.CollectionEntity
import com.renato.launcher.data.database.favorite.FavoriteEntity
import com.renato.launcher.data.database.recent.RecentAppEntity
import com.renato.launcher.data.database.recent.RecentSearchEntity
import com.renato.launcher.favorites.FavoritePickerScreen
import com.renato.launcher.favorites.FavoriteRepository
import com.renato.launcher.home.HomeScreen
import com.renato.launcher.recents.RecentRepository
import com.renato.launcher.search.SearchScreen
import com.renato.launcher.ui.components.LauncherPrimaryActionButton
import com.renato.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

private enum class LauncherScreen {
    HOME,
    FAVORITES,
    COLLECTIONS,
    COLLECTION_EDITOR,
    SEARCH,
    ALL_APPS
}

class MainActivity :
    ComponentActivity() {

    private var homeRequestRevision by
        mutableIntStateOf(
            0
        )

    /*
     * Activity lifecycle revision used to re-check whether this process still
     * owns ROLE_HOME after returning from Settings or another external screen.
     *
     * The default launcher can be changed while this singleTask Activity is
     * alive, so the value captured during onCreate is not sufficient.
     */
    private var resumeRevision by
        mutableIntStateOf(
            0
        )

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(
            intent
        )

        setIntent(
            intent
        )

        if (
            intent.action ==
                Intent.ACTION_MAIN &&
            intent.hasCategory(
                Intent.CATEGORY_HOME
            )
        ) {
            homeRequestRevision +=
                1
        }
    }

    override fun onResume() {
        super.onResume()

        resumeRevision +=
            1
    }

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

                val collectionRepository =
                    remember {
                        CollectionRepository(
                            context =
                                applicationContext,
                            collectionDao =
                                database
                                    .collectionDao()
                        )
                    }

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

                var editingCollectionId by
                    remember {
                        mutableStateOf<Long?>(
                            null
                        )
                    }

                var collectionEditorReturnScreen by
                    remember {
                        mutableStateOf(
                            LauncherScreen
                                .COLLECTIONS
                        )
                    }

                /*
                 * Re-check ROLE_HOME every time the Activity resumes.
                 *
                 * This covers changing the default Home application from
                 * Android Settings while our singleTask Activity remains alive.
                 * If the role was lost, discard transient launcher navigation
                 * so regaining the role always starts from a clean Home state.
                 */
                LaunchedEffect(
                    resumeRevision
                ) {
                    val currentlyHome =
                        roleManager
                            .isRoleHeld(
                                RoleManager
                                    .ROLE_HOME
                            )

                    if (
                        !currentlyHome
                    ) {
                        editingCollectionId =
                            null

                        collectionEditorReturnScreen =
                            LauncherScreen
                                .COLLECTIONS

                        currentScreen =
                            LauncherScreen
                                .HOME
                    }

                    isHomeApp =
                        currentlyHome
                }

                /*
                 * Android's Home button must always mean Home, even while the
                 * launcher is showing Search, an editor, a collection manager,
                 * or a collection bottom sheet.
                 *
                 * Unsaved editor state is intentionally discarded because the
                 * screen leaves composition without calling its save callback.
                 */
                LaunchedEffect(
                    homeRequestRevision
                ) {
                    if (
                        homeRequestRevision >
                        0
                    ) {
                        editingCollectionId =
                            null

                        collectionEditorReturnScreen =
                            LauncherScreen
                                .COLLECTIONS

                        currentScreen =
                            LauncherScreen
                                .HOME
                    }
                }

                /*
                 * Full catalog used by Search, All Apps and Favorite Picker.
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

                var savedCollections by
                    remember {
                        mutableStateOf(
                            emptyList<
                                CollectionEntity
                            >()
                        )
                    }

                var savedCollectionApps by
                    remember {
                        mutableStateOf(
                            emptyList<
                                CollectionAppEntity
                            >()
                        )
                    }


                var savedCollectionsLoaded by
                    remember {
                        mutableStateOf(
                            false
                        )
                    }

                var savedCollectionAppsLoaded by
                    remember {
                        mutableStateOf(
                            false
                        )
                    }

                /*
                 * Collections use the same fast Home strategy as Favorites:
                 * resolve only saved members, without waiting for the full
                 * installed-app catalog.
                 */
                var collectionAppsById by
                    remember {
                        mutableStateOf(
                            emptyMap<
                                Long,
                                List<InstalledApp>
                            >()
                        )
                    }

                var collectionAppsLoaded by
                    remember {
                        mutableStateOf(
                            false
                        )
                    }

                var collectionRefreshRevision by
                    remember {
                        mutableStateOf(
                            0
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
                    favoriteRepository,
                    collectionRepository
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

                                    collectionAppsById =
                                        collectionAppsById
                                            .mapValues {
                                                    (_, apps) ->

                                                apps.filterNot {
                                                        app ->

                                                    app.user ==
                                                        change.user &&
                                                    app.packageName in
                                                        change.packageNames
                                                }
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

                                            collectionRepository
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

                                collectionRefreshRevision +=
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

                LaunchedEffect(
                    collectionRepository
                ) {
                    launch {
                        collectionRepository
                            .collections
                            .collect {
                                    collections ->

                                savedCollections =
                                    collections

                                savedCollectionsLoaded =
                                    true
                            }
                    }

                    launch {
                        collectionRepository
                            .collectionApps
                            .collect {
                                    collectionApps ->

                                savedCollectionApps =
                                    collectionApps

                                savedCollectionAppsLoaded =
                                    true
                            }
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
                    savedCollectionApps,
                    savedCollectionAppsLoaded,
                    collectionRefreshRevision,
                    collectionRepository
                ) {
                    if (
                        !savedCollectionAppsLoaded
                    ) {
                        return@LaunchedEffect
                    }

                    collectionAppsById =
                        collectionRepository
                            .resolveCollectionApps(
                                savedApps =
                                    savedCollectionApps
                            )

                    collectionAppsLoaded =
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
                            key(
                                homeRequestRevision
                            ) {
                                HomeScreen(
                                favoriteApps =
                                    favoriteApps,
                                favoritesLoaded =
                                    favoriteAppsLoaded,
                                collections =
                                    savedCollections,
                                collectionsLoaded =
                                    savedCollectionsLoaded &&
                                        savedCollectionAppsLoaded &&
                                        collectionAppsLoaded,
                                collectionAppsById =
                                    collectionAppsById,
                                onAppClick = {
                                        app ->

                                    appRepository
                                        .launch(
                                            app
                                        )

                                    LauncherMutationQueue
                                        .submit {
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

                                    favoriteApps =
                                        remainingFavorites

                                    LauncherMutationQueue
                                        .submit {
                                            favoriteRepository
                                                .replaceFavorites(
                                                    remainingFavorites
                                                )
                                        }
                                },
                                onReorderFavorites = {
                                        reorderedApps ->

                                    favoriteApps =
                                        reorderedApps

                                    LauncherMutationQueue
                                        .submit {
                                            favoriteRepository
                                                .replaceFavorites(
                                                    reorderedApps
                                                )
                                        }
                                },
                                onEditCollection = {
                                        collectionId ->

                                    editingCollectionId =
                                        collectionId

                                    collectionEditorReturnScreen =
                                        LauncherScreen
                                            .HOME

                                    currentScreen =
                                        LauncherScreen
                                            .COLLECTION_EDITOR
                                },
                                onDeleteCollection = {
                                        collectionId ->

                                    LauncherMutationQueue
                                        .submit {
                                            collectionRepository
                                                .deleteCollection(
                                                    collectionId =
                                                        collectionId
                                                )
                                        }
                                },
                                onReorderCollections = {
                                        reorderedCollections ->

                                    savedCollections =
                                        reorderedCollections

                                    LauncherMutationQueue
                                        .submit {
                                            collectionRepository
                                                .replaceCollectionOrder(
                                                    reorderedCollections
                                                )
                                        }
                                },
                                onReorderCollectionApps = {
                                        collectionId,
                                        reorderedApps ->

                                    collectionAppsById =
                                        collectionAppsById +
                                            (
                                                collectionId to
                                                    reorderedApps
                                            )

                                    LauncherMutationQueue
                                        .submit {
                                            collectionRepository
                                                .replaceCollectionApps(
                                                    collectionId =
                                                        collectionId,
                                                    apps =
                                                        reorderedApps
                                                )
                                        }
                                },
                                onRemoveAppFromCollection = {
                                        collectionId,
                                        app ->

                                    val remainingApps =
                                        collectionAppsById[
                                            collectionId
                                        ]
                                            .orEmpty()
                                            .filterNot {
                                                isSameApp(
                                                    first =
                                                        it,
                                                    second =
                                                        app
                                                )
                                            }

                                    collectionAppsById =
                                        collectionAppsById +
                                            (
                                                collectionId to
                                                    remainingApps
                                            )

                                    LauncherMutationQueue
                                        .submit {
                                            collectionRepository
                                                .replaceCollectionApps(
                                                    collectionId =
                                                        collectionId,
                                                    apps =
                                                        remainingApps
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
                        }

                        LauncherScreen.FAVORITES -> {
                            FavoritePickerScreen(
                                apps =
                                    installedApps,
                                initialSelection =
                                    favoriteApps,
                                onManageCollections = {
                                    currentScreen =
                                        LauncherScreen
                                            .COLLECTIONS
                                },
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

                                    LauncherMutationQueue
                                        .submit {
                                            favoriteRepository
                                                .replaceFavorites(
                                                    selectedApps
                                                )
                                        }
                                }
                            )
                        }

                        LauncherScreen.COLLECTIONS -> {
                            CollectionManagerScreen(
                                collections =
                                    savedCollections,
                                collectionMembers =
                                    savedCollectionApps,
                                resolvedAppsByCollection =
                                    collectionAppsById,
                                catalogLoaded =
                                    installedAppsLoaded,
                                onBack = {
                                    currentScreen =
                                        LauncherScreen
                                            .FAVORITES
                                },
                                onCreateCollection = {
                                    if (
                                        savedCollections.size <
                                        MAX_COLLECTIONS
                                    ) {
                                        editingCollectionId =
                                            null

                                        collectionEditorReturnScreen =
                                            LauncherScreen
                                                .COLLECTIONS

                                        currentScreen =
                                            LauncherScreen
                                                .COLLECTION_EDITOR
                                    }
                                },
                                onEditCollection = {
                                        collectionId ->

                                    editingCollectionId =
                                        collectionId

                                    collectionEditorReturnScreen =
                                        LauncherScreen
                                            .COLLECTIONS

                                    currentScreen =
                                        LauncherScreen
                                            .COLLECTION_EDITOR
                                },
                                onDeleteCollection = {
                                        collectionId ->

                                    LauncherMutationQueue
                                        .submit {
                                            collectionRepository
                                                .deleteCollection(
                                                    collectionId =
                                                        collectionId
                                                )
                                        }
                                }
                            )
                        }

                        LauncherScreen.COLLECTION_EDITOR -> {
                            val editingCollection =
                                editingCollectionId
                                    ?.let {
                                            collectionId ->

                                        savedCollections
                                            .firstOrNull {
                                                    collection ->

                                                collection.id ==
                                                    collectionId
                                            }
                                    }

                            CollectionEditorScreen(
                                apps =
                                    installedApps,
                                initialName =
                                    editingCollection
                                        ?.name
                                        .orEmpty(),
                                initialSelection =
                                    editingCollection
                                        ?.let {
                                                collection ->

                                            collectionAppsById[
                                                collection.id
                                            ].orEmpty()
                                        }
                                        .orEmpty(),
                                isEditing =
                                    editingCollection !=
                                        null,
                                onCancel = {
                                    editingCollectionId =
                                        null

                                    currentScreen =
                                        collectionEditorReturnScreen
                                },
                                onSave = {
                                        name,
                                        selectedApps ->

                                    val collectionId =
                                        editingCollection
                                            ?.id

                                    val canCreateCollection =
                                        savedCollections.size <
                                            MAX_COLLECTIONS

                                    editingCollectionId =
                                        null

                                    currentScreen =
                                        collectionEditorReturnScreen

                                    LauncherMutationQueue
                                        .submit {
                                            if (
                                                collectionId ==
                                                null
                                            ) {
                                                if (
                                                    canCreateCollection
                                                ) {
                                                    collectionRepository
                                                        .createCollection(
                                                            name =
                                                                name,
                                                            apps =
                                                                selectedApps
                                                        )
                                                }
                                            } else {
                                                collectionRepository
                                                    .updateCollection(
                                                        collectionId =
                                                            collectionId,
                                                        name =
                                                            name,
                                                        apps =
                                                            selectedApps
                                                    )
                                            }
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

                                    LauncherMutationQueue
                                        .submit {
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

                                    LauncherMutationQueue
                                        .submit {
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
                                onOpenAllApps = {
                                    currentScreen =
                                        LauncherScreen
                                            .ALL_APPS
                                },
                                onBack = {
                                    currentScreen =
                                        LauncherScreen
                                            .HOME
                                }
                            )
                        }

                        LauncherScreen.ALL_APPS -> {
                            AllAppsScreen(
                                apps =
                                    installedApps,
                                appsLoaded =
                                    installedAppsLoaded,
                                onAppClick = {
                                        app ->

                                    currentScreen =
                                        LauncherScreen
                                            .HOME

                                    appRepository
                                        .launch(
                                            app
                                        )

                                    LauncherMutationQueue
                                        .submit {
                                            recentRepository
                                                .recordLaunch(
                                                    app
                                                )
                                        }
                                },
                                onAppInfo = {
                                        app ->

                                    openAppInfo(
                                        app
                                    )
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
                                            .SEARCH
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

        LauncherPrimaryActionButton(
            text =
                "Set as default launcher",
            onClick =
                onSetDefaultLauncher
        )
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
