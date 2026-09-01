package com.renato.launcher.ui.interactions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Standard launcher interaction for generic controls.
 *
 * Material buttons already provide their own indication; this helper is used
 * by custom clickable surfaces that should follow the same interaction model.
 */
@Composable
fun Modifier.launcherClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val indication =
        LocalIndication.current

    return clickable(
        interactionSource =
            interactionSource,
        indication =
            indication,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.launcherCombinedClickable(
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

    val indication =
        LocalIndication.current

    return combinedClickable(
        interactionSource =
            interactionSource,
        indication =
            indication,
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
 * Launcher app-tile interaction.
 *
 * Instead of shrinking the app or drawing a small circular ripple, the whole
 * temporary tile is softly illuminated while the finger is down. A subtle
 * bounded Material ripple is still drawn over the pressed surface so taps
 * retain immediate touch feedback.
 *
 * Nothing is visible while the tile is idle, so the Home grid remains visually
 * free. The shape can be adjusted per screen: Home uses a softer rounded
 * rectangle, while Search can use a slightly squarer tile.
 */
@Composable
fun Modifier.launcherAppClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
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
                "launcherAppPressedAlpha"
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

    return this
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
        .clickable(
            interactionSource =
                interactionSource,
            indication =
                indication,
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
                "launcherAppLongPressedAlpha"
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

    return this
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
        .combinedClickable(
            interactionSource =
                interactionSource,
            indication =
                indication,
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

