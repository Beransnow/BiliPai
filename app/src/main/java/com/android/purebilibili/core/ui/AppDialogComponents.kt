package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/** Style-neutral dialog entry point backed by the existing adaptive dialog renderer. */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    presentationProgress: Float = 1f,
    properties: DialogProperties = DialogProperties(),
) = AdaptiveAlertDialog(
    onDismissRequest = onDismissRequest,
    title = title,
    text = text,
    confirmButton = confirmButton,
    dismissButton = dismissButton,
    presentationProgress = presentationProgress,
    properties = properties,
)

/** Style-neutral action slot for [AppAlertDialog]. */
@Composable
fun AppDialogAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = AdaptiveDialogAction(onClick = onClick, modifier = modifier, content = content)
