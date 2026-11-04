package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminder.TaskAlarmReceiver
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.bounceClick
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.reminder.ReminderAlertActivity
import com.example.ui.reminders.RemindersTimelineScreen
import com.example.ui.theme.ChronoTaskTheme
import com.example.ui.theme.AuraIrisPrimary
import com.example.ui.theme.AuraRoseUrgent
import com.example.ui.theme.ThemeController
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChronoTaskTheme(darkTheme = ThemeController.isDarkTheme) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

enum class NavTab(val index: Int, val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    DASHBOARD(0, "Reminder", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt),
    REMINDERS(1, "Scheduled", Icons.Filled.Alarm, Icons.Outlined.Alarm),
    ANALYTICS(2, "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart)
}

@Composable
fun MainAppContainer(viewModel: TaskViewModel) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklyAnalytics by viewModel.weeklyAnalytics.collectAsStateWithLifecycle()
    val inAppBanner by viewModel.inAppBanner.collectAsStateWithLifecycle()

    // Android 13+ Notification Permission Request
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Splash Screen Entrance
    if (showSplash) {
        AnimatedSplashScreen(onSplashFinished = { showSplash = false })
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                SlidingIndicatorNavigationBar(
                    selectedTab = selectedTab,
                    onSelectTab = {
                        if (selectedTab != it) {
                            com.example.ui.util.HapticHelper.tick(context)
                        }
                        selectedTab = it
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                // Directional Horizontal Sliding Page Transitions
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val isForward = targetState.index > initialState.index
                        if (isForward) {
                            (slideInHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { width -> width }
                            ) + fadeIn(animationSpec = tween(220))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        targetOffsetX = { width -> -width }
                                    ) + fadeOut(animationSpec = tween(180)))
                        } else {
                            (slideInHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { width -> -width }
                            ) + fadeIn(animationSpec = tween(220))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        targetOffsetX = { width -> width }
                                    ) + fadeOut(animationSpec = tween(180)))
                        }
                    },
                    label = "pageSlideTransition"
                ) { tab ->
                    when (tab) {
                        NavTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            onOpenReminderPreview = {
                                val intent = Intent(context, ReminderAlertActivity::class.java).apply {
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1L)
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, "Workout schedule")
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_DETAILS, "Scheduled workout routine at 3:43 pm")
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_CATEGORY, "HEALTH")
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_PRIORITY, "HIGH")
                                }
                                context.startActivity(intent)
                            }
                        )

                        NavTab.REMINDERS -> RemindersTimelineScreen(
                            viewModel = viewModel,
                            tasks = uiState.tasks,
                            onOpenLivePreview = {
                                val intent = Intent(context, ReminderAlertActivity::class.java).apply {
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1L)
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, "Monthly payment reminder")
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_DETAILS, "Review bills and monthly scheduled payments")
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_CATEGORY, "FINANCE")
                                    putExtra(TaskAlarmReceiver.EXTRA_TASK_PRIORITY, "MEDIUM")
                                }
                                context.startActivity(intent)
                            }
                        )

                        NavTab.ANALYTICS -> AnalyticsScreen(
                            analyticsData = weeklyAnalytics
                        )
                    }
                }

                // In-App Pop Banner
                InAppNotificationBanner(
                    bannerData = inAppBanner,
                    onDismiss = { viewModel.dismissInAppBanner() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Gentle Notification Permission Prompt if needed
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, AuraRoseUrgent, RoundedCornerShape(18.dp))
                            .bounceClick { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AuraRoseUrgent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Enable Notifications for lockscreen alerts. Tap here.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSplashScreen(onSplashFinished: () -> Unit) {
    val scaleAnim = remember { Animatable(0.5f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1.05f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        scaleAnim.animateTo(1.0f, animationSpec = tween(180))
        alphaAnim.animateTo(1f, animationSpec = tween(350))
        delay(900)
        alphaAnim.animateTo(0f, animationSpec = tween(250))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scaleAnim.value)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AuraIrisPrimary.copy(alpha = 0.2f))
                    .border(2.dp, AuraIrisPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "AuraTask Logo",
                    tint = AuraIrisPrimary,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "AuraTask",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Smart Offline Daily Reminders",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Bottom Navigation Bar featuring a continuous sliding pill indicator that
 * physically glides from tab to tab with smooth spring motion.
 */
@Composable
fun SlidingIndicatorNavigationBar(
    selectedTab: NavTab,
    onSelectTab: (NavTab) -> Unit
) {
    val tabs = NavTab.entries

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(31.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    0.8.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(31.dp)
                )
                .padding(4.dp)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / tabs.size

            // Smooth animated horizontal offset for the sliding indicator pill
            val animatedOffsetFraction by animateFloatAsState(
                targetValue = selectedTab.index.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "navIndicatorSlide"
            )

            // Sliding background capsule indicator
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .height(54.dp)
                    .offset(x = tabWidth * animatedOffsetFraction)
                    .clip(RoundedCornerShape(27.dp))
                    .background(AuraIrisPrimary)
            )

            // Interactive Tab Icons & Labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .bounceClick { onSelectTab(tab) }
                            .testTag("nav_tab_${tab.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
