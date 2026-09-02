package com.renato.launcher.ui.icons

import android.graphics.Bitmap
import android.os.UserHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.renato.launcher.core.model.InstalledApp

private data class LauncherIconCacheKey(
    val sizePx: Int,
    val userHash: Int,
    val packageName: String,
    val componentName: String
)

private object LauncherAppIconCache {

    private val bitmaps =
        mutableMapOf<
            LauncherIconCacheKey,
            ImageBitmap
        >()

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

    @Synchronized
    fun invalidate(
        packageNames: Set<String>,
        user: UserHandle
    ) {
        if (
            packageNames.isEmpty()
        ) {
            return
        }

        val userHash =
            user.hashCode()

        bitmaps.keys
            .removeAll {
                    key ->

                key.userHash ==
                    userHash &&
                key.packageName in
                    packageNames
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
    ): LauncherIconCacheKey {
        return LauncherIconCacheKey(
            sizePx =
                sizePx,
            userHash =
                app.user.hashCode(),
            packageName =
                app.packageName,
            componentName =
                app.componentName
                    .flattenToString()
        )
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

    /*
     * Include the InstalledApp snapshot itself in the remember key.
     *
     * Package updates can keep the same component name while replacing the
     * icon Drawable. A refreshed InstalledApp must therefore re-enter the
     * process cache even when its component is unchanged.
     */
    return remember(
        app,
        sizePx
    ) {
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

/**
 * Invalidates only icons owned by packages affected by a LauncherApps callback.
 *
 * Install/update/remove events are rare, so this keeps the common rendering
 * path untouched while preventing a package update or reinstall from showing a
 * bitmap cached from the previous APK.
 */
fun invalidateLauncherAppIcons(
    packageNames: Collection<String>,
    user: UserHandle
) {
    LauncherAppIconCache.invalidate(
        packageNames =
            packageNames.toSet(),
        user =
            user
    )
}
