package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.reminder.TaskAlarmScheduler

class ChronoTaskApp : Application() {

    companion object {
        const val REMINDER_CHANNEL_ID = "chronotask_high_priority_reminders"
        const val REMINDER_CHANNEL_NAME = "ChronoTask Reminders"
        const val NOTIFICATION_ID_BASE = 50000

        lateinit var instance: ChronoTaskApp
            private set
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val alarmScheduler: TaskAlarmScheduler by lazy { TaskAlarmScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent full-screen alerts and reminders for scheduled tasks"
                enableLights(true)
                lightColor = android.graphics.Color.MAGENTA
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
