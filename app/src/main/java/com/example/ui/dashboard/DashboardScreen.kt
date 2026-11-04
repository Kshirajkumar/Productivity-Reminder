package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriorityLevel
import com.example.data.TaskCategory
import com.example.ui.components.InteractiveEmptyState
import com.example.ui.components.TaskItemCard
import com.example.ui.components.TryTheseOutSection
import com.example.ui.components.bounceClick
import com.example.ui.theme.AuraAmberImportant
import com.example.ui.theme.AuraCyanAccent
import com.example.ui.theme.AuraEmeraldDone
import com.example.ui.theme.AuraIrisLight
import com.example.ui.theme.AuraIrisPrimary
import com.example.ui.theme.AuraRoseUrgent
import com.example.ui.theme.ThemeController
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.TaskFilter
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    uiState: DashboardUiState,
    onOpenReminderPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<com.example.data.TaskEntity?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val todayDateString = remember { dateFormat.format(Date()) }

    val scheduledCount = uiState.tasks.count { !it.isCompleted }
    val importantCount = uiState.tasks.count { it.priority == PriorityLevel.HIGH.name && !it.isCompleted }
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
            // Header: AuraTask Title & Actions
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
                ) {
                    Column {
                        Text(
                            text = todayDateString.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AuraIrisLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AuraTask",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

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

                        // Theme Toggle (Sun / Moon)
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
                                    text = { Text("Test Full-Screen Alert (5s)") },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AuraIrisPrimary) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.triggerTestReminder(5)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Preview Lockscreen Alert") },
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
                                focusedBorderColor = AuraIrisPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // Original Aura Momentum Hero Card (Replacing the copied 6 Samsung cards)
            item {
                AuraMomentumHeroCard(
                    completed = uiState.completedCountToday,
                    total = uiState.totalCountToday,
                    completionRatio = uiState.completionRatioToday,
                    streakDays = uiState.streakDays,
                    importantCount = importantCount,
                    onOpenCreate = { showCreateSheet = true },
                    onFilterStarred = { viewModel.setFilter(TaskFilter.HIGH_PRIORITY) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Fluid Pill Filter Rail
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterPills = listOf(
                        TaskFilter.ALL to "All (${uiState.tasks.size})",
                        TaskFilter.TODAY to "Today (${uiState.totalCountToday})",
                        TaskFilter.UPCOMING to "Scheduled ($scheduledCount)",
                        TaskFilter.HIGH_PRIORITY to "Important ($importantCount)",
                        TaskFilter.COMPLETED to "Done ($completedCount)"
                    )

                    filterPills.forEach { (filter, label) ->
                        val isSelected = uiState.activeFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) AuraIrisPrimary else MaterialTheme.colorScheme.surface)
                                .border(
                                    0.8.dp,
                                    if (isSelected) AuraIrisPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(14.dp)
                                )
                                .bounceClick { viewModel.setFilter(filter) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("filter_pill_${filter.name}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Active Filter Heading
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (uiState.activeFilter) {
                            TaskFilter.ALL -> "Active Reminders"
                            TaskFilter.TODAY -> "Today's Schedule"
                            TaskFilter.UPCOMING -> "Upcoming Scheduled"
                            TaskFilter.HIGH_PRIORITY -> "Starred & Important"
                            TaskFilter.COMPLETED -> "Completed"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "${uiState.tasks.size} reminders",
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
                        subtitle = "Tap '+ Add Reminder' below to set up an alarm-grade reminder.",
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
                            onEdit = { editingTask = task },
                            onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // "Try these out" expandable recommendations
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

        // Bottom Inset "+ Add Reminder" Bar
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
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        0.8.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(28.dp)
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AuraIrisPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add new reminder...",
                            fontSize = 15.sp,
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

    // Modal Bottom Sheet - Create Task
    if (showCreateSheet) {
        TaskEditorBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            onSaveTask = { title, details, category, priority, reminderTime, isRecurring, subtasks ->
                viewModel.addNewTask(
                    title = title,
                    details = details,
                    category = category,
                    priority = priority,
                    reminderTimeMillis = reminderTime,
                    isRecurringDaily = isRecurring,
                    subtasks = subtasks
                )
                showCreateSheet = false
            }
        )
    }

    // Modal Bottom Sheet - Edit Existing Task
    editingTask?.let { task ->
        TaskEditorBottomSheet(
            existingTask = task,
            onDismissRequest = { editingTask = null },
            onSaveTask = { title, details, category, priority, reminderTime, isRecurring, subtasks ->
                viewModel.updateExistingTask(
                    taskId = task.id,
                    title = title,
                    details = details,
                    category = category,
                    priority = priority,
                    reminderTimeMillis = reminderTime,
                    isRecurringDaily = isRecurring,
                    subtasks = subtasks
                )
                editingTask = null
            }
        )
    }
}

/**
 * Original AuraTask Momentum Hero Card:
 * Elegant circular progress ring, streak flame counter, and quick status metrics.
 */
@Composable
fun AuraMomentumHeroCard(
    completed: Int,
    total: Int,
    completionRatio: Float,
    streakDays: Int,
    importantCount: Int,
    onOpenCreate: () -> Unit,
    onFilterStarred: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = completionRatio,
        label = "heroProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.8.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(24.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's Focus",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (total > 0) "$completed of $total tasks completed" else "All scheduled works on track",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Circular Progress Gauge
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = Color(0x22808080),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(AuraIrisPrimary, AuraCyanAccent, AuraIrisPrimary)),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = stroke
                    )
                }

                val percent = (completionRatio * 100).toInt()
                Text(
                    text = "$percent%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3 Status Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Streak Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraAmberImportant.copy(alpha = 0.15f))
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = AuraAmberImportant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streakDays Day Streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraAmberImportant
                    )
                }
            }

            // Important / Starred Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraIrisPrimary.copy(alpha = 0.15f))
                    .bounceClick { onFilterStarred() }
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AuraIrisPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$importantCount Starred",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraIrisPrimary
                    )
                }
            }
        }
    }
}
