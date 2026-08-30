package com.renato.launcher.search

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.renato.launcher.core.model.InstalledApp
import java.text.Normalizer
import java.util.Locale
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity

@Composable
fun SearchScreen(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    onBack: () -> Unit
) {
    SearchWindowEffect()

    var query by remember {
        mutableStateOf("")
    }

    val focusRequester =
        remember {
            FocusRequester()
        }

    val focusManager =
        LocalFocusManager.current

    val keyboardController =
        LocalSoftwareKeyboardController.current

    val gridState =
        rememberLazyGridState()

    val density =
        LocalDensity.current

    val swipeDownThresholdPx =
        remember(density) {
            with(density) {
                72.dp.toPx()
            }
        }

    val results =
        remember(
            apps,
            query
        ) {
            if (query.isBlank()) {
                emptyList()
            } else {
                val normalizedQuery =
                    normalizeSearchText(
                        query
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

    fun closeSearch() {
        keyboardController?.hide()
        focusManager.clearFocus()
        onBack()
    }

    val swipeDownConnection =
    remember(
        gridState,
        swipeDownThresholdPx,
        keyboardController,
        focusManager,
        onBack
    ) {
        object :
            NestedScrollConnection {

            var accumulatedDownwardDrag =
                0f

            var closeTriggered =
                false

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {

                val isAtTop =
                    gridState
                        .firstVisibleItemIndex == 0 &&
                        gridState
                            .firstVisibleItemScrollOffset == 0

                /*
                 * Finger moving downward while
                 * the list is already at the top.
                 */
                if (
                    source ==
                        NestedScrollSource.UserInput &&
                    isAtTop &&
                    available.y > 0f
                ) {

                    accumulatedDownwardDrag +=
                        available.y

                    if (
                        !closeTriggered &&
                        accumulatedDownwardDrag >=
                            swipeDownThresholdPx
                    ) {
                        closeTriggered = true

                        keyboardController?.hide()
                        focusManager.clearFocus()

                        onBack()
                    }

                } else if (
                    available.y < 0f ||
                    !isAtTop
                ) {
                    accumulatedDownwardDrag =
                        0f

                    closeTriggered =
                        false
                }

                /*
                 * We observe the movement but do not
                 * consume it.
                 *
                 * LazyVerticalGrid continues owning
                 * its normal scrolling behavior.
                 */
                return Offset.Zero
            }

            override suspend fun onPreFling(
                available: Velocity
            ): Velocity {

                accumulatedDownwardDrag =
                    0f

                closeTriggered =
                    false

                return Velocity.Zero
            }
        }
    }

    BackHandler {
        closeSearch()
    }

    /*
     * Do not ask Compose, focus and the IME to do
     * everything in the same frame.
     *
     * Search UI
     *   ↓ next frame
     * focus
     *   ↓ next frame
     * keyboard
     */
    LaunchedEffect(Unit) {
        withFrameNanos { }

        focusRequester.requestFocus()

        withFrameNanos { }

        keyboardController?.show()
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
                            alpha = 0.82f
                        )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            SearchHeader(
                query = query,
                onQueryChange = {
                    query = it
                },
                focusRequester =
                    focusRequester,
                onBack = {
                    closeSearch()
                },
                onClear = {
                    query = ""
                },
                onSubmit = {
                    results
                        .firstOrNull()
                        ?.let(
                            onAppClick
                        )
                }
            )

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(4),
                state =
                    gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
                    .nestedScroll(
                        swipeDownConnection
                    ),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = 24.dp
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
                 * Empty Search will eventually contain:
                 *
                 * - recent apps
                 * - recently searched apps
                 *
                 * For this first checkpoint we keep
                 * this area intentionally simple.
                 */
                if (query.isBlank()) {

                    item(
                        key = "empty-search",
                        contentType = "message",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {
                        EmptySearchState()
                    }

                } else if (
                    results.isEmpty()
                ) {

                    item(
                        key =
                            "no-results",
                        contentType =
                            "message",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {
                        NoResultsState(
                            query = query
                        )
                    }

                } else {

                    item(
                        key =
                            "results-heading",
                        contentType =
                            "heading",
                        span = {
                            GridItemSpan(
                                maxLineSpan
                            )
                        }
                    ) {
                        Column {
                            Text(
                                text = "Aplicaciones",
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
                                    "${results.size} resultados",
                                fontSize = 13.sp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = results,
                        key = { app ->
                            appKey(app)
                        },
                        contentType = {
                            "app"
                        }
                    ) { app ->

                        SearchAppItem(
                            app = app,
                            onClick = {
                                onAppClick(app)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha = 0.88f
                )
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = 8.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            PressTextButton(
                text = "‹",
                fontSize = 32,
                onClick =
                    onBack
            )

            Spacer(
                modifier =
                    Modifier.width(
                        2.dp
                    )
            )

            OutlinedTextField(
                value = query,
                onValueChange =
                    onQueryChange,
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
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(
                            onClick =
                                onClear
                        ) {
                            Text(
                                text = "×",
                                fontSize = 22.sp
                            )
                        }
                    }
                },
                keyboardOptions =
                    KeyboardOptions(
                        imeAction =
                            ImeAction.Go
                    ),
                keyboardActions =
                    KeyboardActions(
                        onGo = {
                            onSubmit()
                        }
                    ),
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
private fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 48.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text =
                "Buscar aplicaciones",
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Medium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        Text(
            text =
                "Empieza a escribir el nombre de una aplicación.",
            modifier =
                Modifier.padding(
                    horizontal = 24.dp
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
private fun NoResultsState(
    query: String
) {
    Text(
        text =
            "No encontramos una aplicación para \"$query\".",
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 48.dp,
                start = 24.dp,
                end = 24.dp
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

@Composable
private fun SearchAppItem(
    app: InstalledApp,
    onClick: () -> Unit
) {
    val iconBitmap =
        remember(
            app.componentName,
            app.user
        ) {
            app.icon
                .toBitmap(
                    width = 96,
                    height = 96
                )
                .asImageBitmap()
        }

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val indication =
        LocalIndication.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(
                interactionSource =
                    interactionSource
            )
            .clip(
                RoundedCornerShape(
                    20.dp
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
                horizontal = 4.dp,
                vertical = 10.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
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
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}

@Composable
private fun PressTextButton(
    text: String,
    fontSize: Int,
    onClick: () -> Unit
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    TextButton(
        onClick =
            onClick,
        interactionSource =
            interactionSource,
        modifier =
            Modifier.pressScale(
                interactionSource =
                    interactionSource
            )
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp
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
                        durationMillis = 55,
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
                "searchPressScale"
        )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun SearchWindowEffect() {
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

private fun appKey(
    app: InstalledApp
): String {

    return "${app.user.hashCode()}:" +
        app.componentName
            .flattenToString()
}
