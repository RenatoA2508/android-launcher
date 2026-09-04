package com.renato.launcher.favorites

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.renato.launcher.collections.CollectionEditorScreen
import com.renato.launcher.collections.MAX_COLLECTIONS
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.collection.CollectionEntity
import com.renato.launcher.notifications.NotificationAppKey
import com.renato.launcher.notifications.NotificationBadgeStore
import com.renato.launcher.search.AppSearchEngine
import com.renato.launcher.ui.components.LauncherAppCatalog
import com.renato.launcher.ui.components.LauncherAppCatalogItem
import com.renato.launcher.ui.components.CollectionContextMenu
import com.renato.launcher.ui.components.LauncherAppIconWithBadge
import com.renato.launcher.ui.components.LauncherDropdownMenu
import com.renato.launcher.ui.components.LauncherMenuItem
import com.renato.launcher.ui.components.LauncherPrimaryActionButton
import com.renato.launcher.ui.components.LauncherTextActionButton
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RC2 Favorites editor.
 *
 * The top of the screen is a real two-column Home preview. The lower section
 * uses the canonical All Apps catalog, so "show every app" has one visual
 * language everywhere in the launcher.
 *
 * Edits remain transactional: Room is only changed when Listo is pressed.
 */
@Composable
fun FavoritePickerScreen(
    apps: List<InstalledApp>,
    appsLoaded: Boolean,
    initialSelection: List<InstalledApp>,
    collections: List<CollectionEntity>,
    collectionAppsById: Map<Long, List<InstalledApp>>,
    onManageCollections: () -> Unit,
    onCancel: () -> Unit,
    onSave: (List<InstalledApp>, List<CollectionEntity>, Map<Long, List<InstalledApp>>) -> Unit
) {
    FavoritePickerWindowEffect()

    val notificationCounts by
        NotificationBadgeStore.counts.collectAsState()

    val selectedApps =
        remember(initialSelection) {
            mutableStateListOf<InstalledApp>().apply {
                addAll(initialSelection.take(MAX_FAVORITES))
            }
        }

    val previewCollections =
        remember(collections) {
            mutableStateListOf<CollectionEntity>().apply {
                addAll(collections.take(MAX_COLLECTIONS))
            }
        }

    val previewCollectionAppsById =
        remember(collections, collectionAppsById) {
            mutableStateMapOf<Long, List<InstalledApp>>().apply {
                collections
                    .take(MAX_COLLECTIONS)
                    .forEach { collection ->
                        put(
                            collection.id,
                            collectionAppsById[collection.id].orEmpty()
                        )
                    }
            }
        }

    var editingPreviewCollectionId by
        remember { mutableStateOf<Long?>(null) }

    val editingPreviewCollection =
        editingPreviewCollectionId
            ?.let { collectionId ->
                previewCollections.firstOrNull { it.id == collectionId }
            }

    if (editingPreviewCollection != null) {
        CollectionEditorScreen(
            apps = apps,
            appsLoaded = appsLoaded,
            initialName = editingPreviewCollection.name,
            initialSelection =
                previewCollectionAppsById[editingPreviewCollection.id].orEmpty(),
            isEditing = true,
            applyWindowEffect = false,
            onCancel = {
                editingPreviewCollectionId = null
            },
            onSave = { name, selectedCollectionApps ->
                val index =
                    previewCollections.indexOfFirst {
                        it.id == editingPreviewCollection.id
                    }

                if (index >= 0) {
                    previewCollections[index] =
                        previewCollections[index].copy(name = name.trim())
                    previewCollectionAppsById[editingPreviewCollection.id] =
                        selectedCollectionApps
                }

                editingPreviewCollectionId = null
            }
        )
        return
    }

    BackHandler(onBack = onCancel)

    val selectedSnapshot = selectedApps.toList()
    val collectionSnapshot = previewCollections.toList()
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
            if (searchQuery.isBlank()) {
                apps
            } else {
                searchEngine.search(searchQuery)
            }
        }

    var limitNoticeVisible by remember { mutableStateOf(false) }
    var limitNoticeRevision by remember { mutableLongStateOf(0L) }

    LaunchedEffect(limitNoticeRevision) {
        if (limitNoticeRevision == 0L) return@LaunchedEffect
        limitNoticeVisible = true
        delay(1800L)
        limitNoticeVisible = false
    }

    val itemBoundsByKey = remember { mutableStateMapOf<String, Rect>() }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dropTargetKey by remember { mutableStateOf<String?>(null) }
    var dragTranslation by remember { mutableStateOf(Offset.Zero) }
    var dragPointerInRoot by remember { mutableStateOf(Offset.Zero) }

    var swapAnimationSequence by remember { mutableLongStateOf(0L) }
    var swapAnimationRequest by remember { mutableStateOf<PreviewSwapAnimationRequest?>(null) }

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
            PreviewSwapAnimationRequest(
                id = swapAnimationSequence,
                offsets = offsets
            )
    }

    val collectionBoundsById = remember { mutableStateMapOf<Long, Rect>() }
    var draggedCollectionId by remember { mutableStateOf<Long?>(null) }
    var dropTargetCollectionId by remember { mutableStateOf<Long?>(null) }
    var collectionDragTranslation by remember { mutableStateOf(Offset.Zero) }
    var collectionDragPointerInRoot by remember { mutableStateOf(Offset.Zero) }

    var collectionSwapAnimationSequence by remember { mutableLongStateOf(0L) }
    var collectionSwapAnimationRequest by remember { mutableStateOf<PreviewSwapAnimationRequest?>(null) }

    fun clearCollectionDrag() {
        draggedCollectionId = null
        dropTargetCollectionId = null
        collectionDragTranslation = Offset.Zero
        collectionDragPointerInRoot = Offset.Zero
    }

    fun requestCollectionSettleAnimation(offsets: Map<String, Offset>) {
        if (offsets.isEmpty()) return
        collectionSwapAnimationSequence += 1L
        collectionSwapAnimationRequest =
            PreviewSwapAnimationRequest(
                id = collectionSwapAnimationSequence,
                offsets = offsets
            )
    }

    fun toggleCatalogSelection(app: InstalledApp) {
        val existingIndex =
            selectedApps.indexOfFirst {
                isSameApp(it, app)
            }

        if (existingIndex >= 0) {
            selectedApps.removeAt(existingIndex)
            return
        }

        if (selectedApps.size >= MAX_FAVORITES) {
            limitNoticeRevision += 1L
            return
        }

        selectedApps.add(app)
    }

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
            FavoritesEditorHeader(
                onBack = onCancel,
                onSave = {
                    onSave(
                        selectedApps.toList(),
                        previewCollections.toList(),
                        previewCollectionAppsById.toMap()
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
                userScrollEnabled = draggedKey == null && draggedCollectionId == null,
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
                        PreviewHeading(
                            selectedCount = selectedSnapshot.size
                        )

                        Spacer(Modifier.height(10.dp))

                        HomePreview(
                            selectedApps = selectedSnapshot,
                            collections = collectionSnapshot,
                            collectionAppsById = previewCollectionAppsById,
                            notificationCounts = notificationCounts,
                            itemBoundsByKey = itemBoundsByKey,
                            draggedKey = draggedKey,
                            dropTargetKey = dropTargetKey,
                            dragTranslation = dragTranslation,
                            swapAnimationRequest = swapAnimationRequest,
                            collectionBoundsById = collectionBoundsById,
                            draggedCollectionId = draggedCollectionId,
                            dropTargetCollectionId = dropTargetCollectionId,
                            collectionDragTranslation = collectionDragTranslation,
                            collectionSwapAnimationRequest = collectionSwapAnimationRequest,
                            onEditCollection = { collectionId ->
                                editingPreviewCollectionId = collectionId
                            },
                            onDeleteCollection = { collectionId ->
                                previewCollections.removeAll { it.id == collectionId }
                                previewCollectionAppsById.remove(collectionId)
                                collectionBoundsById.remove(collectionId)
                            },
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
                                        findPreviewDropTarget(
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
                            },
                            onCollectionDragStart = { collection, touchOffset ->
                                val bounds = collectionBoundsById[collection.id]
                                if (bounds != null) {
                                    draggedCollectionId = collection.id
                                    dropTargetCollectionId = null
                                    collectionDragTranslation = Offset.Zero
                                    collectionDragPointerInRoot =
                                        Offset(
                                            x = bounds.left + touchOffset.x,
                                            y = bounds.top + touchOffset.y
                                        )
                                }
                            },
                            onCollectionDrag = { dragAmount ->
                                val sourceId = draggedCollectionId
                                if (sourceId != null) {
                                    collectionDragTranslation += dragAmount
                                    collectionDragPointerInRoot += dragAmount
                                    dropTargetCollectionId =
                                        findPreviewCollectionDropTarget(
                                            collections = collectionSnapshot,
                                            itemBoundsById = collectionBoundsById,
                                            draggedCollectionId = sourceId,
                                            pointer = collectionDragPointerInRoot
                                        )
                                }
                            },
                            onCollectionDragEnd = {
                                val sourceId = draggedCollectionId
                                val targetId = dropTargetCollectionId

                                if (
                                    sourceId != null &&
                                    targetId != null &&
                                    sourceId != targetId
                                ) {
                                    val sourceBounds = collectionBoundsById[sourceId]
                                    val targetBounds = collectionBoundsById[targetId]

                                    if (sourceBounds != null && targetBounds != null) {
                                        val sourceOld = Offset(sourceBounds.left, sourceBounds.top)
                                        val targetOld = Offset(targetBounds.left, targetBounds.top)
                                        requestCollectionSettleAnimation(
                                            mapOf(
                                                collectionPreviewKey(sourceId) to
                                                    (sourceOld + collectionDragTranslation - targetOld),
                                                collectionPreviewKey(targetId) to
                                                    (targetOld - sourceOld)
                                            )
                                        )
                                    }

                                    swapPreviewCollections(
                                        collections = previewCollections,
                                        sourceCollectionId = sourceId,
                                        targetCollectionId = targetId
                                    )
                                } else if (
                                    sourceId != null &&
                                    collectionDragTranslation != Offset.Zero
                                ) {
                                    requestCollectionSettleAnimation(
                                        mapOf(
                                            collectionPreviewKey(sourceId) to collectionDragTranslation
                                        )
                                    )
                                }

                                clearCollectionDrag()
                            },
                            onCollectionDragCancel = {
                                val sourceId = draggedCollectionId
                                if (
                                    sourceId != null &&
                                    collectionDragTranslation != Offset.Zero
                                ) {
                                    requestCollectionSettleAnimation(
                                        mapOf(
                                            collectionPreviewKey(sourceId) to collectionDragTranslation
                                        )
                                    )
                                }
                                clearCollectionDrag()
                            }
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            LauncherTextActionButton(
                                text =
                                    if (collectionSnapshot.isEmpty()) {
                                        "Crear colecciones"
                                    } else {
                                        "Editar colecciones (${collectionSnapshot.size}/$MAX_COLLECTIONS)"
                                    },
                                onClick = onManageCollections
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "Añadir aplicaciones",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(8.dp))

                        FavoriteCatalogSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text =
                                when {
                                    limitNoticeVisible ->
                                        "Quita una favorita para añadir otra"
                                    selectedSnapshot.size >= MAX_FAVORITES ->
                                        "$MAX_FAVORITES de $MAX_FAVORITES favoritas · límite de Inicio"
                                    else ->
                                        "${selectedSnapshot.size} de $MAX_FAVORITES favoritas"
                                },
                            fontSize = 13.sp,
                            color =
                                if (limitNoticeVisible) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
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
                                "Quitar ${app.label} de Favoritas"
                            } else {
                                "Añadir ${app.label} a Favoritas"
                            },
                        onClick = {
                            toggleCatalogSelection(app)
                        }
                    )

                    if (isSelected) {
                        SelectedCatalogMarker(
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
private fun FavoritesEditorHeader(
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 6.dp,
                    end = 20.dp,
                    top = 10.dp,
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
            text = "Favoritas",
            modifier = Modifier.weight(1f),
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LauncherPrimaryActionButton(
            text = "Listo",
            onClick = onSave
        )
    }
}

@Composable
private fun PreviewHeading(
    selectedCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Vista previa de Inicio",
            modifier = Modifier.weight(1f),
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "$selectedCount / $MAX_FAVORITES",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomePreview(
    selectedApps: List<InstalledApp>,
    collections: List<CollectionEntity>,
    collectionAppsById: Map<Long, List<InstalledApp>>,
    notificationCounts: Map<NotificationAppKey, Int>,
    itemBoundsByKey: MutableMap<String, Rect>,
    draggedKey: String?,
    dropTargetKey: String?,
    dragTranslation: Offset,
    swapAnimationRequest: PreviewSwapAnimationRequest?,
    collectionBoundsById: MutableMap<Long, Rect>,
    draggedCollectionId: Long?,
    dropTargetCollectionId: Long?,
    collectionDragTranslation: Offset,
    collectionSwapAnimationRequest: PreviewSwapAnimationRequest?,
    onEditCollection: (Long) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onRemove: (InstalledApp) -> Unit,
    onDragStart: (InstalledApp, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onCollectionDragStart: (CollectionEntity, Offset) -> Unit,
    onCollectionDrag: (Offset) -> Unit,
    onCollectionDragEnd: () -> Unit,
    onCollectionDragCancel: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.34f)
                )
                .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        PreviewClock()

        Spacer(Modifier.height(22.dp))

        if (selectedApps.isEmpty()) {
            Text(
                text = "Añade aplicaciones para ver cómo quedarán en Inicio.",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            selectedApps.chunked(2).forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                        FavoritePreviewItem(
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

                    if (rowApps.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (collections.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            CollectionsPreview(
                collections = collections,
                collectionAppsById = collectionAppsById,
                notificationCounts = notificationCounts,
                itemBoundsById = collectionBoundsById,
                draggedCollectionId = draggedCollectionId,
                dropTargetCollectionId = dropTargetCollectionId,
                dragTranslation = collectionDragTranslation,
                swapAnimationRequest = collectionSwapAnimationRequest,
                onEdit = onEditCollection,
                onDelete = onDeleteCollection,
                onDragStart = onCollectionDragStart,
                onDrag = onCollectionDrag,
                onDragEnd = onCollectionDragEnd,
                onDragCancel = onCollectionDragCancel
            )
        }
    }
}

@Composable
private fun PreviewClock() {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMillis = now
            delay(60_000L - (now % 60_000L) + 50L)
        }
    }

    val timeFormatter =
        remember(context) {
            DateFormat.getTimeFormat(context)
        }

    val dateFormatter =
        remember(locale) {
            SimpleDateFormat(
                DateFormat.getBestDateTimePattern(locale, "EEEE d MMMM"),
                locale
            )
        }

    val date = remember(currentTimeMillis) { Date(currentTimeMillis) }
    val dateText =
        dateFormatter
            .format(date)
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(locale) else char.toString()
            }

    Column {
        Text(
            text = timeFormatter.format(date),
            fontSize = 34.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = dateText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FavoritePreviewItem(
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
            label = "favoritePreviewDragLift"
        )

    val dropTargetProgress by
        animateFloatAsState(
            targetValue = if (isDropTarget) 1f else 0f,
            animationSpec = tween(100),
            label = "favoritePreviewDropTarget"
        )

    var menuExpanded by remember(app.componentName, app.user) { mutableStateOf(false) }
    val itemShape = RoundedCornerShape(18.dp)
    val animatedSettleTranslation = settleOffset * settleProgress.value

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
                .zIndex(if (isGestureActive) 3f else if (settleProgress.value > 0.001f) 1f else 0f)
                .graphicsLayer {
                    translationX =
                        if (isGestureActive) dragTranslation.x
                        else animatedSettleTranslation.x
                    translationY =
                        if (isGestureActive) dragTranslation.y
                        else animatedSettleTranslation.y
                    shadowElevation = dragElevationPx * dragLiftProgress
                    shape = itemShape
                    clip = false
                }
                .background(
                    color =
                        if (isGestureActive) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                        } else {
                            Color.Transparent
                        },
                    shape = itemShape
                )
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.045f * dropTargetProgress
                    ),
                    itemShape
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.30f * dropTargetProgress
                    ),
                    itemShape
                )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
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
                        shape = itemShape,
                        onClickLabel = "Favorita ${app.label}",
                        onClick = {}
                    )
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LauncherAppIconWithBadge(
                bitmap = iconBitmap,
                contentDescription = app.label,
                iconSize = 30.dp,
                notificationCount = notificationCount
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = app.label,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
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
                    text = "Quitar de Favoritas",
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CollectionsPreview(
    collections: List<CollectionEntity>,
    collectionAppsById: Map<Long, List<InstalledApp>>,
    notificationCounts: Map<NotificationAppKey, Int>,
    itemBoundsById: MutableMap<Long, Rect>,
    draggedCollectionId: Long?,
    dropTargetCollectionId: Long?,
    dragTranslation: Offset,
    swapAnimationRequest: PreviewSwapAnimationRequest?,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDragStart: (CollectionEntity, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        collections.chunked(2).forEach { rowCollections ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCollections.forEach { collection ->
                    val key = collectionPreviewKey(collection.id)
                    val isGestureActive = draggedCollectionId == collection.id
                    val settleRequest = swapAnimationRequest
                    val settleOffset = settleRequest?.offsets?.get(key) ?: Offset.Zero
                    val settleToken =
                        if (settleRequest?.offsets?.containsKey(key) == true) {
                            settleRequest.id
                        } else {
                            null
                        }

                    CollectionPreviewItem(
                        collection = collection,
                        apps = collectionAppsById[collection.id].orEmpty(),
                        notificationCounts = notificationCounts,
                        isGestureActive = isGestureActive,
                        isDragging = isGestureActive && dragTranslation != Offset.Zero,
                        isDropTarget = dropTargetCollectionId == collection.id,
                        dragTranslation = if (isGestureActive) dragTranslation else Offset.Zero,
                        settleOffset = settleOffset,
                        settleAnimationToken = settleToken,
                        onBoundsChanged = { bounds ->
                            itemBoundsById[collection.id] = bounds
                        },
                        onEdit = { onEdit(collection.id) },
                        onDelete = { onDelete(collection.id) },
                        onDragStart = { touchOffset ->
                            onDragStart(collection, touchOffset)
                        },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowCollections.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CollectionPreviewItem(
    collection: CollectionEntity,
    apps: List<InstalledApp>,
    notificationCounts: Map<NotificationAppKey, Int>,
    isGestureActive: Boolean,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragTranslation: Offset,
    settleOffset: Offset,
    settleAnimationToken: Long?,
    onBoundsChanged: (Rect) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aggregateCount =
        apps.sumOf { app ->
            NotificationBadgeStore.countFor(notificationCounts, app)
        }

    val hapticFeedback = LocalHapticFeedback.current
    val gestureScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragStartThresholdPx = remember(density) { with(density) { 6.dp.toPx() } }
    val dragElevationPx = remember(density) { with(density) { 12.dp.toPx() } }
    val shape = RoundedCornerShape(18.dp)
    var menuExpanded by remember(collection.id) { mutableStateOf(false) }

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
            label = "collectionPreviewDragLift"
        )

    val dropTargetProgress by
        animateFloatAsState(
            targetValue = if (isDropTarget) 1f else 0f,
            animationSpec = tween(100),
            label = "collectionPreviewDropTarget"
        )

    val animatedSettleTranslation = settleOffset * settleProgress.value
    val dragSurfaceColor =
        MaterialTheme.colorScheme.surface.copy(
            alpha = 0.62f + (0.24f * dragLiftProgress)
        )
    val baseSurfaceColor =
        MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)

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
                .background(
                    color = if (isGestureActive) dragSurfaceColor else baseSurfaceColor,
                    shape = shape
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .pointerInput(collection.id, dragStartThresholdPx) {
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
                        onClickLabel = "Colección ${collection.name}",
                        onClick = {}
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CollectionPreviewMiniIcon(apps)
            Spacer(Modifier.width(9.dp))
            Text(
                text = collection.name,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (aggregateCount > 0) {
            PreviewNotificationBadge(
                count = aggregateCount,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 5.dp, end = 5.dp)
            )
        }

        CollectionContextMenu(
            expanded = menuExpanded,
            collectionName = collection.name,
            onDismiss = { menuExpanded = false },
            onEdit = {
                menuExpanded = false
                onEdit()
            },
            onDelete = {
                menuExpanded = false
                onDelete()
            }
        )
    }
}

@Composable
private fun CollectionPreviewMiniIcon(
    apps: List<InstalledApp>
) {
    val miniApps = apps.take(4)
    val shape = RoundedCornerShape(13.dp)

    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(shape)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                ),
        contentAlignment = Alignment.Center
    ) {
        if (miniApps.size == 1) {
            val iconBitmap = rememberLauncherAppIcon(miniApps[0])
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(23.dp)
            )
        } else {
            miniApps.forEachIndexed { index, app ->
                val iconBitmap = rememberLauncherAppIcon(app)
                val alignment =
                    when (index) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.TopEnd
                        2 -> Alignment.BottomStart
                        else -> Alignment.BottomEnd
                    }

                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .align(alignment)
                            .padding(2.dp)
                            .size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewNotificationBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = Color(0xFFC74646),
        contentColor = Color.White,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier =
                Modifier
                    .height(18.dp)
                    .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FavoriteCatalogSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth(),
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
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {}),
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun SelectedCatalogMarker(
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .size(18.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
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

@Composable
private fun FavoritePickerWindowEffect() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        val window = activity?.window

        if (
            window != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val attributes = window.attributes
            attributes.setBlurBehindRadius(32)
            window.attributes = attributes
        }

        onDispose {
            if (
                window != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val attributes = window.attributes
                attributes.setBlurBehindRadius(0)
                window.attributes = attributes
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}

private data class PreviewSwapAnimationRequest(
    val id: Long,
    val offsets: Map<String, Offset>
)

private fun findPreviewDropTarget(
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

private fun findPreviewCollectionDropTarget(
    collections: List<CollectionEntity>,
    itemBoundsById: Map<Long, Rect>,
    draggedCollectionId: Long,
    pointer: Offset
): Long? {
    return collections
        .asSequence()
        .filter { it.id != draggedCollectionId }
        .mapNotNull { collection ->
            val bounds = itemBoundsById[collection.id] ?: return@mapNotNull null
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
                collection.id to (dx * dx + dy * dy)
            }
        }
        .minByOrNull { it.second }
        ?.first
}

private fun swapPreviewCollections(
    collections: MutableList<CollectionEntity>,
    sourceCollectionId: Long,
    targetCollectionId: Long
) {
    val sourceIndex = collections.indexOfFirst { it.id == sourceCollectionId }
    val targetIndex = collections.indexOfFirst { it.id == targetCollectionId }

    if (sourceIndex < 0 || targetIndex < 0 || sourceIndex == targetIndex) return

    val source = collections[sourceIndex]
    collections[sourceIndex] = collections[targetIndex]
    collections[targetIndex] = source
}

private fun collectionPreviewKey(collectionId: Long): String =
    "preview-collection:$collectionId"

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

private fun appKey(app: InstalledApp): String {
    return app.componentName.flattenToString() + "@" + app.user.hashCode()
}

private fun Context.findActivity(): Activity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }

    return null
}

private const val LONG_PRESS_MENU_REVEAL_DELAY_MILLIS = 140L
