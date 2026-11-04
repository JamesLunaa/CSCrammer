package com.example.civora.data.repo

import com.example.civora.data.db.BookmarkDao
import com.example.civora.data.db.BookmarkEntity
import com.example.civora.data.db.PoolCount
import com.example.civora.data.db.QuestionDao
import com.example.civora.data.db.QuestionEntity
import com.example.civora.data.seed.QuestionSeeder
import com.example.civora.domain.QuestionDraft
import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.Subject
import com.example.civora.domain.model.choiceIndex
import com.example.civora.domain.model.choiceLetter
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

    /** Questions the user wrote, newest first. */
    val custom: Flow<List<Question>> = questionDao.custom().map { list -> list.map { it.toModel() } }

    suspend fun question(id: Int): Question? = questionDao.byId(id)?.toModel()

    /** Saves [draft] (expected to be valid) as a new custom question, or over [id]. Returns the question id. */
    suspend fun saveCustom(draft: QuestionDraft, id: Int?): Int {
        require(id == null || id >= QuestionSeeder.CUSTOM_ID_START) { "Bundled question $id can't be edited" }
        val d = draft.trimmed()
        val questionId = id ?: questionDao.nextIdFrom(QuestionSeeder.CUSTOM_ID_START)
        questionDao.upsert(
            QuestionEntity(
                id = questionId,
                subject = d.subject.label,
                topic = d.topic,
                difficulty = d.difficulty.name,
                question = d.text,
                a = d.choices[0],
                b = d.choices[1],
                c = d.choices[2],
                d = d.choices[3],
                answer = choiceLetter(requireNotNull(d.answerIndex)),
                explanation = d.explanation,
                isCustom = true,
            ),
        )
        return questionId
    }

    /** Past attempts stay in history; stats join on questions, so they stop counting. */
    suspend fun deleteCustom(id: Int) {
        questionDao.deleteCustom(id)
        bookmarkDao.delete(id)
    }

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
