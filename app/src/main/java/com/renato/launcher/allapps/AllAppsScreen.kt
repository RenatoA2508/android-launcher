package com.renato.launcher.allapps

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.notifications.NotificationBadgeStore
import com.renato.launcher.ui.components.AppContextMenu
import com.renato.launcher.ui.components.LauncherAppIconWithBadge
import com.renato.launcher.ui.components.LauncherTextActionButton
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherTransitionCombinedClickable
import com.renato.launcher.ui.interactions.launcherClickable
import java.text.Normalizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AllAppsScreen(
    apps: List<InstalledApp>,
    appsLoaded: Boolean,
    onAppClick: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit,
    onBack: () -> Unit
) {
    AllAppsWindowEffect()

    val notificationCounts by
        NotificationBadgeStore
            .counts
            .collectAsState()

    val layout =
        remember(
            apps
        ) {
            buildAllAppsLayout(
                apps =
                    apps
            )
        }

    /*
     * Keep entry into All Apps lightweight.
     *
     * The process-level icon cache is preserved. We only warm the first
     * visible rows here instead of converting every installed icon up front.
     * Remaining icons are cached lazily as their cells enter composition.
     */
    PreloadLauncherAppIcons(
        apps =
            apps.take(
                16
            )
    )

    val gridState =
        rememberLazyGridState()

    val coroutineScope =
        rememberCoroutineScope()

    var scrollJob by
        remember {
            mutableStateOf<Job?>(
                null
            )
        }

    var indicatorLetter by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var indicatorRevision by
        remember {
            mutableIntStateOf(
                0
            )
        }

    val currentSectionKey by
        remember(
            gridState,
            layout.sections
        ) {
            derivedStateOf {
                val firstVisibleIndex =
                    gridState
                        .firstVisibleItemIndex

                layout.sections
                    .lastOrNull {
                            section ->

                        section.gridStartIndex <=
                            firstVisibleIndex
                    }
                    ?.key
                    ?: layout.sections
                        .firstOrNull()
                        ?.key
            }
        }

    fun jumpToSection(
        key: String
    ) {
        val targetIndex =
            layout.startIndexByKey[
                key
            ] ?: return

        indicatorLetter =
            key

        indicatorRevision +=
            1

        scrollJob
            ?.cancel()

        scrollJob =
            coroutineScope.launch {
                gridState.scrollToItem(
                    targetIndex
                )
            }
    }

    LaunchedEffect(
        indicatorRevision
    ) {
        if (
            indicatorRevision ==
            0
        ) {
            return@LaunchedEffect
        }

        delay(
            500L
        )

        indicatorLetter =
            null
    }

    BackHandler {
        onBack()
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
                    ALL_APPS_ROOT_TAG
                ),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha =
                        0.82f
                )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {
            AllAppsHeader(
                onBack =
                    onBack
            )

            Box(
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxWidth()
            ) {
                if (
                    layout.sections
                        .isEmpty()
                ) {
                    EmptyAllAppsState(
                        appsLoaded =
                            appsLoaded
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
                            Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                start =
                                    20.dp,
                                end =
                                    42.dp,
                                top =
                                    6.dp,
                                bottom =
                                    24.dp
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
                        layout.sections
                            .forEach {
                                    section ->

                                item(
                                    key =
                                        "section:${section.key}",
                                    contentType =
                                        "section-heading",
                                    span = {
                                        GridItemSpan(
                                            maxLineSpan
                                        )
                                    }
                                ) {
                                    AllAppsSectionHeader(
                                        key =
                                            section.key
                                    )
                                }

                                items(
                                    items =
                                        section.apps,
                                    key = {
                                            app ->

                                        "all-app:" +
                                            appKey(
                                                app
                                            )
                                    },
                                    contentType = {
                                        "app"
                                    }
                                ) {
                                        app ->

                                    AllAppsAppItem(
                                        app =
                                            app,
                                        notificationCount =
                                            NotificationBadgeStore
                                                .countFor(
                                                    counts =
                                                        notificationCounts,
                                                    app =
                                                        app
                                                ),
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
                                        onUninstallApp = {
                                            onUninstallApp(
                                                app
                                            )
                                        }
                                    )
                                }
                            }
                    }

                    AlphabetRail(
                        availableKeys =
                            layout.startIndexByKey
                                .keys,
                        currentKey =
                            currentSectionKey,
                        onSelect =
                            ::jumpToSection,
                        modifier =
                            Modifier
                                .align(
                                    Alignment.CenterEnd
                                )
                                .fillMaxHeight()
                    )
                }

                indicatorLetter
                    ?.let {
                            letter ->

                        LetterIndicator(
                            letter =
                                letter,
                            modifier =
                                Modifier.align(
                                    Alignment.Center
                                )
                        )
                    }
            }
        }
    }
}

