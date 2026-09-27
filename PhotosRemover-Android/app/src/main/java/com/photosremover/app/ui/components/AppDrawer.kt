package com.photosremover.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Shield
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.SimilarSensitivity
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.BrandPrimary

@Composable
fun AppDrawer(
    cleanedCount: Int,
    cleanedSizeBytes: Long,
    sensitivity: SimilarSensitivity,
    excludedCount: Int = 0,
    onSensitivityChange: (SimilarSensitivity) -> Unit,
    onCloseClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onRateClick: () -> Unit,
    onAboutClick: () -> Unit,
    onClearExclusionsClick: () -> Unit = {},
    onLargeMediaCleanerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        color = Color(0xFFFAFAFA),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menu & Stats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            // Stats Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Big circular counter
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(width = 3.dp, color = BrandAccent, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$cleanedCount",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Photos cleaned so far",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Storage freed: ${PhotoItem.formatByteSize(cleanedSizeBytes)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Similar Sensitivity Setting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Similar Photo Sensitivity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SimilarSensitivity.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSensitivityChange(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sensitivity == option,
                            onClick = { onSensitivityChange(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = option.label,
                            fontSize = 13.sp,
                            fontWeight = if (sensitivity == option) FontWeight.Bold else FontWeight.Normal,
                            color = if (sensitivity == option) BrandPrimary else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Items List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                DrawerMenuItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Dọn tệp lớn (Video & Ảnh nặng)",
                    onClick = onLargeMediaCleanerClick
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                DrawerMenuItem(
                    icon = Icons.Default.Shield,
                    title = "Ảnh đã loại trừ ($excludedCount)",
                    onClick = onClearExclusionsClick
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                DrawerMenuItem(
                    icon = Icons.Default.RateReview,
                    title = "Feedback",
                    onClick = onFeedbackClick
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                DrawerMenuItem(
                    icon = Icons.Default.Star,
                    title = "Rate App",
                    onClick = onRateClick
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                DrawerMenuItem(
                    icon = Icons.Default.Info,
                    title = "About Remo",
                    onClick = onAboutClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
    }
}
