package com.renato.launcher.appearance

import android.app.Activity
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.content.ContextWrapper
import android.content.Context
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

data class LauncherColors(
    val custom: Boolean = false,
    val accent: Int = 0xFF4268DA.toInt(),
    val panel: Int = 0xFF20242E.toInt(),
    val opacity: Int = 86
) {
    fun save(context: Context) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit()
            .putBoolean("custom", custom).putInt("accent", accent)
            .putInt("panel", panel).putInt("opacity", opacity).apply()
    }

    companion object {
        fun read(context: Context): LauncherColors {
            val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
            val defaults = LauncherColors()
            return LauncherColors(prefs.getBoolean("custom", false),
                prefs.getInt("accent", defaults.accent), prefs.getInt("panel", defaults.panel),
                prefs.getInt("opacity", defaults.opacity).coerceIn(0, 100))
        }
    }
}

val LocalLauncherColors = compositionLocalOf { LauncherColors() }

internal fun ink(background: Color) =
    if (background.luminance() > 0.179f) Color.Black else Color.White

private fun contrast(a: Color, b: Color): Float {
    val first = a.luminance()
    val second = b.luminance()
    return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
}

internal fun readableAccent(accent: Color, background: Color): Color {
    if (contrast(accent, background) >= 4.5f) return accent
    val target = ink(background)
    return (1..20).asSequence().map { lerp(accent, target, it / 20f) }
        .firstOrNull { contrast(it, background) >= 4.5f } ?: target
}

@Composable
fun panelColor(systemAlpha: Float): Color {
    val settings = LocalLauncherColors.current
    return if (settings.custom) Color(settings.panel).copy(alpha = settings.opacity / 100f)
    else MaterialTheme.colorScheme.surface.copy(alpha = systemAlpha)
}

@Composable
private fun wallpaperColor(): Color {
    val manager = WallpaperManager.getInstance(LocalContext.current)
    val fallback = MaterialTheme.colorScheme.surface
    fun current() = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        ?.primaryColor?.toArgb()?.let { Color(it) } ?: fallback
    var color by remember(manager, fallback) { mutableStateOf(current()) }
    DisposableEffect(manager, fallback) {
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) color = current()
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }
    return color
}

@Composable
fun PanelTheme(scrim: Color = Color.Transparent, content: @Composable () -> Unit) {
    val settings = LocalLauncherColors.current
    if (!settings.custom) {
        content()
        return
    }
    val base = panelColor(1f).compositeOver(scrim.compositeOver(wallpaperColor()))
    val foreground = ink(base)
    val accent = readableAccent(Color(settings.accent), base)
    val container = lerp(base, foreground, .09f)
    val scheme = MaterialTheme.colorScheme.copy(
        surface = base, onSurface = foreground, onSurfaceVariant = foreground,
        surfaceContainer = container, surfaceContainerHigh = container,
        surfaceVariant = container,
        outline = foreground.copy(alpha = .72f), primary = accent, onPrimary = ink(accent)
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides foreground, content = content)
    }
}

@Composable
internal fun StatusBarOn(background: Color) {
    val view = LocalView.current
    val window = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>().firstOrNull()?.window ?: return
    val darkIcons = ink(background) == Color.Black
    DisposableEffect(window, darkIcons) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = darkIcons
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}
