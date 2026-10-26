package com.example.civora.data.repo

import androidx.room.withTransaction
import com.example.civora.data.db.Accuracy
import com.example.civora.data.db.AttemptEntity
import com.example.civora.data.db.CivoraDatabase
import com.example.civora.data.db.SessionEntity
import com.example.civora.data.db.SubjectAccuracy
import com.example.civora.data.db.TopicAccuracy
import kotlinx.coroutines.flow.Flow

class ProgressRepository(
    private val db: CivoraDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val sessionDao = db.sessionDao()
    private val attemptDao = db.attemptDao()

    val history: Flow<List<SessionEntity>> = sessionDao.history()
    val overall: Flow<Accuracy> = attemptDao.overall()
    val bySubject: Flow<List<SubjectAccuracy>> = attemptDao.bySubject()
    val byTopic: Flow<List<TopicAccuracy>> = attemptDao.byTopic()
    val activeDays: Flow<List<String>> = attemptDao.activeDays()

    suspend fun startSession(mode: String, filtersJson: String, total: Int): Long =
        sessionDao.insert(SessionEntity(mode = mode, filtersJson = filtersJson, startedAt = now(), total = total))

    suspend fun recordAttempt(sessionId: Long, questionId: Int, chosen: String?, isCorrect: Boolean, timeMs: Long) {
        attemptDao.insert(
            AttemptEntity(
                sessionId = sessionId,
                questionId = questionId,
                chosen = chosen,
                isCorrect = isCorrect,
                answeredAt = now(),
                timeMs = timeMs,
            ),
        )
    }

    /** Mock exams record all answers at submit time in one go. */
    suspend fun recordAttempts(attempts: List<AttemptEntity>) = attemptDao.insertAll(attempts)

    suspend fun finishSession(sessionId: Long, correct: Int, durationMs: Long) =
        sessionDao.finish(sessionId, now(), correct, durationMs)

    suspend fun session(sessionId: Long): SessionEntity? = sessionDao.byId(sessionId)

    suspend fun attempts(sessionId: Long): List<AttemptEntity> = attemptDao.forSession(sessionId)

    /** Clears attempts, sessions and bookmarks. The question bank stays. */
    suspend fun resetProgress() = db.withTransaction {
        attemptDao.deleteAll()
        sessionDao.deleteAll()
        db.bookmarkDao().deleteAll()
    }
}
