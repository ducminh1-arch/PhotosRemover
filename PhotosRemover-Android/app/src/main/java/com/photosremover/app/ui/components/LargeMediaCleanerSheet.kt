package com.photosremover.app.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.VideoItem
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.DangerRed
import com.photosremover.app.ui.theme.TextPrimaryLight
import com.photosremover.app.ui.theme.TextSecondaryLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LargeMediaEntry(
    val id: Long,
    val uri: Uri,
    val sizeBytes: Long,
    val dateTakenMs: Long,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val isSelected: Boolean = false
) {
    val formattedSize: String get() = PhotoItem.formatByteSize(sizeBytes)
}

enum class LargeMediaFilter(val label: String) {
    ALL("Tất cả"),
    PHOTOS_ONLY("📸 Chỉ Ảnh"),
    VIDEOS_ONLY("🎬 Chỉ Video"),
    VIDEO_20MB("Video > 20 MB"),
    VIDEO_50MB("Video > 50 MB"),
    PHOTO_2MB("Ảnh > 2 MB"),
    PHOTO_5MB("Ảnh > 5 MB")
}

enum class LargeMediaSort(val title: String, val shortLabel: String) {
    SIZE_DESC("Dung lượng: Lớn nhất ➔ Nhỏ nhất", "Lớn nhất ▾"),
    SIZE_ASC("Dung lượng: Nhỏ nhất ➔ Lớn nhất", "Nhỏ nhất ▾"),
    DATE_DESC("Ngày: Mới nhất ➔ Cũ nhất", "Mới nhất ▾"),
    DATE_ASC("Ngày: Cũ nhất ➔ Mới nhất", "Cũ nhất ▾")
}

private object LargeVideoThumbnailCache {
    private val memoryCache = LruCache<Long, Bitmap>(100)

