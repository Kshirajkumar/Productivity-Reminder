package com.example.ui.reminder

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ChronoTaskApp
import com.example.reminder.TaskAlarmReceiver
import com.example.ui.components.AnimatedNumberTicker
import com.example.ui.components.bounceClick
import com.example.ui.theme.ChronoTaskTheme
import com.example.ui.theme.SamsungAmbientBottom
import com.example.ui.theme.SamsungAmbientMid
import com.example.ui.theme.SamsungAmbientTop
import com.example.ui.theme.SamsungCompleteButton
import com.example.ui.theme.SamsungDismissButton
import com.example.ui.theme.SamsungDismissButtonBorder
import com.example.ui.theme.SamsungSnoozePill
import kotlinx.coroutines.launch

import com.example.reminder.TaskReminderManager
import com.example.ui.util.HapticHelper

class ReminderAlertActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        val taskId = intent.getLongExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE) ?: "Scheduled reminder"
        val details = intent.getStringExtra(TaskAlarmReceiver.EXTRA_TASK_DETAILS) ?: ""
        val category = intent.getStringExtra(TaskAlarmReceiver.EXTRA_TASK_CATEGORY) ?: "Personal"
        val priority = intent.getStringExtra(TaskAlarmReceiver.EXTRA_TASK_PRIORITY) ?: "Medium"

        startAudioVibe()

        setContent {
            ChronoTaskTheme {
                SamsungReminderScreen(
                    title = title,
                    details = details,
                    category = category,
                    priority = priority,
                    onDismiss = {
                        HapticHelper.click(this@ReminderAlertActivity)
                        stopAudioVibe()
                        dismissNotification(taskId)
                        finish()
                    },
                    onComplete = {
                        HapticHelper.success(this@ReminderAlertActivity)
                        stopAudioVibe()
                        dismissNotification(taskId)
                        if (taskId > 0) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                TaskReminderManager.completeTask(applicationContext, taskId)
                            }
                        }
                        finish()
                    },
                    onSnooze = { minutes ->
                        HapticHelper.warning(this@ReminderAlertActivity)
                        stopAudioVibe()
                        dismissNotification(taskId)
                        if (taskId > 0) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                TaskReminderManager.snoozeTask(applicationContext, taskId, minutes)
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun startAudioVibe() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, alertUri)
            ringtone?.play()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), 0)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 200, 300), 0)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun stopAudioVibe() {
        try {
            ringtone?.stop()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun dismissNotification(taskId: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val id = if (taskId > 0) taskId.toInt() else 9999
        notificationManager?.cancel(id)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioVibe()
    }
}

@Composable
fun SamsungReminderScreen(
    title: String,
    details: String,
    category: String,
    priority: String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    var snoozeMinutes by remember { mutableIntStateOf(15) }
    var showDetails by remember { mutableStateOf(false) }

    // Ambient Breathing Halo Animation
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                SamsungAmbientTop,
                SamsungAmbientMid,
                SamsungAmbientBottom,
                SamsungAmbientMid,
                SamsungAmbientTop
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("samsung_reminder_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Breathing Ring with Bell Icon
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(haloPulse)
                        .clip(CircleShape)
                        .background(Color(0x33A670FA))
                )

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                        .border(1.5.dp, Color(0x60FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Reminder Title
            Text(
                text = title.ifBlank { "Scheduled reminder" },
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("reminder_title_text")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tapable "Details" Toggle
            Text(
                text = if (showDetails) "Hide details" else "Details",
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFD6C7F4),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .bounceClick { showDetails = !showDetails }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("toggle_details_btn")
            )

            // Expanded Details View
            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x38000000))
                        .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            fontSize = 15.sp,
                            color = Color(0xFFECE7F7),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Category: $category",
                            fontSize = 12.sp,
                            color = Color(0xFFC7BFE0)
                        )
                        Text(text = "•", fontSize = 12.sp, color = Color(0xFFC7BFE0))
                        Text(
                            text = "Priority: $priority",
                            fontSize = 12.sp,
                            color = Color(0xFFC7BFE0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Dismiss Button (Pill with bounceClick)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SamsungDismissButton)
                    .border(1.dp, SamsungDismissButtonBorder, RoundedCornerShape(28.dp))
                    .bounceClick { onDismiss() }
                    .testTag("dismiss_reminder_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Dismiss",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Complete Button (Vibrant Pill with bounceClick)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SamsungCompleteButton)
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(28.dp))
                    .bounceClick { onComplete() }
                    .testTag("complete_reminder_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Complete",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Controls: [-] Snooze X mins [+] with animated rolling ticker
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Decrement Button [-] with bounceClick
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SamsungSnoozePill.copy(alpha = 0.85f))
                    .border(0.8.dp, Color(0x30FFFFFF), CircleShape)
                    .bounceClick {
                        if (snoozeMinutes > 5) snoozeMinutes -= 5
                    }
                    .testTag("snooze_decrement_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease snooze time",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Central Snooze Action Pill with Animated Number Ticker
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SamsungSnoozePill)
                    .border(0.8.dp, Color(0x35FFFFFF), RoundedCornerShape(28.dp))
                    .bounceClick { onSnooze(snoozeMinutes) }
                    .testTag("snooze_action_pill"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Snooze ",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Rolling animated sliding digits
                    AnimatedNumberTicker(
                        number = snoozeMinutes,
                        suffix = " mins",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Increment Button [+] with bounceClick
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SamsungSnoozePill.copy(alpha = 0.85f))
                    .border(0.8.dp, Color(0x30FFFFFF), CircleShape)
                    .bounceClick {
                        if (snoozeMinutes < 60) snoozeMinutes += 5
                    }
                    .testTag("snooze_increment_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase snooze time",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
