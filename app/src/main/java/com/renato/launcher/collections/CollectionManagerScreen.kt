package com.renato.launcher.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.collection.CollectionAppEntity
import com.renato.launcher.data.database.collection.CollectionEntity
import com.renato.launcher.ui.components.LauncherMenuItem
import com.renato.launcher.ui.components.LauncherPrimaryActionButton
import com.renato.launcher.ui.components.LauncherTextActionButton
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherTransitionCombinedClickable

@Composable
fun CollectionManagerScreen(
    collections: List<CollectionEntity>,
    collectionMembers: List<CollectionAppEntity>,
    resolvedAppsByCollection:
        Map<Long, List<InstalledApp>>,
    catalogLoaded: Boolean,
    onBack: () -> Unit,
    onCreateCollection: () -> Unit,
    onEditCollection: (Long) -> Unit,
    onDeleteCollection: (Long) -> Unit
) {
    CollectionWindowEffect()

    val resolvedApps =
        remember(
            resolvedAppsByCollection
        ) {
            resolvedAppsByCollection
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
            resolvedApps
    )

    val memberCounts =
        remember(
            collectionMembers
        ) {
            collectionMembers
                .groupingBy {
                    it.collectionId
                }
                .eachCount()
        }

    BackHandler(
        onBack =
            onBack
    )

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
            CollectionManagerHeader(
                collectionCount =
                    collections.size,
                catalogLoaded =
                    catalogLoaded,
                onBack =
                    onBack,
                onCreateCollection =
                    onCreateCollection
            )

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(
                        2
                    ),
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        start =
                            20.dp,
                        end =
                            20.dp,
                        top =
                            14.dp,
                        bottom =
                            28.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                if (
                    !catalogLoaded
                ) {
                    item(
                        key =
                            "catalog-loading",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {
                        CatalogLoadingState()
                    }
                } else if (
                    collections.isEmpty()
                ) {
                    item(
                        key =
                            "collections-empty",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {
                        EmptyCollectionsState(
                            onCreateCollection =
                                onCreateCollection
                        )
                    }
                } else {
                    items(
                        items =
                            collections,
                        key = {
                            it.id
                        },
                        contentType = {
                            "collection"
                        }
                    ) {
                            collection ->

                        CollectionCard(
                            collection =
                                collection,
                            apps =
                                resolvedAppsByCollection[
                                    collection.id
                                ].orEmpty(),
                            memberCount =
                                memberCounts[
                                    collection.id
                                ] ?: 0,
                            onClick = {
                                onEditCollection(
                                    collection.id
                                )
                            },
                            onDelete = {
                                onDeleteCollection(
                                    collection.id
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
private fun CollectionManagerHeader(
    collectionCount: Int,
    catalogLoaded: Boolean,
    onBack: () -> Unit,
    onCreateCollection: () -> Unit
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
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
                        "Colecciones",
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
                        when {
                            collectionCount ==
                                MAX_COLLECTIONS ->
                                "Máximo $MAX_COLLECTIONS colecciones"

                            collectionCount >
                                MAX_COLLECTIONS ->
                                "$collectionCount colecciones · elimina hasta quedar en $MAX_COLLECTIONS"

                            else ->
                                "$collectionCount de $MAX_COLLECTIONS colecciones"
                        },
                    fontSize =
                        14.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            CollectionTextActionButton(
                text =
                    "Volver",
                onClick =
                    onBack
            )

            CollectionPrimaryActionButton(
                text =
                    "Nueva",
                enabled =
                    catalogLoaded &&
                        collectionCount <
                            MAX_COLLECTIONS,
                onClick =
                    onCreateCollection
            )
        }
    }
}

@Composable
private fun CollectionCard(
    collection: CollectionEntity,
    apps: List<InstalledApp>,
    memberCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val shape =
        RoundedCornerShape(
            24.dp
        )

    val hapticFeedback =
        LocalHapticFeedback.current

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

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        /*
         * Keep the collection card on the same interaction model as
         * launcher app tiles: the whole card softly lights on press and
         * uses the shared bounded ripple on both tap and long press.
         */
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(
                        shape
                    )
                    .background(
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceContainer
                                .copy(
                                    alpha =
                                        0.84f
                                ),
                        shape =
                            shape
                    )
                    .launcherTransitionCombinedClickable(
                        shape =
                            shape,
                        onClickLabel =
                            "Editar ${collection.name}",
                        onLongClickLabel =
                            "Opciones de ${collection.name}",
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
                            16.dp,
                        vertical =
                            16.dp
                    )
        ) {
            CollectionMiniIcon(
                apps =
                    apps
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    collection.name,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis,
                fontSize =
                    16.sp,
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
                    if (
                        memberCount == 1
                    ) {
                        "1 aplicación"
                    } else {
                        "$memberCount aplicaciones"
                    },
                fontSize =
                    13.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded =
                menuExpanded,
            onDismissRequest = {
                menuExpanded =
                    false
            }
        ) {
            LauncherMenuItem(
                onClick = {
                    menuExpanded =
                        false

                    deleteConfirmationVisible =
                        true
                }
            ) {
                Text(
                    text =
                        "Eliminar colección"
                )
            }
        }
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
                        "¿Quieres eliminar \"${collection.name}\"? " +
                            "Las aplicaciones no se desinstalarán ni se quitarán de Favoritas."
                )
            },
            dismissButton = {
                CollectionTextActionButton(
                    text =
                        "Cancelar",
                    onClick = {
                        deleteConfirmationVisible =
                            false
                    }
                )
            },
            confirmButton = {
                CollectionPrimaryActionButton(
                    text =
                        "Eliminar",
                    onClick = {
                        deleteConfirmationVisible =
                            false

                        onDelete()
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
    val previewApps =
        apps.take(
            4
        )

    Surface(
        modifier =
            Modifier.size(
                68.dp
            ),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(
                    alpha =
                        0.72f
                )
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            previewApps
                .forEachIndexed {
                        index,
                        app ->

                    key(
                        appKey(
                            app
                        )
                    ) {
                        val icon =
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

                        val xOffset =
                            when (
                                index
                            ) {
                                0,
                                2 ->
                                    8.dp

                                else ->
                                    (-8).dp
                            }

                        val yOffset =
                            when (
                                index
                            ) {
                                0,
                                1 ->
                                    8.dp

                                else ->
                                    (-8).dp
                            }

                        Image(
                            bitmap =
                                icon,
                            contentDescription =
                                null,
                            modifier =
                                Modifier
                                    .align(
                                        alignment
                                    )
                                    .offset(
                                        x =
                                            xOffset,
                                        y =
                                            yOffset
                                    )
                                    .size(
                                        24.dp
                                    )
                        )
                    }
                }
        }
    }
}

@Composable
private fun CatalogLoadingState() {
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
                "Preparando las aplicaciones para editar tus colecciones…",
            modifier =
                Modifier.padding(
                    horizontal =
                        24.dp,
                    vertical =
                        20.dp
                ),
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
private fun EmptyCollectionsState(
    onCreateCollection: () -> Unit
) {
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
        Column(
            modifier =
                Modifier.padding(
                    horizontal =
                        24.dp,
                    vertical =
                        22.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    "Todavía no tienes colecciones.",
                fontSize =
                    16.sp,
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
                        4.dp
                    )
            )

            Text(
                text =
                    "Agrupa aplicaciones por contexto sin quitarlas de Favoritas.",
                fontSize =
                    14.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            CollectionPrimaryActionButton(
                text =
                    "Nueva colección",
                onClick =
                    onCreateCollection
            )
        }
    }
}

@Composable
private fun CollectionTextActionButton(
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
private fun CollectionPrimaryActionButton(
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

private fun appKey(
    app: InstalledApp
): String {
    return "${app.user.hashCode()}:" +
        app.componentName
            .flattenToString()
}
