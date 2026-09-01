package com.renato.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.ui.icons.rememberLauncherAppIcon

/**
 * Shared launcher context menu used by Home and Search.
 *
 * Search can hide "Quitar de Inicio" for apps that are not currently
 * favorites, while preserving the same visual language everywhere.
 */
@Composable
fun AppContextMenu(
    expanded: Boolean,
    app: InstalledApp,
    showRemoveFromHome: Boolean,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onUninstallApp: () -> Unit
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    val menuContainerColor =
        if (
            isSystemInDarkTheme()
        ) {
            Color(
                0xFF252527
            )
        } else {
            Color(
                0xFFF5F5F7
            )
        }

    DropdownMenu(
        expanded =
            expanded,
        onDismissRequest =
            onDismiss,
        modifier =
            Modifier.widthIn(
                min =
                    260.dp,
                max =
                    300.dp
            ),
        shape =
            RoundedCornerShape(
                26.dp
            ),
        containerColor =
            menuContainerColor,
        tonalElevation =
            0.dp,
        shadowElevation =
            12.dp
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            18.dp,
                        vertical =
                            14.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Image(
                bitmap =
                    iconBitmap,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(
                        40.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Text(
                text =
                    app.label,
                modifier =
                    Modifier.weight(
                        1f
                    ),
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis,
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }

        MenuDivider()

        DropdownMenuItem(
            modifier =
                Modifier.heightIn(
                    min =
                        52.dp
                ),
            text = {
                Text(
                    text =
                        "Información de la app",
                    fontSize =
                        15.sp
                )
            },
            contentPadding =
                PaddingValues(
                    horizontal =
                        18.dp
                ),
            onClick =
                onAppInfo
        )

        if (
            showRemoveFromHome
        ) {
            DropdownMenuItem(
                modifier =
                    Modifier.heightIn(
                        min =
                            52.dp
                    ),
                text = {
                    Text(
                        text =
                            "Quitar de Inicio",
                        fontSize =
                            15.sp
                    )
                },
                contentPadding =
                    PaddingValues(
                        horizontal =
                            18.dp
                    ),
                onClick =
                    onRemoveFavorite
            )
        }

        MenuDivider()

        DropdownMenuItem(
            modifier =
                Modifier.heightIn(
                    min =
                        52.dp
                ),
            text = {
                Text(
                    text =
                        "Desinstalar",
                    fontSize =
                        15.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            },
            contentPadding =
                PaddingValues(
                    horizontal =
                        18.dp
                ),
            onClick =
                onUninstallApp
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier =
            Modifier.padding(
                horizontal =
                    12.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .outlineVariant
                .copy(
                    alpha =
                        0.55f
                )
    )
}
