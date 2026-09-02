package com.renato.launcher.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.ui.interactions.launcherTransitionClickable

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
                onClick =
                    onBack
            )

            Spacer(
                modifier =
                    Modifier.width(
                        2.dp
                    )
            )

            OutlinedTextField(
                value =
                    query,
                onValueChange =
                    onQueryChange,
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .focusRequester(
                            focusRequester
                        )
                        .then(
                            if (
                                fieldTestTag != null
                            ) {
                                Modifier.testTag(
                                    fieldTestTag
                                )
                            } else {
                                Modifier
                            }
                        ),
                placeholder = {
                    Text(
                        text =
                            "Buscar aplicaciones"
                    )
                },
                trailingIcon = {
                    if (
                        query.isNotEmpty()
                    ) {
                        LauncherTextActionButton(
                            text =
                                "×",
                            fontSize =
                                22.sp,
                            deferActionForRipple =
                                false,
                            onClick =
                                onClear
                        )
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
                singleLine =
                    true,
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
    val shape =
        RoundedCornerShape(
            28.dp
        )

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    shape
                )
                .launcherTransitionClickable(
                    shape =
                        shape,
                    onClickLabel =
                        "Buscar aplicaciones",
                    onClick =
                        onClick
                ),
        shape =
            shape,
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
            fontSize =
                16.sp,
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
    LauncherTextActionButton(
        text =
            "‹",
        fontSize =
            32.sp,
        onClick =
            onClick
    )
}
