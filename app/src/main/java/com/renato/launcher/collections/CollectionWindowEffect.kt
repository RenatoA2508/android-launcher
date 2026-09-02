package com.renato.launcher.collections

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun CollectionWindowEffect() {
    val context =
        LocalContext.current

    val activity =
        remember(context) {
            context.findActivity()
        }

    DisposableEffect(
        activity
    ) {
        val window =
            activity?.window

        if (
            window != null &&
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
        ) {
            window.addFlags(
                WindowManager
                    .LayoutParams
                    .FLAG_BLUR_BEHIND
            )

            val attributes =
                window.attributes

            attributes
                .setBlurBehindRadius(
                    32
                )

            window.attributes =
                attributes
        }

        onDispose {
            if (
                window != null &&
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
            ) {
                val attributes =
                    window.attributes

                attributes
                    .setBlurBehindRadius(
                        0
                    )

                window.attributes =
                    attributes

                window.clearFlags(
                    WindowManager
                        .LayoutParams
                        .FLAG_BLUR_BEHIND
                )
            }
        }
    }
}

private fun Context.findActivity():
    Activity? {

    var currentContext =
        this

    while (
        currentContext is
            ContextWrapper
    ) {
        if (
            currentContext is Activity
        ) {
            return currentContext
        }

        currentContext =
            currentContext.baseContext
    }

    return null
}
