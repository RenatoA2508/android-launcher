package com.renato.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherClickable
import com.renato.launcher.ui.interactions.launcherTransitionCombinedClickable
import java.text.Normalizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Canonical full application catalog used across the launcher.
 *
 * Visual and navigation rules live here so every surface that means
 * "show all applications" shares the same four-column grid, alphabetical
 * sections, A-Z rail, spacing and section-jump feedback.
 *
 * [headerContent] is intentionally a full-span item inside the same grid.
 * Editors can therefore place contextual controls or a Home preview above the
 * catalog without creating a second, visually different app browser.
 */
@Composable
fun LauncherAppCatalog(
    apps: List<InstalledApp>,
    appsLoaded: Boolean,
    modifier: Modifier = Modifier,
    preloadCount: Int = 16,
    userScrollEnabled: Boolean = true,
    emptyStateText: String? = null,
    headerContent: (@Composable () -> Unit)? = null,
    appContent: @Composable (InstalledApp) -> Unit
) {
    val headerItemCount =
        if (headerContent != null) {
            1
        } else {
            0
        }

    val layout =
        remember(
            apps,
            headerItemCount
        ) {
            buildLauncherAppCatalogLayout(
                apps = apps,
                leadingGridItems = headerItemCount
            )
        }

    PreloadLauncherAppIcons(
        apps = apps.take(preloadCount)
    )

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var scrollJob by
        remember {
            mutableStateOf<Job?>(null)
        }

    var indicatorLetter by
        remember {
            mutableStateOf<String?>(null)
        }

    var indicatorRevision by
        remember {
            mutableIntStateOf(0)
        }

    val currentSectionKey by
        remember(
            gridState,
            layout.sections
        ) {
            derivedStateOf {
                val firstVisibleIndex =
                    gridState.firstVisibleItemIndex

                layout.sections
                    .lastOrNull { section ->
                        section.gridStartIndex <= firstVisibleIndex
                    }
                    ?.key
                    ?: layout.sections.firstOrNull()?.key
            }
        }

    val showAlphabetRail by
        remember(
            gridState,
            headerItemCount,
            layout.sections
        ) {
            derivedStateOf {
                layout.sections.isNotEmpty() &&
                    (headerItemCount == 0 ||
                        gridState.firstVisibleItemIndex >= headerItemCount)
            }
        }

    fun jumpToSection(key: String) {
        val targetIndex =
            layout.startIndexByKey[key] ?: return

        indicatorLetter = key
        indicatorRevision += 1

        scrollJob?.cancel()
        scrollJob =
            coroutineScope.launch {
                gridState.scrollToItem(targetIndex)
            }
    }

    LaunchedEffect(indicatorRevision) {
        if (indicatorRevision == 0) {
            return@LaunchedEffect
        }

        delay(500L)
        indicatorLetter = null
    }

    Box(
        modifier =
            modifier.fillMaxSize()
    ) {
        if (
            layout.sections.isEmpty() &&
            headerContent == null
        ) {
            LauncherAppCatalogEmptyState(
                appsLoaded = appsLoaded,
                fillAvailableSpace = true,
                emptyStateText = emptyStateText
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 42.dp,
                        top = 6.dp,
                        bottom = 24.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = userScrollEnabled
            ) {
                headerContent?.let { header ->
                    item(
                        key = "launcher-catalog-header",
                        contentType = "catalog-header",
                        span = {
                            GridItemSpan(maxLineSpan)
                        }
                    ) {
                        header()
                    }
                }

                if (layout.sections.isEmpty()) {
                    item(
                        key = "launcher-catalog-empty",
                        contentType = "catalog-empty",
                        span = {
                            GridItemSpan(maxLineSpan)
                        }
                    ) {
                        LauncherAppCatalogEmptyState(
                            appsLoaded = appsLoaded,
                            fillAvailableSpace = false,
                            emptyStateText = emptyStateText
                        )
                    }
                } else {
                    layout.sections.forEach { section ->
                        item(
                        key = "section:${section.key}",
                        contentType = "section-heading",
                        span = {
                            GridItemSpan(maxLineSpan)
                        }
                    ) {
                        LauncherAppCatalogSectionHeader(
                            key = section.key
                        )
                    }

                    items(
                        items = section.apps,
                        key = { app ->
                            "catalog-app:" + launcherCatalogAppKey(app)
                        },
                        contentType = {
                            "app"
                        }
                    ) { app ->
                        appContent(app)
                    }
                    }
                }
            }
        }

        if (showAlphabetRail) {
            LauncherAlphabetRail(
                availableKeys = layout.startIndexByKey.keys,
                currentKey = currentSectionKey,
                onSelect = ::jumpToSection,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
            )
        }

        indicatorLetter?.let { letter ->
            LauncherCatalogLetterIndicator(
                letter = letter,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Canonical app tile for the full catalog.
 *
 * Different catalog modes may change what a tap means, but they should reuse
 * this geometry and press feedback rather than inventing another app grid.
 */
@Composable
fun LauncherAppCatalogItem(
    app: InstalledApp,
    notificationCount: Int = 0,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val iconBitmap = rememberLauncherAppIcon(app)
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .launcherTransitionCombinedClickable(
                    shape = shape,
                    onClickLabel = onClickLabel,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .padding(
                    horizontal = 3.dp,
                    vertical = 6.dp
                ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LauncherAppIconWithBadge(
            bitmap = iconBitmap,
            contentDescription = app.label,
            iconSize = 44.dp,
            notificationCount = notificationCount
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = app.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LauncherAppCatalogSectionHeader(
    key: String
) {
    Text(
        text = key,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                    bottom = 2.dp
                ),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun LauncherAlphabetRail(
    availableKeys: Set<String>,
    currentKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var railHeightPx by
        remember {
            mutableStateOf(0f)
        }

    fun keyAt(y: Float): String? {
        if (railHeightPx <= 0f) {
            return null
        }

        val normalizedY =
            y.coerceIn(
                minimumValue = 0f,
                maximumValue =
                    (railHeightPx - 1f)
                        .coerceAtLeast(0f)
            )

        val index =
            (
                normalizedY /
                    railHeightPx *
                    LAUNCHER_APP_CATALOG_INDEX_KEYS.size
                )
                .toInt()
                .coerceIn(
                    minimumValue = 0,
                    maximumValue =
                        LAUNCHER_APP_CATALOG_INDEX_KEYS.lastIndex
                )

        return LAUNCHER_APP_CATALOG_INDEX_KEYS[index]
    }

    Column(
        modifier =
            modifier
                .width(32.dp)
                .padding(
                    end = 4.dp,
                    top = 6.dp,
                    bottom = 6.dp
                )
                .onSizeChanged { size ->
                    railHeightPx = size.height.toFloat()
                }
                .pointerInput(
                    availableKeys,
                    railHeightPx
                ) {
                    var lastDraggedKey: String? = null

                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val key = keyAt(offset.y)

                            if (
                                key != null &&
                                key in availableKeys
                            ) {
                                lastDraggedKey = key
                                onSelect(key)
                            }
                        },
                        onVerticalDrag = { change, _ ->
                            val key = keyAt(change.position.y)

                            if (
                                key != null &&
                                key in availableKeys &&
                                key != lastDraggedKey
                            ) {
                                change.consume()
                                lastDraggedKey = key
                                onSelect(key)
                            }
                        },
                        onDragEnd = {
                            lastDraggedKey = null
                        },
                        onDragCancel = {
                            lastDraggedKey = null
                        }
                    )
                }
    ) {
        LAUNCHER_APP_CATALOG_INDEX_KEYS.forEach { key ->
            val enabled = key in availableKeys
            val active = key == currentKey
            val shape = CircleShape

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (active) {
                                Modifier.background(
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                            .copy(alpha = 0.12f),
                                    shape = shape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .launcherClickable(
                            enabled = enabled,
                            shape = shape,
                            onClickLabel =
                                if (enabled) {
                                    "Ir a $key"
                                } else {
                                    null
                                },
                            onClick = {
                                onSelect(key)
                            }
                        ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight =
                        if (active) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        },
                    color =
                        when {
                            !enabled ->
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                                    .copy(alpha = 0.24f)

                            active ->
                                MaterialTheme.colorScheme.primary

                            else ->
                                MaterialTheme.colorScheme.onSurfaceVariant
                        }
                )
            }
        }
    }
}

@Composable
private fun LauncherCatalogLetterIndicator(
    letter: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(72.dp),
        shape = RoundedCornerShape(24.dp),
        color =
            MaterialTheme
                .colorScheme
                .surfaceContainerHigh
                .copy(alpha = 0.96f),
        shadowElevation = 10.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LauncherAppCatalogEmptyState(
    appsLoaded: Boolean,
    fillAvailableSpace: Boolean,
    emptyStateText: String?
) {
    Box(
        modifier =
            if (fillAvailableSpace) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text =
                if (appsLoaded) {
                    emptyStateText ?: "No hay aplicaciones disponibles"
                } else {
                    "Cargando aplicaciones…"
                },
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildLauncherAppCatalogLayout(
    apps: List<InstalledApp>,
    leadingGridItems: Int
): LauncherAppCatalogLayout {
    val buckets =
        LAUNCHER_APP_CATALOG_INDEX_KEYS
            .associateWith {
                mutableListOf<InstalledApp>()
            }
            .toMutableMap()

    apps.forEach { app ->
        val key = launcherAlphabeticalSectionKey(app.label)

        buckets
            .getValue(key)
            .add(app)
    }

    var gridIndex = leadingGridItems

    val sections =
        buildList {
            LAUNCHER_APP_CATALOG_INDEX_KEYS.forEach { key ->
                val sectionApps = buckets[key].orEmpty()

                if (sectionApps.isNotEmpty()) {
                    add(
                        LauncherAppCatalogSection(
                            key = key,
                            apps = sectionApps,
                            gridStartIndex = gridIndex
                        )
                    )

                    gridIndex += 1 + sectionApps.size
                }
            }
        }

    return LauncherAppCatalogLayout(
        sections = sections,
        startIndexByKey =
            sections.associate { section ->
                section.key to section.gridStartIndex
            }
    )
}

private fun launcherAlphabeticalSectionKey(
    label: String
): String {
    val normalized =
        Normalizer
            .normalize(
                label.trim(),
                Normalizer.Form.NFD
            )
            .replace(
                LAUNCHER_COMBINING_MARKS_REGEX,
                ""
            )

    val firstCharacter =
        normalized
            .firstOrNull()
            ?.uppercaseChar()
            ?: return "#"

    return if (firstCharacter in 'A'..'Z') {
        firstCharacter.toString()
    } else {
        "#"
    }
}

private fun launcherCatalogAppKey(
    app: InstalledApp
): String {
    return "${app.user.hashCode()}:" +
        app.componentName.flattenToString()
}

private data class LauncherAppCatalogSection(
    val key: String,
    val apps: List<InstalledApp>,
    val gridStartIndex: Int
)

private data class LauncherAppCatalogLayout(
    val sections: List<LauncherAppCatalogSection>,
    val startIndexByKey: Map<String, Int>
)

private val LAUNCHER_APP_CATALOG_INDEX_KEYS =
    buildList {
        add("#")
        ('A'..'Z').forEach { letter ->
            add(letter.toString())
        }
    }

private val LAUNCHER_COMBINING_MARKS_REGEX =
    Regex("\\p{Mn}+")
