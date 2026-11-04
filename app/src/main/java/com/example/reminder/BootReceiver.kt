package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ChronoTaskApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Device rebooted. Rescheduling future task alarms...")
            CoroutineScope(Dispatchers.IO).launch {
                val repository = ChronoTaskApp.instance.repository
                val scheduler = ChronoTaskApp.instance.alarmScheduler
                val futureTasks = repository.getPendingFutureReminders()
                futureTasks.forEach { task ->
                    scheduler.scheduleReminder(task)
                }
                Log.d("BootReceiver", "Rescheduled ${futureTasks.size} alarms.")
            }
        }
    }
}
