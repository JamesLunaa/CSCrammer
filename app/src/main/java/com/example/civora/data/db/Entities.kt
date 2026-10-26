package com.example.civora.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    indices = [Index("subject"), Index("topic"), Index("difficulty")],
)
data class QuestionEntity(
    @PrimaryKey val id: Int,
    val subject: String,
    val topic: String,
    val difficulty: String,
    val question: String,
    val a: String,
    val b: String,
    val c: String,
    val d: String,
    val answer: String,
    val explanation: String,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val filtersJson: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val total: Int,
    val correct: Int = 0,
    val durationMs: Long = 0,
)

// No foreign key to questions: a re-seed replaces that table and attempts must survive it.
@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("questionId")],
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val questionId: Int,
    /** "A".."D", or null when left unanswered (e.g. mock time-out). */
    val chosen: String?,
    val isCorrect: Boolean,
    val answeredAt: Long,
    val timeMs: Long,
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val questionId: Int,
    val createdAt: Long,
)
