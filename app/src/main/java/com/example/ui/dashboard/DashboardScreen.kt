package com.example.ui.dashboard

import com.example.ui.components.bounceClick

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriorityLevel
import com.example.data.TaskCategory
import com.example.ui.components.InteractiveEmptyState
import com.example.ui.components.IosReminderGrid
import com.example.ui.components.TaskItemCard
import com.example.ui.components.TryTheseOutSection
import com.example.ui.theme.IosColorScheduled
import com.example.ui.theme.ThemeController
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.TaskFilter
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    uiState: DashboardUiState,
    onOpenReminderPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Counts for the 6 grid cards
    val todayCountString = "${uiState.completedCountToday}/${uiState.totalCountToday}"
    val scheduledCount = uiState.tasks.count { !it.isCompleted }
    val importantCount = uiState.tasks.count { it.priority == PriorityLevel.HIGH.name && !it.isCompleted }
    val allCount = uiState.tasks.size
    val noAlertCount = uiState.tasks.count { it.snoozeCount == 0 && !it.isCompleted }
    val completedCount = uiState.tasks.count { it.isCompleted }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Header: "Reminder" with Search, Theme Toggle, and 3-dots menu
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "Reminder",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Button
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Theme Toggle Button (Sun / Moon)
                        IconButton(
                            onClick = { ThemeController.toggle() },
                            modifier = Modifier.testTag("theme_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (ThemeController.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Options Menu
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Test Samsung Alert (5s)") },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = IosColorScheduled) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.triggerTestReminder(5)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Preview Full-Screen Alert") },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenReminderPreview()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search input field
            if (showSearch) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search reminders...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = IosColorScheduled,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // 6 Apple/Samsung-style Grid Cards (Today, Scheduled, Important, All, Notes, Completed)
            item {
                IosReminderGrid(
                    activeFilter = uiState.activeFilter,
                    todayCount = todayCountString,
                    scheduledCount = scheduledCount,
                    importantCount = importantCount,
                    allCount = allCount,
                    noAlertCount = noAlertCount,
                    completedCount = completedCount,
                    onSelectFilter = { viewModel.setFilter(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section Header matching screenshot (e.g. "Tomorrow" or "Today" or Filter Name)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = when (uiState.activeFilter) {
                            TaskFilter.ALL -> "All Reminders"
                            TaskFilter.TODAY -> "Today"
                            TaskFilter.UPCOMING -> "Scheduled"
                            TaskFilter.HIGH_PRIORITY -> "Important"
                            TaskFilter.COMPLETED -> "Completed"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "${uiState.tasks.size} items",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Task list items or empty state
            if (uiState.tasks.isEmpty()) {
                item {
                    InteractiveEmptyState(
                        title = "No reminders here",
                        subtitle = "Tap '+ Add reminder' below or select from suggestions to create one.",
                        onCreateClick = { showCreateSheet = true },
                        onTemplateClick = { template -> viewModel.addFromTemplate(template) }
                    )
                }
            } else {
                items(
                    items = uiState.tasks,
                    key = { it.id }
                ) { task ->
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

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // "Try these out" expandable section matching Screenshot 1
            item {
                TryTheseOutSection(
                    onSelectRecommendation = { rec ->
                        val reminderTime = System.currentTimeMillis() + (rec.minutesFromNow * 60 * 1000L)
                        viewModel.addNewTask(
                            title = rec.title,
                            details = rec.subtitle,
                            category = rec.category,
                            priority = rec.priority,
                            reminderTimeMillis = reminderTime,
                            subtasks = emptyList()
                        )
                    }
                )
            }
        }

        // Bottom "+ Add reminder" pill bar matching Screenshot 1 & 2
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        0.8.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(27.dp)
                    )
                    .bounceClick { showCreateSheet = true }
                    .padding(horizontal = 18.dp)
                    .testTag("add_reminder_bottom_bar"),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add reminder",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet
    if (showCreateSheet) {
        TaskEditorBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            onSaveTask = { title, details, category, priority, reminderTime, subtasks ->
                viewModel.addNewTask(
                    title = title,
                    details = details,
                    category = category,
                    priority = priority,
                    reminderTimeMillis = reminderTime,
                    subtasks = subtasks
                )
                showCreateSheet = false
            }
        )
    }
}
