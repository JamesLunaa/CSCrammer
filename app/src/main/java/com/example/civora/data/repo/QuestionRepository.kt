package com.example.civora.data.repo

import com.example.civora.data.db.BookmarkDao
import com.example.civora.data.db.BookmarkEntity
import com.example.civora.data.db.PoolCount
import com.example.civora.data.db.QuestionDao
import com.example.civora.data.db.QuestionEntity
import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.Subject
import com.example.civora.domain.model.choiceIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuestionRepository(
    private val questionDao: QuestionDao,
    private val bookmarkDao: BookmarkDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Question counts per subject/topic/difficulty, for the practice setup screen. */
    val poolCounts: Flow<List<PoolCount>> = questionDao.poolCounts()

    val mistakes: Flow<List<Question>> = questionDao.mistakes().map { list -> list.map { it.toModel() } }

    val bookmarks: Flow<List<Question>> = questionDao.bookmarked().map { list -> list.map { it.toModel() } }

    val bookmarkIds: Flow<Set<Int>> = bookmarkDao.ids().map { it.toSet() }

    suspend fun practice(subject: Subject, topics: List<String>, difficulties: List<Difficulty>, limit: Int): List<Question> =
        questionDao.filtered(subject.label, topics, difficulties.map { it.name }, limit).map { it.toModel() }

    suspend fun random(limit: Int): List<Question> = questionDao.random(limit).map { it.toModel() }

    suspend fun randomBySubject(subject: Subject, limit: Int): List<Question> =
        questionDao.randomBySubject(subject.label, limit).map { it.toModel() }

    /** Returns questions in the order of [ids]; ids no longer in the bank are dropped. */
    suspend fun byIds(ids: List<Int>): List<Question> {
        val found = questionDao.byIds(ids).associateBy { it.id }
        return ids.mapNotNull { found[it]?.toModel() }
    }

    suspend fun isBookmarked(questionId: Int): Boolean = bookmarkDao.isBookmarked(questionId)

    /** Returns the new bookmarked state. */
    suspend fun toggleBookmark(questionId: Int): Boolean =
        if (bookmarkDao.isBookmarked(questionId)) {
            bookmarkDao.delete(questionId)
            false
        } else {
            bookmarkDao.insert(BookmarkEntity(questionId, now()))
            true
        }
}

fun QuestionEntity.toModel() = Question(
    id = id,
    subject = Subject.fromLabel(subject),
    topic = topic,
    difficulty = Difficulty.fromLabel(difficulty),
    text = question,
    choices = listOf(a, b, c, d),
    answerIndex = choiceIndex(answer),
    explanation = explanation,
)
