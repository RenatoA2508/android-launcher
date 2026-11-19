package com.renato.launcher.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelContrastTest {
    @Test
    fun textAndAccentContrastWithTheCompositedPanel() {
        for (wallpaper in listOf(Color.White, Color.Black)) {
            for (panel in listOf(Color.White, Color.Black, Color(0xFF778899))) {
                for (opacity in listOf(0f, .25f, .5f, .75f, 1f)) {
                    val background = panel.copy(alpha = opacity).compositeOver(wallpaper)
                    // An accent identical to the visible panel needs the largest correction.
                    for (foreground in listOf(ink(background), readableAccent(background, background))) {
                        val high = maxOf(foreground.luminance(), background.luminance())
                        val low = minOf(foreground.luminance(), background.luminance())
                        assertTrue((high + .05f) / (low + .05f) >= 4.5f)
                    }
                }
            }
        }
    }

    @Test
    fun aReadableAccentKeepsItsOriginalColor() {
        val accent = Color(0xFF135A39)
        assertEquals(accent, readableAccent(accent, Color.White))
    }
}
