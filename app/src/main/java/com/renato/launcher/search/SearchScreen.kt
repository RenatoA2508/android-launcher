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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.renato.launcher.core.model.InstalledApp
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

@Composable
fun SearchScreen(
    apps: List<InstalledApp>,
    recentApps: List<InstalledApp>,
    recentSearchApps: List<InstalledApp>,
    onAppClick: (
        InstalledApp,
        Boolean
    ) -> Unit,
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

    /*
     * Build the normalized search index only when
     * the installed application list changes.
     *
     * We do NOT normalize every application again
     * for every keyboard character.
     */
    val searchIndex =
        remember(apps) {
            apps.map { app ->

                val normalizedLabel =
                    normalizeSearchText(
                        app.label
                    )

                val words =
                    splitSearchWords(
                        normalizedLabel
                    )

                SearchIndexEntry(
                    app = app,
                    normalizedLabel =
                        normalizedLabel,
                    normalizedWords =
                        words,
                    acronym =
                        buildAcronym(
                            words
                        ),
                    aliases =
                        searchAliasesFor(
                            normalizedLabel
                        )
                )
            }
        }

    val collator =
        remember {
            Collator
                .getInstance(
                    Locale.getDefault()
                )
                .apply {
                    strength =
                        Collator.PRIMARY
                }
        }

    val results =
        remember(
            searchIndex,
            query
        ) {
            rankSearchResults(
                searchIndex =
                    searchIndex,
                query =
                    query,
                collator =
                    collator
            )
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
                            closeTriggered =
                                true

                            keyboardController
                                ?.hide()

                            focusManager
                                .clearFocus()

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
                     * Observe the gesture without
                     * stealing scrolling from the grid.
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
     * Frame 1:
     * Search UI becomes visible.
     *
     * Frame 2:
     * Search field receives focus.
     *
     * Frame 3:
     * Keyboard opens.
     *
     * This proved smoother than doing all three
     * operations in the same frame.
     */
    LaunchedEffect(Unit) {
        withFrameNanos { }

        focusRequester.requestFocus()

        withFrameNanos { }

        keyboardController?.show()
    }

    /*
     * If the user has scrolled search results and
     * changes the query, move the new results back
     * to the beginning.
     */
    LaunchedEffect(query) {
        if (
            query.isNotBlank() &&
            (
                gridState
                    .firstVisibleItemIndex > 0 ||
                    gridState
                        .firstVisibleItemScrollOffset > 0
                )
        ) {
            gridState.scrollToItem(0)
        }
    }

    Surface(
        modifier =
            Modifier.fillMaxSize(),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha = 0.82f
                )
    ) {
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
                        ?.let { app ->

                            /*
                             * true:
                             * this application was opened
                             * from an actual typed search.
                             */
                            onAppClick(
                                app,
                                true
                            )
                        }
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
                 * SEARCH IS EMPTY
                 *
                 * Show recent applications and
                 * recently searched applications.
                 */
                if (query.isBlank()) {

                    if (
                        recentApps.isEmpty() &&
                        recentSearchApps.isEmpty()
                    ) {

                        item(
                            key =
                                "empty-search",
                            contentType =
                                "message",
                            span = {
                                GridItemSpan(
                                    maxLineSpan
                                )
                            }
                        ) {
                            EmptySearchState()
                        }

                    } else {

                        if (
                            recentApps
                                .isNotEmpty()
                        ) {

                            item(
                                key =
                                    "recent-heading",
                                contentType =
                                    "heading",
                                span = {
                                    GridItemSpan(
                                        maxLineSpan
                                    )
                                }
                            ) {
                                SearchSectionHeader(
                                    title =
                                        "Recientes"
                                )
                            }

                            items(
                                items =
                                    recentApps,
                                key = { app ->
                                    "recent:" +
                                        appKey(app)
                                },
                                contentType = {
                                    "app"
                                }
                            ) { app ->

                                SearchAppItem(
                                    app = app,
                                    onClick = {

                                        /*
                                         * Opening from Recents
                                         * updates Recents but is
                                         * not considered a typed
                                         * search.
                                         */
                                        onAppClick(
                                            app,
                                            false
                                        )
                                    }
                                )
                            }
                        }

                        if (
                            recentSearchApps
                                .isNotEmpty()
                        ) {

                            item(
                                key =
                                    "recent-search-heading",
                                contentType =
                                    "heading",
                                span = {
                                    GridItemSpan(
                                        maxLineSpan
                                    )
                                }
                            ) {
                                SearchSectionHeader(
                                    title =
                                        "Buscadas recientemente"
                                )
                            }

                            items(
                                items =
                                    recentSearchApps,
                                key = { app ->
                                    "searched:" +
                                        appKey(app)
                                },
                                contentType = {
                                    "app"
                                }
                            ) { app ->

                                SearchAppItem(
                                    app = app,
                                    onClick = {

                                        /*
                                         * Reopening an application
                                         * from search history moves
                                         * it back to the beginning
                                         * of that history.
                                         */
                                        onAppClick(
                                            app,
                                            true
                                        )
                                    }
                                )
                            }
                        }
                    }

                } else if (
                    results.isEmpty()
                ) {

                    /*
                     * QUERY WITH NO RESULTS
                     */
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
                            query =
                                query
                        )
                    }

                } else {

                    /*
                     * QUERY WITH RESULTS
                     */
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
                                text =
                                    "Aplicaciones",
                                fontSize =
                                    19.sp,
                                fontWeight =
                                    FontWeight
                                        .SemiBold,
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
                                        results.size ==
                                        1
                                    ) {
                                        "1 resultado"
                                    } else {
                                        "${results.size} resultados"
                                    },
                                fontSize =
                                    13.sp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items =
                            results,
                        key = { app ->
                            "result:" +
                                appKey(app)
                        },
                        contentType = {
                            "app"
                        }
                    ) { app ->

                        SearchAppItem(
                            app = app,
                            onClick = {

                                onAppClick(
                                    app,
                                    true
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
                value =
                    query,
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
                        text =
                            "Buscar aplicaciones"
                    )
                },
                trailingIcon = {

                    if (
                        query.isNotEmpty()
                    ) {
                        TextButton(
                            onClick =
                                onClear
                        ) {
                            Text(
                                text = "×",
                                fontSize =
                                    22.sp
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
private fun SearchSectionHeader(
    title: String
) {
    Text(
        text =
            title,
        modifier =
            Modifier.padding(
                top = 8.dp,
                bottom = 2.dp
            ),
        fontSize =
            19.sp,
        fontWeight =
            FontWeight.SemiBold,
        color =
            MaterialTheme
                .colorScheme
                .onSurface
    )
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
            fontSize =
                18.sp,
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
                    horizontal =
                        24.dp
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
        fontSize =
            14.sp,
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
            fontSize =
                12.sp,
            lineHeight =
                14.sp,
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
            text =
                text,
            fontSize =
                fontSize.sp
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
                "searchPressScale"
        )

    return this.graphicsLayer {
        scaleX =
            scale

        scaleY =
            scale
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

/*
 * ------------------------------------------------------------------
 * SEARCH ENGINE
 * ------------------------------------------------------------------
 */

private data class SearchIndexEntry(
    val app: InstalledApp,
    val normalizedLabel: String,
    val normalizedWords: List<String>,
    val acronym: String,
    val aliases: Set<String>
)

private data class RankedSearchResult(
    val entry: SearchIndexEntry,
    val score: Int
)

private fun rankSearchResults(
    searchIndex:
        List<SearchIndexEntry>,
    query: String,
    collator: Collator
): List<InstalledApp> {

    val normalizedQuery =
        normalizeSearchText(
            query
        )

    if (
        normalizedQuery.isBlank()
    ) {
        return emptyList()
    }

    val rankedResults =
        searchIndex
            .mapNotNull { entry ->

                val score =
                    searchScore(
                        entry =
                            entry,
                        query =
                            normalizedQuery
                    )

                if (score == null) {
                    null
                } else {
                    RankedSearchResult(
                        entry =
                            entry,
                        score =
                            score
                    )
                }
            }

    /*
     * Explicit Comparator instead of labeled
     * returns inside sortedWith.
     *
     * This avoids the Kotlin return-type problem
     * we encountered previously.
     */
    val comparator =
        Comparator<RankedSearchResult> {
                first,
                second ->

            when {

                first.score !=
                    second.score -> {

                    first.score
                        .compareTo(
                            second.score
                        )
                }

                first.entry
                    .normalizedLabel
                    .length !=
                    second.entry
                        .normalizedLabel
                        .length -> {

                    first.entry
                        .normalizedLabel
                        .length
                        .compareTo(
                            second.entry
                                .normalizedLabel
                                .length
                        )
                }

                else -> {

                    collator.compare(
                        first.entry
                            .app
                            .label,
                        second.entry
                            .app
                            .label
                    )
                }
            }
        }

    return rankedResults
        .sortedWith(
            comparator
        )
        .map { rankedResult ->
            rankedResult.entry.app
        }
}

private fun searchScore(
    entry: SearchIndexEntry,
    query: String
): Int? {

    /*
     * Acronyms and aliases don't contain spaces,
     * so make a compact version of the query too.
     *
     * Example:
     *
     * "g m" → "gm"
     */
    val compactQuery =
        query.replace(
            " ",
            ""
        )

    return when {

        /*
         * 1. Exact application name.
         *
         * "spotify"
         *      ↓
         * Spotify
         */
        entry.normalizedLabel ==
            query -> 0

        /*
         * 2. Exact known abbreviation.
         *
         * wsp → WhatsApp
         * ds  → Discord
         *
         * Or exact automatic acronym:
         *
         * gm → Google Maps
         * sn → Samsung Notes
         */
        entry.aliases
            .contains(
                compactQuery
            ) -> 1

        entry.acronym
            .isNotEmpty() &&
            entry.acronym ==
                compactQuery -> 1

        /*
         * 3. Application name starts with query.
         *
         * spo → Spotify
         */
        entry.normalizedLabel
            .startsWith(
                query
            ) -> 2

        /*
         * 4. A word inside the app name starts
         *    with the query.
         *
         * maps → Google Maps
         * notes → Samsung Notes
         */
        entry.normalizedWords
            .any { word ->
                word.startsWith(
                    query
                )
            } -> 3

        /*
         * 5. Partial abbreviation/acronym.
         *
         * ws → WhatsApp via "wsp"
         *
         * We require at least two characters
         * to prevent a one-character search from
         * matching dozens of acronyms.
         */
        compactQuery.length >= 2 &&
            entry.aliases
                .any { alias ->
                    alias.startsWith(
                        compactQuery
                    )
                } -> 4

        compactQuery.length >= 2 &&
            entry.acronym
                .isNotEmpty() &&
            entry.acronym
                .startsWith(
                    compactQuery
                ) -> 4

        /*
         * 6. Last resort:
         * query appears somewhere in the name.
         */
        entry.normalizedLabel
            .contains(
                query
            ) -> 5

        else ->
            null
    }
}

private fun splitSearchWords(
    normalizedLabel: String
): List<String> {

    return normalizedLabel
        .split(
            Regex(
                "[^\\p{L}\\p{N}]+"
            )
        )
        .filter {
            it.isNotBlank()
        }
}

private fun buildAcronym(
    words: List<String>
): String {

    /*
     * Acronyms only make sense when an app
     * has at least two words.
     *
     * Google Maps     → gm
     * Google Photos   → gp
     * Samsung Notes   → sn
     * Microsoft Teams → mt
     */
    if (
        words.size < 2
    ) {
        return ""
    }

    return words
        .mapNotNull { word ->
            word.firstOrNull()
        }
        .joinToString(
            separator = ""
        )
}

private fun searchAliasesFor(
    normalizedLabel: String
): Set<String> {

    /*
     * Some commonly used abbreviations cannot be
     * derived reliably from the application name.
     *
     * Keep this list intentionally small and
     * predictable. We can expand it later when
     * there is a real use case.
     */
    return when {

        normalizedLabel
            .startsWith(
                "whatsapp"
            ) -> {
            setOf(
                "wsp",
                "ws",
                "wa",
                "wpp"
            )
        }

        normalizedLabel
            .startsWith(
                "discord"
            ) -> {
            setOf(
                "ds",
                "dc"
            )
        }

        normalizedLabel
            .startsWith(
                "instagram"
            ) -> {
            setOf(
                "ig"
            )
        }

        normalizedLabel
            .startsWith(
                "youtube"
            ) -> {
            setOf(
                "yt"
            )
        }

        normalizedLabel
            .startsWith(
                "telegram"
            ) -> {
            setOf(
                "tg"
            )
        }

        normalizedLabel
            .startsWith(
                "chatgpt"
            ) -> {
            setOf(
                "gpt"
            )
        }

        else ->
            emptySet()
    }
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

private fun appKey(
    app: InstalledApp
): String {

    return "${app.user.hashCode()}:" +
        app.componentName
            .flattenToString()
}
