package com.renato.launcher.favorites

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.renato.launcher.core.model.InstalledApp
import java.text.Normalizer
import java.util.Locale
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
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

    val selectedApps =
        remember(initialSelection) {
            mutableStateListOf<InstalledApp>().apply {
                addAll(initialSelection)
            }
        }

    var searchMode by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    val normalGridState =
        rememberLazyGridState()

    val searchGridState =
        rememberLazyGridState()

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

    var searchExitPending by remember {
        mutableStateOf(false)
    }

    /*
     * IMPORTANT:
     *
     * Create small icon bitmaps ONE TIME instead
     * of converting Drawables as new grid items
     * enter the viewport during scrolling.
     */
    val iconBitmaps =
        rememberAppIconBitmaps(
            apps = apps
        )

    val selectedSnapshot =
        selectedApps.toList()

    val selectionPositions =
        remember(selectedSnapshot) {
            selectedSnapshot
                .mapIndexed { index, app ->
                    appKey(app) to (index + 1)
                }
                .toMap()
        }

    val filteredApps =
        remember(
            apps,
            searchQuery
        ) {
            if (searchQuery.isBlank()) {
                apps
            } else {
                val normalizedQuery =
                    normalizeSearchText(
                        searchQuery
                    )

                apps.filter { app ->
                    normalizeSearchText(
                        app.label
                    ).contains(
                        normalizedQuery
                    )
                }
            }
        }

    fun exitSearch() {
        if (searchExitPending) {
            return
        }

        searchExitPending = true

        /*
         * First let Samsung's IME begin closing.
         */
        keyboardController?.hide()
        focusManager.clearFocus()

        coroutineScope.launch {

            /*
             * Avoid rebuilding the picker while
             * the IME starts its exit animation.
             */
            delay(100)

            searchQuery = ""
            searchMode = false

            searchExitPending = false
        }
    }

    BackHandler(
        enabled = searchMode
    ) {
        exitSearch()
    }

    /*
     * When search opens:
     * - focus immediately
     * - show keyboard
     *
     * No full-screen transition animation.
     */
    LaunchedEffect(searchMode) {
        if (searchMode) {

            /*
             * Frame 1:
             * let Compose switch to search UI.
             */
            withFrameNanos { }

            /*
             * Frame 2:
             * give focus to the search field.
             */
            focusRequester.requestFocus()

            withFrameNanos { }

            /*
             * Frame 3:
             * start IME animation.
             */
            keyboardController?.show()
        }
    }

    /*
     * New query always starts results from top.
     */
    LaunchedEffect(searchQuery) {
        if (
            searchMode &&
            (
                searchGridState.firstVisibleItemIndex > 0 ||
                    searchGridState.firstVisibleItemScrollOffset > 0
                )
        ) {
            searchGridState.scrollToItem(0)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(
                            alpha = 0.78f
                        )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            /*
             * We animate ONLY the tiny header.
             *
             * The giant LazyVerticalGrid remains
             * one single composable.
             */
            if (searchMode) {
                SearchHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                    },
                    focusRequester = focusRequester,
                    onBack = {
                        exitSearch()
                    }
                )
            } else {
                PickerHeader(
                    selectedCount = selectedApps.size,
                    onCancel = onCancel,
                    onSave = {
                        onSave(
                            selectedApps.toList()
                        )
                    }
                )
            }

            /*
             * ONE grid.
             *
             * We switch only its content/state.
             */
            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(4),
                state =
                    if (searchMode) {
                        searchGridState
                    } else {
                        normalGridState
                    },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding(),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 28.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    )
            ) {

                /*
                 * NORMAL MODE
                 */
                if (!searchMode) {

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
                                    appKey(app)
                            },
                            contentType = {
                                "selected-app"
                            }
                        ) { app ->

                            val key =
                                appKey(app)

                            SelectedAppItem(
                                app = app,
                                iconBitmap =
                                    iconBitmaps
                                        .getValue(
                                            key
                                        ),
                                position =
                                    selectionPositions[
                                        key
                                    ] ?: 0,
                                onRemove = {
                                    removeSelectedApp(
                                        selectedApps =
                                            selectedApps,
                                        app = app
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
                                    top = 8.dp
                                ),
                            fontSize = 19.sp,
                            fontWeight =
                                FontWeight
                                    .SemiBold,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )
                    }
                }

                /*
                 * SEARCH MODE
                 */
                if (searchMode) {

                    item(
                        key =
                            "search-results-heading",
                        contentType =
                            "section",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {
                        Column {
                            Text(
                                text =
                                    if (
                                        searchQuery
                                            .isBlank()
                                    ) {
                                        "Todas las aplicaciones"
                                    } else {
                                        "Resultados"
                                    },
                                fontSize = 19.sp,
                                fontWeight =
                                    FontWeight
                                        .SemiBold,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                            )

                            if (
                                searchQuery
                                    .isNotBlank()
                            ) {
                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            2.dp
                                        )
                                )

                                Text(
                                    text =
                                        "${filteredApps.size} encontradas",
                                    fontSize =
                                        13.sp,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val displayedApps =
                    if (searchMode) {
                        filteredApps
                    } else {
                        apps
                    }

                if (
                    searchMode &&
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
                            "app:" +
                                appKey(app)
                        },
                        contentType = {
                            "app"
                        }
                    ) { app ->

                        val key =
                            appKey(app)

                        AllAppsItem(
                            app = app,
                            iconBitmap =
                                iconBitmaps
                                    .getValue(
                                        key
                                    ),
                            selectionPosition =
                                selectionPositions[
                                    key
                                ],
                            onToggle = {
                                toggleSelection(
                                    selectedApps =
                                        selectedApps,
                                    app = app
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
private fun rememberAppIconBitmaps(
    apps: List<InstalledApp>
): Map<String, ImageBitmap> {

    val density =
        LocalDensity.current

    /*
     * Picker displays icons around 44-46dp.
     * 48dp is enough and avoids uploading
     * unnecessarily huge launcher icons.
     */
    val iconSizePx =
        remember(density) {
            with(density) {
                48.dp.roundToPx()
            }
        }

    return remember(
        apps,
        iconSizePx
    ) {
        apps.associate { app ->

            val bitmap =
                app.icon.toBitmap(
                    width =
                        iconSizePx,
                    height =
                        iconSizePx,
                    config =
                        Bitmap.Config.ARGB_8888
                )

            /*
             * Ask Android to prepare the bitmap
             * before it is required by scrolling.
             */
            bitmap.prepareToDraw()

            appKey(app) to
                bitmap.asImageBitmap()
        }
    }
}

@Composable
private fun SelectedAppsHeading() {
    Column(
        modifier =
            Modifier.padding(
                top = 8.dp
            )
    ) {
        Text(
            text =
                "Seleccionadas",
            fontSize = 19.sp,
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
                    "El número indica su posición en Inicio.",
            fontSize = 13.sp,
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
                    alpha = 0.78f
                )
    ) {
        Text(
            text =
                "Todavía no has seleccionado aplicaciones.\n" +
                    "Elige las que quieras tener en Inicio.",
            modifier =
                Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 20.dp
                ),
            textAlign =
                TextAlign.Center,
            fontSize = 14.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
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
            Modifier.fillMaxWidth(),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha = 0.82f
                )
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 14.dp,
                    bottom = 12.dp
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
                    fontSize = 30.sp,
                    fontWeight =
                        FontWeight
                            .SemiBold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Text(
                    text =
                        "$selectedCount seleccionadas",
                    fontSize = 14.sp,
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
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha = 0.86f
                )
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = 8.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            AnimatedTextButton(
                text = "‹",
                fontSize = 32,
                onClick =
                    onBack
            )

            Spacer(
                modifier =
                    Modifier.width(
                        4.dp
                    )
            )

            OutlinedTextField(
                value =
                    searchQuery,
                onValueChange =
                    onSearchQueryChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(
                            focusRequester
                        ),
                placeholder = {
                    Text(
                        "Buscar aplicaciones"
                    )
                },
                singleLine = true,
                shape =
                    RoundedCornerShape(
                        28.dp
                    )
            )
        }
    }
}

@Composable
private fun SearchLauncher(
    onClick: () -> Unit
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val indication =
        LocalIndication.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 14.dp
            )
            .pressScale(
                interactionSource =
                    interactionSource,
                pressedScale =
                    0.98f
            )
            .clip(
                RoundedCornerShape(
                    28.dp
                )
            )
            .background(
                MaterialTheme
                    .colorScheme
                    .surfaceContainer
                    .copy(
                        alpha = 0.84f
                    )
            )
            .clickable(
                interactionSource =
                    interactionSource,
                indication =
                    indication,
                onClick =
                    onClick
            )
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {
        Text(
            text =
                "Buscar aplicaciones",
            fontSize = 16.sp,
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
    iconBitmap: ImageBitmap,
    position: Int,
    onRemove: () -> Unit
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val indication =
        LocalIndication.current

    val shape =
        RoundedCornerShape(
            22.dp
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(
                interactionSource =
                    interactionSource
            )
            .clip(shape)
            .background(
                MaterialTheme
                    .colorScheme
                    .primaryContainer
                    .copy(
                        alpha = 0.72f
                    )
            )
            .clickable(
                interactionSource =
                    interactionSource,
                indication =
                    indication,
                onClick =
                    onRemove
            )
            .padding(
                horizontal = 6.dp,
                vertical = 12.dp
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
                    7.dp
                )
        )

        Text(
            text =
                app.label,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            textAlign =
                TextAlign.Center,
            fontSize = 12.sp,
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
    iconBitmap: ImageBitmap,
    selectionPosition: Int?,
    onToggle: () -> Unit
) {
    val isSelected =
        selectionPosition != null

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val indication =
        LocalIndication.current

    val shape =
        RoundedCornerShape(
            20.dp
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(
                interactionSource =
                    interactionSource
            )
            .clip(shape)
            .background(
                if (isSelected) {
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(
                            alpha = 0.42f
                        )
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                interactionSource =
                    interactionSource,
                indication =
                    indication,
                onClick =
                    onToggle
            )
            .padding(
                horizontal = 4.dp,
                vertical = 10.dp
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
                        46.dp
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
                    7.dp
                )
        )

        Text(
            text =
                app.label,
            maxLines = 2,
            overflow =
                TextOverflow.Ellipsis,
            textAlign =
                TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight =
                if (isSelected) {
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
    modifier: Modifier = Modifier
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
                fontSize = 11.sp,
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
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    Button(
        onClick =
            onClick,
        modifier =
            Modifier.pressScale(
                interactionSource =
                    interactionSource
            ),
        interactionSource =
            interactionSource,
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {
        Text(
            text
        )
    }
}

@Composable
private fun AnimatedTextButton(
    text: String,
    onClick: () -> Unit,
    fontSize: Int? = null
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    TextButton(
        onClick =
            onClick,
        modifier =
            Modifier.pressScale(
                interactionSource =
                    interactionSource
            ),
        interactionSource =
            interactionSource
    ) {
        Text(
            text =
                text,
            fontSize =
                fontSize?.sp
                    ?: 14.sp
        )
    }
}

@Composable
private fun Modifier.pressScale(
    interactionSource:
    MutableInteractionSource,
    pressedScale: Float = 0.965f
): Modifier {

    val isPressed by
    interactionSource
        .collectIsPressedAsState()

    val scale by
    animateFloatAsState(
        targetValue =
            if (isPressed) {
                pressedScale
            } else {
                1f
            },
        animationSpec =
            if (isPressed) {
                tween(
                    durationMillis =
                        55,
                    easing =
                        FastOutSlowInEasing
                )
            } else {
                spring(
                    dampingRatio =
                        0.82f,
                    stiffness =
                        900f
                )
            },
        label =
            "pressScale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
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

private fun toggleSelection(
    selectedApps:
    MutableList<InstalledApp>,
    app: InstalledApp
) {
    val index =
        selectedApps.indexOfFirst {
            isSameApp(
                first = it,
                second = app
            )
        }

    if (index >= 0) {
        selectedApps.removeAt(
            index
        )
    } else {
        selectedApps.add(
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
        selectedApps.indexOfFirst {
            isSameApp(
                first = it,
                second = app
            )
        }

    if (index >= 0) {
        selectedApps.removeAt(
            index
        )
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

private fun normalizeSearchText(
    value: String
): String {

    return Normalizer
        .normalize(
            value,
            Normalizer.Form.NFD
        )
        .replace(
            Regex("\\p{Mn}+"),
            ""
        )
        .lowercase(
            Locale.getDefault()
        )
        .trim()
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
