package com.example.schoolspace

import com.google.firebase.Timestamp
import java.util.Date

data class Lesson(
    val day: String = "",
    val time: String = "",
    val subject: String = "",
    val room: String = "",
    val teacher: String = ""
)

data class ScheduleChange(
    val date: String = "",
    val time: String = "",
    val newSubject: String = "",
    val isCancelled: Boolean = false
)

data class Reservation(
    val room: String = "",
    val topic: String = "",
    val date: String = "",
    val time: String = "",
    val timestamp: Date = Date(),
    val uid: String = ""
)

data class Grade(
    val id: String = "",
    val subject: String = "",
    val value: String = "",
    val description: String = "",
    val weight: Int = 1,
    val date: Timestamp = Timestamp.now()
)

data class Message(
    val id: String = "",
    val senderUid: String = "",
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val subject: String = "",
    val body: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    @field:JvmField val isRead: Boolean = false,
    @field:JvmField val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null
)
