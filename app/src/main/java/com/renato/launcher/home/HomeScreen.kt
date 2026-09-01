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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.renato.launcher.ui.components.AppContextMenu
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppClickable
import com.renato.launcher.ui.interactions.launcherCombinedClickable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    favoriteApps: List<InstalledApp>,
    favoritesLoaded: Boolean,
    onAppClick: (InstalledApp) -> Unit,
    onChooseFavorites: () -> Unit,
    onEditFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onRemoveFavorite: (InstalledApp) -> Unit,
    onReorderFavorites: (List<InstalledApp>) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit
) {
    val wallpaperTextColor =
        rememberWallpaperTextColor()

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
                    .launcherCombinedClickable(
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
                        72.dp
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

        Button(
            onClick =
                onChooseFavorites
        ) {
            Text(
                text =
                    "Seleccionar aplicaciones"
            )
        }
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
            Image(
                bitmap =
                    iconBitmap,
                contentDescription =
                    app.label,
                modifier =
                    Modifier.size(
                        30.dp
                    )
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

