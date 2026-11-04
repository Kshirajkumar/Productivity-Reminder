package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PriorityLevel(val label: String, val weight: Int) {
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1)
}

enum class TaskCategory(val label: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    HEALTH("Health"),
    STUDY("Study"),
    FINANCE("Finance"),
    OTHER("Other")
}

data class Subtask(
    val id: String,
    val title: String,
    val isDone: Boolean = false
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val details: String = "",
    val category: String = TaskCategory.PERSONAL.name,
    val priority: String = PriorityLevel.MEDIUM.name,
    val reminderTimeMillis: Long,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val snoozeCount: Int = 0,
    val subtasksJson: String = "", // serialized string of subtasks
    val isRecurringDaily: Boolean = false
) {
    fun getSubtaskList(): List<Subtask> {
        if (subtasksJson.isBlank()) return emptyList()
        return subtasksJson.split(";;").mapNotNull { part ->
            val segments = part.split("||")
            if (segments.size >= 3) {
                Subtask(id = segments[0], title = segments[1], isDone = segments[2].toBoolean())
            } else if (segments.size == 2) {
                Subtask(id = segments[0], title = segments[1], isDone = false)
            } else null
        }
    }

    companion object {
        fun encodeSubtasks(subtasks: List<Subtask>): String {
            return subtasks.joinToString(";;") { "${it.id}||${it.title}||${it.isDone}" }
        }
    }
}
