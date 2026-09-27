package com.photosremover.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.BrandPrimary

@Composable
fun LaserScannerAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Box(
        modifier = modifier
            .width(200.dp)
            .height(340.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height

            // Outer phone device border
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Phone speaker notch
            drawRoundRect(
                color = Color(0xFF64748B),
                topLeft = Offset(w * 0.38f, 10.dp.toPx()),
                size = Size(w * 0.24f, 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Front camera dot
            drawCircle(
                color = Color(0xFF64748B),
                radius = 3.dp.toPx(),
                center = Offset(w * 0.28f, 12.dp.toPx())
            )

            // Inner screen border
            drawRoundRect(
                color = Color(0xFFCBD5E1),
                topLeft = Offset(8.dp.toPx(), 26.dp.toPx()),
                size = Size(w - 16.dp.toPx(), h - 56.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Two floating photo cards inside phone screen
            // Card 1
            drawRoundRect(
                color = BrandAccent.copy(alpha = 0.25f),
                topLeft = Offset(w * 0.2f, h * 0.22f),
                size = Size(w * 0.45f, h * 0.18f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Card 2
            drawRoundRect(
                color = BrandAccent.copy(alpha = 0.35f),
                topLeft = Offset(w * 0.38f, h * 0.44f),
                size = Size(w * 0.45f, h * 0.18f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Circular home button at bottom (matching reference)
            drawCircle(
                color = Color(0xFF64748B),
                radius = 10.dp.toPx(),
                center = Offset(w * 0.5f, h - 15.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Laser beam line and glow
            val currentLaserY = h * laserOffset

            // Laser glow gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BrandAccent.copy(alpha = 0.0f),
                        BrandAccent.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.8f),
                        BrandAccent.copy(alpha = 0.45f),
                        BrandAccent.copy(alpha = 0.0f)
                    ),
                    startY = currentLaserY - 24.dp.toPx(),
                    endY = currentLaserY + 24.dp.toPx()
                ),
                topLeft = Offset(4.dp.toPx(), currentLaserY - 24.dp.toPx()),
                size = Size(w - 8.dp.toPx(), 48.dp.toPx())
            )

            // Sharp laser central line
            drawLine(
                color = BrandAccent,
                start = Offset(6.dp.toPx(), currentLaserY),
                end = Offset(w - 6.dp.toPx(), currentLaserY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
