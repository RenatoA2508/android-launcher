package com.renato.launcher.search

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.ui.components.AppContextMenu
import com.renato.launcher.ui.components.LauncherSearchBar
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppCombinedClickable

@Composable
fun SearchScreen(
    apps: List<InstalledApp>,
    recentApps: List<InstalledApp>,
    recentSearchApps: List<InstalledApp>,
    favoriteApps: List<InstalledApp>,
    onAppClick: (
        InstalledApp,
        Boolean
    ) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onRemoveFavorite: (InstalledApp) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit,
    onBack: () -> Unit
) {
    SearchWindowEffect()

    var query by remember {
        mutableStateOf("")
    }

    val focusRequester =
        remember {
            FocusRequester()
        }

    val focusManager =
        LocalFocusManager.current

    val keyboardController =
        LocalSoftwareKeyboardController.current

    val gridState =
        rememberLazyGridState()

    val density =
        LocalDensity.current

    val swipeDownThresholdPx =
        remember(density) {
            with(density) {
                72.dp.toPx()
            }
        }

    /*
     * Shared search engine.
     *
     * The index is built only when the installed
     * application list changes.
     */
    val searchEngine =
        remember(apps) {
            AppSearchEngine(
                apps =
                    apps
            )
        }

    /*
     * Search now consumes exactly the same ranking
     * engine as FavoritePickerScreen.
     */
    val results =
        remember(
            searchEngine,
            query
        ) {
            searchEngine.search(
                query =
                    query
            )
        }

    val favoriteAppKeys =
        remember(
            favoriteApps
        ) {
            favoriteApps
                .mapTo(
                    HashSet()
                ) { app ->
                    appKey(
                        app
                    )
                }
        }

    /*
     * Shared launcher icon cache.
     *
     * Empty Search:
     * preload immediately visible history.
     *
     * Typed Search:
     * preload the first results.
     */
    val iconsToPreload =
        if (query.isBlank()) {

            recentApps +
                recentSearchApps

        } else {

            results.take(
                12
            )
        }

    PreloadLauncherAppIcons(
        apps =
            iconsToPreload
                .distinctBy {
                    appKey(it)
                }
    )

    fun closeSearch() {

        keyboardController?.hide()

        focusManager.clearFocus()

        onBack()
    }

    /*
     * Swipe down closes Search only when the grid is
     * already at its top.
     */
    val swipeDownConnection =
        remember(
            gridState,
            swipeDownThresholdPx,
            keyboardController,
            focusManager,
            onBack
        ) {

            object :
                NestedScrollConnection {

                var accumulatedDownwardDrag =
                    0f

                var closeTriggered =
                    false

                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {

                    val isAtTop =
                        gridState
                            .firstVisibleItemIndex == 0 &&
                            gridState
                                .firstVisibleItemScrollOffset == 0

                    if (
                        source ==
                            NestedScrollSource.UserInput &&
                        isAtTop &&
                        available.y > 0f
                    ) {

                        accumulatedDownwardDrag +=
                            available.y

                        if (
                            !closeTriggered &&
                            accumulatedDownwardDrag >=
                                swipeDownThresholdPx
                        ) {

                            closeTriggered =
                                true

                            keyboardController
                                ?.hide()

                            focusManager
                                .clearFocus()

                            onBack()
                        }

                    } else if (
                        available.y < 0f ||
                        !isAtTop
                    ) {

                        accumulatedDownwardDrag =
                            0f

                        closeTriggered =
                            false
                    }

                    /*
                     * Observe the drag but don't consume
                     * it, so LazyVerticalGrid continues
                     * scrolling normally.
                     */
                    return Offset.Zero
                }

                override suspend fun onPreFling(
                    available: Velocity
                ): Velocity {

                    accumulatedDownwardDrag =
                        0f

                    closeTriggered =
                        false

                    return Velocity.Zero
                }
            }
        }

    BackHandler {
        closeSearch()
    }

    /*
     * Search startup:
     *
     * frame 1 -> compose
     * frame 2 -> request focus
     * frame 3 -> show keyboard
     */
    LaunchedEffect(Unit) {

        withFrameNanos { }

        focusRequester.requestFocus()

        withFrameNanos { }

        keyboardController?.show()
    }

    /*
     * New queries always begin at the top of the
     * result grid.
     */
    LaunchedEffect(query) {

        if (
            query.isNotBlank() &&
            (
                gridState
                    .firstVisibleItemIndex > 0 ||
                    gridState
                        .firstVisibleItemScrollOffset > 0
                )
        ) {

            gridState.scrollToItem(
                0
            )
        }
    }

    /*
     * testTagsAsResourceId keeps Search discoverable
     * by UI Automator for Baseline Profile generation.
     */
    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics {
                    testTagsAsResourceId =
                        true
                }
                .testTag(
                    SEARCH_ROOT_TAG
                ),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha = 0.82f
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {

            LauncherSearchBar(
                query =
                    query,
                onQueryChange = {
                    query = it
                },
                focusRequester =
                    focusRequester,
                onBack = {
                    closeSearch()
                },
                onClear = {
                    query = ""
                },
                onSubmit = {

                    results
                        .firstOrNull()
                        ?.let { app ->

                            onAppClick(
                                app,
                                true
                            )
                        }
                },
                fieldTestTag =
                    SEARCH_FIELD_TAG
            )

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(
                        4
                    ),
                state =
                    gridState,
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxWidth()
                        .imePadding()
                        .nestedScroll(
                            swipeDownConnection
                        ),
                contentPadding =
                    PaddingValues(
                        start =
                            20.dp,
                        end =
                            20.dp,
                        top =
                            6.dp,
                        bottom =
                            16.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                /*
                 * =================================================
                 * EMPTY QUERY
                 * =================================================
                 */

                if (query.isBlank()) {

                    if (
                        recentApps.isEmpty() &&
                        recentSearchApps.isEmpty()
                    ) {

                        item(
                            key =
                                "empty-search",
                            contentType =
                                "message",
                            span = {
                                GridItemSpan(
                                    maxLineSpan
                                )
                            }
                        ) {

                            EmptySearchState()
                        }

                    } else {

                        /*
                         * RECENT APPS
                         */
                        if (
                            recentApps.isNotEmpty()
                        ) {

                            item(
                                key =
                                    "recent-heading",
                                contentType =
                                    "heading",
                                span = {
                                    GridItemSpan(
                                        maxLineSpan
                                    )
                                }
                            ) {

                                SearchSectionHeader(
                                    title =
                                        "Recientes"
                                )
                            }

                            items(
                                items =
                                    recentApps,
                                key = { app ->

                                    "recent:" +
                                        appKey(
                                            app
                                        )
                                },
                                contentType = {
                                    "app"
                                }
                            ) { app ->

                                SearchAppItem(
                                    app =
                                        app,
                                    isFavorite =
                                        appKey(app) in
                                            favoriteAppKeys,
                                    onClick = {
                                        onAppClick(
                                            app,
                                            false
                                        )
                                    },
                                    onAppInfo = {
                                        onAppInfo(
                                            app
                                        )
                                    },
                                    onRemoveFavorite = {
                                        onRemoveFavorite(
                                            app
                                        )
                                    },
                                    onUninstallApp = {
                                        onUninstallApp(
                                            app
                                        )
                                    }
                                )
                            }
                        }

                        /*
                         * RECENTLY SEARCHED APPS
                         */
                        if (
                            recentSearchApps
                                .isNotEmpty()
                        ) {

                            item(
                                key =
                                    "recent-search-heading",
                                contentType =
                                    "heading",
                                span = {
                                    GridItemSpan(
                                        maxLineSpan
                                    )
                                }
                            ) {

                                SearchSectionHeader(
                                    title =
                                        "Buscadas recientemente"
                                )
                            }

                            items(
                                items =
                                    recentSearchApps,
                                key = { app ->

                                    "searched:" +
                                        appKey(
                                            app
                                        )
                                },
                                contentType = {
                                    "app"
                                }
                            ) { app ->

                                SearchAppItem(
                                    app =
                                        app,
                                    isFavorite =
                                        appKey(app) in
                                            favoriteAppKeys,
                                    onClick = {
                                        onAppClick(
                                            app,
                                            true
                                        )
                                    },
                                    onAppInfo = {
                                        onAppInfo(
                                            app
                                        )
                                    },
                                    onRemoveFavorite = {
                                        onRemoveFavorite(
                                            app
                                        )
                                    },
                                    onUninstallApp = {
                                        onUninstallApp(
                                            app
                                        )
                                    }
                                )
                            }
                        }
                    }

                } else if (
                    results.isEmpty()
                ) {

                    /*
                     * =================================================
                     * NO RESULTS
                     * =================================================
                     */
                    item(
                        key =
                            "no-results",
                        contentType =
                            "message",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {

                        NoResultsState(
                            query =
                                query
                        )
                    }

                } else {

                    /*
                     * =================================================
                     * SEARCH RESULTS
                     * =================================================
                     *
                     * No heading.
                     * No result count.
                     *
                     * Results begin immediately below
                     * the search bar.
                     */
                    itemsIndexed(
                        items =
                            results,
                        key = {
                                _,
                                app ->

                            "result:" +
                                appKey(
                                    app
                                )
                        },
                        contentType = {
                                _,
                                _ ->

                            "app"
                        }
                    ) {
                            index,
                            app ->

                        SearchAppItem(
                            app =
                                app,
                            testTag =
                                "$SEARCH_RESULT_TAG_PREFIX$index",
                            isFavorite =
                                appKey(app) in
                                    favoriteAppKeys,
                            onClick = {
                                onAppClick(
                                    app,
                                    true
                                )
                            },
                            onAppInfo = {
                                onAppInfo(
                                    app
                                )
                            },
                            onRemoveFavorite = {
                                onRemoveFavorite(
                                    app
                                )
                            },
                            onUninstallApp = {
                                onUninstallApp(
                                    app
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String
) {

    Text(
        text =
            title,
        modifier =
            Modifier.padding(
                top =
                    8.dp,
                bottom =
                    0.dp
            ),
        fontSize =
            17.sp,
        fontWeight =
            FontWeight.SemiBold,
        color =
            MaterialTheme
                .colorScheme
                .onSurface
    )
}

@Composable
private fun EmptySearchState() {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        40.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                "Buscar aplicaciones",
            fontSize =
                18.sp,
            fontWeight =
                FontWeight.Medium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        Text(
            text =
                "Empieza a escribir el nombre de una aplicación.",
            modifier =
                Modifier.padding(
                    horizontal =
                        24.dp
                ),
            textAlign =
                TextAlign.Center,
            fontSize =
                14.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun NoResultsState(
    query: String
) {

    Text(
        text =
            "No encontramos una aplicación para \"$query\".",
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        40.dp,
                    start =
                        24.dp,
                    end =
                        24.dp
                ),
        textAlign =
            TextAlign.Center,
        fontSize =
            14.sp,
        color =
            MaterialTheme
                .colorScheme
                .onSurfaceVariant
    )
}

@Composable
private fun SearchAppItem(
    app: InstalledApp,
    testTag: String? = null,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onAppInfo: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onUninstallApp: () -> Unit
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    val hapticFeedback =
        LocalHapticFeedback.current

    var menuExpanded by
        remember(
            app.componentName,
            app.user
        ) {
            mutableStateOf(
                false
            )
        }

    /*
     * Search tiles are intentionally a little squarer than Home.
     * The pressed state illuminates this rounded rectangle instead
     * of showing a circular spot in the middle of the cell.
     */
    val shape =
        RoundedCornerShape(
            14.dp
        )

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (
                            testTag != null
                        ) {
                            Modifier.testTag(
                                testTag
                            )
                        } else {
                            Modifier
                        }
                    )
                    .launcherAppCombinedClickable(
                        shape =
                            shape,
                        onClickLabel =
                            "Abrir ${app.label}",
                        onLongClickLabel =
                            "Opciones de ${app.label}",
                        onLongClick = {
                            hapticFeedback
                                .performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )

                            menuExpanded =
                                true
                        },
                        onClick =
                            onClick
                    )
                    .padding(
                        horizontal =
                            3.dp,
                        vertical =
                            6.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Image(
                bitmap =
                    iconBitmap,
                contentDescription =
                    app.label,
                modifier =
                    Modifier.size(
                        44.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    app.label,
                maxLines =
                    2,
                overflow =
                    TextOverflow.Ellipsis,
                textAlign =
                    TextAlign.Center,
                fontSize =
                    12.sp,
                lineHeight =
                    13.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }

        AppContextMenu(
            expanded =
                menuExpanded,
            app =
                app,
            showRemoveFromHome =
                isFavorite,
            onDismiss = {
                menuExpanded =
                    false
            },
            onAppInfo = {
                menuExpanded =
                    false

                onAppInfo()
            },
            onRemoveFavorite = {
                menuExpanded =
                    false

                onRemoveFavorite()
            },
            onUninstallApp = {
                menuExpanded =
                    false

                onUninstallApp()
            }
        )
    }
}

@Composable
private fun SearchWindowEffect() {

    val context =
        LocalContext.current

    val activity =
        remember(context) {
            context.findActivity()
        }

    DisposableEffect(
        activity
    ) {

        val window =
            activity?.window

        if (
            window != null &&
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
        ) {

            window.addFlags(
                WindowManager
                    .LayoutParams
                    .FLAG_BLUR_BEHIND
            )

            val attributes =
                window.attributes

            attributes
                .setBlurBehindRadius(
                    24
                )

            window.attributes =
                attributes
        }

        onDispose {

            if (
                window != null &&
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
            ) {

                val attributes =
                    window.attributes

                attributes
                    .setBlurBehindRadius(
                        0
                    )

                window.attributes =
                    attributes

                window.clearFlags(
                    WindowManager
                        .LayoutParams
                        .FLAG_BLUR_BEHIND
                )
            }
        }
    }
}

private fun Context.findActivity():
    Activity? {

    var currentContext =
        this

    while (
        currentContext is
            ContextWrapper
    ) {

        if (
            currentContext is Activity
        ) {
            return currentContext
        }

        currentContext =
            currentContext.baseContext
    }

    return null
}

private fun appKey(
    app: InstalledApp
): String {

    return "${app.user.hashCode()}:" +
        app.componentName
            .flattenToString()
}

/*
 * Stable automation identifiers used by
 * BaselineProfileGenerator.
 */
private const val SEARCH_ROOT_TAG =
    "launcher_search_root"

private const val SEARCH_FIELD_TAG =
    "launcher_search_field"

private const val SEARCH_RESULT_TAG_PREFIX =
    "launcher_search_result_"

