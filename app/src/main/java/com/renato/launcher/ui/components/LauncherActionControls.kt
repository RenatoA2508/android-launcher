package com.renato.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.ui.interactions.launcherClickable
import com.renato.launcher.ui.interactions.launcherTransitionClickable

@Composable
fun LauncherTextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    deferActionForRipple: Boolean = true,
    contentColor: Color =
        MaterialTheme.colorScheme.primary,
    fontSize: androidx.compose.ui.unit.TextUnit =
        14.sp,
    fontWeight: FontWeight =
        FontWeight.Normal,
    shape: Shape =
        RoundedCornerShape(20.dp)
) {
    Box(
        modifier =
            modifier
                .then(
                    if (
                        deferActionForRipple
                    ) {
                        Modifier
                            .launcherTransitionClickable(
                                enabled =
                                    enabled,
                                shape =
                                    shape,
                                onClickLabel =
                                    text,
                                onClick =
                                    onClick
                            )
                    } else {
                        Modifier
                            .launcherClickable(
                                enabled =
                                    enabled,
                                shape =
                                    shape,
                                onClickLabel =
                                    text,
                                onClick =
                                    onClick
                            )
                    }
                )
                .padding(
                    horizontal =
                        14.dp,
                    vertical =
                        10.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                text,
            fontSize =
                fontSize,
            fontWeight =
                fontWeight,
            color =
                if (enabled) {
                    contentColor
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(
                            alpha =
                                0.38f
                        )
                }
        )
    }
}

@Composable
fun LauncherPrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    deferActionForRipple: Boolean = true,
    shape: Shape =
        RoundedCornerShape(20.dp)
) {
    val containerColor =
        if (enabled) {
            MaterialTheme
                .colorScheme
                .primary
        } else {
            MaterialTheme
                .colorScheme
                .onSurface
                .copy(
                    alpha =
                        0.12f
                )
        }

    val contentColor =
        if (enabled) {
            MaterialTheme
                .colorScheme
                .onPrimary
        } else {
            MaterialTheme
                .colorScheme
                .onSurface
                .copy(
                    alpha =
                        0.38f
                )
        }

    Box(
        modifier =
            modifier
                .background(
                    color =
                        containerColor,
                    shape =
                        shape
                )
                .then(
                    if (
                        deferActionForRipple
                    ) {
                        Modifier
                            .launcherTransitionClickable(
                                enabled =
                                    enabled,
                                shape =
                                    shape,
                                onClickLabel =
                                    text,
                                onClick =
                                    onClick
                            )
                    } else {
                        Modifier
                            .launcherClickable(
                                enabled =
                                    enabled,
                                shape =
                                    shape,
                                onClickLabel =
                                    text,
                                onClick =
                                    onClick
                            )
                    }
                )
                .padding(
                    horizontal =
                        18.dp,
                    vertical =
                        10.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                text,
            fontSize =
                14.sp,
            fontWeight =
                FontWeight.SemiBold,
            color =
                contentColor
        )
    }
}

@Composable
fun LauncherMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape =
        RoundedCornerShape(18.dp),
    contentPadding: PaddingValues =
        PaddingValues(
            horizontal =
                18.dp
        ),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        52.dp
                )
                .launcherTransitionClickable(
                    enabled =
                        enabled,
                    shape =
                        shape,
                    onClick =
                        onClick
                )
                .padding(
                    contentPadding
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        content =
            content
    )
}
