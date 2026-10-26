package com.example.civora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Roles Material 3 has no slot for. */
@Immutable
data class CivoraColors(
    val field: Color,
    val onField: Color,
    val fieldLilac: Color,
    val correct: Color,
    val wrong: Color,
    val fieldCorrect: Color,
    val fieldWrong: Color,
    val warning: Color,
    val isDark: Boolean,
)

private val DarkExtras = CivoraColors(
    field = UbeField, onField = PriceCardInk, fieldLilac = FieldLilac,
    correct = IncomeGreen, wrong = ExpenseCoral,
    fieldCorrect = IncomeGreen, fieldWrong = ExpenseCoral,
    warning = SodiumAmber, isDark = true,
)

private val DayExtras = CivoraColors(
    field = DayField, onField = DayOnField, fieldLilac = DayFieldLilac,
    correct = DayIncome, wrong = DayExpense,
    fieldCorrect = DayFieldIncome, fieldWrong = DayFieldExpense,
    warning = DayAmber, isDark = false,
)

private val DarkScheme = darkColorScheme(
    primary = UbeLamp, onPrimary = OnLamp,
    primaryContainer = LampContainer, onPrimaryContainer = OnLampContainer,
    secondary = OperatorPlum, onSecondary = OnOperator,
    secondaryContainer = OperatorPlum, onSecondaryContainer = OnOperator,
    tertiary = SodiumAmber, onTertiary = OnLamp,
    background = NightGround, onBackground = PriceCardInk,
    surface = Stall, onSurface = PriceCardInk, onSurfaceVariant = DuskInk,
    surfaceDim = StallShadow, surfaceBright = StallHighest,
    surfaceContainerLowest = StallShadow, surfaceContainerLow = StallLow,
    surfaceContainer = Stall, surfaceContainerHigh = StallHigh,
    surfaceContainerHighest = StallHighest,
    error = ExpenseCoral, onError = OnLamp,
    outline = OutlineDusk, outlineVariant = OutlineFaint,
)

private val DayScheme = lightColorScheme(
    primary = DayLamp, onPrimary = Color.White,
    primaryContainer = DayLampContainer, onPrimaryContainer = LampContainer,
    secondary = DayOperator, onSecondary = DayInk,
    secondaryContainer = DayOperator, onSecondaryContainer = DayInk,
    tertiary = DayAmber, onTertiary = Color.White,
    background = DayGround, onBackground = DayInk,
    surface = DayGround, onSurface = DayInk, onSurfaceVariant = DayMutedInk,
    surfaceContainerLowest = Color.White, surfaceContainerLow = DayStallLow,
    surfaceContainer = DayStallLow, surfaceContainerHigh = DayStallHigh,
    surfaceContainerHighest = DayStallHighest,
    error = DayExpense, onError = Color.White,
    outline = DayMutedInk, outlineVariant = DayStallHighest,
)

enum class ThemeMode { Dark, Day, System }

val LocalCivoraColors = staticCompositionLocalOf { DarkExtras }

object CivoraTheme {
    val colors: CivoraColors
        @Composable get() = LocalCivoraColors.current
}

@Composable
fun categoryColor(category: Category): Color =
    if (CivoraTheme.colors.isDark) category.dark else category.day

@Composable
fun CivoraTheme(
    mode: ThemeMode = ThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.Dark -> true
        ThemeMode.Day -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalCivoraColors provides if (dark) DarkExtras else DayExtras) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else DayScheme,
            typography = CivoraTypography,
            content = content,
        )
    }
}
