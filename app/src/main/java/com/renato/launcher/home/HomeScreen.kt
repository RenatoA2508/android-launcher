package com.renato.launcher.home

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.renato.launcher.core.model.InstalledApp
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
    onOpenSearch: () -> Unit
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
            Modifier.fillMaxSize(),
        color =
            Color.Transparent
    ) {
        Column(
            modifier = Modifier
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
                             * Consume only once this has
                             * clearly become a vertical
                             * swipe rather than a tap.
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
                .combinedClickable(
                    onClick = {
                        // Empty Home tap does nothing.
                    },
                    onLongClick = {
                        if (favoritesLoaded) {
                            onEditFavorites()
                        }
                    }
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = 24.dp
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
    textColor: Color
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {
        apps
            .chunked(2)
            .forEach { rowApps ->

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

                        FavoriteAppItem(
                            app = app,
                            onClick = {
                                onAppClick(
                                    app
                                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppItem(
    app: InstalledApp,
    onClick: () -> Unit,
    textColor: Color,
    modifier: Modifier =
        Modifier
) {
    val iconBitmap =
        remember(
            app.componentName,
            app.user
        ) {
            app.icon
                .toBitmap()
                .asImageBitmap()
        }

    Row(
        modifier = modifier
            .heightIn(
                min = 52.dp
            )
            .combinedClickable(
                onClick =
                    onClick,
                onLongClick = {
                    /*
                     * App-specific context
                     * actions will come later.
                     */
                }
            )
            .padding(
                horizontal = 4.dp,
                vertical = 6.dp
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
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
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
                alpha = 0.45f
            )
        } else {
            Color.White.copy(
                alpha = 0.35f
            )
        }

    return TextStyle(
        color = color,
        fontSize = fontSize,
        fontWeight =
            fontWeight,
        shadow =
            Shadow(
                color =
                    shadowColor,
                offset =
                    Offset(
                        x = 0f,
                        y = 1f
                    ),
                blurRadius = 3f
            )
    )
}
