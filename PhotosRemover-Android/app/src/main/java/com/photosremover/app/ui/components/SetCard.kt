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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.photosremover.app.data.model.DuplicateSet
import com.photosremover.app.data.model.DuplicateSetType
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.VideoItem
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.BrandPrimary
import com.photosremover.app.ui.theme.DangerRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SetCard(
    set: DuplicateSet,
    onPhotoClick: (PhotoItem) -> Unit,
    onTogglePhotoSelect: (Long) -> Unit,
    onToggleSetSelect: () -> Unit,
    onSelectAllExceptBest: () -> Unit,
    onTogglePhotoExclude: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onToggleSetSelect() }
                ) {
                    // Checkbox for the set
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (set.isAllSelected) BrandAccent else Color(0xFFE2E8F0))
                            .border(
                                width = 1.dp,
                                color = if (set.isAllSelected) Color(0xFFB48C00) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (set.isAllSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Set selected",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Video icon for video sets
                    if (set.isVideo) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            tint = BrandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = set.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val countLabel = if (set.isVideo) {
                        "${set.videos.size} videos • ${PhotoItem.formatByteSize(set.totalSize)}"
                    } else {
                        "${set.photos.size} photos • ${PhotoItem.formatByteSize(set.totalSize)}"
                    }
                    Text(
                        text = "($countLabel)",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1
                    )
                }

                TextButton(
                    onClick = onSelectAllExceptBest,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Auto-pick",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal scrolling gallery
            if (set.isVideo) {
                // Video thumbnails row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(set.videos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            onToggleSelect = { onTogglePhotoSelect(video.id) }
                        )
                    }
                }
            } else {
                // Photo thumbnails row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(set.photos, key = { it.id }) { photo ->
                        PhotoCard(
                            photo = photo,
                            onCardClick = { onPhotoClick(photo) },
                            onToggleSelect = { onTogglePhotoSelect(photo.id) },
                            onToggleExclude = onTogglePhotoExclude?.let { fn -> { fn(photo.id) } }
                        )
                    }
                }
            }
        }
    }
}

private object VideoThumbnailCache {
    private val memoryCache = LruCache<Long, Bitmap>(80)

    fun get(id: Long): Bitmap? = memoryCache.get(id)
    fun put(id: Long, bitmap: Bitmap) {
        memoryCache.put(id, bitmap)
    }
}

@Composable
fun VideoThumbnailView(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(video.id) {
        mutableStateOf(VideoThumbnailCache.get(video.id))
    }

    LaunchedEffect(video.id) {
        if (bitmap == null && video.uri != null) {
            withContext(Dispatchers.IO) {
                val loadedBitmap = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(
                            video.uri,
                            android.util.Size(300, 380),
                            null
                        )
                    } else {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, video.uri)
                        val frame = retriever.getFrameAtTime(1000000) ?: retriever.frameAtTime
                        retriever.release()
                        frame
                    }
                } catch (_: Exception) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, video.uri)
                        val frame = retriever.frameAtTime
                        retriever.release()
                        frame
                    } catch (_: Exception) {
                        null
                    }
                }
                if (loadedBitmap != null) {
                    VideoThumbnailCache.put(video.id, loadedBitmap)
                    withContext(Dispatchers.Main) {
                        bitmap = loadedBitmap
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
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * A card to display a video item in the horizontal list.
 * Styled IDENTICALLY to PhotoCard:
 * - Real video thumbnail frame
 * - Dim overlay & red border when selected
 * - Top-Left "BEST" badge
 * - Top-Right checkbox button with yellow check badge
 * - Play icon overlay in center
 * - Duration badge
 * - Bottom solid yellow ribbon with file size matching reference Image 1
 */
@Composable
fun VideoCard(
    video: VideoItem,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 12.dp
    val isSelected = video.isSelected
    val context = LocalContext.current

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
            .clickable { onToggleSelect() }
    ) {
        // 1. Real Video Thumbnail Frame
        VideoThumbnailView(
            video = video,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Dim overlay when selected for deletion
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DangerRed.copy(alpha = 0.25f))
            )
        }

        // 3. Center Play Icon Overlay (can click to preview video)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { playVideo(context, video.uri) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Video",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // 4. Top Left: "BEST" Badge (matching PhotoCard)
        if (video.isBest) {
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

        // 5. Top Right: Checkbox Button (Yellow check badge matching PhotoCard)
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

        // 6. Floating Duration Badge (above bottom ribbon)
        if (video.formattedDuration.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 22.dp, end = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 7. Bottom: File size solid yellow ribbon matching PhotoCard & Image 1
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(BrandAccent)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = video.formattedSize,
                color = Color(0xFF1E293B),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun playVideo(context: Context, uri: Uri?) {
    if (uri == null) return
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
