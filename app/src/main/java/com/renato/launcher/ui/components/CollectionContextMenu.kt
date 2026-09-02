package com.renato.launcher.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Context menu for a launcher collection.
 *
 * It deliberately mirrors AppContextMenu so collection interactions feel like
 * the same launcher feature rather than a separate UI system.
 */
@Composable
fun CollectionContextMenu(
    expanded: Boolean,
    collectionName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    240.dp,
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
        Text(
            text =
                collectionName,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            18.dp,
                        vertical =
                            16.dp
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

        MenuDivider()

        LauncherMenuItem(
            onClick =
                onEdit
        ) {
            Text(
                text =
                    "Editar colección",
                fontSize =
                    15.sp
            )
        }

        MenuDivider()

        LauncherMenuItem(
            onClick =
                onDelete
        ) {
            Text(
                text =
                    "Eliminar colección",
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
