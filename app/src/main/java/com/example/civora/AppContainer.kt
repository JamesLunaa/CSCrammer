package com.example.civora

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.civora.data.db.CivoraDatabase
import com.example.civora.data.prefs.AppPreferences
import com.example.civora.data.repo.ProgressRepository
import com.example.civora.data.repo.QuestionRepository
import com.example.civora.data.seed.QuestionSeeder

private val Context.dataStore by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: CivoraDatabase = CivoraDatabase.create(appContext)
    val preferences = AppPreferences(appContext.dataStore)
    val seeder = QuestionSeeder(database, preferences) { appContext.assets.open("questions.csv") }
    val questions = QuestionRepository(database.questionDao(), database.bookmarkDao())
    val progress = ProgressRepository(database)
}
