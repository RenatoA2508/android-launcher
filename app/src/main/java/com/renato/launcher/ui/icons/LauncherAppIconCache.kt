package com.renato.launcher.ui.icons

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.renato.launcher.core.model.InstalledApp

private object LauncherAppIconCache {

    private val bitmaps =
        mutableMapOf<String, ImageBitmap>()

    @Synchronized
    fun get(
        app: InstalledApp,
        sizePx: Int
    ): ImageBitmap {

        val key =
            cacheKey(
                app = app,
                sizePx = sizePx
            )

        return bitmaps[key]
            ?: createBitmap(
                app = app,
                sizePx = sizePx
            ).also { bitmap ->
                bitmaps[key] = bitmap
            }
    }

    @Synchronized
    fun preload(
        apps: List<InstalledApp>,
        sizePx: Int
    ) {
        apps.forEach { app ->

            val key =
                cacheKey(
                    app = app,
                    sizePx = sizePx
                )

            if (key !in bitmaps) {
                bitmaps[key] =
                    createBitmap(
                        app = app,
                        sizePx = sizePx
                    )
            }
        }
    }

    private fun createBitmap(
        app: InstalledApp,
        sizePx: Int
    ): ImageBitmap {

        val bitmap =
            app.icon.toBitmap(
                width = sizePx,
                height = sizePx,
                config =
                    Bitmap.Config.ARGB_8888
            )

        bitmap.prepareToDraw()

        return bitmap.asImageBitmap()
    }

    private fun cacheKey(
        app: InstalledApp,
        sizePx: Int
    ): String {
        return "$sizePx:" +
            "${app.user.hashCode()}:" +
            app.componentName
                .flattenToString()
    }
}

@Composable
fun rememberLauncherAppIcon(
    app: InstalledApp
): ImageBitmap {

    val density =
        LocalDensity.current

    val sizePx =
        remember(density) {
            with(density) {
                48.dp.roundToPx()
            }
        }

    val key =
        remember(
            app.componentName,
            app.user,
            sizePx
        ) {
            "$sizePx:" +
                "${app.user.hashCode()}:" +
                app.componentName
                    .flattenToString()
        }

    return remember(key) {
        LauncherAppIconCache.get(
            app = app,
            sizePx = sizePx
        )
    }
}

@Composable
fun PreloadLauncherAppIcons(
    apps: List<InstalledApp>
) {
    val density =
        LocalDensity.current

    val sizePx =
        remember(density) {
            with(density) {
                48.dp.roundToPx()
            }
        }

    remember(
        apps,
        sizePx
    ) {
        LauncherAppIconCache.preload(
            apps = apps,
            sizePx = sizePx
        )

        true
    }
}
