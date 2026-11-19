package com.renato.launcher.ui.theme

import com.renato.launcher.appearance.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/*
 * These are only fallbacks for devices older than
 * Android 12, where Material dynamic colors do not exist.
 */
private val LightFallbackColorScheme =
    lightColorScheme()

private val DarkFallbackColorScheme =
    darkColorScheme()

@Composable
fun LauncherTheme(
    darkTheme: Boolean =
        isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colors: LauncherColors = LauncherColors(),
    content: @Composable () -> Unit
) {
    val context =
        LocalContext.current

    val colorScheme =
        when {

            /*
             * Android 12+:
             *
             * Use the device / wallpaper / system
             * Material You palette.
             *
             * On Samsung this follows the One UI
             * color palette selected by the user.
             */
            dynamicColor &&
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S -> {

                if (darkTheme) {
                    dynamicDarkColorScheme(
                        context
                    )
                } else {
                    dynamicLightColorScheme(
                        context
                    )
                }
            }

            darkTheme ->
                DarkFallbackColorScheme

            else ->
                LightFallbackColorScheme
        }

    val palette = if (colors.custom) {
        val accent = Color(colors.accent)
        val foregroundAccent = readableAccent(accent, colorScheme.surface)
        colorScheme.copy(primary = foregroundAccent, onPrimary = ink(foregroundAccent),
            secondary = foregroundAccent, onSecondary = ink(foregroundAccent),
            primaryContainer = accent, onPrimaryContainer = ink(accent),
            secondaryContainer = accent, onSecondaryContainer = ink(accent))
    } else colorScheme
    CompositionLocalProvider(LocalLauncherColors provides colors) {
        MaterialTheme(colorScheme = palette, typography = Typography, content = content)
    }
}
