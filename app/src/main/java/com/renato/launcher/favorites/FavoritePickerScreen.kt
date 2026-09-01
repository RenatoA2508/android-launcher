package com.renato.launcher.favorites

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.search.AppSearchEngine
import com.renato.launcher.ui.components.LauncherSearchBar
import com.renato.launcher.ui.components.LauncherSearchLauncher
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FavoritePickerScreen(
    apps: List<InstalledApp>,
    initialSelection: List<InstalledApp>,
    onCancel: () -> Unit,
    onSave: (List<InstalledApp>) -> Unit
) {
    FavoritePickerWindowEffect()

    /*
     * Shared process-wide icon cache.
     *
     * No app item in this screen performs its own
     * Drawable -> Bitmap conversion.
     */
    PreloadLauncherAppIcons(
        apps =
            apps
    )

    /*
     * Shared search engine.
     *
     * This is exactly the same implementation used
     * by SearchScreen.
     */
    val searchEngine =
        remember(apps) {
            AppSearchEngine(
                apps =
                    apps
            )
        }

    val selectedApps =
        remember(initialSelection) {
            mutableStateListOf<InstalledApp>()
                .apply {
                    addAll(
                        initialSelection
                    )
                }
        }

    var searchMode by remember {
        mutableStateOf(
            false
        )
    }

    var searchQuery by remember {
        mutableStateOf(
            ""
        )
    }

    var searchExitPending by remember {
        mutableStateOf(
            false
        )
    }

    val normalGridState =
        rememberLazyGridState()

    val searchGridState =
        rememberLazyGridState()

    /*
     * Drag state belongs only to the selected-app section.
     *
     * The editor remains transactional:
     * dragging changes the local order immediately, but Room is
     * updated only when the user presses Listo. Cancelar therefore
     * still discards the entire edit session.
     */
    var draggedSelectedAppKey by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var dropTargetSelectedAppKey by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var draggedTranslation by
        remember {
            mutableStateOf(
                Offset.Zero
            )
        }

    var dragPointerInGrid by
        remember {
            mutableStateOf(
                Offset.Zero
            )
        }

    val hapticFeedback =
        LocalHapticFeedback.current

    val focusRequester =
        remember {
            FocusRequester()
        }

    val focusManager =
        LocalFocusManager.current

    val keyboardController =
        LocalSoftwareKeyboardController.current

    val coroutineScope =
        rememberCoroutineScope()

    /*
     * Stable selection snapshot for this composition.
     */
    val selectedSnapshot =
        selectedApps.toList()

    /*
     * Fast lookup:
     *
     * AppKey -> position on Home
     */
    val selectionPositions =
        remember(
            selectedSnapshot
        ) {

            selectedSnapshot
                .mapIndexed {
                        index,
                        app ->

                    appKey(app) to
                        (index + 1)
                }
                .toMap()
        }

    /*
     * ============================================================
     * SHARED RANKING
     * ============================================================
     *
     * This replaces the old Favorite Picker substring filter.
     *
     * Favorite search now understands exactly the same things as
     * main Search:
     *
     * wsp -> WhatsApp
     * ds  -> Discord
     * gm  -> Google Maps
     * sn  -> Samsung Notes
     *
     * plus exact matches, starts-with, word matches and contains.
     */
    val filteredApps =
        remember(
            searchEngine,
            searchQuery
        ) {

            if (
                searchQuery.isBlank()
            ) {

                apps

            } else {

                searchEngine.search(
                    query =
                        searchQuery
                )
            }
        }

    fun clearSelectedDrag() {
        draggedSelectedAppKey =
            null

        dropTargetSelectedAppKey =
            null

        draggedTranslation =
            Offset.Zero

        dragPointerInGrid =
            Offset.Zero
    }

    fun exitSearch() {

        if (
            searchExitPending
        ) {
            return
        }

        searchExitPending =
            true

        keyboardController
            ?.hide()

        focusManager
            .clearFocus()

        coroutineScope.launch {

            delay(
                100
            )

            searchMode =
                false

            searchQuery =
                ""

            searchExitPending =
                false
        }
    }

    BackHandler(
        enabled =
            searchMode
    ) {
        exitSearch()
    }

    /*
     * Search opens over several frames for smoother
     * keyboard presentation.
     */
    LaunchedEffect(
        searchMode
    ) {

        if (
            searchMode
        ) {

            clearSelectedDrag()

            withFrameNanos { }

            focusRequester
                .requestFocus()

            withFrameNanos { }

            keyboardController
                ?.show()
        }
    }

    /*
     * New queries always begin at the top.
     */
    LaunchedEffect(
        searchQuery
    ) {

        if (
            searchMode &&
            (
                searchGridState
                    .firstVisibleItemIndex > 0 ||
                    searchGridState
                        .firstVisibleItemScrollOffset > 0
                )
        ) {

            searchGridState
                .scrollToItem(
                    0
                )
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
    ) {

        /*
         * Translucent fallback behind the picker.
         */
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surface
                            .copy(
                                alpha =
                                    0.78f
                            )
                    )
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {

            /*
             * Header stays fixed.
             */
            if (
                searchMode
            ) {

                SearchHeader(
                    searchQuery =
                        searchQuery,
                    onSearchQueryChange = {
                        searchQuery =
                            it
                    },
                    focusRequester =
                        focusRequester,
                    onBack = {
                        exitSearch()
                    }
                )

            } else {

                PickerHeader(
                    selectedCount =
                        selectedApps.size,
                    onCancel =
                        onCancel,
                    onSave = {

                        onSave(
                            selectedApps
                                .toList()
                        )
                    }
                )
            }

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(
                        4
                    ),
                state =
                    if (
                        searchMode
                    ) {
                        searchGridState
                    } else {
                        normalGridState
                    },
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxWidth()
                        .imePadding(),
                contentPadding =
                    PaddingValues(
                        start =
                            20.dp,
                        end =
                            20.dp,
                        top =
                            8.dp,
                        bottom =
                            24.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
                userScrollEnabled =
                    draggedSelectedAppKey ==
                        null
            ) {

                /*
                 * =================================================
                 * NORMAL FAVORITES MODE
                 * =================================================
                 */
                if (
                    !searchMode
                ) {

                    item(
                        key =
                            "selected-heading",
                        contentType =
                            "section",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {

                        SelectedAppsHeading()
                    }

                    if (
                        selectedSnapshot
                            .isEmpty()
                    ) {

                        item(
                            key =
                                "selected-empty",
                            contentType =
                                "section",
                            span = {
                                GridItemSpan(
                                    maxLineSpan
                                )
                            }
                        ) {

                            EmptySelectionState()
                        }

                    } else {

                        items(
                            items =
                                selectedSnapshot,
                            key = { app ->

                                "selected:" +
                                    appKey(
                                        app
                                    )
                            },
                            contentType = {
                                "selected-app"
                            }
                        ) { app ->

                            val key =
                                appKey(
                                    app
                                )

                            val selectedGridKey =
                                selectedGridKey(
                                    app
                                )

                            val isDragging =
                                draggedSelectedAppKey ==
                                    key

                            val isDropTarget =
                                dropTargetSelectedAppKey ==
                                    key

                            SelectedAppItem(
                                app =
                                    app,
                                position =
                                    selectionPositions[
                                        key
                                    ] ?: 0,
                                isDragging =
                                    isDragging,
                                isDropTarget =
                                    isDropTarget,
                                dragTranslation =
                                    if (
                                        isDragging
                                    ) {
                                        draggedTranslation
                                    } else {
                                        Offset.Zero
                                    },
                                onDragStart = {
                                        touchOffset ->

                                    val itemInfo =
                                        normalGridState
                                            .layoutInfo
                                            .visibleItemsInfo
                                            .firstOrNull {
                                                    item ->

                                                item.key ==
                                                    selectedGridKey
                                            }

                                    if (
                                        itemInfo != null
                                    ) {
                                        draggedSelectedAppKey =
                                            key

                                        dropTargetSelectedAppKey =
                                            null

                                        draggedTranslation =
                                            Offset.Zero

                                        dragPointerInGrid =
                                            Offset(
                                                x =
                                                    itemInfo.offset.x
                                                        .toFloat() +
                                                        touchOffset.x,
                                                y =
                                                    itemInfo.offset.y
                                                        .toFloat() +
                                                        touchOffset.y
                                            )

                                        hapticFeedback
                                            .performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                    }
                                },
                                onDrag = {
                                        dragAmount ->

                                    if (
                                        draggedSelectedAppKey ==
                                            key
                                    ) {
                                        draggedTranslation +=
                                            dragAmount

                                        dragPointerInGrid +=
                                            dragAmount

                                        dropTargetSelectedAppKey =
                                            findSelectedDropTarget(
                                                gridState =
                                                    normalGridState,
                                                draggedAppKey =
                                                    key,
                                                pointer =
                                                    dragPointerInGrid
                                            )
                                    }
                                },
                                onDragEnd = {
                                    val sourceKey =
                                        draggedSelectedAppKey

                                    val targetKey =
                                        dropTargetSelectedAppKey

                                    if (
                                        sourceKey != null &&
                                        targetKey != null &&
                                        sourceKey !=
                                            targetKey
                                    ) {
                                        moveSelectedApp(
                                            selectedApps =
                                                selectedApps,
                                            sourceAppKey =
                                                sourceKey,
                                            targetAppKey =
                                                targetKey
                                        )
                                    }

                                    clearSelectedDrag()
                                },
                                onDragCancel = {
                                    clearSelectedDrag()
                                },
                                onRemove = {
                                    removeSelectedApp(
                                        selectedApps =
                                            selectedApps,
                                        app =
                                            app
                                    )
                                }
                            )
                        }
                    }

                    item(
                        key =
                            "search-launcher",
                        contentType =
                            "section",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {

                        SearchLauncher(
                            onClick = {

                                searchMode =
                                    true
                            }
                        )
                    }

                    item(
                        key =
                            "all-apps-heading",
                        contentType =
                            "section",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {

                        Text(
                            text =
                                "Todas las aplicaciones",
                            modifier =
                                Modifier.padding(
                                    top =
                                        6.dp
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
                }

                /*
                 * =================================================
                 * FAVORITES SEARCH MODE
                 * =================================================
                 */
                if (
                    searchMode &&
                    searchQuery.isNotBlank() &&
                    filteredApps.isNotEmpty()
                ) {

                    item(
                        key =
                            "search-heading",
                        contentType =
                            "section",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {

                        Text(
                            text =
                                "Resultados",
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
                }

                val displayedApps =
                    if (
                        searchMode
                    ) {

                        /*
                         * Blank query preserves the complete
                         * app catalog.
                         *
                         * Typed query uses AppSearchEngine.
                         */
                        filteredApps

                    } else {

                        apps
                    }

                if (
                    searchMode &&
                    searchQuery.isNotBlank() &&
                    displayedApps.isEmpty()
                ) {

                    item(
                        key =
                            "empty-search",
                        contentType =
                            "section",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {

                        Text(
                            text =
                                "No encontramos ninguna aplicación con ese nombre.",
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical =
                                            28.dp
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

                } else {

                    items(
                        items =
                            displayedApps,
                        key = { app ->

                            if (
                                searchMode
                            ) {

                                "search:" +
                                    appKey(
                                        app
                                    )

                            } else {

                                "all:" +
                                    appKey(
                                        app
                                    )
                            }
                        },
                        contentType = {
                            "app"
                        }
                    ) { app ->

                        val key =
                            appKey(
                                app
                            )

                        AllAppsItem(
                            app =
                                app,
                            selectionPosition =
                                selectionPositions[
                                    key
                                ],
                            onToggle = {

                                toggleSelection(
                                    selectedApps =
                                        selectedApps,
                                    app =
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
private fun PickerHeader(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth(),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha =
                        0.84f
                )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    start =
                        20.dp,
                    end =
                        20.dp,
                    top =
                        14.dp,
                    bottom =
                        12.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        "Favoritas",
                    fontSize =
                        30.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Text(
                    text =
                        if (
                            selectedCount == 1
                        ) {
                            "1 seleccionada"
                        } else {
                            "$selectedCount seleccionadas"
                        },
                    fontSize =
                        14.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        6.dp
                    )
            )

            AnimatedTextButton(
                text =
                    "Cancelar",
                onClick =
                    onCancel
            )

            Spacer(
                modifier =
                    Modifier.width(
                        2.dp
                    )
            )

            AnimatedButton(
                text =
                    "Listo",
                onClick =
                    onSave
            )
        }
    }
}

@Composable
private fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange:
        (String) -> Unit,
    focusRequester:
        FocusRequester,
    onBack: () -> Unit
) {

    LauncherSearchBar(
        query =
            searchQuery,
        onQueryChange =
            onSearchQueryChange,
        focusRequester =
            focusRequester,
        onBack =
            onBack,
        onClear = {
            onSearchQueryChange(
                ""
            )
        },

        /*
         * Favorite Picker selects applications.
         * The IME action therefore does not launch
         * the first result.
         */
        onSubmit = {
        }
    )
}

@Composable
private fun SearchLauncher(
    onClick: () -> Unit
) {

    LauncherSearchLauncher(
        onClick =
            onClick
    )
}

@Composable
private fun SelectedAppsHeading() {

    Column(
        modifier =
            Modifier.padding(
                top =
                    6.dp
            )
    ) {

        Text(
            text =
                "Seleccionadas",
            fontSize =
                17.sp,
            fontWeight =
                FontWeight.SemiBold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )

        Text(
            text =
                "Toca una aplicación para quitarla. " +
                    "Mantén pulsado y arrastra para cambiar su posición. " +
                    "El número indica su posición en Inicio.",
            fontSize =
                13.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySelectionState() {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth(),
        shape =
            RoundedCornerShape(
                24.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surfaceContainer
                .copy(
                    alpha =
                        0.78f
                )
    ) {

        Text(
            text =
                "Todavía no has seleccionado aplicaciones.\n" +
                    "Elige las que quieras tener en Inicio.",
            modifier =
                Modifier.padding(
                    horizontal =
                        24.dp,
                    vertical =
                        18.dp
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
private fun SelectedAppItem(
    app: InstalledApp,
    position: Int,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragTranslation: Offset,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onRemove: () -> Unit
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    val tileShape =
        RoundedCornerShape(
            20.dp
        )

    val density =
        LocalDensity.current

    val dragElevationPx =
        remember(density) {
            with(density) {
                12.dp.toPx()
            }
        }

    val dropTargetBorder =
        if (
            isDropTarget
        ) {
            MaterialTheme
                .colorScheme
                .primary
                .copy(
                    alpha =
                        0.88f
                )
        } else {
            Color.Transparent
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .zIndex(
                    if (
                        isDragging
                    ) {
                        2f
                    } else {
                        0f
                    }
                )
                .graphicsLayer {
                    if (
                        isDragging
                    ) {
                        translationX =
                            dragTranslation.x

                        translationY =
                            dragTranslation.y

                        shadowElevation =
                            dragElevationPx

                        this.shape =
                            tileShape

                        clip =
                            false
                    }
                }
                .clip(
                    tileShape
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(
                            alpha =
                                if (
                                    isDragging
                                ) {
                                    0.94f
                                } else {
                                    0.72f
                                }
                        )
                )
                .then(
                    if (
                        isDropTarget
                    ) {
                        Modifier.border(
                            width =
                                2.dp,
                            color =
                                dropTargetBorder,
                            shape =
                                tileShape
                        )
                    } else {
                        Modifier
                    }
                )
                .pointerInput(
                    app.componentName,
                    app.user
                ) {
                    detectDragGesturesAfterLongPress(
                        onDragStart =
                            onDragStart,
                        onDragEnd =
                            onDragEnd,
                        onDragCancel =
                            onDragCancel,
                        onDrag = {
                                change,
                                dragAmount ->

                            change.consume()

                            onDrag(
                                dragAmount
                            )
                        }
                    )
                }
                .launcherAppClickable(
                    enabled =
                        !isDragging,
                    shape =
                        tileShape,
                    onClickLabel =
                        "Quitar ${app.label} de Favoritas",
                    onClick =
                        onRemove
                )
                .padding(
                    horizontal =
                        5.dp,
                    vertical =
                        9.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Box {
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

            PositionBadge(
                position =
                    position,
                modifier =
                    Modifier.align(
                        Alignment.TopEnd
                    )
            )
        }

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
                1,
            overflow =
                TextOverflow.Ellipsis,
            textAlign =
                TextAlign.Center,
            fontSize =
                12.sp,
            fontWeight =
                FontWeight.Medium,
            color =
                MaterialTheme
                    .colorScheme
                    .onPrimaryContainer
        )
    }
}

@Composable
private fun AllAppsItem(
    app: InstalledApp,
    selectionPosition: Int?,
    onToggle: () -> Unit
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    val isSelected =
        selectionPosition != null

    val shape =
        RoundedCornerShape(
            18.dp
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    shape
                )
                .background(
                    if (
                        isSelected
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(
                                alpha =
                                    0.42f
                            )
                    } else {
                        Color.Transparent
                    }
                )
                .launcherAppClickable(
                    onClickLabel =
                        if (
                            isSelected
                        ) {
                            "Quitar ${app.label} de Favoritas"
                        } else {
                            "Agregar ${app.label} a Favoritas"
                        },
                    onClick =
                        onToggle
                )
                .padding(
                    horizontal =
                        3.dp,
                    vertical =
                        7.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Box {
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

            if (
                selectionPosition != null
            ) {
                PositionBadge(
                    position =
                        selectionPosition,
                    modifier =
                        Modifier.align(
                            Alignment.TopEnd
                        )
                )
            }
        }

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
            fontWeight =
                if (
                    isSelected
                ) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}

@Composable
private fun PositionBadge(
    position: Int,
    modifier: Modifier =
        Modifier
) {

    Surface(
        modifier =
            modifier.size(
                20.dp
            ),
        shape =
            CircleShape,
        color =
            MaterialTheme
                .colorScheme
                .primary
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    position
                        .toString(),
                fontSize =
                    11.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimary
            )
        }
    }
}

@Composable
private fun AnimatedButton(
    text: String,
    onClick: () -> Unit
) {
    /*
     * Material Button already provides the shared ripple
     * feedback. The old press-scale animation is intentionally
     * removed.
     */
    Button(
        onClick =
            onClick,
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {
        Text(
            text =
                text
        )
    }
}

@Composable
private fun AnimatedTextButton(
    text: String,
    onClick: () -> Unit
) {
    /*
     * TextButton uses Material's ripple indication by default.
     */
    TextButton(
        onClick =
            onClick
    ) {
        Text(
            text =
                text,
            fontSize =
                14.sp
        )
    }
}

@Composable
private fun FavoritePickerWindowEffect() {

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
                    32
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

private fun selectedGridKey(
    app: InstalledApp
): String {
    return SELECTED_GRID_KEY_PREFIX +
        appKey(
            app
        )
}

private fun findSelectedDropTarget(
    gridState:
        androidx.compose.foundation.lazy.grid.LazyGridState,
    draggedAppKey: String,
    pointer: Offset
): String? {
    return gridState
        .layoutInfo
        .visibleItemsInfo
        .asSequence()
        .mapNotNull {
                item ->

            val gridKey =
                item.key as? String
                    ?: return@mapNotNull null

            if (
                !gridKey.startsWith(
                    SELECTED_GRID_KEY_PREFIX
                )
            ) {
                return@mapNotNull null
            }

            val candidateAppKey =
                gridKey.removePrefix(
                    SELECTED_GRID_KEY_PREFIX
                )

            if (
                candidateAppKey ==
                draggedAppKey
            ) {
                return@mapNotNull null
            }

            val left =
                item.offset.x.toFloat()

            val top =
                item.offset.y.toFloat()

            val right =
                left +
                    item.size.width

            val bottom =
                top +
                    item.size.height

            /*
             * A small expansion lets the target feel forgiving while still
             * requiring the finger to be genuinely near another tile.
             */
            val horizontalTolerance =
                item.size.width *
                    0.18f

            val verticalTolerance =
                item.size.height *
                    0.18f

            val isNearCandidate =
                pointer.x >=
                    left -
                        horizontalTolerance &&
                pointer.x <=
                    right +
                        horizontalTolerance &&
                pointer.y >=
                    top -
                        verticalTolerance &&
                pointer.y <=
                    bottom +
                        verticalTolerance

            if (
                !isNearCandidate
            ) {
                return@mapNotNull null
            }

            val centerX =
                left +
                    item.size.width /
                        2f

            val centerY =
                top +
                    item.size.height /
                        2f

            val dx =
                pointer.x -
                    centerX

            val dy =
                pointer.y -
                    centerY

            candidateAppKey to
                (dx * dx +
                    dy * dy)
        }
        .minByOrNull {
            it.second
        }
        ?.first
}

private fun moveSelectedApp(
    selectedApps:
        MutableList<InstalledApp>,
    sourceAppKey: String,
    targetAppKey: String
) {
    val sourceIndex =
        selectedApps
            .indexOfFirst {
                    app ->

                appKey(
                    app
                ) ==
                    sourceAppKey
            }

    val targetIndex =
        selectedApps
            .indexOfFirst {
                    app ->

                appKey(
                    app
                ) ==
                    targetAppKey
            }

    if (
        sourceIndex < 0 ||
        targetIndex < 0 ||
        sourceIndex ==
            targetIndex
    ) {
        return
    }

    val movedApp =
        selectedApps.removeAt(
            sourceIndex
        )

    selectedApps.add(
        targetIndex,
        movedApp
    )
}

private fun toggleSelection(
    selectedApps:
        MutableList<InstalledApp>,
    app: InstalledApp
) {

    val index =
        selectedApps
            .indexOfFirst {

                isSameApp(
                    first =
                        it,
                    second =
                        app
                )
            }

    if (
        index >= 0
    ) {

        selectedApps
            .removeAt(
                index
            )

    } else {

        selectedApps
            .add(
                app
            )
    }
}

private fun removeSelectedApp(
    selectedApps:
        MutableList<InstalledApp>,
    app: InstalledApp
) {

    val index =
        selectedApps
            .indexOfFirst {

                isSameApp(
                    first =
                        it,
                    second =
                        app
                )
            }

    if (
        index >= 0
    ) {

        selectedApps
            .removeAt(
                index
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

private const val SELECTED_GRID_KEY_PREFIX =
    "selected:"
