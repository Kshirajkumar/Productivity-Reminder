package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriorityLevel
import com.example.data.Subtask
import com.example.data.TaskEntity
import com.example.ui.theme.AuraAmberImportant
import com.example.ui.theme.AuraEmeraldDone
import com.example.ui.theme.AuraIrisPrimary
import com.example.ui.theme.AuraRoseUrgent
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalContext
import com.example.ui.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskItemCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onSnooze15m: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onSubtaskToggle: ((Subtask) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expandedDetails by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(task.reminderTimeMillis) {
        timeFormat.format(Date(task.reminderTimeMillis))
    }

    val subtasks = remember(task.subtasksJson) { task.getSubtaskList() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.8.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .clickable { expandedDetails = !expandedDetails }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("task_item_${task.id}")
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            // iOS Style Radio Circle Checkbox on the left
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(26.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) AuraEmeraldDone else Color.Transparent)
                    .border(
                        1.8.dp,
                        if (task.isCompleted) AuraEmeraldDone else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .clickable {
                        HapticHelper.success(context)
                        onToggleComplete()
                    }
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Subtitle column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = if (expandedDetails) 4 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (task.priority == PriorityLevel.HIGH.name) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Important",
                            tint = AuraAmberImportant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time Row (e.g., "3:43 pm") with Animated Sliding Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedTimeText(
                        timeString = formattedTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (task.details.isNotBlank() && !expandedDetails) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${task.details}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Recurrence or Category pill row matching screenshot
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.isRecurringDaily) "Every day" else "Category: ${task.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (task.snoozeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Snoozed ${task.snoozeCount}x",
                            fontSize = 11.sp,
                            color = AuraAmberImportant
                        )
                    }
                }

                // Expandable subtasks checklist
                AnimatedVisibility(
                    visible = expandedDetails,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        if (task.details.isNotBlank()) {
                            Text(
                                text = task.details,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (subtasks.isNotEmpty()) {
                            subtasks.forEach { subtask ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSubtaskToggle?.invoke(subtask) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (subtask.isDone) AuraIrisPrimary else Color.Transparent)
                                            .border(1.2.dp, if (subtask.isDone) AuraIrisPrimary else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (subtask.isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subtask.title,
                                        fontSize = 13.sp,
                                        color = if (subtask.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Options 3-dots Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("Edit Reminder") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AuraIrisPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Snooze 15 Mins") },
                        leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null, tint = AuraIrisPrimary) },
                        onClick = {
                            menuExpanded = false
                            onSnooze15m()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Reminder", color = AuraRoseUrgent) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AuraRoseUrgent) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
