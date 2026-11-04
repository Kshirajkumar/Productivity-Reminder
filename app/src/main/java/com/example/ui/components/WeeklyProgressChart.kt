package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeeklyAnalyticsData
import com.example.ui.theme.IosColorCompleted
import com.example.ui.theme.IosColorImportant
import com.example.ui.theme.IosColorScheduled

@Composable
fun WeeklyProgressChart(
    analyticsData: WeeklyAnalyticsData,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(analyticsData) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IosColorScheduled.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = IosColorScheduled,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Weekly Activity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rolling 7-day completion",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Streak Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(IosColorImportant.copy(alpha = 0.15f))
                    .border(0.8.dp, IosColorImportant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = IosColorImportant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${analyticsData.currentStreakDays}d streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosColorImportant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Custom Canvas Bar Chart
        val barData = analyticsData.dailyProgressList
        val maxTotal = remember(barData) {
            val max = barData.maxOfOrNull { it.totalCount } ?: 1
            if (max == 0) 1 else max
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = barData.size.coerceAtLeast(1)
            val slotWidth = canvasWidth / barCount
            val barWidth = 22.dp.toPx()
            val cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx())

            barData.forEachIndexed { index, item ->
                val xCenter = (index * slotWidth) + (slotWidth / 2f)
                val xLeft = xCenter - (barWidth / 2f)

                // Background track
                val trackFraction = if (maxTotal > 0) (item.totalCount.toFloat() / maxTotal.toFloat()).coerceIn(0.12f, 1f) else 0.12f
                val trackHeight = canvasHeight * trackFraction * animProgress.value
                val trackTop = canvasHeight - trackHeight

                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(xLeft, trackTop),
                    size = Size(barWidth, trackHeight),
                    cornerRadius = cornerRadius
                )

                // Completed fill bar
                if (item.completedCount > 0) {
                    val fillFraction = if (maxTotal > 0) (item.completedCount.toFloat() / maxTotal.toFloat()).coerceIn(0.08f, 1f) else 0.08f
                    val fillHeight = canvasHeight * fillFraction * animProgress.value
                    val fillTop = canvasHeight - fillHeight

                    val isFullDay = item.totalCount > 0 && item.completedCount >= item.totalCount
                    val gradient = Brush.verticalGradient(
                        colors = if (isFullDay) {
                            listOf(IosColorCompleted, IosColorScheduled)
                        } else {
                            listOf(IosColorScheduled, IosColorScheduled.copy(alpha = 0.7f))
                        },
                        startY = fillTop,
                        endY = canvasHeight
                    )

                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(xLeft, fillTop),
                        size = Size(barWidth, fillHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            barData.forEach { item ->
                Text(
                    text = item.dayLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.completedCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Bottom Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Completion Rate Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Rate",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${analyticsData.overallCompletionPercent}%",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosColorCompleted
                    )
                }
            }

            // Completed Tasks Count
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Completed",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${analyticsData.totalCompletedThisWeek} tasks",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosColorScheduled
                    )
                }
            }
        }
    }
}
