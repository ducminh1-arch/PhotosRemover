package com.photosremover.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.BrandPrimary
import com.photosremover.app.ui.theme.DangerRed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoCard(
    photo: PhotoItem,
    isExcluded: Boolean = false,
    onCardClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onToggleExclude: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 12.dp
    val isSelected = photo.isSelected

    Box(
        modifier = modifier
            .size(width = 110.dp, height = 135.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) DangerRed else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(Color(0xFFF1F5F9))
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onToggleExclude
            )
    ) {
        // Thumbnail Image
        AsyncImage(
            model = photo.uri,
            contentDescription = "Photo thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay when selected for deletion
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DangerRed.copy(alpha = 0.25f))
            )
        }

        // Top Left: "Best" Badge
        if (photo.isBest) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BrandAccent)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "BEST",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Excluded Badge
        if (isExcluded) {
            Box(
                modifier = Modifier
                    .align(if (photo.isBest) Alignment.TopCenter else Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Excluded",
                    tint = BrandAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Top Right: Checkbox Button (Yellow check badge khớp Screen 3)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isSelected) BrandAccent else Color.Black.copy(alpha = 0.4f)
                )
                .border(
                    width = 1.2.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { onToggleSelect() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Bottom: File size solid yellow ribbon matching reference
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(BrandAccent)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = photo.formattedSize,
                color = Color(0xFF453200),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
