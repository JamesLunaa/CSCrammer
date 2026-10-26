package com.example.civora.ui.quiz

import android.net.Uri
import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Subject
import org.json.JSONObject

/** Where a feedback-per-answer quiz gets its questions from. */
sealed interface QuizSource {
    val mode: String
    val filtersJson: String get() = "{}"

    data object Quick : QuizSource {
        override val mode = "quick"
    }

    /** Re-drill questions whose latest attempt is wrong. */
    data object Mistakes : QuizSource {
        override val mode = "mistakes"
    }

    data object Bookmarks : QuizSource {
        override val mode = "bookmarks"
    }

    /** [count] of 0 means every matching question. */
    data class Practice(
        val subject: Subject,
        val topics: List<String>,
        val difficulties: List<Difficulty>,
        val count: Int,
    ) : QuizSource {
        override val mode = "practice"
        override val filtersJson: String
            get() = JSONObject()
                .put("subject", subject.name)
                .put("topics", topics.joinToString("|"))
                .put("difficulties", difficulties.joinToString(",") { it.name })
                .put("count", count)
                .toString()
    }

    companion object {
        const val ROUTE = "quiz/{mode}?subject={subject}&topics={topics}&difficulties={difficulties}&count={count}"

        fun route(source: QuizSource): String = when (source) {
            Quick -> "quiz/quick"
            Mistakes -> "quiz/mistakes"
            Bookmarks -> "quiz/bookmarks"
            is Practice -> "quiz/practice" +
                "?subject=${source.subject.name}" +
                "&topics=${Uri.encode(source.topics.joinToString("|"))}" +
                "&difficulties=${source.difficulties.joinToString(",") { it.name }}" +
                "&count=${source.count}"
        }

        fun fromArgs(mode: String?, subject: String?, topics: String?, difficulties: String?, count: Int): QuizSource? =
            when (mode) {
                Quick.mode -> Quick
                Mistakes.mode -> Mistakes
                Bookmarks.mode -> Bookmarks
                "practice" -> {
                    val s = Subject.entries.firstOrNull { it.name == subject }
                    if (s == null) null else Practice(
                        subject = s,
                        topics = topics.orEmpty().split("|").filter { it.isNotEmpty() },
                        difficulties = difficulties.orEmpty().split(",").mapNotNull { n ->
                            Difficulty.entries.firstOrNull { it.name == n }
                        },
                        count = count,
                    )
                }
                else -> null
            }
    }
}
