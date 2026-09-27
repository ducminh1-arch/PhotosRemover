package com.photosremover.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photosremover.app.data.model.DuplicateSet
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.BrandPrimary
import com.photosremover.app.ui.theme.DangerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PreviewScreen(
    photo: PhotoItem,
    set: DuplicateSet,
    isExcluded: Boolean = false,
    onBackClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onToggleExclude: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val formattedDate = remember(photo.dateTakenMs) {
        dateFormat.format(Date(photo.dateTakenMs))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar (Signature Yellow #FFC800 khớp Screen 4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.photosremover.app.ui.theme.YellowBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = com.photosremover.app.ui.theme.YellowTextDark
                )
            }

            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = com.photosremover.app.ui.theme.YellowTextDark
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shield button for Whitelist / Exclusion
                IconButton(onClick = onToggleExclude) {
                    Icon(
                        imageVector = if (isExcluded) Icons.Filled.Shield else Icons.Outlined.Shield,
                        contentDescription = "Toggle Exclude",
                        tint = if (isExcluded) DangerRed else com.photosremover.app.ui.theme.YellowTextDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Trash icon to toggle deletion
                IconButton(onClick = onToggleSelect) {
                    Icon(
                        imageVector = if (photo.isSelected) Icons.Default.Delete else Icons.Default.DeleteOutline,
                        contentDescription = "Toggle Delete",
                        tint = if (photo.isSelected) DangerRed else com.photosremover.app.ui.theme.YellowTextDark,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Fullscreen Zoomable Photo View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photo.uri,
                contentDescription = "Full Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )

            if (photo.isBest) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(BrandAccent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "★ RECOMMENDED TO KEEP",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (isExcluded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ĐÃ LOẠI TRỪ KHỎI QUÉT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Bottom EXIF / Metadata Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF18181B))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "File size: ${photo.formattedSize}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Date: $formattedDate",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Resolution: ${photo.width} × ${photo.height}",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onToggleExclude,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isExcluded) Icons.Filled.Shield else Icons.Outlined.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isExcluded) DangerRed else Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExcluded) "Đã loại trừ" else "Loại trừ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExcluded) DangerRed else Color.White
                        )
                    }

                    Button(
                        onClick = onToggleSelect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (photo.isSelected) DangerRed else BrandAccent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (photo.isSelected) Icons.Default.Check else Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (photo.isSelected) Color.White else Color(0xFF78350F)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (photo.isSelected) "Marked" else "Delete",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (photo.isSelected) Color.White else Color(0xFF78350F)
                        )
                    }
                }
            }
        }
    }
}
