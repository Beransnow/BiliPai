package com.android.purebilibili.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSCornerRadius
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.core.ui.rememberAppSettingsIcon
import com.android.purebilibili.core.ui.components.AppPreference
import com.android.purebilibili.core.ui.components.AppPreferenceDivider
import com.android.purebilibili.core.ui.components.AppPreferenceGroup
import com.android.purebilibili.core.ui.components.resolveAdaptiveListComponentVisualSpec
import com.android.purebilibili.core.ui.components.resolveAdaptiveSearchBarContainerColor
import com.android.purebilibili.core.ui.components.shouldUseNativeMiuixSearchBar
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.InputField

@Composable
internal fun SettingsSearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val colorScheme = MaterialTheme.colorScheme
    val visualSpec = resolveAdaptiveListComponentVisualSpec(
        uiPreset = uiPreset,
        androidNativeVariant = androidNativeVariant,
    )
    val placeholder = stringResource(R.string.settings_search_placeholder)
    val clearLabel = stringResource(R.string.common_clear)
    val containerColor = resolveAdaptiveSearchBarContainerColor(
        uiPreset = uiPreset,
        colorScheme = colorScheme,
        fallbackColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
        androidNativeVariant = androidNativeVariant,
        globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current,
    )
    val cornerRadius = if (uiPreset == UiPreset.MD3) {
        visualSpec.searchBarCornerRadiusDp.dp
    } else {
        iOSCornerRadius.Small * LocalCornerRadiusScale.current
    }
    val minHeight = visualSpec.searchBarHeightDp.dp
    val focusRequester = remember { FocusRequester() }
    val fieldModifier = Modifier
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .fillMaxWidth()
        .focusRequester(focusRequester)

    LaunchedEffect(focusRequester) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    if (shouldUseNativeMiuixSearchBar(uiPreset, androidNativeVariant)) {
        InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {},
            expanded = true,
            onExpandedChange = {},
            modifier = fieldModifier.defaultMinSize(minHeight = minHeight),
            label = placeholder,
        )
        return
    }

    if (uiPreset == UiPreset.MD3) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = fieldModifier.defaultMinSize(minHeight = minHeight),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = clearLabel,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else {
                null
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
            shape = RoundedCornerShape(cornerRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface,
                disabledTextColor = colorScheme.onSurface,
                cursorColor = colorScheme.primary,
                focusedContainerColor = AppSurfaceTokens.surfaceContainerHigh(),
                unfocusedContainerColor = AppSurfaceTokens.surfaceContainerHigh(),
                disabledContainerColor = AppSurfaceTokens.surfaceContainerHigh(),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
            ),
        )
        return
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = fieldModifier
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.MagnifyingGlass,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f),
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.XmarkCircle,
                            contentDescription = clearLabel,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
internal fun SettingsHomeSearchEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val colorScheme = MaterialTheme.colorScheme
    val visualSpec = resolveSettingsVisualSpec()
    val listVisualSpec = resolveAdaptiveListComponentVisualSpec(
        uiPreset = uiPreset,
        androidNativeVariant = androidNativeVariant,
    )
    val containerColor = resolveAdaptiveSearchBarContainerColor(
        uiPreset = uiPreset,
        colorScheme = colorScheme,
        fallbackColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
        androidNativeVariant = androidNativeVariant,
        globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current,
    )
    val placeholder = stringResource(R.string.settings_search_placeholder)
    val cornerRadius = if (uiPreset == UiPreset.MD3) {
        listVisualSpec.searchBarCornerRadiusDp.dp
    } else {
        iOSCornerRadius.Small * LocalCornerRadiusScale.current
    }
    val minHeight = listVisualSpec.searchBarHeightDp.dp

    Row(
        modifier = modifier
            .padding(
                horizontal = visualSpec.screenHorizontalPadding,
                vertical = visualSpec.searchBarVerticalPadding,
            )
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (uiPreset == UiPreset.MD3) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(
                imageVector = CupertinoIcons.Default.MagnifyingGlass,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SettingsSearchResultsSection(
    results: List<SettingsSearchResult>,
    onResultClick: (SettingsSearchResult) -> Unit,
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val visualSpec = resolveAdaptiveListComponentVisualSpec(
        uiPreset = uiPreset,
        androidNativeVariant = androidNativeVariant,
    )
    SettingsCategoryHeader(stringResource(R.string.settings_search_results_title))
    AppPreferenceGroup {
        if (results.isEmpty()) {
            AppPreference(
                icon = rememberAppSettingsIcon(),
                title = stringResource(R.string.settings_search_empty_title),
                subtitle = stringResource(R.string.settings_search_empty_subtitle),
                onClick = null,
                showChevron = false,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            results.forEachIndexed { index, result ->
                val visual = rememberSettingsEntryVisual(result.target)
                AppPreference(
                    icon = visual.icon,
                    iconPainter = visual.iconResId?.let { painterResource(id = it) },
                    title = result.title,
                    subtitle = result.subtitle,
                    value = result.section,
                    onClick = { onResultClick(result) },
                    iconTint = visual.iconTint,
                )
                if (index != results.lastIndex) {
                    AppPreferenceDivider(startIndent = visualSpec.dividerStartIndentDp.dp)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
