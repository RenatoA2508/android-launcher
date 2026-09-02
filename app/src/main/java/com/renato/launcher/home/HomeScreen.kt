package com.renato.launcher.home

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.collections.MAX_COLLECTIONS
import com.renato.launcher.data.database.collection.CollectionEntity
import com.renato.launcher.notifications.NotificationBadgeStore
import com.renato.launcher.ui.components.AppContextMenu
import com.renato.launcher.ui.components.CollectionContextMenu
import com.renato.launcher.ui.components.LauncherAppIconWithBadge
import com.renato.launcher.ui.components.LauncherPrimaryActionButton
import com.renato.launcher.ui.components.LauncherTextActionButton
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppClickable
import com.renato.launcher.ui.interactions.launcherGestureCombinedClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(
    favoriteApps: List<InstalledApp>,
    favoritesLoaded: Boolean,
    collections: List<CollectionEntity>,
    collectionsLoaded: Boolean,
    collectionAppsById: Map<Long, List<InstalledApp>>,
    onAppClick: (InstalledApp) -> Unit,
    onChooseFavorites: () -> Unit,
    onEditFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onRemoveFavorite: (InstalledApp) -> Unit,
    onReorderFavorites: (List<InstalledApp>) -> Unit,
    onEditCollection: (Long) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onReorderCollections: (List<CollectionEntity>) -> Unit,
    onReorderCollectionApps: (
        Long,
        List<InstalledApp>
    ) -> Unit,
    onRemoveAppFromCollection: (
        Long,
        InstalledApp
    ) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit
) {
    val wallpaperTextColor =
        rememberWallpaperTextColor()

    val notificationCounts by
        NotificationBadgeStore
            .counts
            .collectAsState()

    val collectionApps =
        remember(
            collectionAppsById
        ) {
            collectionAppsById
                .values
                .flatten()
                .distinctBy {
                    appKey(
                        it
                    )
                }
        }

    PreloadLauncherAppIcons(
        apps =
            collectionApps
    )

    val density =
        LocalDensity.current

    val swipeThresholdPx =
        remember(density) {
            with(density) {
                72.dp.toPx()
            }
        }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics {
                    testTagsAsResourceId =
                        true
                }
                .testTag(
                    HOME_ROOT_TAG
                ),
        color =
            Color.Transparent
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(
                        onOpenSearch,
                        swipeThresholdPx
                    ) {
                        var totalVerticalDrag =
                            0f

                        detectVerticalDragGestures(
                            onDragStart = {
                                totalVerticalDrag =
                                    0f
                            },
                            onVerticalDrag = {
                                    change,
                                    dragAmount ->

                                totalVerticalDrag +=
                                    dragAmount

                                /*
                                 * Consume only once this clearly
                                 * becomes a vertical swipe. Normal
                                 * taps and long presses remain
                                 * available to the app item.
                                 */
                                if (
                                    kotlin.math.abs(
                                        totalVerticalDrag
                                    ) > 12f
                                ) {
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (
                                    totalVerticalDrag <=
                                    -swipeThresholdPx
                                ) {
                                    onOpenSearch()
                                }

                                totalVerticalDrag =
                                    0f
                            },
                            onDragCancel = {
                                totalVerticalDrag =
                                    0f
                            }
                        )
                    }
                    .launcherGestureCombinedClickable(
                        onClick = {
                            // Empty Home tap does nothing.
                        },
                        onLongClickLabel =
                            "Editar Favoritas",
                        onLongClick = {
                            if (
                                favoritesLoaded
                            ) {
                                onEditFavorites()
                            }
                        }
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal =
                            24.dp
                    )
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        32.dp
                    )
            )

            ClockHeader(
                textColor =
                    wallpaperTextColor
            )

            Spacer(
                modifier =
                    Modifier.height(
                        40.dp
                    )
            )

            if (
                favoritesLoaded &&
                favoriteApps.isEmpty()
            ) {
                EmptyFavoritesState(
                    textColor =
                        wallpaperTextColor,
                    onChooseFavorites =
                        onChooseFavorites
                )
            } else {
                FavoriteAppsGrid(
                    apps =
                        favoriteApps,
                    onAppClick =
                        onAppClick,
                    onAppInfo =
                        onAppInfo,
                    onRemoveFavorite =
                        onRemoveFavorite,
                    onReorderFavorites =
                        onReorderFavorites,
                    onUninstallApp =
                        onUninstallApp,
                    notificationCountFor = { app ->
                        NotificationBadgeStore
                            .countFor(
                                counts =
                                    notificationCounts,
                                app =
                                    app
                            )
                    },
                    textColor =
                        wallpaperTextColor
                )
            }

            if (
                collectionsLoaded &&
                collections.isNotEmpty()
            ) {
                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )

                HomeCollectionsSection(
                    collections =
                        collections,
                    collectionAppsById =
                        collectionAppsById,
                    notificationCountFor = { app ->
                        NotificationBadgeStore
                            .countFor(
                                counts =
                                    notificationCounts,
                                app =
                                    app
                            )
                    },
                    onAppClick =
                        onAppClick,
                    onAppInfo =
                        onAppInfo,
                    onEditCollection =
                        onEditCollection,
                    onDeleteCollection =
                        onDeleteCollection,
                    onReorderCollections =
                        onReorderCollections,
                    onReorderCollectionApps =
                        onReorderCollectionApps,
                    onRemoveAppFromCollection =
                        onRemoveAppFromCollection,
                    onUninstallApp =
                        onUninstallApp,
                    textColor =
                        wallpaperTextColor
                )
            }
        }
    }
}

@Composable
private fun EmptyFavoritesState(
    textColor: Color,
    onChooseFavorites: () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text =
                "Aún no tienes aplicaciones favoritas",
            style =
                wallpaperTextStyle(
                    color =
                        textColor,
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.Normal
                ),
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        LauncherPrimaryActionButton(
            text =
                "Seleccionar aplicaciones",
            onClick =
                onChooseFavorites
        )
    }
}

