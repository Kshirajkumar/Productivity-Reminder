package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriorityLevel
import com.example.data.Subtask
import com.example.data.TaskCategory
import com.example.data.TaskEntity
import com.example.ui.components.AnimatedNumberTicker
import com.example.ui.components.AnimatedTimeText
import com.example.ui.components.bounceClick
import com.example.ui.theme.AuraAmberImportant
import com.example.ui.theme.AuraCyanAccent
import com.example.ui.theme.AuraIrisLight
import com.example.ui.theme.AuraIrisPrimary
import com.example.ui.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorBottomSheet(
    onDismissRequest: () -> Unit,
    onSaveTask: (
        title: String,
        details: String,
        category: TaskCategory,
        priority: PriorityLevel,
        reminderTimeMillis: Long,
        isRecurringDaily: Boolean,
        subtasks: List<Subtask>
    ) -> Unit,
    existingTask: TaskEntity? = null,
    initialTitle: String = "",
    initialDetails: String = "",
    initialCategory: TaskCategory = TaskCategory.PERSONAL,
    initialPriority: PriorityLevel = PriorityLevel.MEDIUM,
    initialReminderTimeMillis: Long = System.currentTimeMillis() + (30 * 60 * 1000L),
    initialIsRecurringDaily: Boolean = false
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existingTask?.title ?: initialTitle) }
    var details by remember { mutableStateOf(existingTask?.details ?: initialDetails) }
    var isImportant by remember {
        mutableStateOf(
            existingTask?.let { it.priority == PriorityLevel.HIGH.name } ?: (initialPriority == PriorityLevel.HIGH)
        )
    }
    var selectedCategory by remember {
        mutableStateOf(
            existingTask?.let {
                try { TaskCategory.valueOf(it.category) } catch (_: Exception) { initialCategory }
            } ?: initialCategory
        )
    }
    var reminderTime by remember {
        mutableLongStateOf(existingTask?.reminderTimeMillis ?: initialReminderTimeMillis)
    }
    var isDailyRecurring by remember {
        mutableStateOf(existingTask?.isRecurringDaily ?: initialIsRecurringDaily)
    }

    var showCustomTimePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    // Custom Exact Time Picker States
    val initialCal = remember(reminderTime) {
        Calendar.getInstance().apply { timeInMillis = reminderTime }
    }
    var selectedHour by remember {
        val h = initialCal.get(Calendar.HOUR)
        mutableIntStateOf(if (h == 0) 12 else h)
    }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var isPm by remember { mutableStateOf(initialCal.get(Calendar.AM_PM) == Calendar.PM) }
    var dayOffsetDays by remember { mutableIntStateOf(0) }

    fun recalculateReminderTime(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffsetDays)
            val h24 = if (isPm) {
                if (selectedHour == 12) 12 else selectedHour + 12
            } else {
                if (selectedHour == 12) 0 else selectedHour
            }
            set(Calendar.HOUR_OF_DAY, h24)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    val timeFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header: "New Reminder" with Star Priority Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "New Reminder",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { isImportant = !isImportant },
                    modifier = Modifier.testTag("toggle_important_star")
                ) {
                    Icon(
                        imageVector = if (isImportant) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Mark Important",
                        tint = if (isImportant) AuraAmberImportant else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Input Field
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "What do you want to accomplish?",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )

            // Notes field
            TextField(
                value = details,
                onValueChange = { details = it },
                placeholder = {
                    Text(
                        "Add details or notes (optional)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_details_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )

            // Time Row (Tap to open custom exact time selector)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { showCustomTimePicker = !showCustomTimePicker }
                    .padding(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = AuraIrisPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Time (Tap to set exact minute)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AnimatedTimeText(
                        timeString = timeFormatter.format(Date(reminderTime)),
                        fontSize = 13.sp,
                        color = AuraIrisPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Custom Exact Minute & Hour Picker
            AnimatedVisibility(
                visible = showCustomTimePicker,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            0.8.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Exact Time Selection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Day options: Today, Tomorrow, +2 Days, +3 Days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dayOptions = listOf("Today" to 0, "Tomorrow" to 1, "+2 Days" to 2, "+3 Days" to 3)
                        dayOptions.forEach { (label, offset) ->
                            val isSel = dayOffsetDays == offset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) AuraIrisPrimary else MaterialTheme.colorScheme.surface)
                                    .bounceClick {
                                        dayOffsetDays = offset
                                        reminderTime = recalculateReminderTime()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Steppers for Hour & Minute with +/- 1 unit precision!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Hour Stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hour", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .bounceClick {
                                            selectedHour = if (selectedHour <= 1) 12 else selectedHour - 1
                                            reminderTime = recalculateReminderTime()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Hour -1", modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                AnimatedNumberTicker(
                                    number = selectedHour,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .bounceClick {
                                            selectedHour = if (selectedHour >= 12) 1 else selectedHour + 1
                                            reminderTime = recalculateReminderTime()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Hour +1", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(":", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        // Exact Minute Stepper (step by 1 minute: 36, 57, 59, etc.)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Exact Minute", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .bounceClick {
                                            selectedMinute = if (selectedMinute <= 0) 59 else selectedMinute - 1
                                            reminderTime = recalculateReminderTime()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minute -1", modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                val minuteFormatted = if (selectedMinute < 10) "0$selectedMinute" else "$selectedMinute"
                                AnimatedTimeText(
                                    timeString = minuteFormatted,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .bounceClick {
                                            selectedMinute = if (selectedMinute >= 59) 0 else selectedMinute + 1
                                            reminderTime = recalculateReminderTime()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Minute +1", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // AM/PM Toggle
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuraIrisPrimary)
                                .bounceClick {
                                    isPm = !isPm
                                    reminderTime = recalculateReminderTime()
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPm) "PM" else "AM",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Minute Fine-Tuning Slider (0 to 59 with instant drag)
                    Text(
                        text = "Slide to select any minute (0 to 59): $selectedMinute",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = {
                            selectedMinute = it.toInt()
                            reminderTime = recalculateReminderTime()
                        },
                        valueRange = 0f..59f,
                        colors = SliderDefaults.colors(
                            thumbColor = AuraIrisPrimary,
                            activeTrackColor = AuraIrisPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Shortcuts: +1m, +5m, +15m, +1h
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickAddList = listOf(
                            "+1m" to 1,
                            "+5m" to 5,
                            "+15m" to 15,
                            "+30m" to 30,
                            "+1h" to 60
                        )
                        quickAddList.forEach { (lbl, mins) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .bounceClick {
                                        val target = System.currentTimeMillis() + (mins * 60 * 1000L)
                                        reminderTime = target
                                        val c = Calendar.getInstance().apply { timeInMillis = target }
                                        val h = c.get(Calendar.HOUR)
                                        selectedHour = if (h == 0) 12 else h
                                        selectedMinute = c.get(Calendar.MINUTE)
                                        isPm = c.get(Calendar.AM_PM) == Calendar.PM
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lbl, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )

            // Category Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { showCategoryPicker = !showCategoryPicker }
                    .padding(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AuraCyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Category",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedCategory.label,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showCategoryPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCategory.entries.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedCategory == cat) AuraIrisPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .bounceClick {
                                    selectedCategory = cat
                                    showCategoryPicker = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.label,
                                fontSize = 12.sp,
                                color = if (selectedCategory == cat) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )

            // Daily Recurrence Toggle Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick {
                        isDailyRecurring = !isDailyRecurring
                        HapticHelper.click(context)
                    }
                    .padding(vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (isDailyRecurring) AuraIrisPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Daily Recurring Reminder",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDailyRecurring) "Repeats every day at this time" else "Single reminder alert",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isDailyRecurring,
                    onCheckedChange = {
                        isDailyRecurring = it
                        HapticHelper.click(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AuraIrisPrimary
                    )
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )

            // Unlimited Storage Alert feature badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = AuraIrisLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Full-Screen Wake Alert",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Alarm sounds even on lockscreen (No reminder limit)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Buttons: Cancel & Save
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("cancel_task_btn")
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            HapticHelper.click(context)
                            onSaveTask(
                                title.trim(),
                                details.trim(),
                                selectedCategory,
                                if (isImportant) PriorityLevel.HIGH else PriorityLevel.MEDIUM,
                                reminderTime,
                                isDailyRecurring,
                                emptyList()
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("save_task_btn")
                ) {
                    Text(
                        text = if (existingTask != null) "Update Reminder" else "Save Reminder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (title.isNotBlank()) AuraIrisPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