    fun get(id: Long): Bitmap? = memoryCache.get(id)
    fun put(id: Long, bitmap: Bitmap) {
        memoryCache.put(id, bitmap)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeMediaCleanerSheet(
    photos: List<PhotoItem>,
    videos: List<VideoItem>,
    onDeleteSelected: (photosToDelete: List<PhotoItem>, videosToDelete: List<VideoItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Convert to unified entries
    val mediaItems = remember(photos, videos) {
        val list = mutableListOf<LargeMediaEntry>()
        for (v in videos) {
            v.uri?.let {
                list.add(
                    LargeMediaEntry(
                        id = v.id,
                        uri = it,
                        sizeBytes = v.sizeBytes,
                        dateTakenMs = v.dateTakenMs,
                        isVideo = true,
                        durationMs = v.durationMs
                    )
                )
            }
        }
        for (p in photos) {
            p.uri?.let {
                list.add(
                    LargeMediaEntry(
                        id = p.id,
                        uri = it,
                        sizeBytes = p.sizeBytes,
                        dateTakenMs = p.dateTakenMs,
                        isVideo = false
                    )
                )
            }
        }
        list
    }

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var filterCategory by remember { mutableStateOf(LargeMediaFilter.ALL) }
    var sortOrder by remember { mutableStateOf(LargeMediaSort.SIZE_DESC) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var previewEntry by remember { mutableStateOf<LargeMediaEntry?>(null) }

    val filteredList = remember(mediaItems, filterCategory, sortOrder) {
        val filtered = when (filterCategory) {
            LargeMediaFilter.ALL -> mediaItems
            LargeMediaFilter.PHOTOS_ONLY -> mediaItems.filter { !it.isVideo }
            LargeMediaFilter.VIDEOS_ONLY -> mediaItems.filter { it.isVideo }
            LargeMediaFilter.VIDEO_20MB -> mediaItems.filter { it.isVideo && it.sizeBytes >= 20 * 1024 * 1024L }
            LargeMediaFilter.VIDEO_50MB -> mediaItems.filter { it.isVideo && it.sizeBytes >= 50 * 1024 * 1024L }
            LargeMediaFilter.PHOTO_2MB -> mediaItems.filter { !it.isVideo && it.sizeBytes >= 2 * 1024 * 1024L }
            LargeMediaFilter.PHOTO_5MB -> mediaItems.filter { !it.isVideo && it.sizeBytes >= 5 * 1024 * 1024L }
        }

        when (sortOrder) {
            LargeMediaSort.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            LargeMediaSort.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
            LargeMediaSort.DATE_DESC -> filtered.sortedByDescending { it.dateTakenMs }
            LargeMediaSort.DATE_ASC -> filtered.sortedBy { it.dateTakenMs }
        }
    }

    val selectedCount = selectedIds.size
    val selectedBytes = remember(selectedIds, mediaItems) {
        mediaItems.filter { it.id in selectedIds }.sumOf { it.sizeBytes }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextPrimaryLight
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Tệp dung lượng lớn",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        val totalSizeStr = PhotoItem.formatByteSize(filteredList.sumOf { it.sizeBytes })
                        val typeDescription = when (filterCategory) {
                            LargeMediaFilter.PHOTOS_ONLY -> "ảnh"
                            LargeMediaFilter.VIDEOS_ONLY -> "video"
                            else -> "tệp"
                        }
                        Text(
                            text = "${filteredList.size} $typeDescription • $totalSizeStr",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                }
            }

            // Horizontally Scrollable Filter Chips (Photos, Videos, Thresholds)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LargeMediaFilter.values().forEach { cat ->
                    FilterChip(
                        selected = filterCategory == cat,
                        onClick = { filterCategory = cat },
                        label = { Text(cat.label, fontSize = 11.5.sp) }
                    )
                }
            }

            // Quick Selection Actions & Sort Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quick Action Buttons
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            val ids = filteredList.filter { it.sizeBytes >= 50 * 1024 * 1024L }.map { it.id }.toSet()
                            selectedIds = selectedIds + ids
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Tệp > 50 MB", fontSize = 11.sp, color = BrandAccent)
                    }

                    TextButton(
                        onClick = {
                            val ids = filteredList.filter { it.sizeBytes >= 100 * 1024 * 1024L }.map { it.id }.toSet()
                            selectedIds = selectedIds + ids
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Tệp > 100 MB", fontSize = 11.sp, color = BrandAccent)
                    }

                    TextButton(
                        onClick = {
                            val allIds = filteredList.map { it.id }.toSet()
                            selectedIds = selectedIds + allIds
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Chọn tất cả", fontSize = 11.sp, color = BrandAccent)
                    }

                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Bỏ chọn", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }

                // Sort Dropdown Button
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable { sortDropdownExpanded = true }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sắp xếp",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = sortOrder.shortLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }

                    DropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        LargeMediaSort.values().forEach { order ->
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
                                            fontSize = 12.5.sp
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

            // List of media items
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có tệp nào phù hợp trong danh mục này",
                        color = TextSecondaryLight,
                        fontSize = 13.5.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val isSelected = item.id in selectedIds
                        LargeMediaItemRow(
                            item = item,
                            isSelected = isSelected,
                            onToggle = {
                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                            },
                            onThumbnailClick = {
                                if (item.isVideo) {
                                    playVideo(context, item.uri)
                                } else {
                                    previewEntry = item
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = Color(0xFFF1F5F9),
                            thickness = 0.8.dp
                        )
                    }
                }
            }

            // Sticky Bottom Delete Action Bar
            if (selectedCount > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$selectedCount tệp đã chọn",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Giải phóng ${PhotoItem.formatByteSize(selectedBytes)}",
                                color = Color(0xFFFFC800),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = { showConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Xóa (${PhotoItem.formatByteSize(selectedBytes)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Xóa $selectedCount tệp lớn?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Hệ thống sẽ xóa vĩnh viễn hoặc chuyển vào thùng rác $selectedCount video/ảnh đã chọn để giải phóng ${PhotoItem.formatByteSize(selectedBytes)}. Bạn có chắc chắn muốn xóa không?",
                    fontSize = 13.5.sp,
                    color = TextSecondaryLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        val photosToDelete = photos.filter { it.id in selectedIds }
                        val videosToDelete = videos.filter { it.id in selectedIds }
                        onDeleteSelected(photosToDelete, videosToDelete)
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Xóa tệp", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Hủy", color = TextSecondaryLight)
                }
            }
        )
    }

    // Photo Full Preview Dialog
    if (previewEntry != null) {
        val entry = previewEntry!!
        Dialog(
            onDismissRequest = { previewEntry = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { previewEntry = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = entry.formattedSize,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (entry.isVideo) "Video" else "Ảnh",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { previewEntry = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White
                            )
                        }
                    }

                    // Full image in center
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(entry.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Full Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom action in preview
                    val isSelectedInDialog = entry.id in selectedIds
                    Button(
                        onClick = {
                            selectedIds = if (isSelectedInDialog) selectedIds - entry.id else selectedIds + entry.id
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelectedInDialog) DangerRed else BrandAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = if (isSelectedInDialog) "Đã chọn xóa (Bấm để bỏ)" else "Chọn để xóa tệp này",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeMediaThumbnail(
    item: LargeMediaEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (item.isVideo) {
        var bitmap by remember(item.id) {
            mutableStateOf(LargeVideoThumbnailCache.get(item.id))
        }

        LaunchedEffect(item.id) {
            if (bitmap == null) {
                withContext(Dispatchers.IO) {
                    val loaded = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(
                                item.uri,
                                android.util.Size(200, 200),
                                null
                            )
                        } else {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(context, item.uri)
                            val frame = retriever.getFrameAtTime(1000000) ?: retriever.frameAtTime
                            retriever.release()
                            frame
                        }
                    } catch (_: Exception) {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(context, item.uri)
                            val frame = retriever.frameAtTime
                            retriever.release()
                            frame
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (loaded != null) {
                        LargeVideoThumbnailCache.put(item.id, loaded)
                        withContext(Dispatchers.Main) {
                            bitmap = loaded
                        }
                    }
                }
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Video Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier.background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    } else {
        // Photo thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo thumbnail",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
private fun LargeMediaItemRow(
    item: LargeMediaEntry,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onThumbnailClick: () -> Unit
) {
    val dateStr = remember(item.dateTakenMs) {
        if (item.dateTakenMs > 0) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(item.dateTakenMs))
        } else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with Duration / Play Icon (Clickable for preview/play)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE2E8F0))
                .clickable(onClick = onThumbnailClick)
        ) {
            LargeMediaThumbnail(
                item = item,
                modifier = Modifier.fillMaxSize()
            )

            if (item.isVideo) {
                // Play overlay in center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Duration badge
                if (item.durationMs > 0) {
                    val sec = item.durationMs / 1000
                    val durStr = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = durStr,
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Size Badge
                val isVeryLarge = item.sizeBytes >= 50 * 1024 * 1024L
                val isLarge = item.sizeBytes >= 10 * 1024 * 1024L

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isVeryLarge -> Color(0xFFFEE2E2)
                                isLarge -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF1F5F9)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.formattedSize,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isVeryLarge -> DangerRed
                            isLarge -> Color(0xFFD97706)
                            else -> Color(0xFF475569)
                        }
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (item.isVideo) "Video" else "Ảnh",
                    fontSize = 11.5.sp,
                    color = TextSecondaryLight
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = if (dateStr.isNotEmpty()) dateStr else "Tệp #${item.id}",
                fontSize = 12.sp,
                color = TextSecondaryLight
            )
        }

        // Selection Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) DangerRed else Color(0xFFF1F5F9))
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) DangerRed else Color(0xFFCBD5E1),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private fun playVideo(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
