package com.example.civora.data.seed

import androidx.room.withTransaction
import com.example.civora.data.db.CivoraDatabase
import com.example.civora.data.db.QuestionEntity
import com.example.civora.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Loads the bundled CSV into Room. Runs on first launch and whenever [DATA_VERSION] is bumped.
 * Only bundled questions are replaced; custom questions, attempts, sessions and bookmarks survive.
 */
class QuestionSeeder(
    private val db: CivoraDatabase,
    private val prefs: AppPreferences,
    private val openCsv: () -> InputStream,
) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val dao = db.questionDao()
            if (prefs.dataVersion() >= DATA_VERSION && dao.bundledCount() > 0) return@withContext
            val questions = openCsv().reader(Charsets.UTF_8).use { toEntities(CsvParser.parse(it)) }
            db.withTransaction {
                dao.deleteBundled()
                dao.insertAll(questions)
            }
            prefs.setDataVersion(DATA_VERSION)
        }
    }

    companion object {
        /** Bump when `assets/questions.csv` changes. */
        const val DATA_VERSION = 1

        /** Custom questions get ids from here up. Bundled ids must stay below it. */
        const val CUSTOM_ID_START = 1_000_000

        private val COLUMNS = listOf(
            "id", "subject", "topic", "difficulty", "question",
            "choice_a", "choice_b", "choice_c", "choice_d", "answer", "explanation",
        )

        fun toEntities(rows: List<List<String>>): List<QuestionEntity> {
            val header = rows.first().map { it.trim() }
            val col = COLUMNS.associateWith { name ->
                header.indexOf(name).also { require(it >= 0) { "Missing column: $name" } }
            }
            return rows.drop(1).map { r ->
                require(r.size == header.size) { "Row ${r.firstOrNull()} has ${r.size} fields, expected ${header.size}" }
                fun v(name: String) = r[col.getValue(name)].trim()
                QuestionEntity(
                    id = v("id").toInt(),
                    subject = v("subject"),
                    topic = v("topic"),
                    difficulty = v("difficulty"),
                    question = v("question"),
                    a = v("choice_a"),
                    b = v("choice_b"),
                    c = v("choice_c"),
                    d = v("choice_d"),
                    answer = v("answer").uppercase(),
                    explanation = v("explanation"),
                )
            }
        }
    }
}
