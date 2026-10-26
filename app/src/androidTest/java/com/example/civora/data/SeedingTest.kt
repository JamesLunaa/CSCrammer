package com.example.civora.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.civora.data.db.AttemptEntity
import com.example.civora.data.db.BookmarkEntity
import com.example.civora.data.db.CivoraDatabase
import com.example.civora.data.db.SessionEntity
import com.example.civora.data.prefs.AppPreferences
import com.example.civora.data.seed.QuestionSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SeedingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: CivoraDatabase
    private lateinit var prefs: AppPreferences
    private lateinit var prefsFile: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, CivoraDatabase::class.java).build()
        prefsFile = File(context.cacheDir, "seed_test_${System.nanoTime()}.preferences_pb")
        prefs = AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { prefsFile })
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        prefsFile.delete()
    }

    private fun seeder() = QuestionSeeder(db, prefs) { context.assets.open("questions.csv") }

    @Test
    fun freshInstallSeedsAllQuestions() = runBlocking {
        seeder().ensureSeeded()
        assertEquals(2500, db.questionDao().count())
        assertEquals(QuestionSeeder.DATA_VERSION, prefs.dataVersion())
    }

    @Test
    fun reseedKeepsProgress() = runBlocking {
        seeder().ensureSeeded()
        val sessionId = db.sessionDao().insert(SessionEntity(mode = "quick", filtersJson = "{}", startedAt = 0, total = 1))
        db.attemptDao().insert(AttemptEntity(sessionId = sessionId, questionId = 1, chosen = "A", isCorrect = false, answeredAt = 0, timeMs = 0))
        db.bookmarkDao().insert(BookmarkEntity(questionId = 2, createdAt = 0))

        prefs.setDataVersion(0) // simulate an older bundled data version
        seeder().ensureSeeded()

        assertEquals(2500, db.questionDao().count())
        assertEquals(1, db.attemptDao().forSession(sessionId).size)
        assertEquals(listOf(2), db.bookmarkDao().ids().first())
        assertEquals(listOf(1), db.questionDao().mistakes().first().map { it.id })
    }
}
