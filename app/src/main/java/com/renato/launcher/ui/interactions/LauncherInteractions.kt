package com.renato.launcher.ui.interactions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class LauncherPressVisual(
    val interactionSource: MutableInteractionSource,
    val indication: Indication,
    val modifier: Modifier
)

@Composable
private fun rememberLauncherPressVisual(
    shape: Shape
): LauncherPressVisual {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val isPressed by
        interactionSource
            .collectIsPressedAsState()

    val darkTheme =
        isSystemInDarkTheme()

    val pressedAlpha by
        animateFloatAsState(
            targetValue =
                if (isPressed) {
                    if (darkTheme) {
                        0.14f
                    } else {
                        0.08f
                    }
                } else {
                    0f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (isPressed) {
                            55
                        } else {
                            110
                        }
                ),
            label =
                "launcherPressedAlpha"
        )

    val touchColor =
        if (darkTheme) {
            Color.White
        } else {
            Color.Black
        }

    val indication =
        ripple(
            bounded =
                true,
            color =
                touchColor.copy(
                    alpha =
                        0.14f
                )
        )

    return LauncherPressVisual(
        interactionSource =
            interactionSource,
        indication =
            indication,
        modifier =
            Modifier
                .clip(
                    shape
                )
                .background(
                    color =
                        touchColor.copy(
                            alpha =
                                pressedAlpha
                        ),
                    shape =
                        shape
                )
    )
}

/**
 * Shared visible launcher interaction.
 *
 * This is the single source of truth for tap feedback across the launcher.
 * It intentionally matches the interaction used by Favorites on Home:
 *
 * - no scale/shrink animation
 * - the whole bounded surface softly illuminates while pressed
 * - a subtle bounded ripple is drawn over that surface
 * - press/release timings stay short enough for 120 Hz interaction
 *
 * Components may provide their own shape, but not their own ripple behavior.
 */
@Composable
fun Modifier.launcherClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    val visual =
        rememberLauncherPressVisual(
            shape =
                shape
        )

    return this
        .then(
            visual.modifier
        )
        .clickable(
            interactionSource =
                visual.interactionSource,
            indication =
                visual.indication,
            enabled =
                enabled,
            onClickLabel =
                onClickLabel,
            role =
                role,
            onClick =
                onClick
        )
}


/**
 * Same visual as [launcherClickable], but keeps the control composed for a few
 * frames after finger-up before running an action that may replace/dismiss the
 * current UI. This prevents navigation buttons from visually losing their
 * ripple simply because their screen leaves composition immediately.
 *
 * App tiles do not use this by default, so app launch responsiveness and the
 * existing Home interaction path remain unchanged.
 */
@Composable
fun Modifier.launcherTransitionClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    onClickLabel: String? = null,
    role: Role? = null,
    feedbackDelayMillis: Long =
        LAUNCHER_TRANSITION_FEEDBACK_DELAY_MILLIS,
    onClick: () -> Unit
): Modifier {
    val currentOnClick by
        rememberUpdatedState(
            onClick
        )

    val coroutineScope =
        rememberCoroutineScope()

    val clickPending =
        remember {
            AtomicBoolean(
                false
            )
        }

    return this.launcherClickable(
        enabled =
            enabled,
        shape =
            shape,
        onClickLabel =
            onClickLabel,
        role =
            role,
        onClick = {
            if (
                clickPending.compareAndSet(
                    false,
                    true
                )
            ) {
                coroutineScope.launch {
                    try {
                        delay(
                            feedbackDelayMillis
                        )

                        currentOnClick()
                    } finally {
                        clickPending.set(
                            false
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.launcherCombinedClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val visual =
        rememberLauncherPressVisual(
            shape =
                shape
        )

    return this
        .then(
            visual.modifier
        )
        .combinedClickable(
            interactionSource =
                visual.interactionSource,
            indication =
                visual.indication,
            enabled =
                enabled,
            onClickLabel =
                onClickLabel,
            role =
                role,
            onLongClickLabel =
                onLongClickLabel,
            onLongClick =
                onLongClick,
            onDoubleClick =
                null,
            onClick =
                onClick
        )
}


/**
 * Transition-safe variant for controls that support both tap and long press.
 *
 * Long press remains immediate. Only the short-tap action is deferred so the
 * same Home-Favorites ripple remains visible before navigation changes UI.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.launcherTransitionCombinedClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    feedbackDelayMillis: Long =
        LAUNCHER_TRANSITION_FEEDBACK_DELAY_MILLIS,
    onClick: () -> Unit
): Modifier {
    val currentOnClick by
        rememberUpdatedState(
            onClick
        )

    val coroutineScope =
        rememberCoroutineScope()

    val clickPending =
        remember {
            AtomicBoolean(
                false
            )
        }

    return this.launcherCombinedClickable(
        enabled =
            enabled,
        shape =
            shape,
        onClickLabel =
            onClickLabel,
        role =
            role,
        onLongClickLabel =
            onLongClickLabel,
        onLongClick =
            onLongClick,
        onClick = {
            if (
                clickPending.compareAndSet(
                    false,
                    true
                )
            ) {
                coroutineScope.launch {
                    try {
                        delay(
                            feedbackDelayMillis
                        )

                        currentOnClick()
                    } finally {
                        clickPending.set(
                            false
                        )
                    }
                }
            }
        }
    )
}

/**
 * App tiles use exactly the same press visual as every other visible launcher
 * control. The separate name is kept so app-oriented call sites remain clear.
 */
@Composable
fun Modifier.launcherAppClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    return this.launcherClickable(
        enabled =
            enabled,
        shape =
            shape,
        onClickLabel =
            onClickLabel,
        role =
            role,
        onClick =
            onClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.launcherAppCombinedClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    return this.launcherCombinedClickable(
        enabled =
            enabled,
        shape =
            shape,
        onClickLabel =
            onClickLabel,
        role =
            role,
        onLongClickLabel =
            onLongClickLabel,
        onLongClick =
            onLongClick,
        onClick =
            onClick
    )
}

/**
 * Invisible gesture region used only for non-visual surfaces such as the empty
 * Home background. It deliberately has no indication because there is no
 * visible control to illuminate.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.launcherGestureCombinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    return combinedClickable(
        interactionSource =
            interactionSource,
        indication =
            null,
        enabled =
            enabled,
        onClickLabel =
            onClickLabel,
        role =
            role,
        onLongClickLabel =
            onLongClickLabel,
        onLongClick =
            onLongClick,
        onDoubleClick =
            null,
        onClick =
            onClick
    )
}

private const val LAUNCHER_TRANSITION_FEEDBACK_DELAY_MILLIS = 72L