@Composable
private fun AllAppsHeader(
    onBack: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start =
                        8.dp,
                    end =
                        18.dp,
                    top =
                        8.dp,
                    bottom =
                        8.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        LauncherTextActionButton(
            text =
                "‹",
            fontSize =
                32.sp,
            onClick =
                onBack
        )

        Spacer(
            modifier =
                Modifier.width(
                    4.dp
                )
        )

        Text(
            text =
                "Todas las aplicaciones",
            modifier =
                Modifier.weight(
                    1f
                ),
            fontSize =
                22.sp,
            fontWeight =
                FontWeight.SemiBold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}

@Composable
private fun AllAppsSectionHeader(
    key: String
) {
    Text(
        text =
            key,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        10.dp,
                    bottom =
                        2.dp
                ),
        fontSize =
            18.sp,
        fontWeight =
            FontWeight.Bold,
        color =
            MaterialTheme
                .colorScheme
                .onSurface
    )
}

@Composable
private fun AllAppsAppItem(
    app: InstalledApp,
    notificationCount: Int,
    onClick: () -> Unit,
    onAppInfo: () -> Unit,
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
                    .launcherTransitionCombinedClickable(
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
            LauncherAppIconWithBadge(
                bitmap =
                    iconBitmap,
                contentDescription =
                    app.label,
                iconSize =
                    44.dp,
                notificationCount =
                    notificationCount
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
                false,
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
                /*
                 * All Apps is an alphabetical catalog, not a membership surface.
                 * This action is intentionally hidden by showRemoveFromHome=false.
                 */
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
private fun AlphabetRail(
    availableKeys: Set<String>,
    currentKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier =
        Modifier
) {
    var railHeightPx by
        remember {
            mutableStateOf(
                0f
            )
        }

    fun keyAt(
        y: Float
    ): String? {
        if (
            railHeightPx <=
            0f
        ) {
            return null
        }

        val normalizedY =
            y.coerceIn(
                minimumValue =
                    0f,
                maximumValue =
                    (railHeightPx - 1f)
                        .coerceAtLeast(
                            0f
                        )
            )

        val index =
            (
                normalizedY /
                    railHeightPx *
                    ALL_APPS_INDEX_KEYS.size
                )
                .toInt()
                .coerceIn(
                    minimumValue =
                        0,
                    maximumValue =
                        ALL_APPS_INDEX_KEYS
                            .lastIndex
                )

        return ALL_APPS_INDEX_KEYS[
            index
        ]
    }

    Column(
        modifier =
            modifier
                .width(
                    32.dp
                )
                .padding(
                    end =
                        4.dp,
                    top =
                        6.dp,
                    bottom =
                        6.dp
                )
                .onSizeChanged {
                        size ->

                    railHeightPx =
                        size.height
                            .toFloat()
                }
                .pointerInput(
                    availableKeys,
                    railHeightPx
                ) {
                    var lastDraggedKey:
                        String? =
                        null

                    detectVerticalDragGestures(
                        onDragStart = {
                                offset ->

                            val key =
                                keyAt(
                                    offset.y
                                )

                            if (
                                key != null &&
                                key in
                                    availableKeys
                            ) {
                                lastDraggedKey =
                                    key

                                onSelect(
                                    key
                                )
                            }
                        },
                        onVerticalDrag = {
                                change,
                                _ ->

                            val key =
                                keyAt(
                                    change.position.y
                                )

                            if (
                                key != null &&
                                key in
                                    availableKeys &&
                                key !=
                                    lastDraggedKey
                            ) {
                                change.consume()

                                lastDraggedKey =
                                    key

                                onSelect(
                                    key
                                )
                            }
                        },
                        onDragEnd = {
                            lastDraggedKey =
                                null
                        },
                        onDragCancel = {
                            lastDraggedKey =
                                null
                        }
                    )
                }
    ) {
        ALL_APPS_INDEX_KEYS
            .forEach {
                    key ->

                val enabled =
                    key in
                        availableKeys

                val active =
                    key ==
                        currentKey

                val shape =
                    CircleShape

                Box(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .fillMaxWidth()
                            .then(
                                if (
                                    active
                                ) {
                                    Modifier.background(
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                                .copy(
                                                    alpha =
                                                        0.12f
                                                ),
                                        shape =
                                            shape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .launcherClickable(
                                enabled =
                                    enabled,
                                shape =
                                    shape,
                                onClickLabel =
                                    if (
                                        enabled
                                    ) {
                                        "Ir a $key"
                                    } else {
                                        null
                                    },
                                onClick = {
                                    onSelect(
                                        key
                                    )
                                }
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            key,
                        fontSize =
                            10.sp,
                        lineHeight =
                            10.sp,
                        fontWeight =
                            if (
                                active
                            ) {
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
                                        .copy(
                                            alpha =
                                                0.24f
                                        )

                                active ->
                                    MaterialTheme
                                        .colorScheme
                                        .primary

                                else ->
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            }
                    )
                }
            }
    }
}

@Composable
private fun LetterIndicator(
    letter: String,
    modifier: Modifier =
        Modifier
) {
    Surface(
        modifier =
            modifier.size(
                72.dp
            ),
        shape =
            RoundedCornerShape(
                24.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surfaceContainerHigh
                .copy(
                    alpha =
                        0.96f
                ),
        shadowElevation =
            10.dp
    ) {
        Box(
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text =
                    letter,
                fontSize =
                    32.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }
    }
}

@Composable
private fun EmptyAllAppsState(
    appsLoaded: Boolean
) {
    Box(
        modifier =
            Modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                if (
                    appsLoaded
                ) {
                    "No hay aplicaciones disponibles"
                } else {
                    "Cargando aplicaciones…"
                },
            fontSize =
                15.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

private fun buildAllAppsLayout(
    apps: List<InstalledApp>
): AllAppsLayout {
    val buckets =
        ALL_APPS_INDEX_KEYS
            .associateWith {
                mutableListOf<
                    InstalledApp
                >()
            }
            .toMutableMap()

    apps.forEach {
            app ->

        val key =
            alphabeticalSectionKey(
                app.label
            )

        buckets
            .getValue(
                key
            )
            .add(
                app
            )
    }

    var gridIndex =
        0

    val sections =
        buildList {
            ALL_APPS_INDEX_KEYS
                .forEach {
                        key ->

                    val sectionApps =
                        buckets[
                            key
                        ].orEmpty()

                    if (
                        sectionApps
                            .isNotEmpty()
                    ) {
                        add(
                            AllAppsSection(
                                key =
                                    key,
                                apps =
                                    sectionApps,
                                gridStartIndex =
                                    gridIndex
                            )
                        )

                        /*
                         * LazyVerticalGrid item indices count the full-span
                         * section heading plus each application item.
                         */
                        gridIndex +=
                            1 +
                                sectionApps.size
                    }
                }
        }

    return AllAppsLayout(
        sections =
            sections,
        startIndexByKey =
            sections.associate {
                    section ->

                section.key to
                    section.gridStartIndex
            }
    )
}

private fun alphabeticalSectionKey(
    label: String
): String {
    val normalized =
        Normalizer
            .normalize(
                label.trim(),
                Normalizer.Form.NFD
            )
            .replace(
                COMBINING_MARKS_REGEX,
                ""
            )

    val firstCharacter =
        normalized
            .firstOrNull()
            ?.uppercaseChar()
            ?: return "#"

    return if (
        firstCharacter in
        'A'..'Z'
    ) {
        firstCharacter
            .toString()
    } else {
        "#"
    }
}

private fun appKey(
    app: InstalledApp
): String {
    return "${app.user.hashCode()}:" +
        app.componentName
            .flattenToString()
}

private data class AllAppsSection(
    val key: String,
    val apps: List<InstalledApp>,
    val gridStartIndex: Int
)

private data class AllAppsLayout(
    val sections: List<AllAppsSection>,
    val startIndexByKey: Map<String, Int>
)

@Composable
private fun AllAppsWindowEffect() {
    val context =
        LocalContext.current

    val activity =
        remember(
            context
        ) {
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
            currentContext is
            Activity
        ) {
            return currentContext
        }

        currentContext =
            currentContext.baseContext
    }

    return null
}

private const val ALL_APPS_ROOT_TAG =
    "launcher_all_apps_root"

private val ALL_APPS_INDEX_KEYS =
    buildList {
        add(
            "#"
        )

        ('A'..'Z')
            .forEach {
                    letter ->

                add(
                    letter.toString()
                )
            }
    }

private val COMBINING_MARKS_REGEX =
    Regex(
        "\\p{Mn}+"
    )
