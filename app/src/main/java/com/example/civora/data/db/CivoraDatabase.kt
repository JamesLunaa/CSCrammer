package com.example.civora.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QuestionEntity::class, SessionEntity::class, AttemptEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CivoraDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun sessionDao(): SessionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun create(context: Context): CivoraDatabase =
            Room.databaseBuilder(context, CivoraDatabase::class.java, "civora.db").build()
    }
}
