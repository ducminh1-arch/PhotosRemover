package com.photosremover.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.StorageBreakdown
import com.photosremover.app.domain.DeviceStorage
import com.photosremover.app.domain.StorageSegment
import com.photosremover.app.ui.theme.BrandPrimary
import com.photosremover.app.ui.theme.DangerRed
import com.photosremover.app.ui.theme.TextPrimaryLight
import com.photosremover.app.ui.theme.TextSecondaryLight
import com.photosremover.app.ui.theme.YellowTextDark

@Composable
fun StorageDonutChart(
    breakdown: StorageBreakdown,
    modifier: Modifier = Modifier
) {
    val segments = remember(breakdown) {
        DeviceStorage.calculateStorageArcs(
            totalDeviceBytes = breakdown.deviceTotalBytes,
            freeDeviceBytes = breakdown.deviceFreeBytes,
            photosDupBytes = breakdown.photosDuplicateBytes,
            videosDupBytes = breakdown.videosDuplicateBytes,
            photosUniqueBytes = breakdown.photosUniqueBytes,
            videosUniqueBytes = breakdown.videosUniqueBytes
        )
    }

    var showOtherDetails by remember { mutableStateOf(false) }

    // 600ms clockwise drawing animation on first appearance
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTriggered = true
    }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "storage_donut_animation"
    )

    val usedFormatted = DeviceStorage.formatBytesLocale(breakdown.deviceUsedBytes)
    val totalFormatted = DeviceStorage.formatBytesLocale(breakdown.deviceTotalBytes)
    val freeFormatted = DeviceStorage.formatBytesLocale(breakdown.deviceFreeBytes)

    val talkBackSummary = "Dung lượng thiết bị: Đã dùng $usedFormatted trên $totalFormatted. " +
        "Ảnh trùng lặp: ${DeviceStorage.formatBytesLocale(breakdown.photosDuplicateBytes)}, " +
        "Video trùng lặp: ${DeviceStorage.formatBytesLocale(breakdown.videosDuplicateBytes)}, " +
        "Ảnh duy nhất: ${DeviceStorage.formatBytesLocale(breakdown.photosUniqueBytes)}, " +
        "Video duy nhất: ${DeviceStorage.formatBytesLocale(breakdown.videosUniqueBytes)}, " +
        "Khác: ${DeviceStorage.formatBytesLocale(breakdown.otherBytes)}, " +
        "Còn trống: $freeFormatted"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            .padding(18.dp)
            .semantics { contentDescription = talkBackSummary },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dung lượng thiết bị",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
            if (breakdown.totalDuplicateBytes > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DangerRed.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Tiết kiệm ${PhotoItem.formatByteSize(breakdown.totalDuplicateBytes)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Donut Chart with Center Text
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val arcSize = Size(diameter, diameter)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                // Background track
                drawArc(
                    color = Color(0xFFF1F5F9),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // Draw segments with animation
                segments.forEach { segment ->
                    if (segment.sweepAngle > 0f) {
                        val animatedSweep = segment.sweepAngle * animationProgress
                        drawArc(
                            color = segment.color,
                            startAngle = segment.startAngle,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }
                }
            }

            // Center Text: "Đã dùng X GB / Y GB"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Đã dùng $usedFormatted",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "/ $totalFormatted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryLight
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6-item 2-column Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Ảnh trùng lặp (Đỏ) & Video trùng lặp (Cam)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(
                    label = "Ảnh trùng lặp",
                    bytes = breakdown.photosDuplicateBytes,
                    color = DeviceStorage.ColorPhotoDuplicate,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(
                    label = "Video trùng lặp",
                    bytes = breakdown.videosDuplicateBytes,
                    color = DeviceStorage.ColorVideoDuplicate,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Ảnh duy nhất (Xanh) & Video duy nhất (Tím)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(
                    label = "Ảnh duy nhất",
                    bytes = breakdown.photosUniqueBytes,
                    color = DeviceStorage.ColorPhotoUnique,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(
                    label = "Video duy nhất",
                    bytes = breakdown.videosUniqueBytes,
                    color = DeviceStorage.ColorVideoUnique,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Khác (Xám) & Còn trống (Xám nhạt)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(
                    label = "Khác",
                    bytes = breakdown.otherBytes,
                    color = DeviceStorage.ColorOther,
                    modifier = Modifier.weight(1f),
                    isClickable = true,
                    showInfoIcon = true,
                    onClick = { showOtherDetails = true }
                )
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(
                    label = "Còn trống",
                    bytes = breakdown.deviceFreeBytes,
                    color = DeviceStorage.ColorFree,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive hint pill to view "Khác" details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { showOtherDetails = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nhấn vào \"Khác\" để xem phân tích dữ liệu ${DeviceStorage.formatBytesLocale(breakdown.otherBytes)}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            }
        }
    }

    if (showOtherDetails) {
        OtherStorageDetailsSheet(
            breakdown = breakdown,
            onDismiss = { showOtherDetails = false }
        )
    }
}

@Composable
private fun LegendItem(
    label: String,
    bytes: Long,
    color: Color,
    modifier: Modifier = Modifier,
    isClickable: Boolean = false,
    showInfoIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (isClickable && onClick != null) {
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    } else {
        Modifier
    }

    Row(
        modifier = modifier.then(clickModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryLight
                )
                if (showInfoIcon) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Xem chi tiết",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = DeviceStorage.formatBytesLocale(bytes),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherStorageDetailsSheet(
    breakdown: StorageBreakdown,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showAppsListSheet by remember { mutableStateOf(false) }

    val systemBytes = remember { DeviceStorage.querySystemPartitionBytes() }
    val audioBytes = remember { DeviceStorage.queryAudioBytes(context) }
    val downloadsBytes = remember { DeviceStorage.queryDownloadsBytes() }

    val otherFormatted = DeviceStorage.formatBytesLocale(breakdown.otherBytes)

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
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chi tiết dữ liệu \"Khác\"",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Toàn bộ tài nguyên ngoài Thư viện Ảnh & Video",
                        fontSize = 12.sp,
                        color = TextSecondaryLight
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF94A3B8).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = otherFormatted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ứng dụng này chuyên dọn dẹp Thư viện Ảnh & Video. Phần $otherFormatted này là toàn bộ các tài nguyên khác đang chiếm bộ nhớ máy của bạn:",
                fontSize = 13.sp,
                color = TextSecondaryLight,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Detailed Breakdown Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Interactive Apps Card -> Tapping opens installed apps list!
                OtherCategoryCard(
                    icon = Icons.Rounded.Apps,
                    iconBgColor = Color(0xFF3B82F6),
                    title = "Ứng dụng & Dữ liệu ứng dụng (Apps & Games)",
                    subtitle = "Zalo, Facebook, TikTok, YouTube, các game đồ họa nặng... Dữ liệu chat, ảnh/video lưu riêng trong Zalo/Telegram thuộc nhóm này.",
                    estimate = "Thường chiếm ~35 - 50 GB",
                    actionText = "Xem danh sách ứng dụng ➔",
                    onClick = { showAppsListSheet = true }
                )

                OtherCategoryCard(
                    icon = Icons.Rounded.Android,
                    iconBgColor = Color(0xFF10B981),
                    title = "Hệ điều hành Android & One UI",
                    subtitle = "Phân vùng hệ thống Android, giao diện Samsung One UI, driver phần cứng và bản vá bảo mật định kỳ.",
                    estimate = if (systemBytes > 0) "Phân vùng hệ thống: ${DeviceStorage.formatBytesLocale(systemBytes)}" else "Thường chiếm ~15 - 25 GB"
                )

                OtherCategoryCard(
                    icon = Icons.Rounded.Cached,
                    iconBgColor = Color(0xFFF59E0B),
                    title = "Bộ nhớ đệm (Cache)",
                    subtitle = "Dữ liệu tạm của trình duyệt web (Chrome, Samsung Internet), video xem trước (TikTok/Reels), nhạc offline (Spotify/Zing MP3).",
                    estimate = "Thường chiếm ~5 - 10 GB"
                )

                OtherCategoryCard(
                    icon = Icons.Rounded.Folder,
                    iconBgColor = Color(0xFF8B5CF6),
                    title = "Tài liệu, Tải về & Âm thanh",
                    subtitle = "File trong thư mục Download (PDF, Word, zip, apk...) và tệp ghi âm, nhạc MP3.",
                    estimate = if (audioBytes > 0 || downloadsBytes > 0) {
                        "Đã phát hiện: ${DeviceStorage.formatBytesLocale(audioBytes + downloadsBytes)}"
                    } else "Thường chiếm ~2 - 5 GB"
                )

                OtherCategoryCard(
                    icon = Icons.Rounded.DeleteSweep,
                    iconBgColor = Color(0xFFEF4444),
                    title = "Thùng rác hệ thống (Trash)",
                    subtitle = "Ảnh/video bạn đã xóa trong 30 ngày qua nhưng hệ thống vẫn lưu tạm trong Thùng rác của Bộ sưu tập.",
                    estimate = "Được giữ trong 30 ngày để khôi phục"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Button 1: Xem danh sách ứng dụng trong máy
            Button(
                onClick = {
                    showAppsListSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = YellowTextDark
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Xem danh sách ứng dụng trong máy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Button 2: Mở Cài đặt Ứng dụng của máy (Samsung)
            OutlinedButton(
                onClick = {
                    com.photosremover.app.domain.AppStorageManager.openSystemAppsSettings(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = TextPrimaryLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mở Cài đặt Ứng dụng của máy",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Text(
                    text = "Đã hiểu",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryLight
                )
            }
        }
    }

    if (showAppsListSheet) {
        InstalledAppsListSheet(
            onDismiss = { showAppsListSheet = false }
        )
    }
}

@Composable
private fun OtherCategoryCard(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    estimate: String,
    actionText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    } else {
        Modifier.clip(RoundedCornerShape(14.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .background(Color(0xFFF8FAFC))
            .border(
                1.dp,
                if (onClick != null) iconBgColor.copy(alpha = 0.35f) else Color(0xFFF1F5F9),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconBgColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = TextSecondaryLight,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = estimate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iconBgColor
                )
                if (actionText != null) {
                    Text(
                        text = actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconBgColor
                    )
                }
            }
        }
    }
}
