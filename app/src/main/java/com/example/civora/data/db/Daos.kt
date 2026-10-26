package com.example.civora.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PoolCount(val subject: String, val topic: String, val difficulty: String, val count: Int)

data class Accuracy(val answered: Int, val correct: Int)

data class SubjectAccuracy(val subject: String, val answered: Int, val correct: Int)

data class TopicAccuracy(val subject: String, val topic: String, val answered: Int, val correct: Int)

@Dao
interface QuestionDao {
    @Insert
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    @Query("SELECT subject, topic, difficulty, COUNT(*) AS count FROM questions GROUP BY subject, topic, difficulty")
    fun poolCounts(): Flow<List<PoolCount>>

    @Query(
        """SELECT * FROM questions
        WHERE subject = :subject AND topic IN (:topics) AND difficulty IN (:difficulties)
        ORDER BY RANDOM() LIMIT :limit""",
    )
    suspend fun filtered(subject: String, topics: List<String>, difficulties: List<String>, limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun random(limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subject = :subject ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomBySubject(subject: String, limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Int>): List<QuestionEntity>

    /** Questions whose most recent attempt was wrong. */
    @Query(
        """SELECT q.* FROM questions q
        JOIN (SELECT questionId, MAX(id) AS lastId FROM attempts GROUP BY questionId) l ON l.questionId = q.id
        JOIN attempts a ON a.id = l.lastId
        WHERE a.isCorrect = 0
        ORDER BY a.answeredAt DESC""",
    )
    fun mistakes(): Flow<List<QuestionEntity>>

    @Query("SELECT q.* FROM questions q JOIN bookmarks b ON b.questionId = q.id ORDER BY b.createdAt DESC")
    fun bookmarked(): Flow<List<QuestionEntity>>
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE sessions SET finishedAt = :finishedAt, correct = :correct, durationMs = :durationMs WHERE id = :id")
    suspend fun finish(id: Long, finishedAt: Long, correct: Int, durationMs: Long)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun byId(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC")
    fun history(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Insert
    suspend fun insertAll(attempts: List<AttemptEntity>)

    @Query("SELECT * FROM attempts WHERE sessionId = :sessionId ORDER BY id")
    suspend fun forSession(sessionId: Long): List<AttemptEntity>

    @Query("SELECT COUNT(*) AS answered, COALESCE(SUM(isCorrect), 0) AS correct FROM attempts")
    fun overall(): Flow<Accuracy>

    @Query(
        """SELECT q.subject AS subject, COUNT(*) AS answered, SUM(a.isCorrect) AS correct
        FROM attempts a JOIN questions q ON q.id = a.questionId
        GROUP BY q.subject""",
    )
    fun bySubject(): Flow<List<SubjectAccuracy>>

    @Query(
        """SELECT q.subject AS subject, q.topic AS topic, COUNT(*) AS answered, SUM(a.isCorrect) AS correct
        FROM attempts a JOIN questions q ON q.id = a.questionId
        GROUP BY q.subject, q.topic""",
    )
    fun byTopic(): Flow<List<TopicAccuracy>>

    /** Local calendar days ("yyyy-MM-dd") with at least one attempt, newest first. */
    @Query("SELECT DISTINCT date(answeredAt / 1000, 'unixepoch', 'localtime') AS day FROM attempts ORDER BY day DESC")
    fun activeDays(): Flow<List<String>>

    @Query("DELETE FROM attempts")
    suspend fun deleteAll()
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun delete(questionId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    suspend fun isBookmarked(questionId: Int): Boolean

    @Query("SELECT questionId FROM bookmarks")
    fun ids(): Flow<List<Int>>

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
