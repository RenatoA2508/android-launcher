package com.renato.launcher.collections

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.notifications.NotificationAppKey
import com.renato.launcher.notifications.NotificationBadgeStore
import com.renato.launcher.search.AppSearchEngine
import com.renato.launcher.ui.components.LauncherAppCatalog
import com.renato.launcher.ui.components.LauncherAppCatalogItem
import com.renato.launcher.ui.components.LauncherAppIconWithBadge
import com.renato.launcher.ui.components.LauncherDropdownMenu
import com.renato.launcher.ui.components.LauncherMenuItem
import com.renato.launcher.ui.components.LauncherPrimaryActionButton
import com.renato.launcher.ui.components.LauncherTextActionButton
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RC2 collection editor.
 *
 * The selected section is a direct preview of the opened collection: title +
 * four-column app grid. The lower browser is the same canonical All Apps
 * catalog used everywhere else in the launcher.
 */
@Composable
fun CollectionEditorScreen(
    apps: List<InstalledApp>,
    appsLoaded: Boolean,
    initialName: String,
    initialSelection: List<InstalledApp>,
    isEditing: Boolean,
    applyWindowEffect: Boolean = true,
    onCancel: () -> Unit,
    onSave: (String, List<InstalledApp>) -> Unit
) {
    if (applyWindowEffect) {
        CollectionWindowEffect()
    }
    BackHandler(onBack = onCancel)

    val notificationCounts by NotificationBadgeStore.counts.collectAsState()

    var collectionName by
        remember(initialName) {
            mutableStateOf(initialName)
        }

    val selectedApps =
        remember(initialSelection) {
            mutableStateListOf<InstalledApp>().apply {
                addAll(initialSelection)
            }
        }

    val selectedSnapshot = selectedApps.toList()
    val selectedKeys =
        remember(selectedSnapshot) {
            selectedSnapshot.map(::appKey).toSet()
        }

    val searchEngine =
        remember(apps) {
            AppSearchEngine(apps = apps)
        }

    var searchQuery by remember { mutableStateOf("") }
    val displayedApps =
        remember(searchEngine, searchQuery, apps) {
            if (searchQuery.isBlank()) apps else searchEngine.search(searchQuery)
        }

    val itemBoundsByKey = remember { mutableStateMapOf<String, Rect>() }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dropTargetKey by remember { mutableStateOf<String?>(null) }
    var dragTranslation by remember { mutableStateOf(Offset.Zero) }
    var dragPointerInRoot by remember { mutableStateOf(Offset.Zero) }

    var swapAnimationSequence by remember { mutableLongStateOf(0L) }
    var swapAnimationRequest by
        remember {
            mutableStateOf<CollectionPreviewSwapAnimationRequest?>(null)
        }

    fun clearDrag() {
        draggedKey = null
        dropTargetKey = null
        dragTranslation = Offset.Zero
        dragPointerInRoot = Offset.Zero
    }

    fun requestSettleAnimation(offsets: Map<String, Offset>) {
        if (offsets.isEmpty()) return
        swapAnimationSequence += 1L
        swapAnimationRequest =
            CollectionPreviewSwapAnimationRequest(
                id = swapAnimationSequence,
                offsets = offsets
            )
    }

    fun toggleSelection(app: InstalledApp) {
        val existingIndex =
            selectedApps.indexOfFirst { selected ->
                isSameApp(selected, app)
            }

        if (existingIndex >= 0) {
            selectedApps.removeAt(existingIndex)
        } else {
            selectedApps.add(app)
        }
    }

    val canSave =
        collectionName.isNotBlank() && selectedApps.isNotEmpty()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {
            CollectionEditorHeader(
                title = if (isEditing) "Editar colección" else "Nueva colección",
                canSave = canSave,
                onBack = onCancel,
                onSave = {
                    onSave(
                        collectionName.trim(),
                        selectedApps.toList()
                    )
                }
            )

            LauncherAppCatalog(
                apps = displayedApps,
                appsLoaded = appsLoaded,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding(),
                userScrollEnabled = draggedKey == null,
                emptyStateText =
                    if (searchQuery.isNotBlank()) {
                        "No encontramos ninguna aplicación con ese nombre."
                    } else {
                        null
                    },
                headerContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CollectionNameField(
                            value = collectionName,
                            onValueChange = { collectionName = it }
                        )

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vista previa de la colección",
                                modifier = Modifier.weight(1f),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "${selectedSnapshot.size}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        CollectionInsidePreview(
                            collectionName = collectionName,
                            selectedApps = selectedSnapshot,
                            notificationCounts = notificationCounts,
                            itemBoundsByKey = itemBoundsByKey,
                            draggedKey = draggedKey,
                            dropTargetKey = dropTargetKey,
                            dragTranslation = dragTranslation,
                            swapAnimationRequest = swapAnimationRequest,
                            onRemove = { app ->
                                removeSelectedApp(selectedApps, app)
                            },
                            onDragStart = { app, touchOffset ->
                                val key = appKey(app)
                                val bounds = itemBoundsByKey[key]

                                if (bounds != null) {
                                    draggedKey = key
                                    dropTargetKey = null
                                    dragTranslation = Offset.Zero
                                    dragPointerInRoot =
                                        Offset(
                                            x = bounds.left + touchOffset.x,
                                            y = bounds.top + touchOffset.y
                                        )
                                }
                            },
                            onDrag = { dragAmount ->
                                val sourceKey = draggedKey
                                if (sourceKey != null) {
                                    dragTranslation += dragAmount
                                    dragPointerInRoot += dragAmount
                                    dropTargetKey =
                                        findCollectionPreviewDropTarget(
                                            itemBoundsByKey = itemBoundsByKey,
                                            draggedAppKey = sourceKey,
                                            pointer = dragPointerInRoot
                                        )
                                }
                            },
                            onDragEnd = {
                                val sourceKey = draggedKey
                                val targetKey = dropTargetKey

                                if (
                                    sourceKey != null &&
                                    targetKey != null &&
                                    sourceKey != targetKey
                                ) {
                                    val sourceBounds = itemBoundsByKey[sourceKey]
                                    val targetBounds = itemBoundsByKey[targetKey]

                                    if (sourceBounds != null && targetBounds != null) {
                                        val sourceOld = Offset(sourceBounds.left, sourceBounds.top)
                                        val targetOld = Offset(targetBounds.left, targetBounds.top)
                                        requestSettleAnimation(
                                            mapOf(
                                                sourceKey to
                                                    (sourceOld + dragTranslation - targetOld),
                                                targetKey to
                                                    (targetOld - sourceOld)
                                            )
                                        )
                                    }

                                    swapSelectedApps(
                                        selectedApps = selectedApps,
                                        sourceAppKey = sourceKey,
                                        targetAppKey = targetKey
                                    )
                                } else if (
                                    sourceKey != null &&
                                    dragTranslation != Offset.Zero
                                ) {
                                    requestSettleAnimation(
                                        mapOf(sourceKey to dragTranslation)
                                    )
                                }

                                clearDrag()
                            },
                            onDragCancel = {
                                val sourceKey = draggedKey
                                if (
                                    sourceKey != null &&
                                    dragTranslation != Offset.Zero
                                ) {
                                    requestSettleAnimation(
                                        mapOf(sourceKey to dragTranslation)
                                    )
                                }
                                clearDrag()
                            }
                        )

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "Añadir aplicaciones",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(8.dp))

                        CollectionCatalogSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "${selectedSnapshot.size} aplicaciones en la colección",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(4.dp))
                    }
                }
            ) { app ->
                val isSelected = appKey(app) in selectedKeys

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LauncherAppCatalogItem(
                        app = app,
                        notificationCount = 0,
                        onClickLabel =
                            if (isSelected) {
                                "Quitar ${app.label} de la colección"
                            } else {
                                "Añadir ${app.label} a la colección"
                            },
                        onClick = {
                            toggleSelection(app)
                        }
                    )

                    if (isSelected) {
                        CollectionSelectedCatalogMarker(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionEditorHeader(
    title: String,
    canSave: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 14.dp,
                    top = 6.dp,
                    bottom = 8.dp
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LauncherTextActionButton(
            text = "‹",
            fontSize = 32.sp,
            onClick = onBack
        )

        Text(
            text = title,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 2.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LauncherPrimaryActionButton(
            text = "Listo",
            enabled = canSave,
            onClick = onSave
        )
    }
}

@Composable
private fun CollectionNameField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.length <= 40) {
                onValueChange(newValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text("Nombre de la colección")
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun CollectionInsidePreview(
    collectionName: String,
    selectedApps: List<InstalledApp>,
    notificationCounts: Map<NotificationAppKey, Int>,
    itemBoundsByKey: MutableMap<String, Rect>,
    draggedKey: String?,
    dropTargetKey: String?,
    dragTranslation: Offset,
    swapAnimationRequest: CollectionPreviewSwapAnimationRequest?,
    onRemove: (InstalledApp) -> Unit,
    onDragStart: (InstalledApp, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
                )
                .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text =
                collectionName.ifBlank {
                    "Nombre de la colección"
                },
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color =
                if (collectionName.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(10.dp))

        if (selectedApps.isEmpty()) {
            Text(
                text = "Añade aplicaciones para ver cómo quedarán dentro de la colección.",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            selectedApps.chunked(4).forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowApps.forEach { app ->
                        val key = appKey(app)
                        val isDragging = draggedKey == key
                        val settleRequest = swapAnimationRequest
                        val settleOffset = settleRequest?.offsets?.get(key) ?: Offset.Zero
                        val settleToken =
                            if (settleRequest?.offsets?.containsKey(key) == true) {
                                settleRequest.id
                            } else {
                                null
                            }

                        CollectionInsidePreviewAppItem(
                            app = app,
                            notificationCount =
                                NotificationBadgeStore.countFor(
                                    notificationCounts,
                                    app
                                ),
                            isGestureActive = isDragging,
                            isDragging = isDragging,
                            isDropTarget = dropTargetKey == key,
                            dragTranslation =
                                if (isDragging) dragTranslation else Offset.Zero,
                            settleOffset = settleOffset,
                            settleAnimationToken = settleToken,
                            onBoundsChanged = { bounds ->
                                itemBoundsByKey[key] = bounds
                            },
                            onRemove = {
                                onRemove(app)
                            },
                            onDragStart = { offset ->
                                onDragStart(app, offset)
                            },
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    repeat(4 - rowApps.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionInsidePreviewAppItem(
    app: InstalledApp,
    notificationCount: Int,
    isGestureActive: Boolean,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragTranslation: Offset,
    settleOffset: Offset,
    settleAnimationToken: Long?,
    onBoundsChanged: (Rect) -> Unit,
    onRemove: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconBitmap = rememberLauncherAppIcon(app)
    val hapticFeedback = LocalHapticFeedback.current
    val gestureScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragStartThresholdPx = remember(density) { with(density) { 6.dp.toPx() } }
    val dragElevationPx = remember(density) { with(density) { 12.dp.toPx() } }

    val settleProgress =
        remember(settleAnimationToken) {
            Animatable(if (settleAnimationToken != null) 1f else 0f)
        }

    LaunchedEffect(settleAnimationToken) {
        if (settleAnimationToken != null) {
            settleProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 700f)
            )
        }
    }

    val dragLiftProgress by
        animateFloatAsState(
            targetValue = if (isDragging) 1f else 0f,
            animationSpec = tween(110),
            label = "collectionEditorPreviewDragLift"
        )

    val dropTargetProgress by
        animateFloatAsState(
            targetValue = if (isDropTarget) 1f else 0f,
            animationSpec = tween(100),
            label = "collectionEditorPreviewDropTarget"
        )

    val shape = RoundedCornerShape(18.dp)
    val animatedSettleTranslation = settleOffset * settleProgress.value
    var menuExpanded by remember(app.componentName, app.user) { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    onBoundsChanged(
                        Rect(
                            offset = position,
                            size =
                                Size(
                                    coordinates.size.width.toFloat(),
                                    coordinates.size.height.toFloat()
                                )
                        )
                    )
                }
                .zIndex(
                    when {
                        isGestureActive -> 3f
                        settleProgress.value > 0.001f -> 1f
                        else -> 0f
                    }
                )
                .graphicsLayer {
                    translationX =
                        if (isGestureActive) dragTranslation.x
                        else animatedSettleTranslation.x
                    translationY =
                        if (isGestureActive) dragTranslation.y
                        else animatedSettleTranslation.y
                    shadowElevation = dragElevationPx * dragLiftProgress
                    this.shape = shape
                    clip = false
                }
                .then(
                    if (isGestureActive) {
                        Modifier.background(
                            color =
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                    alpha = 0.72f + (0.20f * dragLiftProgress)
                                ),
                            shape = shape
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.045f * dropTargetProgress
                        ),
                    shape = shape
                )
                .border(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.30f * dropTargetProgress
                        ),
                    shape = shape
                )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 92.dp)
                    .pointerInput(app.componentName, app.user, dragStartThresholdPx) {
                        var cumulativeDrag = Offset.Zero
                        var actualDragStarted = false
                        var longPressGestureActive = false

                        detectDragGesturesAfterLongPress(
                            onDragStart = { touchOffset ->
                                cumulativeDrag = Offset.Zero
                                actualDragStarted = false
                                longPressGestureActive = true
                                menuExpanded = false
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart(touchOffset)

                                gestureScope.launch {
                                    delay(LONG_PRESS_MENU_REVEAL_DELAY_MILLIS)
                                    if (
                                        longPressGestureActive &&
                                        !actualDragStarted &&
                                        !menuExpanded
                                    ) {
                                        menuExpanded = true
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                cumulativeDrag += dragAmount
                                if (
                                    !actualDragStarted &&
                                    cumulativeDrag.getDistance() >= dragStartThresholdPx
                                ) {
                                    actualDragStarted = true
                                    menuExpanded = false
                                    change.consume()
                                    onDrag(cumulativeDrag)
                                } else if (actualDragStarted) {
                                    change.consume()
                                    onDrag(dragAmount)
                                }
                            },
                            onDragEnd = {
                                longPressGestureActive = false
                                if (actualDragStarted) {
                                    onDragEnd()
                                } else {
                                    onDragCancel()
                                    if (!menuExpanded) menuExpanded = true
                                }
                                cumulativeDrag = Offset.Zero
                                actualDragStarted = false
                            },
                            onDragCancel = {
                                longPressGestureActive = false
                                onDragCancel()
                                cumulativeDrag = Offset.Zero
                                actualDragStarted = false
                            }
                        )
                    }
                    .launcherAppClickable(
                        enabled = !isGestureActive,
                        shape = shape,
                        onClickLabel = "Aplicación ${app.label}",
                        onClick = {}
                    )
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LauncherAppIconWithBadge(
                bitmap = iconBitmap,
                contentDescription = app.label,
                iconSize = 42.dp,
                notificationCount = notificationCount
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = app.label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        LauncherDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            minWidth = 230.dp,
            maxWidth = 280.dp
        ) {
            LauncherMenuItem(
                onClick = {
                    menuExpanded = false
                    onRemove()
                }
            ) {
                Text(
                    text = "Quitar de la colección",
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CollectionCatalogSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("Buscar aplicaciones")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                LauncherTextActionButton(
                    text = "×",
                    fontSize = 22.sp,
                    deferActionForRipple = false,
                    onClick = { onQueryChange("") }
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun CollectionSelectedCatalogMarker(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(20.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private data class CollectionPreviewSwapAnimationRequest(
    val id: Long,
    val offsets: Map<String, Offset>
)

private fun findCollectionPreviewDropTarget(
    itemBoundsByKey: Map<String, Rect>,
    draggedAppKey: String,
    pointer: Offset
): String? {
    return itemBoundsByKey
        .asSequence()
        .filter { (key, _) -> key != draggedAppKey }
        .mapNotNull { (key, bounds) ->
            val horizontalTolerance = bounds.width * 0.18f
            val verticalTolerance = bounds.height * 0.18f
            val isNear =
                pointer.x >= bounds.left - horizontalTolerance &&
                    pointer.x <= bounds.right + horizontalTolerance &&
                    pointer.y >= bounds.top - verticalTolerance &&
                    pointer.y <= bounds.bottom + verticalTolerance

            if (!isNear) {
                null
            } else {
                val dx = pointer.x - bounds.center.x
                val dy = pointer.y - bounds.center.y
                key to (dx * dx + dy * dy)
            }
        }
        .minByOrNull { it.second }
        ?.first
}

private fun swapSelectedApps(
    selectedApps: MutableList<InstalledApp>,
    sourceAppKey: String,
    targetAppKey: String
) {
    val sourceIndex = selectedApps.indexOfFirst { appKey(it) == sourceAppKey }
    val targetIndex = selectedApps.indexOfFirst { appKey(it) == targetAppKey }

    if (sourceIndex < 0 || targetIndex < 0 || sourceIndex == targetIndex) return

    val sourceApp = selectedApps[sourceIndex]
    selectedApps[sourceIndex] = selectedApps[targetIndex]
    selectedApps[targetIndex] = sourceApp
}

private fun removeSelectedApp(
    selectedApps: MutableList<InstalledApp>,
    app: InstalledApp
) {
    val index = selectedApps.indexOfFirst { isSameApp(it, app) }
    if (index >= 0) selectedApps.removeAt(index)
}

private fun isSameApp(
    first: InstalledApp,
    second: InstalledApp
): Boolean {
    return first.componentName == second.componentName &&
        first.user == second.user
}

private fun appKey(app: InstalledApp): String =
    app.componentName.flattenToString() + "@" + app.user.hashCode()

private const val LONG_PRESS_MENU_REVEAL_DELAY_MILLIS = 140L
