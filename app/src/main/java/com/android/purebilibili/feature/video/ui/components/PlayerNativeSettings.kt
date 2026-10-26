package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.ui.components.appActionClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.LocalAppThemeConfig


/** 只在同窗玻璃设置容器实际接管后移除内容的原生实色底板。 */
internal val LocalPlayerGlassSettingsSurface = compositionLocalOf { false }

internal fun playerNativeMenuConfig(config: com.android.purebilibili.core.ui.AppThemeConfig) =
    config.copy(nativeMiuixPopupsEnabled = false, liquidGlassEnabled = false)
/** 播放器菜单保留原生面板和列表，不随视频按钮切换玻璃菜单或独立 Miuix 菜单。 */
@Composable
internal fun PlayerNativeMenuTheme(content: @Composable () -> Unit) {
    val config = LocalAppThemeConfig.current
    CompositionLocalProvider(
        LocalAppThemeConfig provides playerNativeMenuConfig(config),
        LocalAppUiStyle provides AppUiStyle.MATERIAL3,
        content = content,
    )
}

@Composable
internal fun PlayerNativeSettingsHost(
    onDismiss: () -> Unit,
    bottomPaddingDp: Int = 0,
    isFullscreen: Boolean = false,
    playerBounds: androidx.compose.ui.unit.IntRect? = null,
    content: @Composable () -> Unit,
) {
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    // 播放器提供避让宿主时，横屏更多操作与选集共用侧面板；其他入口保留原有弹窗。
    if (isFullscreen && windowSize.width > windowSize.height && LocalPlayerPanelViewport.current != null) {
        PlayerNativeMenuTheme {
            PlayerDetailPanel("更多操作", isFullscreen, playerBounds, onDismiss,
                modifier = Modifier.testTag("player_settings_panel"), maxSideWidth = 320.dp) { requestDismiss ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("更多操作", fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    PanelAction(Icons.Outlined.Close, "关闭", requestDismiss)
                }
                content()
            }
        }
        return
    }
    val config = LocalConfiguration.current
    val policy = resolveDanmakuSettingsPanelLayoutPolicy(
        isFullscreen = false,
        screenWidthDp = config.screenWidthDp,
        screenHeightDp = config.screenHeightDp,
    ).let { layout ->
        if (config.screenWidthDp > config.screenHeightDp) layout.copy(maxWidthDp = minOf(layout.maxWidthDp, 320))
        else layout
    }
    PlayerNativeMenuTheme {
        DanmakuSettingsPanelHost(false, policy, onDismiss) {
            Surface(
                modifier = Modifier.testTag("player_settings_panel")
                    .widthIn(max = policy.maxWidthDp.dp)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .padding(bottom = bottomPaddingDp.dp)
                    .heightIn(max = policy.maxHeightDp.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            ) { content() }
        }
    }
}

@Composable
internal fun PlayerSettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    checked: Boolean? = null,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (checked != null) Modifier.toggleable(
                    value = checked,
                    enabled = onClick != null,
                    role = Role.Switch,
                    onValueChange = { onClick?.invoke() },
                ) else if (onClick != null) Modifier.appActionClickable(onClick = onClick) else Modifier
            )
            .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null || icon != null) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (leadingContent != null) leadingContent() else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) Text(
                subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (checked != null) {
            Spacer(Modifier.width(12.dp))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Switch(checked = checked, onCheckedChange = null)
            }
        }
    }
}

@Composable
internal fun PlayerSettingsPopupRow(
    title: String,
    current: String,
    options: List<String>,
    enabledOptions: List<Boolean> = List(options.size) { true },
    icon: ImageVector? = null,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PlayerSettingsRow(title, current, icon = icon) { expanded = true }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { index, label ->
                com.android.purebilibili.core.ui.components.AppDropdownMenuItem(
                    text = { Text(label, fontSize = 14.sp) },
                    enabled = enabledOptions.getOrElse(index) { true },
                    onClick = {
                        expanded = false
                        if (label != current) onSelected(index)
                    },
                    trailingIcon = if (label == current) ({ Icon(Icons.Outlined.Done, null) }) else null,
                )
            }
        }
    }
}

@Composable
internal fun PlayerSettingsSelectionRow(
    title: String,
    selected: Boolean,
    enabled: Boolean = true,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)) },
        supportingContent = if (subtitle != null) ({ Text(subtitle, fontSize = 12.sp) }) else null,
        trailingContent = { if (selected) Icon(Icons.Outlined.Done, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth().appActionClickable(enabled = enabled) { if (!selected) onClick() },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
