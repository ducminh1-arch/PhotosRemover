package com.photosremover.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photosremover.app.data.model.ScanPhase
import com.photosremover.app.data.model.ScanProgress
import com.photosremover.app.ui.components.LaserScannerAnimation
import com.photosremover.app.ui.theme.BrandAccent
import com.photosremover.app.ui.theme.BrandPrimary
import com.photosremover.app.ui.theme.YellowBackground
import com.photosremover.app.ui.theme.YellowTextDark
import com.photosremover.app.ui.theme.YellowSubText

@Composable
fun ScanningScreen(
    progress: ScanProgress,
    onCancelScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YellowBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Center Phone Laser Scanner
        LaserScannerAnimation()

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Scanning photos, please wait…",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = YellowTextDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live item counter (e.g. 64 / 3,420 photos)
        Text(
            text = if (progress.total > 0) "${progress.current} / ${progress.total}" else "${progress.current}",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = YellowTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Phase description
        val phaseText = when (progress.phase) {
            ScanPhase.INDEXING -> "Analyzing photo metadata…"
            ScanPhase.EXACT_CHECKING -> "Checking exact duplicates (SHA-256)…"
            ScanPhase.SIMILAR_CHECKING -> "Comparing similar photos (dHash)…"
            ScanPhase.VIDEO_CHECKING -> "Scanning videos for duplicates…"
            else -> "Processing…"
        }

        Text(
            text = phaseText,
            fontSize = 13.sp,
            color = YellowSubText,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar
        if (progress.total > 0) {
            LinearProgressIndicator(
                progress = { progress.progressFraction },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp),
                color = YellowTextDark,
                trackColor = Color.White.copy(alpha = 0.5f),
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp),
                color = YellowTextDark,
                trackColor = Color.White.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Scanning time depends on number of files.",
            fontSize = 12.sp,
            color = YellowSubText,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Cancel Button
        OutlinedButton(
            onClick = onCancelScan,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.35f),
                contentColor = YellowTextDark
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(YellowTextDark.copy(alpha = 0.4f))
            ),
            modifier = Modifier.fillMaxWidth(0.55f)
        ) {
            Text("Cancel Scan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
