package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosColorCompleted
import com.example.ui.theme.IosColorImportant
import com.example.ui.theme.IosColorNoAlert
import com.example.ui.theme.IosColorPlace
import com.example.ui.theme.IosColorScheduled
import com.example.ui.theme.IosColorToday
import com.example.ui.viewmodel.TaskFilter

data class ReminderGridItem(
    val filter: TaskFilter,
    val title: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val count: String
)

@Composable
fun IosReminderGrid(
    activeFilter: TaskFilter,
    todayCount: String,
    scheduledCount: Int,
    importantCount: Int,
    allCount: Int,
    noAlertCount: Int,
    completedCount: Int,
    onSelectFilter: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        ReminderGridItem(TaskFilter.TODAY, "Today", Icons.Default.CalendarToday, IosColorToday, todayCount),
        ReminderGridItem(TaskFilter.UPCOMING, "Scheduled", Icons.Default.Alarm, IosColorScheduled, scheduledCount.toString()),
        ReminderGridItem(TaskFilter.HIGH_PRIORITY, "Important", Icons.Default.Star, IosColorImportant, importantCount.toString()),
        ReminderGridItem(TaskFilter.ALL, "All", Icons.Default.LocationOn, IosColorPlace, allCount.toString()),
        ReminderGridItem(TaskFilter.ALL, "Notes", Icons.Default.NotificationsOff, IosColorNoAlert, noAlertCount.toString()),
        ReminderGridItem(TaskFilter.COMPLETED, "Completed", Icons.Default.CheckCircle, IosColorCompleted, completedCount.toString())
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1 (3 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 0..2) {
                val item = items[i]
                IosGridCard(
                    item = item,
                    isSelected = activeFilter == item.filter,
                    onClick = { onSelectFilter(item.filter) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2 (3 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 3..5) {
                val item = items[i]
                IosGridCard(
                    item = item,
                    isSelected = activeFilter == item.filter && (item.filter != TaskFilter.ALL || i == 3),
                    onClick = { onSelectFilter(item.filter) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun IosGridCard(
    item: ReminderGridItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        item.iconBgColor
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(if (isSelected) 1.5.dp else 0.8.dp, borderColor, RoundedCornerShape(20.dp))
            .bounceClick { onClick() }
            .padding(10.dp)
            .testTag("grid_card_${item.title.lowercase()}")
    ) {
        // Icon at top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(34.dp)
                .clip(CircleShape)
                .background(item.iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Large bold count at top right with animated sliding numbers
        AnimatedTimeText(
            timeString = item.count,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Label at bottom left
        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
