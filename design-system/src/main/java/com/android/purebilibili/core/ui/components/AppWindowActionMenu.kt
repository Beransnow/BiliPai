package com.android.purebilibili.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu

/**
 * An action exposed from a page-level overflow menu.
 *
 * [children] are rendered by Miuix as a cascading window popup, which keeps secondary choices
 * (for example a sort order) in the same interaction model as settings dropdowns.
 */
@Immutable
data class AppWindowAction(
    val label: String,
    val onClick: (() -> Unit)? = null,
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val summary: String? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val children: List<AppWindowAction> = emptyList(),
)

/**
 * Miuix window-level action menu used by page-level overflow buttons.
 *
 * Groups preserve the visual separation between related actions while sharing the same popup
 * implementation as Miuix settings choices.
 */
@Composable
fun AppWindowActionMenu(
    groups: List<List<AppWindowAction>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val entries = groups
        .filter { it.isNotEmpty() }
        .map { actions -> DropdownEntry(items = actions.map(AppWindowAction::toDropdownItem)) }

    WindowIconDropdownMenu(
        entries = entries,
        modifier = modifier,
        enabled = enabled,
        collapseOnSelection = true,
        onExpandedChange = onExpandedChange,
        minHeight = 48.dp,
        minWidth = 48.dp,
        content = content,
    )
}

private fun AppWindowAction.toDropdownItem(): DropdownItem = DropdownItem(
    text = label,
    enabled = enabled,
    selected = selected,
    onClick = onClick,
    icon = icon?.let { imageVector ->
        { modifier ->
            if (iconTint == null) {
                AppIcon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = modifier,
                )
            } else {
                AppIcon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = modifier,
                )
            }
        }
    },
    summary = summary,
    children = children.takeIf { it.isNotEmpty() }?.map(AppWindowAction::toDropdownItem),
)
