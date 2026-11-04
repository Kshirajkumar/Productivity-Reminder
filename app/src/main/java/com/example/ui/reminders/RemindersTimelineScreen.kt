package com.example.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriorityLevel
import com.example.data.TaskEntity
import com.example.ui.components.InteractiveEmptyState
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.IosColorImportant
import com.example.ui.theme.IosColorScheduled
import com.example.ui.theme.IosColorToday
import com.example.ui.theme.PriorityHigh
import com.example.ui.viewmodel.TaskViewModel
import java.util.Calendar

@Composable
fun RemindersTimelineScreen(
    viewModel: TaskViewModel,
    tasks: List<TaskEntity>,
    onOpenLivePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTasks = remember(tasks) {
        tasks.filter { !it.isCompleted }.sortedBy { it.reminderTimeMillis }
    }

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendar.timeInMillis
    val endOfToday = startOfToday + (24 * 60 * 60 * 1000L) - 1
    val endOfTomorrow = endOfToday + (24 * 60 * 60 * 1000L)

    val todayReminders = activeTasks.filter { it.reminderTimeMillis in startOfToday..endOfToday }
    val tomorrowReminders = activeTasks.filter { it.reminderTimeMillis in (endOfToday + 1)..endOfTomorrow }
    val laterReminders = activeTasks.filter { it.reminderTimeMillis > endOfTomorrow }
    val overdueReminders = activeTasks.filter { it.reminderTimeMillis < startOfToday }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header Hero Banner with Samsung Reminder Test Triggers
            item {
                RemindersHeroBanner(
                    activeCount = activeTasks.size,
                    onTriggerTest5s = { viewModel.triggerTestReminder(5) },
                    onOpenPreview = onOpenLivePreview
                )
            }

            if (activeTasks.isEmpty()) {
                item {
                    InteractiveEmptyState(
                        title = "No Pending Reminders",
                        subtitle = "You are all caught up! Create a new task with a reminder or tap a starter template to test.",
                        onCreateClick = { viewModel.triggerTestReminder(3) },
                        onTemplateClick = { template -> viewModel.addFromTemplate(template) },
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }
            } else {
                // Overdue Section
                if (overdueReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Overdue",
                            count = overdueReminders.size,
                            color = PriorityHigh
                        )
                    }
                    items(overdueReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }

                // Today Section
                if (todayReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Today",
                            count = todayReminders.size,
                            color = IosColorToday
                        )
                    }
                    items(todayReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }

                // Tomorrow Section
                if (tomorrowReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Tomorrow",
                            count = tomorrowReminders.size,
                            color = IosColorScheduled
                        )
                    }
                    items(tomorrowReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }

                // Later This Week Section
                if (laterReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Upcoming Later",
                            count = laterReminders.size,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(laterReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersHeroBanner(
    activeCount: Int,
    onTriggerTest5s: () -> Unit,
    onOpenPreview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(IosColorScheduled),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Smart Reminders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$activeCount active alarms scheduled",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "When time triggers, the Samsung full-screen alert overlay wakes your device even when locked.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTriggerTest5s,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IosColorScheduled,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("trigger_test_alarm_btn")
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Alarm in 5s", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onOpenPreview,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("preview_alert_screen_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview Alert", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TimelineSectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
