package com.jayathu.minstagram.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val intention: String,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val wasIntercepted: Boolean,
    val endReason: String,
    val reelsWatched: Int = 0
)