@Composable
private fun ClockHeader(
    textColor: Color
) {
    val context =
        LocalContext.current

    val locale =
        remember {
            context.resources
                .configuration
                .locales[0]
        }

    var currentTimeMillis by
        remember {
            mutableLongStateOf(
                System.currentTimeMillis()
            )
        }

    LaunchedEffect(Unit) {
        while (true) {
            val now =
                System.currentTimeMillis()

            currentTimeMillis =
                now

            val millisecondsUntilNextMinute =
                60_000L -
                    (now % 60_000L)

            delay(
                millisecondsUntilNextMinute +
                    50L
            )
        }
    }

    val timeFormatter =
        remember(context) {
            DateFormat
                .getTimeFormat(
                    context
                )
        }

    val dateFormatter =
        remember(locale) {
            val pattern =
                DateFormat
                    .getBestDateTimePattern(
                        locale,
                        "EEEE d MMMM"
                    )

            SimpleDateFormat(
                pattern,
                locale
            )
        }

    val currentDate =
        remember(
            currentTimeMillis
        ) {
            Date(
                currentTimeMillis
            )
        }

    val timeText =
        timeFormatter.format(
            currentDate
        )

    val dateText =
        dateFormatter
            .format(
                currentDate
            )
            .replaceFirstChar {
                    firstCharacter ->

                if (
                    firstCharacter
                        .isLowerCase()
                ) {
                    firstCharacter
                        .titlecase(
                            locale
                        )
                } else {
                    firstCharacter
                        .toString()
                }
            }

    Column {
        Text(
            text =
                timeText,
            style =
                wallpaperTextStyle(
                    color =
                        textColor,
                    fontSize =
                        48.sp,
                    fontWeight =
                        FontWeight.Normal
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )

        Text(
            text =
                dateText,
            style =
                wallpaperTextStyle(
                    color =
                        textColor.copy(
                            alpha =
                                0.78f
                        ),
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.Normal
                )
        )
    }
}

@Composable
private fun FavoriteAppsGrid(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onRemoveFavorite: (InstalledApp) -> Unit,
    onReorderFavorites: (List<InstalledApp>) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit,
    notificationCountFor: (InstalledApp) -> Int,
    textColor: Color
) {
    /*
     * Home is intentionally not a LazyGrid. The favorite list is small and
     * already laid out as two compact columns, so we keep the existing layout
     * and track each app's real bounds for drag-and-drop hit testing.
     */
    val itemBoundsByKey =
        remember {
            mutableMapOf<String, Rect>()
        }

    var draggedAppKey by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var dropTargetAppKey by
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

    var dragPointerInRoot by
        remember {
            mutableStateOf(
                Offset.Zero
            )
        }

    /*
     * FLIP-style settle animation.
     *
     * When a swap is committed, Home immediately adopts the new logical
     * order. These offsets visually keep the two affected apps where they
     * were for that first frame, then a spring carries them into their new
     * slots. The dragged app starts from the exact point where the finger
     * released it instead of teleporting.
     */
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
        if (offsets.isEmpty()) {
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

    fun clearDrag() {
        draggedAppKey =
            null

        dropTargetAppKey =
            null

        draggedTranslation =
            Offset.Zero

        dragPointerInRoot =
            Offset.Zero
    }

    LaunchedEffect(
        apps
    ) {
        val currentDraggedKey =
            draggedAppKey

        if (
            currentDraggedKey != null &&
            apps.none {
                    app ->

                appKey(app) ==
                    currentDraggedKey
            }
        ) {
            clearDrag()
        }
    }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {
        apps
            .chunked(
                2
            )
            .forEach {
                    rowApps ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            16.dp
                        )
                ) {
                    rowApps.forEach {
                            app ->

                        val key =
                            appKey(
                                app
                            )

                        val isGestureActive =
                            draggedAppKey ==
                                key

                        val isDragging =
                            isGestureActive &&
                                draggedTranslation !=
                                    Offset.Zero

                        val isDropTarget =
                            dropTargetAppKey ==
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

                        key(
                            key
                        ) {
                            FavoriteAppItem(
                                app =
                                    app,
                                notificationCount =
                                    notificationCountFor(
                                        app
                                    ),
                                isGestureActive =
                                    isGestureActive,
                                isDragging =
                                    isDragging,
                                isDropTarget =
                                    isDropTarget,
                                dragTranslation =
                                    if (
                                        isGestureActive
                                    ) {
                                        draggedTranslation
                                    } else {
                                        Offset.Zero
                                    },
                                settleOffset =
                                    settleOffset,
                                settleAnimationToken =
                                    settleAnimationToken,
                                onBoundsChanged = {
                                        bounds ->

                                    itemBoundsByKey[
                                        key
                                    ] =
                                        bounds
                                },
                                onClick = {
                                    onAppClick(
                                        app
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
                                },
                                onDragStart = {
                                        touchOffset ->

                                    val bounds =
                                        itemBoundsByKey[
                                            key
                                        ]

                                    if (
                                        bounds != null
                                    ) {
                                        draggedAppKey =
                                            key

                                        dropTargetAppKey =
                                            null

                                        draggedTranslation =
                                            Offset.Zero

                                        dragPointerInRoot =
                                            Offset(
                                                x =
                                                    bounds.left +
                                                        touchOffset.x,
                                                y =
                                                    bounds.top +
                                                        touchOffset.y
                                            )
                                    }
                                },
                                onDrag = {
                                        dragAmount ->

                                    if (
                                        draggedAppKey ==
                                        key
                                    ) {
                                        draggedTranslation +=
                                            dragAmount

                                        dragPointerInRoot +=
                                            dragAmount

                                        dropTargetAppKey =
                                            findHomeDropTarget(
                                                apps =
                                                    apps,
                                                itemBoundsByKey =
                                                    itemBoundsByKey,
                                                draggedAppKey =
                                                    key,
                                                pointer =
                                                    dragPointerInRoot
                                            )
                                    }
                                },
                                onDragEnd = {
                                    val sourceKey =
                                        draggedAppKey

                                    val targetKey =
                                        dropTargetAppKey

                                    if (
                                        sourceKey != null &&
                                        targetKey != null &&
                                        sourceKey !=
                                            targetKey
                                    ) {
                                        val sourceBounds =
                                            itemBoundsByKey[
                                                sourceKey
                                            ]

                                        val targetBounds =
                                            itemBoundsByKey[
                                                targetKey
                                            ]

                                        val reorderedApps =
                                            swapFavoriteApps(
                                                apps =
                                                    apps,
                                                sourceAppKey =
                                                    sourceKey,
                                                targetAppKey =
                                                    targetKey
                                            )

                                        if (
                                            reorderedApps !=
                                            apps
                                        ) {
                                            if (
                                                sourceBounds != null &&
                                                targetBounds != null
                                            ) {
                                                val sourceOldPosition =
                                                    Offset(
                                                        sourceBounds.left,
                                                        sourceBounds.top
                                                    )

                                                val targetOldPosition =
                                                    Offset(
                                                        targetBounds.left,
                                                        targetBounds.top
                                                    )

                                                /*
                                                 * Source: start exactly where
                                                 * the finger released it.
                                                 *
                                                 * Target: remain in its old
                                                 * slot for the first frame.
                                                 */
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

                                            onReorderFavorites(
                                                reorderedApps
                                            )
                                        }
                                    } else if (
                                        sourceKey != null &&
                                        draggedTranslation !=
                                            Offset.Zero
                                    ) {
                                        /*
                                         * Invalid drop: glide back to the
                                         * original slot instead of snapping.
                                         */
                                        requestSettleAnimation(
                                            mapOf(
                                                sourceKey to
                                                    draggedTranslation
                                            )
                                        )
                                    }

                                    clearDrag()
                                },
                                onDragCancel = {
                                    val sourceKey =
                                        draggedAppKey

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

                                    clearDrag()
                                },
                                textColor =
                                    textColor,
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            )
                        }
                    }

                    if (
                        rowApps.size ==
                        1
                    ) {
                        Spacer(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }
            }
    }
}

@Composable
private fun FavoriteAppItem(
    app: InstalledApp,
    notificationCount: Int,
    isGestureActive: Boolean,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragTranslation: Offset,
    settleOffset: Offset,
    settleAnimationToken: Long?,
    onBoundsChanged: (Rect) -> Unit,
    onClick: () -> Unit,
    onAppInfo: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onUninstallApp: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    textColor: Color,
    modifier: Modifier =
        Modifier
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    val hapticFeedback =
        LocalHapticFeedback.current

    val density =
        LocalDensity.current

    val dragStartThresholdPx =
        remember(
            density
        ) {
            with(density) {
                6.dp.toPx()
            }
        }

    val dragElevationPx =
        remember(
            density
        ) {
            with(density) {
                12.dp.toPx()
            }
        }

    /*
     * Starting at 1f on a new token is important: the very first frame after
     * the logical swap is already visually inverted to the pre-swap position.
     * From there the spring settles naturally to the new slot.
     */
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
                "homeDragLift"
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
                "homeDropTarget"
        )

    val isSettling =
        settleAnimationToken != null &&
            settleProgress.value >
                0.001f

    val animatedSettleTranslation =
        settleOffset *
            settleProgress.value

    var menuExpanded by
        remember(
            app.componentName,
            app.user
        ) {
            mutableStateOf(
                false
            )
        }

    val itemShape =
        RoundedCornerShape(
            18.dp
        )

    val dragSurfaceColor =
        MaterialTheme
            .colorScheme
            .surface
            .copy(
                alpha =
                    0.58f +
                        (0.28f *
                            dragLiftProgress)
            )

    /*
     * Home sits directly over the wallpaper, so using Material primary here
     * can create a strong blue/palette-colored outline that feels detached
     * from the rest of the surface. Reuse the adaptive wallpaper text color
     * instead: on a dark wallpaper it becomes a soft white highlight, and on
     * a light wallpaper it naturally becomes dark.
     */
    val dropTargetColor =
        textColor

    /*
     * Gesture contract on Home:
     *
     * tap                         -> open app
     * hold + release without move -> context menu
     * hold + move                -> drag and drop
     *
     * The small 6dp threshold absorbs normal finger tremor, so a stationary
     * long press does not accidentally turn into a reorder operation.
     */
    Box(
        modifier =
            modifier
                .onGloballyPositioned {
                        coordinates ->

                    val position =
                        coordinates
                            .positionInRoot()

                    onBoundsChanged(
                        Rect(
                            offset =
                                position,
                            size =
                                Size(
                                    width =
                                        coordinates
                                            .size
                                            .width
                                            .toFloat(),
                                    height =
                                        coordinates
                                            .size
                                            .height
                                            .toFloat()
                                )
                        )
                    )
                }
                .zIndex(
                    when {
                        isGestureActive ->
                            3f

                        isSettling ->
                            1f

                        else ->
                            0f
                    }
                )
                .graphicsLayer {
                    translationX =
                        if (
                            isGestureActive
                        ) {
                            dragTranslation.x
                        } else {
                            animatedSettleTranslation.x
                        }

                    translationY =
                        if (
                            isGestureActive
                        ) {
                            dragTranslation.y
                        } else {
                            animatedSettleTranslation.y
                        }

                    shadowElevation =
                        dragElevationPx *
                            dragLiftProgress

                    shape =
                        itemShape

                    clip =
                        false
                }
                .then(
                    if (
                        isGestureActive
                    ) {
                        Modifier
                            .background(
                                color =
                                    dragSurfaceColor,
                                shape =
                                    itemShape
                            )
                    } else {
                        Modifier
                    }
                )
                .background(
                    color =
                        dropTargetColor.copy(
                            alpha =
                                0.045f *
                                    dropTargetProgress
                        ),
                    shape =
                        itemShape
                )
                .border(
                    width =
                        1.dp,
                    color =
                        dropTargetColor.copy(
                            alpha =
                                0.30f *
                                    dropTargetProgress
                        ),
                    shape =
                        itemShape
                )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min =
                            52.dp
                    )
                    .pointerInput(
                        app.componentName,
                        app.user,
                        dragStartThresholdPx
                    ) {
                        var cumulativeDrag =
                            Offset.Zero

                        var actualDragStarted =
                            false

                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                    touchOffset ->

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false

                                menuExpanded =
                                    false

                                hapticFeedback
                                    .performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )

                                onDragStart(
                                    touchOffset
                                )
                            },
                            onDragEnd = {
                                if (
                                    actualDragStarted
                                ) {
                                    onDragEnd()
                                } else {
                                    /*
                                     * A stationary long press is the context
                                     * menu gesture. The menu opens on release
                                     * so it never steals the active pointer
                                     * from a possible drag.
                                     */
                                    onDragCancel()

                                    menuExpanded =
                                        true
                                }

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false
                            },
                            onDragCancel = {
                                onDragCancel()

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false
                            },
                            onDrag = {
                                    change,
                                    dragAmount ->

                                cumulativeDrag +=
                                    dragAmount

                                if (
                                    !actualDragStarted &&
                                    cumulativeDrag
                                        .getDistance() >=
                                        dragStartThresholdPx
                                ) {
                                    actualDragStarted =
                                        true

                                    change.consume()

                                    /*
                                     * Apply all movement accumulated while we
                                     * were inside the tremor threshold so the
                                     * tile catches up with the finger at once.
                                     */
                                    onDrag(
                                        cumulativeDrag
                                    )

                                } else if (
                                    actualDragStarted
                                ) {
                                    change.consume()

                                    onDrag(
                                        dragAmount
                                    )
                                }
                            }
                        )
                    }
                    .launcherAppClickable(
                        enabled =
                            !isGestureActive,
                        shape =
                            itemShape,
                        onClickLabel =
                            "Abrir ${app.label}",
                        onClick =
                            onClick
                    )
                    .padding(
                        horizontal =
                            4.dp,
                        vertical =
                            6.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            LauncherAppIconWithBadge(
                bitmap =
                    iconBitmap,
                contentDescription =
                    app.label,
                iconSize =
                    30.dp,
                notificationCount =
                    notificationCount
            )

            Spacer(
                modifier =
                    Modifier.width(
                        10.dp
                    )
            )

            Text(
                text =
                    app.label,
                modifier =
                    Modifier.weight(
                        1f
                    ),
                style =
                    wallpaperTextStyle(
                        color =
                            textColor,
                        fontSize =
                            15.sp,
                        fontWeight =
                            FontWeight.Normal
                    ),
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        AppContextMenu(
            expanded =
                menuExpanded,
            app =
                app,
            showRemoveFromHome =
                true,
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
private fun HomeCollectionsSection(
    collections: List<CollectionEntity>,
    collectionAppsById:
        Map<Long, List<InstalledApp>>,
    notificationCountFor:
        (InstalledApp) -> Int,
    onAppClick: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onEditCollection: (Long) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onReorderCollections:
        (List<CollectionEntity>) -> Unit,
    onReorderCollectionApps: (
        Long,
        List<InstalledApp>
    ) -> Unit,
    onRemoveAppFromCollection: (
        Long,
        InstalledApp
    ) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit,
    textColor: Color
) {
    val itemBoundsById =
        remember {
            mutableMapOf<Long, Rect>()
        }

    var draggedCollectionId by
        remember {
            mutableStateOf<Long?>(
                null
            )
        }

    var dropTargetCollectionId by
        remember {
            mutableStateOf<Long?>(
                null
            )
        }

    var draggedTranslation by
        remember {
            mutableStateOf(
                Offset.Zero
            )
        }

    var dragPointerInRoot by
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

    var openCollectionId by
        remember {
            mutableStateOf<Long?>(
                null
            )
        }

    val coroutineScope =
        rememberCoroutineScope()

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

    fun clearDrag() {
        draggedCollectionId =
            null

        dropTargetCollectionId =
            null

        draggedTranslation =
            Offset.Zero

        dragPointerInRoot =
            Offset.Zero
    }

    LaunchedEffect(
        collections
    ) {
        val currentDraggedId =
            draggedCollectionId

        if (
            currentDraggedId != null &&
            collections.none {
                it.id ==
                    currentDraggedId
            }
        ) {
            clearDrag()
        }

        val currentOpenId =
            openCollectionId

        if (
            currentOpenId != null &&
            collections.none {
                it.id ==
                    currentOpenId
            }
        ) {
            openCollectionId =
                null
        }
    }

    Column {
        collections
            .take(
                MAX_COLLECTIONS
            )
            .chunked(
                2
            )
            .forEach {
                    rowCollections ->

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {
                    rowCollections.forEach {
                            collection ->

                        val collectionId =
                            collection.id

                        val collectionKey =
                            collectionKey(
                                collectionId
                            )

                        val isGestureActive =
                            draggedCollectionId ==
                                collectionId

                        val isDragging =
                            isGestureActive &&
                                draggedTranslation !=
                                    Offset.Zero

                        val isDropTarget =
                            dropTargetCollectionId ==
                                collectionId

                        val settleRequest =
                            swapAnimationRequest

                        val settleOffset =
                            settleRequest
                                ?.offsets
                                ?.get(
                                    collectionKey
                                )
                                ?: Offset.Zero

                        val settleAnimationToken =
                            if (
                                settleRequest
                                    ?.offsets
                                    ?.containsKey(
                                        collectionKey
                                    ) == true
                            ) {
                                settleRequest.id
                            } else {
                                null
                            }

                        key(
                            collectionId
                        ) {
                            HomeCollectionItem(
                                collection =
                                    collection,
                                apps =
                                    collectionAppsById[
                                        collectionId
                                    ].orEmpty(),
                                notificationCountFor =
                                    notificationCountFor,
                                isGestureActive =
                                    isGestureActive,
                                isDragging =
                                    isDragging,
                                isDropTarget =
                                    isDropTarget,
                                dragTranslation =
                                    if (
                                        isGestureActive
                                    ) {
                                        draggedTranslation
                                    } else {
                                        Offset.Zero
                                    },
                                settleOffset =
                                    settleOffset,
                                settleAnimationToken =
                                    settleAnimationToken,
                                onBoundsChanged = {
                                        bounds ->

                                    itemBoundsById[
                                        collectionId
                                    ] =
                                        bounds
                                },
                                onClick = {
                                    /*
                                     * Keep the card alive for a few frames so
                                     * the same visible tap ripple used elsewhere
                                     * in the launcher is not hidden by the sheet.
                                     */
                                    coroutineScope.launch {
                                        delay(
                                            75L
                                        )

                                        openCollectionId =
                                            collectionId
                                    }
                                },
                                onEdit = {
                                    onEditCollection(
                                        collectionId
                                    )
                                },
                                onDelete = {
                                    onDeleteCollection(
                                        collectionId
                                    )
                                },
                                onDragStart = {
                                        touchOffset ->

                                    val bounds =
                                        itemBoundsById[
                                            collectionId
                                        ]

                                    if (
                                        bounds != null
                                    ) {
                                        draggedCollectionId =
                                            collectionId

                                        dropTargetCollectionId =
                                            null

                                        draggedTranslation =
                                            Offset.Zero

                                        dragPointerInRoot =
                                            Offset(
                                                x =
                                                    bounds.left +
                                                        touchOffset.x,
                                                y =
                                                    bounds.top +
                                                        touchOffset.y
                                            )
                                    }
                                },
                                onDrag = {
                                        dragAmount ->

                                    if (
                                        draggedCollectionId ==
                                            collectionId
                                    ) {
                                        draggedTranslation +=
                                            dragAmount

                                        dragPointerInRoot +=
                                            dragAmount

                                        dropTargetCollectionId =
                                            findCollectionDropTarget(
                                                collections =
                                                    collections,
                                                itemBoundsById =
                                                    itemBoundsById,
                                                draggedCollectionId =
                                                    collectionId,
                                                pointer =
                                                    dragPointerInRoot
                                            )
                                    }
                                },
                                onDragEnd = {
                                    val sourceId =
                                        draggedCollectionId

                                    val targetId =
                                        dropTargetCollectionId

                                    if (
                                        sourceId != null &&
                                        targetId != null &&
                                        sourceId !=
                                            targetId
                                    ) {
                                        val sourceBounds =
                                            itemBoundsById[
                                                sourceId
                                            ]

                                        val targetBounds =
                                            itemBoundsById[
                                                targetId
                                            ]

                                        val reordered =
                                            swapHomeCollections(
                                                collections =
                                                    collections,
                                                sourceCollectionId =
                                                    sourceId,
                                                targetCollectionId =
                                                    targetId
                                            )

                                        if (
                                            reordered !=
                                            collections
                                        ) {
                                            if (
                                                sourceBounds != null &&
                                                targetBounds != null
                                            ) {
                                                val sourceOldPosition =
                                                    Offset(
                                                        sourceBounds.left,
                                                        sourceBounds.top
                                                    )

                                                val targetOldPosition =
                                                    Offset(
                                                        targetBounds.left,
                                                        targetBounds.top
                                                    )

                                                requestSettleAnimation(
                                                    mapOf(
                                                        collectionKey(
                                                            sourceId
                                                        ) to
                                                            (
                                                                sourceOldPosition +
                                                                    draggedTranslation -
                                                                    targetOldPosition
                                                            ),
                                                        collectionKey(
                                                            targetId
                                                        ) to
                                                            (
                                                                targetOldPosition -
                                                                    sourceOldPosition
                                                            )
                                                    )
                                                )
                                            }

                                            onReorderCollections(
                                                reordered
                                            )
                                        }
                                    } else if (
                                        sourceId != null &&
                                        draggedTranslation !=
                                            Offset.Zero
                                    ) {
                                        requestSettleAnimation(
                                            mapOf(
                                                collectionKey(
                                                    sourceId
                                                ) to
                                                    draggedTranslation
                                            )
                                        )
                                    }

                                    clearDrag()
                                },
                                onDragCancel = {
                                    val sourceId =
                                        draggedCollectionId

                                    if (
                                        sourceId != null &&
                                        draggedTranslation !=
                                            Offset.Zero
                                    ) {
                                        requestSettleAnimation(
                                            mapOf(
                                                collectionKey(
                                                    sourceId
                                                ) to
                                                    draggedTranslation
                                            )
                                        )
                                    }

                                    clearDrag()
                                },
                                textColor =
                                    textColor,
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            )
                        }
                    }

                    if (
                        rowCollections.size ==
                            1
                    ) {
                        Spacer(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )
            }
    }

    val openCollection =
        openCollectionId
            ?.let {
                    collectionId ->

                collections
                    .firstOrNull {
                        it.id ==
                            collectionId
                    }
            }

    if (
        openCollection != null
    ) {
        CollectionHomeBottomSheet(
            collection =
                openCollection,
            apps =
                collectionAppsById[
                    openCollection.id
                ].orEmpty(),
            notificationCountFor =
                notificationCountFor,
            onDismiss = {
                openCollectionId =
                    null
            },
            onEditCollection = {
                openCollectionId =
                    null

                onEditCollection(
                    openCollection.id
                )
            },
            onAppClick = {
                    app ->

                openCollectionId =
                    null

                onAppClick(
                    app
                )
            },
            onAppInfo =
                onAppInfo,
            onReorderApps = {
                    reorderedApps ->

                onReorderCollectionApps(
                    openCollection.id,
                    reorderedApps
                )
            },
            onRemoveApp = {
                    app ->

                onRemoveAppFromCollection(
                    openCollection.id,
                    app
                )
            },
            onUninstallApp =
                onUninstallApp
        )
    }
}

@Composable
private fun HomeCollectionItem(
    collection: CollectionEntity,
    apps: List<InstalledApp>,
    notificationCountFor:
        (InstalledApp) -> Int,
    isGestureActive: Boolean,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragTranslation: Offset,
    settleOffset: Offset,
    settleAnimationToken: Long?,
    onBoundsChanged: (Rect) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    textColor: Color,
    modifier: Modifier =
        Modifier
) {
    val hapticFeedback =
        LocalHapticFeedback.current

    val density =
        LocalDensity.current

    val dragStartThresholdPx =
        remember(
            density
        ) {
            with(density) {
                6.dp.toPx()
            }
        }

    val dragElevationPx =
        remember(
            density
        ) {
            with(density) {
                12.dp.toPx()
            }
        }

    val settleProgress =
        remember(
            settleAnimationToken
        ) {
            Animatable(
                if (
                    settleAnimationToken !=
                    null
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
            settleAnimationToken !=
            null
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
                "homeCollectionDragLift"
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
                "homeCollectionDropTarget"
        )

    val isSettling =
        settleAnimationToken !=
            null &&
            settleProgress.value >
                0.001f

    val animatedSettleTranslation =
        settleOffset *
            settleProgress.value

    var menuExpanded by
        remember(
            collection.id
        ) {
            mutableStateOf(
                false
            )
        }

    var deleteConfirmationVisible by
        remember(
            collection.id
        ) {
            mutableStateOf(
                false
            )
        }

    val itemShape =
        RoundedCornerShape(
            18.dp
        )

    val aggregatedNotificationCount =
        remember(
            apps,
            notificationCountFor
        ) {
            apps.sumOf {
                notificationCountFor(
                    it
                )
            }
        }

    val dragSurfaceColor =
        MaterialTheme
            .colorScheme
            .surface
            .copy(
                alpha =
                    0.62f +
                        (0.24f *
                            dragLiftProgress)
            )

    val baseSurfaceColor =
        MaterialTheme
            .colorScheme
            .surface
            .copy(
                alpha =
                    0.38f
            )

    Box(
        modifier =
            modifier
                .onGloballyPositioned {
                        coordinates ->

                    val position =
                        coordinates
                            .positionInRoot()

                    onBoundsChanged(
                        Rect(
                            offset =
                                position,
                            size =
                                Size(
                                    width =
                                        coordinates
                                            .size
                                            .width
                                            .toFloat(),
                                    height =
                                        coordinates
                                            .size
                                            .height
                                            .toFloat()
                                )
                        )
                    )
                }
                .zIndex(
                    when {
                        isGestureActive ->
                            3f

                        isSettling ->
                            1f

                        else ->
                            0f
                    }
                )
                .graphicsLayer {
                    translationX =
                        if (
                            isGestureActive
                        ) {
                            dragTranslation.x
                        } else {
                            animatedSettleTranslation.x
                        }

                    translationY =
                        if (
                            isGestureActive
                        ) {
                            dragTranslation.y
                        } else {
                            animatedSettleTranslation.y
                        }

                    shadowElevation =
                        dragElevationPx *
                            dragLiftProgress

                    shape =
                        itemShape

                    clip =
                        false
                }
                .background(
                    color =
                        if (
                            isGestureActive
                        ) {
                            dragSurfaceColor
                        } else {
                            baseSurfaceColor
                        },
                    shape =
                        itemShape
                )
                .background(
                    color =
                        textColor.copy(
                            alpha =
                                0.045f *
                                    dropTargetProgress
                        ),
                    shape =
                        itemShape
                )
                .border(
                    width =
                        1.dp,
                    color =
                        textColor.copy(
                            alpha =
                                0.30f *
                                    dropTargetProgress
                        ),
                    shape =
                        itemShape
                )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min =
                            58.dp
                    )
                    .pointerInput(
                        collection.id,
                        dragStartThresholdPx
                    ) {
                        var cumulativeDrag =
                            Offset.Zero

                        var actualDragStarted =
                            false

                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                    touchOffset ->

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false

                                menuExpanded =
                                    false

                                hapticFeedback
                                    .performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )

                                onDragStart(
                                    touchOffset
                                )
                            },
                            onDragEnd = {
                                if (
                                    actualDragStarted
                                ) {
                                    onDragEnd()
                                } else {
                                    onDragCancel()

                                    menuExpanded =
                                        true
                                }

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false
                            },
                            onDragCancel = {
                                onDragCancel()

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false
                            },
                            onDrag = {
                                    change,
                                    dragAmount ->

                                cumulativeDrag +=
                                    dragAmount

                                if (
                                    !actualDragStarted &&
                                    cumulativeDrag
                                        .getDistance() >=
                                        dragStartThresholdPx
                                ) {
                                    actualDragStarted =
                                        true

                                    change.consume()

                                    onDrag(
                                        cumulativeDrag
                                    )
                                } else if (
                                    actualDragStarted
                                ) {
                                    change.consume()

                                    onDrag(
                                        dragAmount
                                    )
                                }
                            }
                        )
                    }
                    .launcherAppClickable(
                        enabled =
                            !isGestureActive,
                        shape =
                            itemShape,
                        onClickLabel =
                            "Abrir ${collection.name}",
                        onClick =
                            onClick
                    )
                    .padding(
                        horizontal =
                            8.dp,
                        vertical =
                            6.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            CollectionMiniIcon(
                apps =
                    apps
            )

            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )

            Text(
                text =
                    collection.name,
                modifier =
                    Modifier.weight(
                        1f
                    ),
                style =
                    wallpaperTextStyle(
                        color =
                            textColor,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.Medium
                    ),
                maxLines =
                    2,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        if (
            aggregatedNotificationCount >
                0
        ) {
            HomeNotificationBadge(
                count =
                    aggregatedNotificationCount,
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top =
                                5.dp,
                            end =
                                5.dp
                        )
            )
        }

        CollectionContextMenu(
            expanded =
                menuExpanded,
            collectionName =
                collection.name,
            onDismiss = {
                menuExpanded =
                    false
            },
            onEdit = {
                menuExpanded =
                    false

                onEdit()
            },
            onDelete = {
                menuExpanded =
                    false

                deleteConfirmationVisible =
                    true
            }
        )
    }

    if (
        deleteConfirmationVisible
    ) {
        AlertDialog(
            onDismissRequest = {
                deleteConfirmationVisible =
                    false
            },
            title = {
                Text(
                    text =
                        "Eliminar colección"
                )
            },
            text = {
                Text(
                    text =
                        "¿Quieres eliminar ${collection.name}? Las aplicaciones no se desinstalarán."
                )
            },
            confirmButton = {
                LauncherTextActionButton(
                    text =
                        "Eliminar",
                    contentColor =
                        MaterialTheme
                            .colorScheme
                            .error,
                    onClick = {
                        deleteConfirmationVisible =
                            false

                        onDelete()
                    }
                )
            },
            dismissButton = {
                LauncherTextActionButton(
                    text =
                        "Cancelar",
                    onClick = {
                        deleteConfirmationVisible =
                            false
                    }
                )
            }
        )
    }
}

@Composable
private fun CollectionMiniIcon(
    apps: List<InstalledApp>
) {
    val miniApps =
        apps.take(
            4
        )

    val shape =
        RoundedCornerShape(
            13.dp
        )

    Box(
        modifier =
            Modifier
                .size(
                    36.dp
                )
                .clip(
                    shape
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
                        .copy(
                            alpha =
                                0.72f
                        )
                ),
        contentAlignment =
            Alignment.Center
    ) {
        if (
            miniApps.size ==
                1
        ) {
            val iconBitmap =
                rememberLauncherAppIcon(
                    miniApps[0]
                )

            Image(
                bitmap =
                    iconBitmap,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(
                        23.dp
                    )
            )
        } else {
            miniApps.forEachIndexed {
                    index,
                    app ->

                val iconBitmap =
                    rememberLauncherAppIcon(
                        app
                    )

                val alignment =
                    when (
                        index
                    ) {
                        0 ->
                            Alignment.TopStart

                        1 ->
                            Alignment.TopEnd

                        2 ->
                            Alignment.BottomStart

                        else ->
                            Alignment.BottomEnd
                    }

                Image(
                    bitmap =
                        iconBitmap,
                    contentDescription =
                        null,
                    modifier =
                        Modifier
                            .align(
                                alignment
                            )
                            .padding(
                                2.dp
                            )
                            .size(
                                15.dp
                            )
                )
            }
        }
    }
}

@Composable
private fun HomeNotificationBadge(
    count: Int,
    modifier: Modifier =
        Modifier
) {
    Surface(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                50.dp
            ),
        color =
            Color(
                0xFFC74646
            ),
        contentColor =
            Color.White,
        shadowElevation =
            1.dp
    ) {
        Box(
            modifier =
                Modifier
                    .height(
                        18.dp
                    )
                    .padding(
                        horizontal =
                            5.dp
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text =
                    if (
                        count >
                        99
                    ) {
                        "99+"
                    } else {
                        count.toString()
                    },
                fontSize =
                    9.sp,
                lineHeight =
                    10.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    Color.White,
                maxLines =
                    1
            )
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun CollectionHomeBottomSheet(
    collection: CollectionEntity,
    apps: List<InstalledApp>,
    notificationCountFor:
        (InstalledApp) -> Int,
    onDismiss: () -> Unit,
    onEditCollection: () -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onReorderApps:
        (List<InstalledApp>) -> Unit,
    onRemoveApp: (InstalledApp) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded =
                true
        )

    val gridState =
        rememberLazyGridState()

    val coroutineScope =
        rememberCoroutineScope()

    val localApps =
        remember(
            collection.id
        ) {
            mutableStateListOf<InstalledApp>()
                .apply {
                    addAll(
                        apps
                    )
                }
        }

    var draggedAppKey by
        remember(
            collection.id
        ) {
            mutableStateOf<String?>(
                null
            )
        }

    var dropTargetAppKey by
        remember(
            collection.id
        ) {
            mutableStateOf<String?>(
                null
            )
        }

    var draggedTranslation by
        remember(
            collection.id
        ) {
            mutableStateOf(
                Offset.Zero
            )
        }

    var dragPointerInGrid by
        remember(
            collection.id
        ) {
            mutableStateOf(
                Offset.Zero
            )
        }

    var swapAnimationSequence by
        remember(
            collection.id
        ) {
            mutableStateOf(
                0L
            )
        }

    var swapAnimationRequest by
        remember(
            collection.id
        ) {
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

    fun clearDrag() {
        draggedAppKey =
            null

        dropTargetAppKey =
            null

        draggedTranslation =
            Offset.Zero

        dragPointerInGrid =
            Offset.Zero
    }

    LaunchedEffect(
        apps
    ) {
        if (
            draggedAppKey ==
                null
        ) {
            val incomingKeys =
                apps.map {
                    appKey(
                        it
                    )
                }

            val currentKeys =
                localApps.map {
                    appKey(
                        it
                    )
                }

            if (
                incomingKeys !=
                currentKeys
            ) {
                localApps.clear()

                localApps.addAll(
                    apps
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest =
            onDismiss,
        sheetState =
            sheetState,
        containerColor =
            MaterialTheme
                .colorScheme
                .surface
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        bottom =
                            12.dp
                    )
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start =
                                24.dp,
                            end =
                                16.dp,
                            bottom =
                                12.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        collection.name,
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    fontSize =
                        26.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                val editShape =
                    RoundedCornerShape(
                        20.dp
                    )

                Box(
                    modifier =
                        Modifier
                            .clip(
                                editShape
                            )
                            .launcherAppClickable(
                                shape =
                                    editShape,
                                onClickLabel =
                                    "Editar colección",
                                onClick = {
                                    coroutineScope.launch {
                                        delay(
                                            75L
                                        )

                                        onEditCollection()
                                    }
                                }
                            )
                            .padding(
                                horizontal =
                                    14.dp,
                                vertical =
                                    10.dp
                            )
                ) {
                    Text(
                        text =
                            "Editar",
                        fontSize =
                            14.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            if (
                localApps.isEmpty()
            ) {
                Text(
                    text =
                        "Esta colección no tiene aplicaciones.",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    24.dp,
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
            } else {
                LazyVerticalGrid(
                    columns =
                        GridCells.Fixed(
                            4
                        ),
                    state =
                        gridState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min =
                                    110.dp,
                                max =
                                    520.dp
                            ),
                    contentPadding =
                        PaddingValues(
                            start =
                                16.dp,
                            end =
                                16.dp,
                            top =
                                4.dp,
                            bottom =
                                20.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        ),
                    userScrollEnabled =
                        draggedAppKey ==
                            null
                ) {
                    items(
                        items =
                            localApps,
                        key = {
                                app ->

                            collectionSheetGridKey(
                                collectionId =
                                    collection.id,
                                app =
                                    app
                            )
                        },
                        contentType = {
                            "collection-app"
                        }
                    ) {
                            app ->

                        val key =
                            appKey(
                                app
                            )

                        val gridKey =
                            collectionSheetGridKey(
                                collectionId =
                                    collection.id,
                                app =
                                    app
                            )

                        val isGestureActive =
                            draggedAppKey ==
                                key

                        val isDragging =
                            isGestureActive &&
                                draggedTranslation !=
                                    Offset.Zero

                        val isDropTarget =
                            dropTargetAppKey ==
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

                        CollectionSheetAppItem(
                            app =
                                app,
                            notificationCount =
                                notificationCountFor(
                                    app
                                ),
                            isGestureActive =
                                isGestureActive,
                            isDragging =
                                isDragging,
                            isDropTarget =
                                isDropTarget,
                            dragTranslation =
                                if (
                                    isGestureActive
                                ) {
                                    draggedTranslation
                                } else {
                                    Offset.Zero
                                },
                            settleOffset =
                                settleOffset,
                            settleAnimationToken =
                                settleAnimationToken,
                            onClick = {
                                coroutineScope.launch {
                                    delay(
                                        75L
                                    )

                                    onAppClick(
                                        app
                                    )
                                }
                            },
                            onAppInfo = {
                                onAppInfo(
                                    app
                                )
                            },
                            onRemoveFromCollection = {
                                val index =
                                    localApps
                                        .indexOfFirst {
                                            appKey(
                                                it
                                            ) ==
                                                key
                                        }

                                if (
                                    index >=
                                    0
                                ) {
                                    localApps.removeAt(
                                        index
                                    )

                                    onRemoveApp(
                                        app
                                    )
                                }
                            },
                            onUninstallApp = {
                                onUninstallApp(
                                    app
                                )
                            },
                            onDragStart = {
                                    touchOffset ->

                                val itemInfo =
                                    gridState
                                        .layoutInfo
                                        .visibleItemsInfo
                                        .firstOrNull {
                                            it.key ==
                                                gridKey
                                        }

                                if (
                                    itemInfo !=
                                    null
                                ) {
                                    draggedAppKey =
                                        key

                                    dropTargetAppKey =
                                        null

                                    draggedTranslation =
                                        Offset.Zero

                                    dragPointerInGrid =
                                        Offset(
                                            x =
                                                itemInfo
                                                    .offset
                                                    .x
                                                    .toFloat() +
                                                    touchOffset.x,
                                            y =
                                                itemInfo
                                                    .offset
                                                    .y
                                                    .toFloat() +
                                                    touchOffset.y
                                        )
                                }
                            },
                            onDrag = {
                                    dragAmount ->

                                if (
                                    draggedAppKey ==
                                        key
                                ) {
                                    draggedTranslation +=
                                        dragAmount

                                    dragPointerInGrid +=
                                        dragAmount

                                    dropTargetAppKey =
                                        findCollectionSheetDropTarget(
                                            gridState =
                                                gridState,
                                            collectionId =
                                                collection.id,
                                            draggedAppKey =
                                                key,
                                            pointer =
                                                dragPointerInGrid
                                        )
                                }
                            },
                            onDragEnd = {
                                val sourceKey =
                                    draggedAppKey

                                val targetKey =
                                    dropTargetAppKey

                                if (
                                    sourceKey != null &&
                                    targetKey != null &&
                                    sourceKey !=
                                        targetKey
                                ) {
                                    val sourceGridKey =
                                        collectionSheetGridKey(
                                            collectionId =
                                                collection.id,
                                            appKey =
                                                sourceKey
                                        )

                                    val targetGridKey =
                                        collectionSheetGridKey(
                                            collectionId =
                                                collection.id,
                                            appKey =
                                                targetKey
                                        )

                                    val sourceItemInfo =
                                        gridState
                                            .layoutInfo
                                            .visibleItemsInfo
                                            .firstOrNull {
                                                it.key ==
                                                    sourceGridKey
                                            }

                                    val targetItemInfo =
                                        gridState
                                            .layoutInfo
                                            .visibleItemsInfo
                                            .firstOrNull {
                                                it.key ==
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

                                    swapCollectionSheetApps(
                                        apps =
                                            localApps,
                                        sourceAppKey =
                                            sourceKey,
                                        targetAppKey =
                                            targetKey
                                    )

                                    onReorderApps(
                                        localApps.toList()
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

                                clearDrag()
                            },
                            onDragCancel = {
                                val sourceKey =
                                    draggedAppKey

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

                                clearDrag()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionSheetAppItem(
    app: InstalledApp,
    notificationCount: Int,
    isGestureActive: Boolean,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragTranslation: Offset,
    settleOffset: Offset,
    settleAnimationToken: Long?,
    onClick: () -> Unit,
    onAppInfo: () -> Unit,
    onRemoveFromCollection: () -> Unit,
    onUninstallApp: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    val hapticFeedback =
        LocalHapticFeedback.current

    val density =
        LocalDensity.current

    val dragStartThresholdPx =
        remember(
            density
        ) {
            with(density) {
                6.dp.toPx()
            }
        }

    val dragElevationPx =
        remember(
            density
        ) {
            with(density) {
                12.dp.toPx()
            }
        }

    val settleProgress =
        remember(
            settleAnimationToken
        ) {
            Animatable(
                if (
                    settleAnimationToken !=
                    null
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
            settleAnimationToken !=
            null
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
                "collectionSheetDragLift"
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
                "collectionSheetDropTarget"
        )

    val isSettling =
        settleAnimationToken !=
            null &&
            settleProgress.value >
                0.001f

    val animatedSettleTranslation =
        settleOffset *
            settleProgress.value

    var menuExpanded by
        remember(
            app.componentName,
            app.user
        ) {
            mutableStateOf(
                false
            )
        }

    val itemShape =
        RoundedCornerShape(
            18.dp
        )

    val dragSurfaceColor =
        MaterialTheme
            .colorScheme
            .surfaceContainerHigh
            .copy(
                alpha =
                    0.72f +
                        (0.20f *
                            dragLiftProgress)
            )

    val dropTargetColor =
        MaterialTheme
            .colorScheme
            .onSurface

    Box(
        modifier =
            Modifier
                .zIndex(
                    when {
                        isGestureActive ->
                            3f

                        isSettling ->
                            1f

                        else ->
                            0f
                    }
                )
                .graphicsLayer {
                    translationX =
                        if (
                            isGestureActive
                        ) {
                            dragTranslation.x
                        } else {
                            animatedSettleTranslation.x
                        }

                    translationY =
                        if (
                            isGestureActive
                        ) {
                            dragTranslation.y
                        } else {
                            animatedSettleTranslation.y
                        }

                    shadowElevation =
                        dragElevationPx *
                            dragLiftProgress

                    shape =
                        itemShape

                    clip =
                        false
                }
                .then(
                    if (
                        isGestureActive
                    ) {
                        Modifier.background(
                            color =
                                dragSurfaceColor,
                            shape =
                                itemShape
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    color =
                        dropTargetColor.copy(
                            alpha =
                                0.045f *
                                    dropTargetProgress
                        ),
                    shape =
                        itemShape
                )
                .border(
                    width =
                        1.dp,
                    color =
                        dropTargetColor.copy(
                            alpha =
                                0.30f *
                                    dropTargetProgress
                        ),
                    shape =
                        itemShape
                )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min =
                            92.dp
                    )
                    .pointerInput(
                        app.componentName,
                        app.user,
                        dragStartThresholdPx
                    ) {
                        var cumulativeDrag =
                            Offset.Zero

                        var actualDragStarted =
                            false

                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                    touchOffset ->

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false

                                menuExpanded =
                                    false

                                hapticFeedback
                                    .performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )

                                onDragStart(
                                    touchOffset
                                )
                            },
                            onDragEnd = {
                                if (
                                    actualDragStarted
                                ) {
                                    onDragEnd()
                                } else {
                                    onDragCancel()

                                    menuExpanded =
                                        true
                                }

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false
                            },
                            onDragCancel = {
                                onDragCancel()

                                cumulativeDrag =
                                    Offset.Zero

                                actualDragStarted =
                                    false
                            },
                            onDrag = {
                                    change,
                                    dragAmount ->

                                cumulativeDrag +=
                                    dragAmount

                                if (
                                    !actualDragStarted &&
                                    cumulativeDrag
                                        .getDistance() >=
                                        dragStartThresholdPx
                                ) {
                                    actualDragStarted =
                                        true

                                    change.consume()

                                    onDrag(
                                        cumulativeDrag
                                    )
                                } else if (
                                    actualDragStarted
                                ) {
                                    change.consume()

                                    onDrag(
                                        dragAmount
                                    )
                                }
                            }
                        )
                    }
                    .launcherAppClickable(
                        enabled =
                            !isGestureActive,
                        shape =
                            itemShape,
                        onClickLabel =
                            "Abrir ${app.label}",
                        onClick =
                            onClick
                    )
                    .padding(
                        horizontal =
                            4.dp,
                        vertical =
                            8.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            LauncherAppIconWithBadge(
                bitmap =
                    iconBitmap,
                contentDescription =
                    app.label,
                iconSize =
                    42.dp,
                notificationCount =
                    notificationCount
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    app.label,
                fontSize =
                    12.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                textAlign =
                    TextAlign.Center,
                maxLines =
                    2,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        AppContextMenu(
            expanded =
                menuExpanded,
            app =
                app,
            showRemoveFromHome =
                true,
            removeLabel =
                "Quitar de la colección",
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

                onRemoveFromCollection()
            },
            onUninstallApp = {
                menuExpanded =
                    false

                onUninstallApp()
            }
        )
    }
}

private fun findCollectionDropTarget(
    collections: List<CollectionEntity>,
    itemBoundsById: Map<Long, Rect>,
    draggedCollectionId: Long,
    pointer: Offset
): Long? {
    return collections
        .asSequence()
        .mapNotNull {
                collection ->

            if (
                collection.id ==
                draggedCollectionId
            ) {
                return@mapNotNull null
            }

            val bounds =
                itemBoundsById[
                    collection.id
                ] ?: return@mapNotNull null

            val horizontalTolerance =
                bounds.width *
                    0.16f

            val verticalTolerance =
                bounds.height *
                    0.18f

            val isNearCandidate =
                pointer.x >=
                    bounds.left -
                        horizontalTolerance &&
                pointer.x <=
                    bounds.right +
                        horizontalTolerance &&
                pointer.y >=
                    bounds.top -
                        verticalTolerance &&
                pointer.y <=
                    bounds.bottom +
                        verticalTolerance

            if (
                !isNearCandidate
            ) {
                return@mapNotNull null
            }

            val dx =
                pointer.x -
                    bounds.center.x

            val dy =
                pointer.y -
                    bounds.center.y

            collection.id to
                (dx * dx +
                    dy * dy)
        }
        .minByOrNull {
            it.second
        }
        ?.first
}

private fun swapHomeCollections(
    collections: List<CollectionEntity>,
    sourceCollectionId: Long,
    targetCollectionId: Long
): List<CollectionEntity> {
    val reordered =
        collections.toMutableList()

    val sourceIndex =
        reordered
            .indexOfFirst {
                it.id ==
                    sourceCollectionId
            }

    val targetIndex =
        reordered
            .indexOfFirst {
                it.id ==
                    targetCollectionId
            }

    if (
        sourceIndex <
            0 ||
        targetIndex <
            0 ||
        sourceIndex ==
            targetIndex
    ) {
        return collections
    }

    val sourceCollection =
        reordered[
            sourceIndex
        ]

    reordered[
        sourceIndex
    ] =
        reordered[
            targetIndex
        ]

    reordered[
        targetIndex
    ] =
        sourceCollection

    return reordered
        .mapIndexed {
                index,
                collection ->

            collection.copy(
                position =
                    index
            )
        }
}

private fun collectionKey(
    collectionId: Long
): String {
    return "collection:$collectionId"
}

private fun collectionSheetGridKey(
    collectionId: Long,
    app: InstalledApp
): String {
    return collectionSheetGridKey(
        collectionId =
            collectionId,
        appKey =
            appKey(
                app
            )
    )
}

private fun collectionSheetGridKey(
    collectionId: Long,
    appKey: String
): String {
    return "collection-sheet:$collectionId:$appKey"
}

private fun findCollectionSheetDropTarget(
    gridState:
        androidx.compose.foundation.lazy.grid.LazyGridState,
    collectionId: Long,
    draggedAppKey: String,
    pointer: Offset
): String? {
    val prefix =
        "collection-sheet:$collectionId:"

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
                    prefix
                )
            ) {
                return@mapNotNull null
            }

            val candidateAppKey =
                gridKey.removePrefix(
                    prefix
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

private fun swapCollectionSheetApps(
    apps: MutableList<InstalledApp>,
    sourceAppKey: String,
    targetAppKey: String
) {
    val sourceIndex =
        apps
            .indexOfFirst {
                appKey(
                    it
                ) ==
                    sourceAppKey
            }

    val targetIndex =
        apps
            .indexOfFirst {
                appKey(
                    it
                ) ==
                    targetAppKey
            }

    if (
        sourceIndex <
            0 ||
        targetIndex <
            0 ||
        sourceIndex ==
            targetIndex
    ) {
        return
    }

    val sourceApp =
        apps[
            sourceIndex
        ]

    apps[
        sourceIndex
    ] =
        apps[
            targetIndex
        ]

    apps[
        targetIndex
    ] =
        sourceApp
}


private data class SwapAnimationRequest(
    val id: Long,
    val offsets: Map<String, Offset>
)

private fun findHomeDropTarget(
    apps: List<InstalledApp>,
    itemBoundsByKey: Map<String, Rect>,
    draggedAppKey: String,
    pointer: Offset
): String? {
    return apps
        .asSequence()
        .mapNotNull {
                app ->

            val candidateKey =
                appKey(
                    app
                )

            if (
                candidateKey ==
                draggedAppKey
            ) {
                return@mapNotNull null
            }

            val bounds =
                itemBoundsByKey[
                    candidateKey
                ] ?: return@mapNotNull null

            val horizontalTolerance =
                bounds.width *
                    0.16f

            val verticalTolerance =
                bounds.height *
                    0.18f

            val isNearCandidate =
                pointer.x >=
                    bounds.left -
                        horizontalTolerance &&
                pointer.x <=
                    bounds.right +
                        horizontalTolerance &&
                pointer.y >=
                    bounds.top -
                        verticalTolerance &&
                pointer.y <=
                    bounds.bottom +
                        verticalTolerance

            if (
                !isNearCandidate
            ) {
                return@mapNotNull null
            }

            val dx =
                pointer.x -
                    bounds.center.x

            val dy =
                pointer.y -
                    bounds.center.y

            candidateKey to
                (dx * dx +
                    dy * dy)
        }
        .minByOrNull {
            it.second
        }
        ?.first
}

private fun swapFavoriteApps(
    apps: List<InstalledApp>,
    sourceAppKey: String,
    targetAppKey: String
): List<InstalledApp> {
    val reordered =
        apps.toMutableList()

    val sourceIndex =
        reordered
            .indexOfFirst {
                    app ->

                appKey(
                    app
                ) ==
                    sourceAppKey
            }

    val targetIndex =
        reordered
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
        return apps
    }

    /*
     * The Home layout is positional: dropping one favorite over
     * another swaps only those two slots. Apps between them keep
     * their exact positions.
     */
    val sourceApp =
        reordered[
            sourceIndex
        ]

    reordered[
        sourceIndex
    ] =
        reordered[
            targetIndex
        ]

    reordered[
        targetIndex
    ] =
        sourceApp

    return reordered
}

private fun appKey(
    app: InstalledApp
): String {
    return "${app.user.hashCode()}:" +
        app.componentName
            .flattenToString()
}

private fun wallpaperTextStyle(
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight
): TextStyle {
    val shadowColor =
        if (
            color.luminance() >
            0.5f
        ) {
            Color.Black.copy(
                alpha =
                    0.45f
            )
        } else {
            Color.White.copy(
                alpha =
                    0.35f
            )
        }

    return TextStyle(
        color =
            color,
        fontSize =
            fontSize,
        fontWeight =
            fontWeight,
        shadow =
            Shadow(
                color =
                    shadowColor,
                offset =
                    Offset(
                        x =
                            0f,
                        y =
                            1f
                    ),
                blurRadius =
                    3f
            )
    )
}

private const val HOME_ROOT_TAG =
    "launcher_home_root"
