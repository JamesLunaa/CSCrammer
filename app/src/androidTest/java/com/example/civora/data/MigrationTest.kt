package com.example.civora.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.civora.data.db.CivoraDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CivoraDatabase::class.java)

    @Test
    fun migrate1To2KeepsQuestionsAsBundled() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """INSERT INTO questions (id, subject, topic, difficulty, question, a, b, c, d, answer, explanation)
                VALUES (1, 'Verbal Ability', 'Grammar', 'Easy', 'Q?', 'a', 'b', 'c', 'd', 'A', 'e')""",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 2, true)
        db.query("SELECT id, isCustom FROM questions").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
            assertEquals(0, c.getInt(1))
        }
        db.close()
    }
}
