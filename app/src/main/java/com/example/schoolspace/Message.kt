package com.example.schoolspace

import com.google.firebase.Timestamp

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
    @field:JvmField val isRead: Boolean = false
)
