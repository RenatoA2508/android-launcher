package com.renato.launcher.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Process-scoped FIFO queue for short durable launcher mutations.
 *
 * UI state is updated optimistically on the main thread, while Room writes are
 * submitted here so they are not cancelled just because MainActivity is
 * recreated. A single consumer also preserves user-action order; an older
 * reorder cannot finish after a newer reorder and overwrite it.
 *
 * This does not attempt to survive actual process termination. Room
 * transactions remain atomic, so an abrupt process death leaves either the old
 * committed state or the new committed state, never a partially written one.
 */
object LauncherMutationQueue {

    private const val TAG =
        "LauncherMutationQueue"

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        )

    private val mutations =
        Channel<suspend () -> Unit>(
            capacity =
                Channel.UNLIMITED
        )

    init {
        scope.launch {
            for (
                mutation in
                mutations
            ) {
                try {
                    mutation()
                } catch (
                    exception: Exception
                ) {
                    Log.e(
                        TAG,
                        "Launcher mutation failed.",
                        exception
                    )
                }
            }
        }
    }

    fun submit(
        mutation: suspend () -> Unit
    ) {
        val result =
            mutations.trySend(
                mutation
            )

        check(
            result.isSuccess
        ) {
            "Launcher mutation queue is unavailable."
        }
    }
}
