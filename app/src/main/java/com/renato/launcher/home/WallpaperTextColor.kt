package com.renato.launcher.home

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberWallpaperTextColor(): Color {
    val context = LocalContext.current

    var textColor by remember {
        mutableStateOf(Color.White)
    }

    LaunchedEffect(Unit) {
        val useDarkText = withContext(Dispatchers.IO) {
            val wallpaperManager =
                WallpaperManager.getInstance(context)

            val wallpaperColors =
                wallpaperManager.getWallpaperColors(
                    WallpaperManager.FLAG_SYSTEM
                )

            shouldUseDarkText(wallpaperColors)
        }

        textColor =
            if (useDarkText) {
                Color.Black
            } else {
                Color.White
            }
    }

    return textColor
}

private fun shouldUseDarkText(
    wallpaperColors: WallpaperColors?
): Boolean {

    if (wallpaperColors == null) {
        return false
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val supportsDarkText =
            wallpaperColors.colorHints and
                WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0

        if (supportsDarkText) {
            return true
        }
    }

    val primaryColor =
        wallpaperColors.primaryColor.toArgb()

    val luminance =
        ColorUtils.calculateLuminance(primaryColor)

    return luminance > 0.5
}
