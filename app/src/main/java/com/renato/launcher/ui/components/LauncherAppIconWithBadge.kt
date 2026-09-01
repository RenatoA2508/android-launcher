package com.renato.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LauncherAppIconWithBadge(
    bitmap: ImageBitmap,
    contentDescription: String,
    iconSize: Dp,
    notificationCount: Int,
    modifier: Modifier =
        Modifier
) {
    val compactBadge =
        iconSize <= 32.dp

    val badgeMinSize =
        if (compactBadge) {
            16.dp
        } else {
            18.dp
        }

    val horizontalOffset =
        if (compactBadge) {
            5.dp
        } else {
            6.dp
        }

    val verticalOffset =
        if (compactBadge) {
            (-4).dp
        } else {
            (-5).dp
        }

    Box(
        modifier =
            modifier.size(
                iconSize
            )
    ) {
        Image(
            bitmap =
                bitmap,
            contentDescription =
                contentDescription,
            modifier =
                Modifier.fillMaxSize()
        )

        if (notificationCount > 0) {
            Surface(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .offset(
                            x =
                                horizontalOffset,
                            y =
                                verticalOffset
                        )
                        .defaultMinSize(
                            minWidth =
                                badgeMinSize,
                            minHeight =
                                badgeMinSize
                        ),
                shape =
                    CircleShape,
                color =
                    NotificationBadgeColor,
                contentColor =
                    Color.White,
                shadowElevation =
                    1.dp
            ) {
                Box(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                4.dp,
                            vertical =
                                1.dp
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            badgeLabel(
                                notificationCount
                            ),
                        fontSize =
                            if (compactBadge) {
                                9.sp
                            } else {
                                10.sp
                            },
                        lineHeight =
                            10.sp,
                        fontWeight =
                            FontWeight.Bold,
                        textAlign =
                            TextAlign.Center,
                        maxLines =
                            1
                    )
                }
            }
        }
    }
}

private fun badgeLabel(
    count: Int
): String {
    return if (count > 99) {
        "99+"
    } else {
        count.toString()
    }
}

private val NotificationBadgeColor =
    Color(
        0xFFC74646
    )
