package com.example.civora.domain.model

enum class Subject(val label: String, val shortLabel: String) {
    Verbal("Verbal Ability", "Verbal"),
    Numerical("Numerical Ability", "Numerical"),
    Analytical("Analytical Ability", "Analytical"),
    GeneralInfo("General Information", "Gen Info"),
    Constitution("Philippine Constitution and Public Service Ethics", "Constitution & Ethics");

    companion object {
        fun fromLabel(label: String): Subject =
            entries.firstOrNull { it.label == label } ?: error("Unknown subject: $label")
    }
}

enum class Difficulty {
    Easy, Medium, Hard;

    companion object {
        fun fromLabel(label: String): Difficulty =
            entries.firstOrNull { it.name == label } ?: error("Unknown difficulty: $label")
    }
}

data class Question(
    val id: Int,
    val subject: Subject,
    val topic: String,
    val difficulty: Difficulty,
    val text: String,
    val choices: List<String>,
    val answerIndex: Int,
    val explanation: String,
)

/** Letter used in the CSV and stored in attempts: 0 -> "A" .. 3 -> "D". */
fun choiceLetter(index: Int): String = ('A' + index).toString()

fun choiceIndex(letter: String): Int = letter.single() - 'A'
