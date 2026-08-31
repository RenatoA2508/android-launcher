package com.renato.launcher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag

@Composable
fun LauncherSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    fieldTestTag: String? = null
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha = 0.88f
                )
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = 8.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            SearchBackButton(
                onClick = onBack
            )

            Spacer(
                modifier =
                    Modifier.width(
                        2.dp
                    )
            )

            OutlinedTextField(
                value = query,
                onValueChange =
                    onQueryChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(
                            focusRequester
                        )
                        .then(
                            if (fieldTestTag != null) {
                                Modifier.testTag(
                                    fieldTestTag
                                )
                            } else {
                                Modifier
                            }
                        ),
                placeholder = {
                    Text(
                        "Buscar aplicaciones"
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(
                            onClick =
                                onClear
                        ) {
                            Text(
                                text = "×",
                                fontSize = 22.sp
                            )
                        }
                    }
                },
                keyboardOptions =
                    KeyboardOptions(
                        imeAction =
                            ImeAction.Go
                    ),
                keyboardActions =
                    KeyboardActions(
                        onGo = {
                            onSubmit()
                        }
                    ),
                singleLine = true,
                shape =
                    RoundedCornerShape(
                        28.dp
                    )
            )
        }
    }
}

@Composable
fun LauncherSearchLauncher(
    onClick: () -> Unit
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(
                interactionSource =
                    interactionSource,
                pressedScale =
                    0.98f
            )
            .clip(
                RoundedCornerShape(
                    28.dp
                )
            )
            .clickable(
                interactionSource =
                    interactionSource,
                indication = null,
                onClick =
                    onClick
            ),
        shape =
            RoundedCornerShape(
                28.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surfaceContainer
                .copy(
                    alpha = 0.84f
                )
    ) {
        Text(
            text =
                "Buscar aplicaciones",
            modifier =
                Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                ),
            fontSize = 16.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun SearchBackButton(
    onClick: () -> Unit
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    TextButton(
        onClick =
            onClick,
        interactionSource =
            interactionSource,
        modifier =
            Modifier.pressScale(
                interactionSource =
                    interactionSource
            )
    ) {
        Text(
            text = "‹",
            fontSize = 32.sp
        )
    }
}

@Composable
private fun Modifier.pressScale(
    interactionSource:
        MutableInteractionSource,
    pressedScale: Float = 0.965f
): Modifier {

    val isPressed by
        interactionSource
            .collectIsPressedAsState()

    val scale by
        animateFloatAsState(
            targetValue =
                if (isPressed) {
                    pressedScale
                } else {
                    1f
                },
            animationSpec =
                if (isPressed) {
                    tween(
                        durationMillis = 55,
                        easing =
                            FastOutSlowInEasing
                    )
                } else {
                    spring(
                        dampingRatio = 0.82f,
                        stiffness = 900f
                    )
                },
            label =
                "launcherSearchPressScale"
        )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
