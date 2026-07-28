package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

/** Style-neutral dialog entry point backed by the existing adaptive dialog renderer. */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    presentationProgress: Float = 1f,
    properties: DialogProperties = DialogProperties(),
) = IOSAlertDialog(
    onDismissRequest = onDismissRequest,
    icon = icon,
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
) = IOSDialogAction(onClick = onClick, modifier = modifier, content = content)

/** Style-neutral modal sheet entry point backed by the existing adaptive sheet renderer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    presentationProgress: Float = 1f,
    dragHandle: @Composable (() -> Unit)? = { AppSheetDragHandle() },
    windowInsets: WindowInsets = BottomSheetDefaults.modalWindowInsets,
    content: @Composable () -> Unit,
) = IOSModalBottomSheet(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    sheetState = sheetState,
    containerColor = containerColor,
    scrimColor = scrimColor,
    presentationProgress = presentationProgress,
    dragHandle = dragHandle,
    windowInsets = windowInsets,
    content = content,
)

/** Style-neutral drag handle slot for custom sheet layouts. */
@Composable
fun AppSheetDragHandle() = IOSDragHandle()
