package com.renato.launcher.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val SHORTCUT_WINDOW_MS = 180L
private val ShortcutMinTravel = 32.dp

internal fun Modifier.searchSwipe(
    onDrag: (Float) -> Unit,
    onRelease: (cancelled: Boolean) -> Unit,
    onShortcut: () -> Unit
): Modifier = pointerInput(onDrag, onRelease, onShortcut) {
    val minTravel = ShortcutMinTravel.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // A favorite that owns the long press consumes movement before its parent.
        var change = awaitVerticalTouchSlopOrCancellation(down.id) { motion, _ ->
            motion.consume()
        } ?: return@awaitEachGesture

        val height = size.height.coerceAtLeast(1)
        var interactive = false
        var peak = 0f
        var previousTravel = 0f
        var released = false
        try {
            while (true) {
                val travel = down.position.y - change.position.y
                peak = maxOf(peak, travel)
                val remaining = SHORTCUT_WINDOW_MS - (change.uptimeMillis - down.uptimeMillis)
                interactive = interactive || remaining <= 0 ||
                    peak - travel > viewConfiguration.touchSlop

                if (interactive) {
                    val upwardTravel = travel.coerceAtLeast(0f)
                    onDrag((upwardTravel - previousTravel) / height)
                    previousTravel = upwardTravel
                }
                change.consume()
                if (!change.pressed) {
                    released = true
                    if (!interactive && travel >= minTravel &&
                        travel > abs(change.position.x - down.position.x)
                    ) {
                        onShortcut()
                    }
                    break
                }

                // A held finger produces no MOVE events. Reveal its actual position
                // as soon as the short-swipe window expires, even while stationary.
                val event = if (interactive) awaitPointerEvent() else {
                    withTimeoutOrNull(remaining) { awaitPointerEvent() }
                }
                if (event == null) {
                    interactive = true
                    continue
                }
                change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) break
            }
        } finally {
            if (interactive) onRelease(!released)
        }
    }
}
