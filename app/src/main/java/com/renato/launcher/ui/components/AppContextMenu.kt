package com.renato.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    removeLabel: String = "Quitar de Inicio",
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onUninstallApp: () -> Unit
) {
    val iconBitmap =
        rememberLauncherAppIcon(
            app
        )

    LauncherDropdownMenu(
        expanded =
            expanded,
        onDismissRequest =
            onDismiss,
        minWidth =
            260.dp,
        maxWidth =
            300.dp
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

        LauncherMenuItem(
            onClick =
                onAppInfo
        ) {
            Text(
                text =
                    "Información de la app",
                fontSize =
                    15.sp
            )
        }

        if (
            showRemoveFromHome
        ) {
            LauncherMenuItem(
                onClick =
                    onRemoveFavorite
            ) {
                Text(
                    text =
                        removeLabel,
                    fontSize =
                        15.sp
                )
            }
        }

        MenuDivider()

        LauncherMenuItem(
            onClick =
                onUninstallApp
        ) {
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
        }
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
