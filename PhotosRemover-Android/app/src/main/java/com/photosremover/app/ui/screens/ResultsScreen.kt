package com.photosremover.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.photosremover.app.data.model.DuplicateSet
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.StorageBreakdown
import com.photosremover.app.ui.components.BottomActionBar
import com.photosremover.app.ui.components.SetCard
import com.photosremover.app.ui.components.StorageDonutChart
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.BrandPrimary
import com.photosremover.app.ui.theme.DangerRed
import com.photosremover.app.ui.theme.YellowBackground
import com.photosremover.app.ui.theme.YellowSubText
import com.photosremover.app.ui.theme.YellowTextDark
import com.photosremover.app.ui.viewmodel.ResultsTab

enum class SetSortOrder(val title: String) {
    SIZE_DESC("Dung lượng lớn nhất"),
    SIZE_ASC("Dung lượng nhỏ nhất"),
    COUNT_DESC("Nhiều tệp nhất"),
    ORIGINAL("Thứ tự quét gốc")
}

@Composable
fun ResultsScreen(
    exactSets: List<DuplicateSet>,
    similarSets: List<DuplicateSet>,
    exactVideoSets: List<DuplicateSet>,
    similarVideoSets: List<DuplicateSet>,
    selectedTab: ResultsTab,
    storageBreakdown: StorageBreakdown? = null,
    onTabSelected: (ResultsTab) -> Unit,
    onPhotoClick: (PhotoItem, DuplicateSet) -> Unit,
    onTogglePhotoSelect: (String, Long) -> Unit,
    onToggleSetSelect: (String) -> Unit,
    onSelectAllExceptBest: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onTogglePhotoExclude: ((Long) -> Unit)? = null,
    onOpenLargeMediaCleaner: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allVideoSets = exactVideoSets + similarVideoSets
    val currentSets = when (selectedTab) {
        ResultsTab.EXACT -> exactSets
        ResultsTab.SIMILAR -> similarSets
        ResultsTab.VIDEOS -> allVideoSets
    }

    var sortOrder by remember { mutableStateOf(SetSortOrder.SIZE_DESC) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    val sortedSets = remember(currentSets, sortOrder) {
        when (sortOrder) {
            SetSortOrder.SIZE_DESC -> currentSets.sortedByDescending { it.totalSize }
            SetSortOrder.SIZE_ASC -> currentSets.sortedBy { it.totalSize }
            SetSortOrder.COUNT_DESC -> currentSets.sortedByDescending { if (it.isVideo) it.videos.size else it.photos.size }
            SetSortOrder.ORIGINAL -> currentSets
        }
    }

    val totalSelectedCount = currentSets.sumOf { it.selectedCount }
    val totalSelectedSize = currentSets.sumOf { it.selectedSize }
    val totalCategorySize = remember(currentSets) {
        PhotoItem.formatByteSize(currentSets.sumOf { it.totalSize })
    }

    var showDeleteAllWarning by remember { mutableStateOf(false) }
    val hasAnySetWithAllSelected = currentSets.any { it.isAllSelected }

    val tabIndex = when (selectedTab) {
        ResultsTab.EXACT -> 0
        ResultsTab.SIMILAR -> 1
        ResultsTab.VIDEOS -> 2
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(YellowBackground)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = YellowTextDark)
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black)
                        .padding(1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp)).background(YellowBackground))
                }
                Text(
                    text = "Remo Duplicate Photos Remover",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = YellowTextDark,
                    maxLines = 1
                )
            }
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(34.dp)) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = YellowTextDark)
            }
        }

        // 3-Tab Navigation: Exact | Similar | Videos
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.White,
            contentColor = BrandPrimary,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = BrandAccent,
                    height = 3.dp
                )
            }
        ) {
            // Tab 0: Exact
            Tab(
                selected = selectedTab == ResultsTab.EXACT,
                onClick = { onTabSelected(ResultsTab.EXACT) },
                text = {
                    Text(
                        text = "Exact (${exactSets.size})",
                        fontWeight = if (selectedTab == ResultsTab.EXACT) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (selectedTab == ResultsTab.EXACT) Color(0xFF1E293B) else Color(0xFF64748B)
                    )
                }
            )
            // Tab 1: Similar
            Tab(
                selected = selectedTab == ResultsTab.SIMILAR,
                onClick = { onTabSelected(ResultsTab.SIMILAR) },
                text = {
                    Text(
                        text = "Similar (${similarSets.size})",
                        fontWeight = if (selectedTab == ResultsTab.SIMILAR) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (selectedTab == ResultsTab.SIMILAR) Color(0xFF1E293B) else Color(0xFF64748B)
                    )
                }
            )
            // Tab 2: Videos
            Tab(
                selected = selectedTab == ResultsTab.VIDEOS,
                onClick = { onTabSelected(ResultsTab.VIDEOS) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (selectedTab == ResultsTab.VIDEOS) Color(0xFF1E293B) else Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Videos (${allVideoSets.size})",
                            fontWeight = if (selectedTab == ResultsTab.VIDEOS) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (selectedTab == ResultsTab.VIDEOS) Color(0xFF1E293B) else Color(0xFF64748B)
                        )
                    }
                }
            )
        }

        // Sort and Quick Action Toolbar Strip
        if (currentSets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Summary Chip / Count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentSets.size} nhóm",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = " • $totalCategorySize",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Right: Sort Dropdown & Clean Large Files button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button to open Large Media Cleaner
                    if (onOpenLargeMediaCleaner != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .clickable { onOpenLargeMediaCleaner() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📁 Dọn tệp lớn",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Sort Dropdown Button
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .clickable { sortDropdownExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sắp xếp",
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (sortOrder) {
                                    SetSortOrder.SIZE_DESC -> "Lớn nhất ▾"
                                    SetSortOrder.SIZE_ASC -> "Nhỏ nhất ▾"
                                    SetSortOrder.COUNT_DESC -> "Nhiều tệp ▾"
                                    SetSortOrder.ORIGINAL -> "Thứ tự gốc ▾"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }

                        DropdownMenu(
                            expanded = sortDropdownExpanded,
                            onDismissRequest = { sortDropdownExpanded = false }
                        ) {
                            SetSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (sortOrder == order) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = BrandAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(16.dp))
                                            }
                                            Text(
                                                text = order.title,
                                                fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        sortOrder = order
                                        sortDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sets List or Empty State
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (currentSets.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (storageBreakdown != null) {
                        item(key = "storage_breakdown_donut_empty") {
                            StorageDonutChart(
                                breakdown = storageBreakdown,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == ResultsTab.VIDEOS) Icons.Default.PlayCircle else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BrandAccent,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (selectedTab) {
                                    ResultsTab.EXACT -> "No Exact Duplicates Found!"
                                    ResultsTab.SIMILAR -> "No Similar Photos Found!"
                                    ResultsTab.VIDEOS -> "No Duplicate Videos Found!"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your library is clean in this category.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (storageBreakdown != null) {
                        item(key = "storage_breakdown_donut") {
                            StorageDonutChart(
                                breakdown = storageBreakdown,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(sortedSets, key = { it.id }) { set ->
                        SetCard(
                            set = set,
                            onPhotoClick = { photo -> onPhotoClick(photo, set) },
                            onTogglePhotoSelect = { photoId -> onTogglePhotoSelect(set.id, photoId) },
                            onToggleSetSelect = { onToggleSetSelect(set.id) },
                            onSelectAllExceptBest = { onSelectAllExceptBest(set.id) },
                            onTogglePhotoExclude = onTogglePhotoExclude
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // Sticky Bottom Action Bar
        BottomActionBar(
            selectedCount = totalSelectedCount,
            selectedSizeBytes = totalSelectedSize,
            onDeleteClick = {
                if (hasAnySetWithAllSelected) showDeleteAllWarning = true else onDeleteClick()
            }
        )

        if (showDeleteAllWarning) {
            AlertDialog(
                onDismissRequest = { showDeleteAllWarning = false },
                title = {
                    Text(
                        text = if (selectedTab == ResultsTab.VIDEOS) "Xóa toàn bộ video trong nhóm" else "Cảnh báo: Xóa toàn bộ ảnh trong bộ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                text = {
                    Text(
                        text = if (selectedTab == ResultsTab.VIDEOS)
                            "Bạn đã chọn xóa TOÀN BỘ video trong một hoặc nhiều nhóm. Bạn có chắc chắn muốn xóa toàn bộ?"
                        else
                            "Bạn đã chọn xóa TOÀN BỘ ảnh trong một hoặc nhiều nhóm trùng lặp. Bạn có chắc chắn muốn xóa toàn bộ?",
                        color = Color(0xFF475569),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showDeleteAllWarning = false; onDeleteClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Xóa toàn bộ", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllWarning = false }) {
                        Text("Xem lại", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}
