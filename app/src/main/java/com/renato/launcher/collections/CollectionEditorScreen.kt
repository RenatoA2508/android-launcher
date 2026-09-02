package com.renato.launcher.collections

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.search.AppSearchEngine
import com.renato.launcher.ui.components.LauncherPrimaryActionButton
import com.renato.launcher.ui.components.LauncherSearchBar
import com.renato.launcher.ui.components.LauncherTextActionButton
import com.renato.launcher.ui.components.LauncherSearchLauncher
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CollectionEditorScreen(
    apps: List<InstalledApp>,
    initialName: String,
    initialSelection: List<InstalledApp>,
    isEditing: Boolean,
    onCancel: () -> Unit,
    onSave: (
        String,
        List<InstalledApp>
    ) -> Unit
) {
    CollectionWindowEffect()

    PreloadLauncherAppIcons(
        apps =
            apps
    )

    val searchEngine =
        remember(
            apps
        ) {
            AppSearchEngine(
                apps =
                    apps
            )
        }

    var collectionName by
        remember(
            initialName
        ) {
            mutableStateOf(
                initialName
            )
        }

    val selectedApps =
        remember(
            initialSelection
        ) {
            mutableStateListOf<InstalledApp>()
                .apply {
                    addAll(
                        initialSelection
                    )
                }
        }

    var searchMode by
        remember {
            mutableStateOf(
                false
            )
        }

    var searchQuery by
        remember {
            mutableStateOf(
                ""
            )
        }

    var searchExitPending by
        remember {
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
     * Changes remain local until the user presses Listo, so Cancelar
     * still discards every reorder made during this edit session.
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

    var swapAnimationSequence by
        remember {
            mutableStateOf(
                0L
            )
        }

    var swapAnimationRequest by
        remember {
            mutableStateOf<SwapAnimationRequest?>(
                null
            )
        }

    fun requestSettleAnimation(
        offsets: Map<String, Offset>
    ) {
        if (
            offsets.isEmpty()
        ) {
            return
        }

        swapAnimationSequence +=
            1L

        swapAnimationRequest =
            SwapAnimationRequest(
                id =
                    swapAnimationSequence,
                offsets =
                    offsets
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

    val selectedSnapshot =
        selectedApps.toList()

    val selectionPositions =
        remember(
            selectedSnapshot
        ) {
            selectedSnapshot
                .mapIndexed {
                        index,
                        app ->

                    appKey(
                        app
                    ) to
                        (index + 1)
                }
                .toMap()
        }

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

    val canSave =
        collectionName
            .isNotBlank() &&
            selectedApps
                .isNotEmpty()

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

        coroutineScope
            .launch {
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

    BackHandler {
        if (
            searchMode
        ) {
            exitSearch()
        } else {
            onCancel()
        }
    }

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
            Modifier.fillMaxSize()
    ) {
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
            if (
                searchMode
            ) {
                LauncherSearchBar(
                    query =
                        searchQuery,
                    onQueryChange = {
                        searchQuery =
                            it
                    },
                    focusRequester =
                        focusRequester,
                    onBack = {
                        exitSearch()
                    },
                    onClear = {
                        searchQuery =
                            ""
                    },
                    onSubmit = {
                    }
                )
            } else {
                CollectionEditorHeader(
                    isEditing =
                        isEditing,
                    selectedCount =
                        selectedApps.size,
                    canSave =
                        canSave,
                    onCancel =
                        onCancel,
                    onSave = {
                        onSave(
                            collectionName.trim(),
                            selectedApps.toList()
                        )
                    }
                )

                CollectionNameField(
                    name =
                        collectionName,
                    onNameChange = {
                        collectionName =
                            it
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
                            10.dp,
                        bottom =
                            28.dp
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
                            key = {
                                    app ->

                                selectedGridKey(
                                    app
                                )
                            },
                            contentType = {
                                "selected-app"
                            }
                        ) {
                                app ->

                            val key =
                                appKey(
                                    app
                                )

                            val gridKey =
                                selectedGridKey(
                                    app
                                )

                            val isDragging =
                                draggedSelectedAppKey ==
                                    key

                            val isDropTarget =
                                dropTargetSelectedAppKey ==
                                    key

                            val settleRequest =
                                swapAnimationRequest

                            val settleOffset =
                                settleRequest
                                    ?.offsets
                                    ?.get(
                                        key
                                    )
                                    ?: Offset.Zero

                            val settleAnimationToken =
                                if (
                                    settleRequest
                                        ?.offsets
                                        ?.containsKey(
                                            key
                                        ) == true
                                ) {
                                    settleRequest.id
                                } else {
                                    null
                                }

                            SelectedCollectionAppItem(
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
                                settleOffset =
                                    settleOffset,
                                settleAnimationToken =
                                    settleAnimationToken,
                                onDragStart = {
                                        touchOffset ->

                                    val itemInfo =
                                        normalGridState
                                            .layoutInfo
                                            .visibleItemsInfo
                                            .firstOrNull {
                                                    item ->

                                                item.key ==
                                                    gridKey
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
                                        val sourceGridKey =
                                            COLLECTION_SELECTED_GRID_KEY_PREFIX +
                                                sourceKey

                                        val targetGridKey =
                                            COLLECTION_SELECTED_GRID_KEY_PREFIX +
                                                targetKey

                                        val sourceItemInfo =
                                            normalGridState
                                                .layoutInfo
                                                .visibleItemsInfo
                                                .firstOrNull {
                                                        item ->

                                                    item.key ==
                                                        sourceGridKey
                                                }

                                        val targetItemInfo =
                                            normalGridState
                                                .layoutInfo
                                                .visibleItemsInfo
                                                .firstOrNull {
                                                        item ->

                                                    item.key ==
                                                        targetGridKey
                                                }

                                        if (
                                            sourceItemInfo != null &&
                                            targetItemInfo != null
                                        ) {
                                            val sourceOldPosition =
                                                Offset(
                                                    sourceItemInfo
                                                        .offset
                                                        .x
                                                        .toFloat(),
                                                    sourceItemInfo
                                                        .offset
                                                        .y
                                                        .toFloat()
                                                )

                                            val targetOldPosition =
                                                Offset(
                                                    targetItemInfo
                                                        .offset
                                                        .x
                                                        .toFloat(),
                                                    targetItemInfo
                                                        .offset
                                                        .y
                                                        .toFloat()
                                                )

                                            requestSettleAnimation(
                                                mapOf(
                                                    sourceKey to
                                                        (
                                                            sourceOldPosition +
                                                                draggedTranslation -
                                                                targetOldPosition
                                                        ),
                                                    targetKey to
                                                        (
                                                            targetOldPosition -
                                                                sourceOldPosition
                                                        )
                                                )
                                            )
                                        }

                                        swapSelectedApps(
                                            selectedApps =
                                                selectedApps,
                                            sourceAppKey =
                                                sourceKey,
                                            targetAppKey =
                                                targetKey
                                        )
                                    } else if (
                                        sourceKey != null &&
                                        draggedTranslation !=
                                            Offset.Zero
                                    ) {
                                        requestSettleAnimation(
                                            mapOf(
                                                sourceKey to
                                                    draggedTranslation
                                            )
                                        )
                                    }

                                    clearSelectedDrag()
                                },
                                onDragCancel = {
                                    val sourceKey =
                                        draggedSelectedAppKey

                                    if (
                                        sourceKey != null &&
                                        draggedTranslation !=
                                            Offset.Zero
                                    ) {
                                        requestSettleAnimation(
                                            mapOf(
                                                sourceKey to
                                                    draggedTranslation
                                            )
                                        )
                                    }

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
                        LauncherSearchLauncher(
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
                        key = {
                                app ->

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
                    ) {
                            app ->

                        val key =
                            appKey(
                                app
                            )

                        CollectionAppItem(
                            app =
                                app,
                            selectionPosition =
                                selectionPositions[
                                    key
                                ],
                            selected =
                                selectionPositions
                                    .containsKey(
                                        key
                                    ),
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
private fun CollectionEditorHeader(
    isEditing: Boolean,
    selectedCount: Int,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            Color.Transparent
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
                        8.dp
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
                        if (
                            isEditing
                        ) {
                            "Editar colección"
                        } else {
                            "Nueva colección"
                        },
                    fontSize =
                        27.sp,
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

            CollectionEditorTextActionButton(
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

            CollectionEditorPrimaryActionButton(
                text =
                    "Listo",
                enabled =
                    canSave,
                onClick =
                    onSave
            )
        }
    }
}

@Composable
private fun CollectionEditorTextActionButton(
    text: String,
    onClick: () -> Unit
) {
    LauncherTextActionButton(
        text =
            text,
        onClick =
            onClick
    )
}

@Composable
private fun CollectionEditorPrimaryActionButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    LauncherPrimaryActionButton(
        text =
            text,
        enabled =
            enabled,
        onClick =
            onClick
    )
}

@Composable
private fun CollectionNameField(
    name: String,
    onNameChange: (String) -> Unit
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            Color.Transparent
    ) {
        OutlinedTextField(
            value =
                name,
            onValueChange =
                onNameChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start =
                            20.dp,
                        end =
                            20.dp,
                        bottom =
                            12.dp
                    ),
            label = {
                Text(
                    text =
                        "Nombre"
                )
            },
            placeholder = {
                Text(
                    text =
                        "Ej. Universidad"
                )
            },
            singleLine =
                true,
            shape =
                RoundedCornerShape(
                    24.dp
                )
        )
    }
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
                    "Mantén pulsado y arrastra para intercambiar su posición. " +
                    "El número indica su posición dentro de la colección.",
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
            Modifier.fillMaxWidth(),
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
                "Elige al menos una aplicación para esta colección.",
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
private fun SelectedCollectionAppItem(
    app: InstalledApp,
    position: Int,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragTranslation: Offset,
    settleOffset: Offset,
    settleAnimationToken: Long?,
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
        remember(
            density
        ) {
            with(
                density
            ) {
                12.dp.toPx()
            }
        }

    val settleProgress =
        remember(
            settleAnimationToken
        ) {
            Animatable(
                if (
                    settleAnimationToken != null
                ) {
                    1f
                } else {
                    0f
                }
            )
        }

    LaunchedEffect(
        settleAnimationToken
    ) {
        if (
            settleAnimationToken != null
        ) {
            settleProgress.animateTo(
                targetValue =
                    0f,
                animationSpec =
                    spring(
                        dampingRatio =
                            0.86f,
                        stiffness =
                            700f
                    )
            )
        }
    }

    val dragLiftProgress by
        animateFloatAsState(
            targetValue =
                if (
                    isDragging
                ) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(
                    durationMillis =
                        110
                ),
            label =
                "collectionDragLift"
        )

    val dropTargetProgress by
        animateFloatAsState(
            targetValue =
                if (
                    isDropTarget
                ) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(
                    durationMillis =
                        100
                ),
            label =
                "collectionDropTarget"
        )

    val isSettling =
        settleAnimationToken != null &&
            settleProgress.value >
                0.001f

    val animatedSettleTranslation =
        settleOffset *
            settleProgress.value

    val dropTargetBorder =
        MaterialTheme
            .colorScheme
            .primary
            .copy(
                alpha =
                    0.88f *
                        dropTargetProgress
            )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .zIndex(
                    when {
                        isDragging ->
                            2f

                        isSettling ->
                            1f

                        else ->
                            0f
                    }
                )
                .graphicsLayer {
                    translationX =
                        if (
                            isDragging
                        ) {
                            dragTranslation.x
                        } else {
                            animatedSettleTranslation.x
                        }

                    translationY =
                        if (
                            isDragging
                        ) {
                            dragTranslation.y
                        } else {
                            animatedSettleTranslation.y
                        }

                    shadowElevation =
                        dragElevationPx *
                            dragLiftProgress

                    this.shape =
                        tileShape

                    clip =
                        false
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
                                0.72f +
                                    (0.22f *
                                        dragLiftProgress) +
                                    (0.05f *
                                        dropTargetProgress)
                        )
                )
                .border(
                    width =
                        2.dp,
                    color =
                        dropTargetBorder,
                    shape =
                        tileShape
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
                        "Quitar ${app.label} de la colección",
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
private fun CollectionAppItem(
    app: InstalledApp,
    selectionPosition: Int?,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

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
                        selected
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
                    shape =
                        shape,
                    onClickLabel =
                        if (
                            selected
                        ) {
                            "Quitar ${app.label} de la colección"
                        } else {
                            "Agregar ${app.label} a la colección"
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
                    selected
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
                    position.toString(),
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

private data class SwapAnimationRequest(
    val id: Long,
    val offsets: Map<String, Offset>
)

private fun selectedGridKey(
    app: InstalledApp
): String {
    return COLLECTION_SELECTED_GRID_KEY_PREFIX +
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
                    COLLECTION_SELECTED_GRID_KEY_PREFIX
                )
            ) {
                return@mapNotNull null
            }

            val candidateAppKey =
                gridKey.removePrefix(
                    COLLECTION_SELECTED_GRID_KEY_PREFIX
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

private fun swapSelectedApps(
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

    /*
     * Keep the same positional contract as Favorites: dropping one app
     * over another swaps only those two positions. Apps in between do
     * not shift.
     */
    val sourceApp =
        selectedApps[
            sourceIndex
        ]

    selectedApps[
        sourceIndex
    ] =
        selectedApps[
            targetIndex
        ]

    selectedApps[
        targetIndex
    ] =
        sourceApp
}

private fun toggleSelection(
    selectedApps: MutableList<InstalledApp>,
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
    selectedApps: MutableList<InstalledApp>,
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

private fun appKey(
    app: InstalledApp
): String {
    return "${app.user.hashCode()}:" +
        app.componentName
            .flattenToString()
}

private const val COLLECTION_SELECTED_GRID_KEY_PREFIX =
    "collection-selected:"
