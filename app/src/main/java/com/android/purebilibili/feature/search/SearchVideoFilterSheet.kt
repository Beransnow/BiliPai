package com.android.purebilibili.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.data.repository.SearchDuration
import com.android.purebilibili.data.repository.SearchOrder
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * PiliPlus-style video filter chrome:
 * horizontal order chips + filter icon that opens a bottom sheet
 * with publish time / duration / zone selectors.
 */
@Composable
fun SearchVideoFilterBar(
    currentOrder: SearchOrder,
    currentDurations: Set<SearchDuration>,
    currentVideoTid: Int,
    currentPubTimeType: SearchVideoPubTimeType,
    currentPubBegin: Long?,
    currentPubEnd: Long?,
    onOrderChange: (SearchOrder) -> Unit,
    onDurationSelect: (SearchDuration) -> Unit,
    onVideoTidChange: (Int) -> Unit,
    onPubTimeTypeChange: (SearchVideoPubTimeType) -> Unit,
    onCustomPubTimeRange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = rememberSearchNativeChrome()
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterActive = hasActiveSearchVideoFilters(
        durations = currentDurations,
        videoTid = currentVideoTid,
        pubTimeType = currentPubTimeType
    )
    val orderOptions = remember { resolveSearchVideoOrderOptions() }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            orderOptions.forEach { order ->
                val selected = order == currentOrder
                Text(
                    text = resolveSearchOrderChipLabel(order),
                    modifier = Modifier
                        .clickable { onOrderChange(order) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = if (selected) primary else outline,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        VerticalDivider(
            modifier = Modifier
                .height(18.dp)
                .padding(horizontal = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        when (chrome) {
            SearchNativeChrome.MIUIX -> MiuixIconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "筛选",
                    tint = if (filterActive) primary else primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            SearchNativeChrome.MATERIAL3 -> IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "筛选",
                    tint = primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showFilterSheet) {
        SearchVideoFilterSheetHost(
            chrome = chrome,
            currentDurations = currentDurations,
            currentVideoTid = currentVideoTid,
            currentPubTimeType = currentPubTimeType,
            currentPubBegin = currentPubBegin,
            currentPubEnd = currentPubEnd,
            onDismiss = { showFilterSheet = false },
            onDurationSelect = {
                onDurationSelect(it)
                showFilterSheet = false
            },
            onVideoTidChange = {
                onVideoTidChange(it)
                showFilterSheet = false
            },
            onPubTimeTypeChange = {
                onPubTimeTypeChange(it)
                showFilterSheet = false
            },
            onCustomPubTimeRange = { begin, end ->
                onCustomPubTimeRange(begin, end)
                showFilterSheet = false
            }
        )
    }
}

@Composable
private fun rememberSearchNativeChrome(): SearchNativeChrome {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    return remember(uiPreset, androidNativeVariant) {
        resolveSearchNativeChrome(uiPreset, androidNativeVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchVideoFilterSheetHost(
    chrome: SearchNativeChrome,
    currentDurations: Set<SearchDuration>,
    currentVideoTid: Int,
    currentPubTimeType: SearchVideoPubTimeType,
    currentPubBegin: Long?,
    currentPubEnd: Long?,
    onDismiss: () -> Unit,
    onDurationSelect: (SearchDuration) -> Unit,
    onVideoTidChange: (Int) -> Unit,
    onPubTimeTypeChange: (SearchVideoPubTimeType) -> Unit,
    onCustomPubTimeRange: (Long, Long) -> Unit
) {
    when (chrome) {
        SearchNativeChrome.MIUIX -> {
            OverlayBottomSheet(
                show = true,
                title = "筛选",
                onDismissRequest = onDismiss,
                content = {
                    SearchVideoFilterSheetContent(
                        currentDurations = currentDurations,
                        currentVideoTid = currentVideoTid,
                        currentPubTimeType = currentPubTimeType,
                        currentPubBegin = currentPubBegin,
                        currentPubEnd = currentPubEnd,
                        onDurationSelect = onDurationSelect,
                        onVideoTidChange = onVideoTidChange,
                        onPubTimeTypeChange = onPubTimeTypeChange,
                        onCustomPubTimeRange = onCustomPubTimeRange
                    )
                }
            )
        }
        SearchNativeChrome.MATERIAL3 -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState
            ) {
                SearchVideoFilterSheetContent(
                    currentDurations = currentDurations,
                    currentVideoTid = currentVideoTid,
                    currentPubTimeType = currentPubTimeType,
                    currentPubBegin = currentPubBegin,
                    currentPubEnd = currentPubEnd,
                    onDurationSelect = onDurationSelect,
                    onVideoTidChange = onVideoTidChange,
                    onPubTimeTypeChange = onPubTimeTypeChange,
                    onCustomPubTimeRange = onCustomPubTimeRange
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SearchVideoFilterSheetContent(
    currentDurations: Set<SearchDuration>,
    currentVideoTid: Int,
    currentPubTimeType: SearchVideoPubTimeType,
    currentPubBegin: Long?,
    currentPubEnd: Long?,
    onDurationSelect: (SearchDuration) -> Unit,
    onVideoTidChange: (Int) -> Unit,
    onPubTimeTypeChange: (SearchVideoPubTimeType) -> Unit,
    onCustomPubTimeRange: (Long, Long) -> Unit
) {
    val chrome = rememberSearchNativeChrome()
    val selectedDuration = resolveSelectedSearchDuration(currentDurations)
    val zoneOptions = remember { resolveSearchVideoZoneOptions() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }
    var showBeginPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val defaultBegin = currentPubBegin
        ?: TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    val defaultEnd = currentPubEnd
        ?: TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp)
    ) {
        SearchFilterSectionTitle(text = "发布时间", chrome = chrome)
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SearchVideoPubTimeType.ALL,
                SearchVideoPubTimeType.DAY,
                SearchVideoPubTimeType.WEEK,
                SearchVideoPubTimeType.HALF_YEAR
            ).forEach { type ->
                SearchFilterSelectableChip(
                    label = type.label,
                    selected = currentPubTimeType == type,
                    onClick = { onPubTimeTypeChange(type) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchFilterSelectableChip(
                label = dateFormat.format(Date(TimeUnit.SECONDS.toMillis(defaultBegin))),
                selected = currentPubTimeType == SearchVideoPubTimeType.CUSTOM,
                onClick = { showBeginPicker = true },
                modifier = Modifier.weight(1f),
                center = true
            )
            Text(
                text = "至",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SearchFilterSelectableChip(
                label = dateFormat.format(Date(TimeUnit.SECONDS.toMillis(defaultEnd))),
                selected = currentPubTimeType == SearchVideoPubTimeType.CUSTOM,
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f),
                center = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SearchFilterSectionTitle(text = "内容时长", chrome = chrome)
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            resolveSearchVideoDurationOptions().forEach { duration ->
                SearchFilterSelectableChip(
                    label = resolveSearchDurationChipLabel(duration),
                    selected = selectedDuration == duration,
                    onClick = { onDurationSelect(duration) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SearchFilterSectionTitle(text = "内容分区", chrome = chrome)
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            zoneOptions.forEach { zone ->
                SearchFilterSelectableChip(
                    label = zone.label,
                    selected = currentVideoTid == zone.tid,
                    onClick = { onVideoTidChange(zone.tid) }
                )
            }
        }
    }

    if (showBeginPicker) {
        SearchDatePickerDialog(
            initialEpochMillis = TimeUnit.SECONDS.toMillis(defaultBegin),
            onDismiss = { showBeginPicker = false },
            onConfirm = { millis ->
                val begin = TimeUnit.MILLISECONDS.toSeconds(millis)
                val end = currentPubEnd ?: defaultEnd
                onCustomPubTimeRange(begin, end)
                showBeginPicker = false
            }
        )
    }
    if (showEndPicker) {
        SearchDatePickerDialog(
            initialEpochMillis = TimeUnit.SECONDS.toMillis(defaultEnd),
            onDismiss = { showEndPicker = false },
            onConfirm = { millis ->
                val end = TimeUnit.MILLISECONDS.toSeconds(millis)
                val begin = currentPubBegin ?: defaultBegin
                onCustomPubTimeRange(begin, end)
                showEndPicker = false
            }
        )
    }
}

@Composable
private fun SearchFilterSectionTitle(
    text: String,
    chrome: SearchNativeChrome
) {
    when (chrome) {
        SearchNativeChrome.MIUIX -> MiuixText(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
        )
        SearchNativeChrome.MATERIAL3 -> Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
        )
    }
}

@Composable
private fun SearchFilterSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    center: Boolean = false
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                maxLines = 1,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
                modifier = if (center) Modifier.fillMaxWidth() else Modifier
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDatePickerDialog(
    initialEpochMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = state.selectedDateMillis ?: return@TextButton
                    onConfirm(selected)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = state)
    }
}
