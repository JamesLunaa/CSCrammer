package com.example.civora.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.civora.domain.model.Subject

fun Subject.category(): Category = when (this) {
    Subject.Verbal -> Category.Sky
    Subject.Numerical -> Category.Mustard
    Subject.Analytical -> Category.Teal
    Subject.GeneralInfo -> Category.Tan
    Subject.Constitution -> Category.Orchid
}

@Composable
fun subjectColor(subject: Subject): Color = categoryColor(subject.category())
