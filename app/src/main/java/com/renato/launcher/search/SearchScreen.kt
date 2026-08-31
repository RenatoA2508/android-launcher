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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.ui.components.LauncherSearchBar
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
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
     * Search index.
     *
     * Application names are normalized only when
     * the installed app list changes, not for every
     * keyboard character.
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
                    app =
                        app,
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

    /*
     * Shared launcher icon cache.
     *
     * When Search is empty, preload the applications
     * that will immediately be visible.
     *
     * When typing, preload the first search results.
     */
    val iconsToPreload =
        if (query.isBlank()) {

            recentApps +
                recentSearchApps

        } else {

            results.take(
                12
            )
        }

    PreloadLauncherAppIcons(
        apps =
            iconsToPreload
                .distinctBy {
                    appKey(it)
                }
    )

    fun closeSearch() {

        keyboardController?.hide()

        focusManager.clearFocus()

        onBack()
    }

    /*
     * Swipe down closes Search only when the list is
     * already at the top.
     *
     * Therefore normal scrolling through results
     * cannot accidentally close Search.
     */
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
                     * Observe only.
                     *
                     * Do not steal the gesture from
                     * LazyVerticalGrid.
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
     * Search startup:
     *
     * frame 1 -> compose Search
     * frame 2 -> request focus
     * frame 3 -> show IME
     *
     * This produced better 120 Hz behavior than
     * performing everything simultaneously.
     */
    LaunchedEffect(Unit) {

        withFrameNanos { }

        focusRequester.requestFocus()

        withFrameNanos { }

        keyboardController?.show()
    }

    /*
     * A changed query should show its results
     * from the beginning.
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

            gridState.scrollToItem(
                0
            )
        }
    }

    /*
     * testTagsAsResourceId allows UI Automator to
     * find our Compose testTag values through
     * By.res(...).
     *
     * This has no visual effect.
     */
    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics {
                    testTagsAsResourceId =
                        true
                }
                .testTag(
                    SEARCH_ROOT_TAG
                ),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha = 0.82f
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {

            /*
             * Stable automation tag:
             *
             * launcher_search_field
             */
            LauncherSearchBar(
                query =
                    query,
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

                            onAppClick(
                                app,
                                true
                            )
                        }
                },
                fieldTestTag =
                    SEARCH_FIELD_TAG
            )

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(
                        4
                    ),
                state =
                    gridState,
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxWidth()
                        .imePadding()
                        .nestedScroll(
                            swipeDownConnection
                        ),
                contentPadding =
                    PaddingValues(
                        start =
                            20.dp,
                        end =
                            20.dp,
                        top =
                            6.dp,
                        bottom =
                            16.dp
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

                /*
                 * =================================================
                 * EMPTY QUERY
                 * =================================================
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

                        /*
                         * -----------------------------
                         * RECENT APPS
                         * -----------------------------
                         */

                        if (
                            recentApps.isNotEmpty()
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
                                        appKey(
                                            app
                                        )
                                },
                                contentType = {
                                    "app"
                                }
                            ) { app ->

                                SearchAppItem(
                                    app =
                                        app,
                                    onClick = {

                                        /*
                                         * Opening from Recents:
                                         *
                                         * update general recents,
                                         * but don't count as a new
                                         * typed search.
                                         */
                                        onAppClick(
                                            app,
                                            false
                                        )
                                    }
                                )
                            }
                        }

                        /*
                         * -----------------------------
                         * RECENT SEARCH APPS
                         * -----------------------------
                         */

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
                                        appKey(
                                            app
                                        )
                                },
                                contentType = {
                                    "app"
                                }
                            ) { app ->

                                SearchAppItem(
                                    app =
                                        app,
                                    onClick = {

                                        /*
                                         * Reopening something from
                                         * the search history moves
                                         * it back to the top of
                                         * that history.
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
                     * =================================================
                     * QUERY WITH NO RESULTS
                     * =================================================
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
                     * =================================================
                     * SEARCH RESULTS
                     * =================================================
                     *
                     * No "Aplicaciones" heading.
                     * No result counter.
                     *
                     * Results start immediately below
                     * the search field.
                     *
                     * Every result gets an automation
                     * tag:
                     *
                     * launcher_search_result_0
                     * launcher_search_result_1
                     * launcher_search_result_2
                     * ...
                     */

                    itemsIndexed(
                        items =
                            results,
                        key = {
                                _,
                                app ->

                            "result:" +
                                appKey(
                                    app
                                )
                        },
                        contentType = {
                                _,
                                _ ->

                            "app"
                        }
                    ) {
                            index,
                            app ->

                        SearchAppItem(
                            app =
                                app,
                            testTag =
                                "$SEARCH_RESULT_TAG_PREFIX$index",
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
private fun SearchSectionHeader(
    title: String
) {

    Text(
        text =
            title,
        modifier =
            Modifier.padding(
                top =
                    8.dp,
                bottom =
                    0.dp
            ),
        fontSize =
            17.sp,
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        40.dp
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        40.dp,
                    start =
                        24.dp,
                    end =
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

@Composable
private fun SearchAppItem(
    app: InstalledApp,
    testTag: String? = null,
    onClick: () -> Unit
) {

    /*
     * Shared process-wide launcher icon cache.
     *
     * No Drawable -> Bitmap conversion happens
     * inside Search.
     */
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val indication =
        LocalIndication.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()

                /*
                 * Search result automation tag.
                 *
                 * Recent apps don't get one because
                 * the Baseline Profile only needs
                 * deterministic tags for typed
                 * search results.
                 */
                .then(
                    if (
                        testTag != null
                    ) {
                        Modifier.testTag(
                            testTag
                        )
                    } else {
                        Modifier
                    }
                )

                .pressScale(
                    interactionSource =
                        interactionSource
                )
                .clip(
                    RoundedCornerShape(
                        18.dp
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
                    horizontal =
                        3.dp,
                    vertical =
                        6.dp
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
                    44.dp
                )
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
}

@Composable
private fun Modifier.pressScale(
    interactionSource:
    MutableInteractionSource,
    pressedScale: Float =
        0.965f
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

    return graphicsLayer {

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
 * ============================================================
 * SEARCH ENGINE
 * ============================================================
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

                if (
                    score == null
                ) {

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
     * Ranking:
     *
     * 1. quality score
     * 2. shorter application name
     * 3. locale-aware alphabetical order
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

            rankedResult
                .entry
                .app
        }
}

private fun searchScore(
    entry: SearchIndexEntry,
    query: String
): Int? {

    val compactQuery =
        query.replace(
            " ",
            ""
        )

    return when {

        /*
         * Exact application name.
         *
         * spotify -> Spotify
         */
        entry.normalizedLabel ==
            query -> 0

        /*
         * Known alias.
         *
         * wsp -> WhatsApp
         * ds  -> Discord
         * gpt -> ChatGPT
         */
        entry.aliases
            .contains(
                compactQuery
            ) -> 1

        /*
         * Automatic acronym.
         *
         * gm -> Google Maps
         * sn -> Samsung Notes
         */
        entry.acronym
            .isNotEmpty() &&
            entry.acronym ==
            compactQuery -> 1

        /*
         * Application name starts with query.
         *
         * spo -> Spotify
         */
        entry.normalizedLabel
            .startsWith(
                query
            ) -> 2

        /*
         * A word starts with query.
         *
         * notes -> Samsung Notes
         */
        entry.normalizedWords
            .any { word ->

                word.startsWith(
                    query
                )
            } -> 3

        /*
         * Partial alias.
         *
         * ws -> WhatsApp
         */
        compactQuery.length >= 2 &&
            entry.aliases
                .any { alias ->

                    alias.startsWith(
                        compactQuery
                    )
                } -> 4

        /*
         * Partial automatic acronym.
         */
        compactQuery.length >= 2 &&
            entry.acronym
                .isNotEmpty() &&
            entry.acronym
                .startsWith(
                    compactQuery
                ) -> 4

        /*
         * Last fallback:
         *
         * query appears somewhere inside the name.
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

/*
 * ============================================================
 * AUTOMATION TAGS
 * ============================================================
 *
 * These strings are intentionally stable.
 *
 * BaselineProfileGenerator will use them through UI Automator.
 */

private const val SEARCH_FIELD_TAG =
    "launcher_search_field"

private const val SEARCH_RESULT_TAG_PREFIX =
    "launcher_search_result_"

private const val SEARCH_ROOT_TAG =
    "launcher_search_root"
