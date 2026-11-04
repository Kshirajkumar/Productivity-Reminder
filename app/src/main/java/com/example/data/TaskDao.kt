package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, reminderTimeMillis ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY reminderTimeMillis ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderTimeMillis >= :nowMillis ORDER BY reminderTimeMillis ASC")
    fun getUpcomingReminders(nowMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET reminderTimeMillis = :newReminderTime, snoozeCount = snoozeCount + 1 WHERE id = :id")
    suspend fun snoozeTask(id: Long, newReminderTime: Long)

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAtMillis >= :startMillis AND completedAtMillis <= :endMillis")
    suspend fun getCompletedTasksBetween(startMillis: Long, endMillis: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderTimeMillis > :nowMillis")
    suspend fun getPendingFutureReminders(nowMillis: Long): List<TaskEntity>
}
